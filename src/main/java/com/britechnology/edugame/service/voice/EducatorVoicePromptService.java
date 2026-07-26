package com.britechnology.edugame.service.voice;

import com.britechnology.edugame.dto.voice.CreateVoicePromptRequest;
import com.britechnology.edugame.dto.voice.UpdateVoicePromptRequest;
import com.britechnology.edugame.dto.voice.VoicePromptDTO;
import com.britechnology.edugame.entity.*;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.user.UserRepository;
import com.britechnology.edugame.repository.voice.VoicePromptRepository;
import com.britechnology.edugame.repository.voice.VoiceSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducatorVoicePromptService {

    private final VoicePromptRepository voicePromptRepository;
    private final VoiceSeriesRepository voiceSeriesRepository;
    private final UserRepository userRepository;

    public List<VoicePromptDTO> listBySeries(Long seriesId, String educatorEmail) {
        VoiceSeries series = loadOwnedSeries(seriesId, educatorEmail);
        return voicePromptRepository.findBySeriesIdOrderByOrdreAscIdAsc(series.getId()).stream()
                .map(EducatorVoiceSeriesService::toPromptDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public VoicePromptDTO create(CreateVoicePromptRequest request, String educatorEmail) {
        if (request == null || request.getSeriesId() == null) {
            throw ApiException.badRequest("seriesId est requis");
        }
        VoiceSeries series = loadOwnedSeries(request.getSeriesId(), educatorEmail);
        VoiceContentValidator.requireDraft(series);
        VoiceContentValidator.validatePromptText(request.getTexteReference());

        int ordre = request.getOrdre() != null
                ? request.getOrdre()
                : (int) voicePromptRepository.countBySeriesId(series.getId());

        VoicePrompt prompt = VoicePrompt.builder()
                .series(series)
                .ordre(ordre)
                .texteReference(request.getTexteReference().trim())
                .sousType(VoicePromptSousType.fromCode(request.getSousType()))
                .tolerance(VoiceTolerance.fromCode(request.getTolerance()))
                .indice(trimToNull(request.getIndice()))
                .dureeMaxSecondes(request.getDureeMaxSecondes() != null ? request.getDureeMaxSecondes() : 30)
                .build();
        prompt = voicePromptRepository.save(prompt);
        return EducatorVoiceSeriesService.toPromptDto(prompt);
    }

    @Transactional
    public VoicePromptDTO update(Long id, UpdateVoicePromptRequest request, String educatorEmail) {
        VoicePrompt prompt = voicePromptRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Consigne introuvable"));
        VoiceSeries series = prompt.getSeries();
        if (series == null) {
            throw ApiException.badRequest("Consigne sans série");
        }
        loadOwnedSeries(series.getId(), educatorEmail);
        VoiceContentValidator.requireDraft(series);

        if (request.getTexteReference() != null) {
            VoiceContentValidator.validatePromptText(request.getTexteReference());
            prompt.setTexteReference(request.getTexteReference().trim());
        }
        if (request.getOrdre() != null) prompt.setOrdre(request.getOrdre());
        if (request.getSousType() != null) prompt.setSousType(VoicePromptSousType.fromCode(request.getSousType()));
        if (request.getTolerance() != null) prompt.setTolerance(VoiceTolerance.fromCode(request.getTolerance()));
        if (request.getIndice() != null) prompt.setIndice(trimToNull(request.getIndice()));
        if (request.getDureeMaxSecondes() != null) prompt.setDureeMaxSecondes(request.getDureeMaxSecondes());

        prompt = voicePromptRepository.save(prompt);
        return EducatorVoiceSeriesService.toPromptDto(prompt);
    }

    @Transactional
    public void delete(Long id, String educatorEmail) {
        VoicePrompt prompt = voicePromptRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Consigne introuvable"));
        VoiceSeries series = prompt.getSeries();
        if (series == null) {
            throw ApiException.badRequest("Consigne sans série");
        }
        loadOwnedSeries(series.getId(), educatorEmail);
        VoiceContentValidator.requireDraft(series);
        voicePromptRepository.delete(prompt);
    }

    private VoiceSeries loadOwnedSeries(Long seriesId, String educatorEmail) {
        User educateur = userRepository.findByEmail(educatorEmail)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        VoiceSeries series = voiceSeriesRepository.findById(seriesId)
                .orElseThrow(() -> ApiException.notFound("Série introuvable"));
        if (series.getEducateur() == null || !series.getEducateur().getId().equals(educateur.getId())) {
            throw ApiException.badRequest("Cette série ne vous appartient pas");
        }
        return series;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
