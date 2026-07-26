package com.britechnology.edugame.service.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.britechnology.edugame.dto.voice.*;
import com.britechnology.edugame.entity.*;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.user.UserRepository;
import com.britechnology.edugame.repository.voice.SessionOralRepository;
import com.britechnology.edugame.repository.voice.VoiceAttemptRepository;
import com.britechnology.edugame.repository.voice.VoicePromptRepository;
import com.britechnology.edugame.repository.voice.VoiceSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerVoiceService {

    private static final int MAX_SCORE_ORAL = 250;
    private static final int SUCCESS_THRESHOLD_PERCENT = 70;

    private final VoiceSeriesRepository voiceSeriesRepository;
    private final VoicePromptRepository voicePromptRepository;
    private final SessionOralRepository sessionOralRepository;
    private final VoiceAttemptRepository voiceAttemptRepository;
    private final UserRepository userRepository;
    private final VoiceTranscriptionService voiceTranscriptionService;
    private final VoiceTextMatcherService voiceTextMatcherService;
    private final PlayerProgressionService playerProgressionService;
    private final ObjectMapper objectMapper;

    @Value("${voice.scoring.rules-version:voice-v1}")
    private String scoringRulesVersion;

    public List<VoiceSeriesDTO> listPublishedSeries() {
        return voiceSeriesRepository.findByEtatOrderByPublishedAtDesc(EtatVoiceSeries.PUBLIE).stream()
                .map(series -> VoiceSeriesDTO.builder()
                        .id(series.getId())
                        .titre(series.getTitre())
                        .description(series.getDescription())
                        .langue(series.getLangue())
                        .difficulte(series.getDifficulte())
                        .etat(series.getEtat().name())
                        .promptsCount((int) voicePromptRepository.countBySeriesId(series.getId()))
                        .publishedAt(series.getPublishedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public VoiceSeriesDTO getPublishedSeries(Long seriesId) {
        VoiceSeries series = voiceSeriesRepository.findById(seriesId)
                .orElseThrow(() -> ApiException.notFound("Série introuvable"));
        if (series.getEtat() != EtatVoiceSeries.PUBLIE) {
            throw ApiException.badRequest("Cette série n'est pas disponible");
        }
        return VoiceSeriesDTO.builder()
                .id(series.getId())
                .titre(series.getTitre())
                .description(series.getDescription())
                .langue(series.getLangue())
                .difficulte(series.getDifficulte())
                .etat(series.getEtat().name())
                .promptsCount((int) voicePromptRepository.countBySeriesId(series.getId()))
                .prompts(voicePromptRepository.findBySeriesIdOrderByOrdreAscIdAsc(series.getId()).stream()
                        .map(EducatorVoiceSeriesService::toPromptDto)
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public StartVoiceSessionResponse startSession(Long seriesId, String playerEmail) {
        User player = resolvePlayer(playerEmail);
        VoiceSeries series = voiceSeriesRepository.findById(seriesId)
                .orElseThrow(() -> ApiException.notFound("Série introuvable"));
        if (series.getEtat() != EtatVoiceSeries.PUBLIE) {
            throw ApiException.badRequest("Cette série n'est pas publiée");
        }
        int promptsTotal = (int) voicePromptRepository.countBySeriesId(series.getId());
        if (promptsTotal < 1) {
            throw ApiException.badRequest("Cette série n'a pas encore de consignes");
        }

        SessionOral session = SessionOral.builder()
                .utilisateur(player)
                .series(series)
                .dateDebut(LocalDateTime.now())
                .promptsTotal(promptsTotal)
                .promptsReussis(0)
                .etatSession(EtatSessionOral.EN_COURS)
                .scoringRulesVersion(scoringRulesVersion)
                .build();
        session = sessionOralRepository.save(session);

        return StartVoiceSessionResponse.builder()
                .sessionOralId(session.getId())
                .seriesId(series.getId())
                .seriesTitle(series.getTitre())
                .promptsTotal(promptsTotal)
                .dateDebut(session.getDateDebut())
                .build();
    }

    @Transactional
    public VoiceEvaluationResultDTO evaluate(
            Long promptId,
            Long sessionOralId,
            MultipartFile audio,
            Integer dureeSecondes,
            String browserTranscript,
            String playerEmail
    ) {
        User player = resolvePlayer(playerEmail);
        VoicePrompt prompt = voicePromptRepository.findById(promptId)
                .orElseThrow(() -> ApiException.notFound("Consigne introuvable"));
        VoiceSeries series = prompt.getSeries();
        if (series == null || series.getEtat() != EtatVoiceSeries.PUBLIE) {
            throw ApiException.badRequest("Consigne indisponible");
        }

        SessionOral session = sessionOralRepository.findById(sessionOralId)
                .orElseThrow(() -> ApiException.notFound("Session introuvable"));
        if (!session.getUtilisateur().getId().equals(player.getId())) {
            throw ApiException.badRequest("Session invalide");
        }
        if (session.getEtatSession() != EtatSessionOral.EN_COURS) {
            throw ApiException.badRequest("Cette session est déjà terminée");
        }
        if (!session.getSeries().getId().equals(series.getId())) {
            throw ApiException.badRequest("La consigne ne correspond pas à la session");
        }

        int safeDuration = dureeSecondes != null ? Math.max(1, Math.min(dureeSecondes, prompt.getDureeMaxSecondes())) : 1;
        if (audio == null || audio.isEmpty()) {
            if (browserTranscript == null || browserTranscript.isBlank()) {
                throw ApiException.badRequest("Enregistrement audio ou transcription navigateur requis");
            }
        }
        String transcription = voiceTranscriptionService.transcribe(audio, series.getLangue(), browserTranscript);
        VoiceTextMatcherService.MatchResult match = voiceTextMatcherService.compare(
                prompt.getTexteReference(),
                transcription,
                prompt.getTolerance()
        );

        String feedbackJson = serializeFeedback(match);
        VoiceAttempt attempt = VoiceAttempt.builder()
                .prompt(prompt)
                .user(player)
                .sessionOral(session)
                .transcription(transcription)
                .scoreContenu(match.scoreContenu())
                .feedbackJson(feedbackJson)
                .dureeSecondes(safeDuration)
                .reussite(match.reussite())
                .build();
        attempt = voiceAttemptRepository.save(attempt);

        return VoiceEvaluationResultDTO.builder()
                .attemptId(attempt.getId())
                .sessionOralId(session.getId())
                .promptId(prompt.getId())
                .transcription(transcription)
                .scoreContenu(match.scoreContenu())
                .reussite(match.reussite())
                .dureeSecondes(safeDuration)
                .expectedWords(match.expectedWords())
                .spokenWords(match.spokenWords())
                .correctWords(match.correctWords())
                .missedWords(match.missedWords())
                .extraWords(match.extraWords())
                .build();
    }

    @Transactional
    public CompleteVoiceSessionResponse completeSession(Long sessionOralId, String playerEmail) {
        User player = resolvePlayer(playerEmail);
        SessionOral session = sessionOralRepository.findById(sessionOralId)
                .orElseThrow(() -> ApiException.notFound("Session introuvable"));
        if (!session.getUtilisateur().getId().equals(player.getId())) {
            throw ApiException.badRequest("Session invalide");
        }
        if (session.getEtatSession() == EtatSessionOral.TERMINE) {
            throw ApiException.badRequest("Session déjà terminée");
        }

        List<VoiceAttempt> attempts = voiceAttemptRepository.findBySessionOralIdOrderByCreatedAtAsc(session.getId());
        if (attempts.isEmpty()) {
            throw ApiException.badRequest("Aucune tentative enregistrée pour cette session");
        }

        int promptsTotal = session.getPromptsTotal() != null ? session.getPromptsTotal() : attempts.size();
        int promptsReussis = (int) attempts.stream().filter(a -> Boolean.TRUE.equals(a.getReussite())).count();
        int accuracy = Math.round(
                (float) attempts.stream().mapToInt(a -> a.getScoreContenu() != null ? a.getScoreContenu() : 0).sum()
                        / attempts.size()
        );

        int scoreBase = accuracy;
        int difficultyBonus = session.getSeries().getDifficulte() != null
                ? Math.min(20, session.getSeries().getDifficulte() * 2)
                : 0;
        int scoreFinal = Math.min(MAX_SCORE_ORAL, Math.max(10, (scoreBase * 2) + difficultyBonus));

        long recentSameSeries = sessionOralRepository.countByUtilisateurIdAndSeriesIdAndDateDebutAfter(
                player.getId(),
                session.getSeries().getId(),
                LocalDateTime.now().minusHours(1)
        );
        int xpGained = Math.max(5, scoreFinal / 10);
        if (recentSameSeries > 2) {
            xpGained = Math.max(3, xpGained / 2);
        }
        if (accuracy >= 80) {
            xpGained += 5;
        }

        int previousLevel = player.getNiveau() != null ? Math.max(1, player.getNiveau()) : 1;
        int previousXp = player.getPointsExperience() != null ? Math.max(0, player.getPointsExperience()) : 0;
        int previousTotalScore = player.getScoreTotal() != null ? Math.max(0, player.getScoreTotal()) : 0;

        boolean allowLevelUp = accuracy >= SUCCESS_THRESHOLD_PERCENT;
        PlayerProgressionService.ProgressionResult progression = playerProgressionService.applyProgression(
                previousLevel,
                previousXp,
                xpGained,
                allowLevelUp,
                true
        );

        int newTotalScore = previousTotalScore + scoreFinal;
        player.setNiveau(progression.newLevel());
        player.setPointsExperience(progression.newXp());
        player.setScoreTotal(newTotalScore);
        userRepository.save(player);

        LocalDateTime end = LocalDateTime.now();
        int durationSeconds = (int) Math.max(
                1,
                attempts.stream().mapToInt(a -> a.getDureeSecondes() != null ? a.getDureeSecondes() : 0).sum()
        );

        session.setDateFin(end);
        session.setDurationSeconds(durationSeconds);
        session.setScoreBase(scoreBase);
        session.setScoreFinal(scoreFinal);
        session.setXpGained(xpGained);
        session.setAccuracyPercent(accuracy);
        session.setPromptsReussis(promptsReussis);
        session.setPromptsTotal(promptsTotal);
        session.setNiveauAtteint(progression.newLevel());
        session.setEtatSession(EtatSessionOral.TERMINE);
        sessionOralRepository.save(session);

        return CompleteVoiceSessionResponse.builder()
                .sessionOralId(session.getId())
                .scoreFinal(scoreFinal)
                .scoreBase(scoreBase)
                .xpGained(xpGained)
                .accuracyPercent(accuracy)
                .promptsReussis(promptsReussis)
                .promptsTotal(promptsTotal)
                .totalScore(newTotalScore)
                .previousLevel(previousLevel)
                .newLevel(progression.newLevel())
                .previousXp(previousXp)
                .newXp(progression.newXp())
                .xpToNextLevel(progression.xpToNextLevel())
                .levelUp(progression.newLevel() > previousLevel)
                .dateFin(end)
                .durationSeconds(durationSeconds)
                .build();
    }

    public List<PlayerOralHistorySessionDTO> getHistory(String playerEmail) {
        User player = resolvePlayer(playerEmail);
        return sessionOralRepository.findTop50ByUtilisateurIdOrderByDateDebutDesc(player.getId()).stream()
                .filter(s -> s.getEtatSession() == EtatSessionOral.TERMINE)
                .map(session -> PlayerOralHistorySessionDTO.builder()
                        .sessionId(session.getId())
                        .seriesId(session.getSeries() != null ? session.getSeries().getId() : null)
                        .seriesTitle(session.getSeries() != null ? session.getSeries().getTitre() : null)
                        .scoreFinal(session.getScoreFinal())
                        .accuracyPercent(session.getAccuracyPercent())
                        .xpGained(session.getXpGained())
                        .promptsReussis(session.getPromptsReussis())
                        .promptsTotal(session.getPromptsTotal())
                        .durationSeconds(session.getDurationSeconds())
                        .dateFin(session.getDateFin())
                        .build())
                .collect(Collectors.toList());
    }

    private User resolvePlayer(String playerEmail) {
        if (playerEmail == null || playerEmail.isBlank()) {
            throw ApiException.badRequest("Joueur non authentifié");
        }
        User user = userRepository.findByEmail(playerEmail)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent utiliser l'atelier oral");
        }
        return user;
    }

    private String serializeFeedback(VoiceTextMatcherService.MatchResult match) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("expectedWords", match.expectedWords());
            payload.put("spokenWords", match.spokenWords());
            payload.put("correctWords", match.correctWords());
            payload.put("missedWords", match.missedWords());
            payload.put("extraWords", match.extraWords());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }
}
