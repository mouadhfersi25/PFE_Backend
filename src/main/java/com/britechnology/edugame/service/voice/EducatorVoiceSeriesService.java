package com.britechnology.edugame.service.voice;

import com.britechnology.edugame.dto.voice.*;
import com.britechnology.edugame.entity.*;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.user.UserRepository;
import com.britechnology.edugame.repository.voice.SessionOralRepository;
import com.britechnology.edugame.repository.voice.VoiceAttemptRepository;
import com.britechnology.edugame.repository.voice.VoicePromptRepository;
import com.britechnology.edugame.repository.voice.VoiceSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducatorVoiceSeriesService {

    private final VoiceSeriesRepository voiceSeriesRepository;
    private final VoicePromptRepository voicePromptRepository;
    private final SessionOralRepository sessionOralRepository;
    private final VoiceAttemptRepository voiceAttemptRepository;
    private final UserRepository userRepository;

    public List<VoiceSeriesDTO> listByEducator(String educatorEmail) {
        User educateur = resolveEducator(educatorEmail);
        return voiceSeriesRepository.findByEducateurIdOrderByUpdatedAtDesc(educateur.getId()).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    public VoiceSeriesDTO findById(Long id, String educatorEmail) {
        VoiceSeries series = loadOwnedSeries(id, educatorEmail);
        return toDetailDto(series);
    }

    @Transactional
    public VoiceSeriesDTO create(CreateVoiceSeriesRequest request, String educatorEmail) {
        User educateur = resolveEducator(educatorEmail);
        if (request == null || request.getTitre() == null || request.getTitre().isBlank()) {
            throw ApiException.badRequest("Le titre est requis");
        }
        VoiceSeries series = VoiceSeries.builder()
                .titre(request.getTitre().trim())
                .description(trimToNull(request.getDescription()))
                .langue(VoiceContentValidator.normalizeLangue(request.getLangue()))
                .difficulte(request.getDifficulte())
                .etat(EtatVoiceSeries.BROUILLON)
                .educateur(educateur)
                .build();
        series = voiceSeriesRepository.save(series);
        return toDetailDto(series);
    }

    @Transactional
    public VoiceSeriesDTO update(Long id, UpdateVoiceSeriesRequest request, String educatorEmail) {
        VoiceSeries series = loadOwnedSeries(id, educatorEmail);
        VoiceContentValidator.requireDraft(series);
        if (request.getTitre() != null) {
            if (request.getTitre().isBlank()) {
                throw ApiException.badRequest("Le titre ne peut pas être vide");
            }
            series.setTitre(request.getTitre().trim());
        }
        if (request.getDescription() != null) {
            series.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getLangue() != null) {
            series.setLangue(VoiceContentValidator.normalizeLangue(request.getLangue()));
        }
        if (request.getDifficulte() != null) {
            series.setDifficulte(request.getDifficulte());
        }
        series = voiceSeriesRepository.save(series);
        return toDetailDto(series);
    }

    @Transactional
    public void delete(Long id, String educatorEmail) {
        VoiceSeries series = loadOwnedSeries(id, educatorEmail);
        Long seriesId = series.getId();
        // Cascade manuelle : sessions / tentatives bloquent sinon la FK.
        voiceAttemptRepository.deleteByPromptSeriesId(seriesId);
        voiceAttemptRepository.deleteBySessionOralSeriesId(seriesId);
        sessionOralRepository.deleteBySeriesId(seriesId);
        voicePromptRepository.deleteBySeriesId(seriesId);
        voiceSeriesRepository.delete(series);
    }

    @Transactional
    public VoiceSeriesDTO publish(Long id, String educatorEmail) {
        VoiceSeries series = loadOwnedSeries(id, educatorEmail);
        VoiceContentValidator.requireDraft(series);
        long count = voicePromptRepository.countBySeriesId(series.getId());
        VoiceContentValidator.validateSeriesForPublish(series, count);
        series.setEtat(EtatVoiceSeries.PUBLIE);
        series.setPublishedAt(LocalDateTime.now());
        series = voiceSeriesRepository.save(series);
        return toDetailDto(series);
    }

    @Transactional
    public VoiceSeriesDTO archive(Long id, String educatorEmail) {
        VoiceSeries series = loadOwnedSeries(id, educatorEmail);
        series.setEtat(EtatVoiceSeries.ARCHIVE);
        series = voiceSeriesRepository.save(series);
        return toDetailDto(series);
    }

    private VoiceSeries loadOwnedSeries(Long id, String educatorEmail) {
        User educateur = resolveEducator(educatorEmail);
        VoiceSeries series = voiceSeriesRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Série introuvable"));
        if (series.getEducateur() == null || !series.getEducateur().getId().equals(educateur.getId())) {
            throw ApiException.badRequest("Cette série ne vous appartient pas");
        }
        return series;
    }

    private User resolveEducator(String educatorEmail) {
        if (educatorEmail == null || educatorEmail.isBlank()) {
            throw ApiException.badRequest("Éducateur non authentifié");
        }
        return userRepository.findByEmail(educatorEmail)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }

    private VoiceSeriesDTO toSummaryDto(VoiceSeries series) {
        return VoiceSeriesDTO.builder()
                .id(series.getId())
                .titre(series.getTitre())
                .description(series.getDescription())
                .langue(series.getLangue())
                .difficulte(series.getDifficulte())
                .etat(series.getEtat().name())
                .educateurId(series.getEducateur() != null ? series.getEducateur().getId() : null)
                .promptsCount((int) voicePromptRepository.countBySeriesId(series.getId()))
                .createdAt(series.getCreatedAt())
                .updatedAt(series.getUpdatedAt())
                .publishedAt(series.getPublishedAt())
                .build();
    }

    private VoiceSeriesDTO toDetailDto(VoiceSeries series) {
        VoiceSeriesDTO dto = toSummaryDto(series);
        dto.setEducateurNom(formatUserName(series.getEducateur()));
        dto.setPrompts(voicePromptRepository.findBySeriesIdOrderByOrdreAscIdAsc(series.getId()).stream()
                .map(EducatorVoiceSeriesService::toPromptDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private static String formatUserName(User user) {
        if (user == null) return null;
        String full = ((user.getPrenom() != null ? user.getPrenom() : "") + " " + (user.getNom() != null ? user.getNom() : "")).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }

    static VoicePromptDTO toPromptDto(VoicePrompt prompt) {
        return VoicePromptDTO.builder()
                .id(prompt.getId())
                .seriesId(prompt.getSeries() != null ? prompt.getSeries().getId() : null)
                .ordre(prompt.getOrdre())
                .texteReference(prompt.getTexteReference())
                .sousType(prompt.getSousType() != null ? prompt.getSousType().name() : VoicePromptSousType.READ_ALOUD.name())
                .tolerance(prompt.getTolerance() != null ? prompt.getTolerance().name() : VoiceTolerance.NORMAL.name())
                .indice(prompt.getIndice())
                .dureeMaxSecondes(prompt.getDureeMaxSecondes())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
