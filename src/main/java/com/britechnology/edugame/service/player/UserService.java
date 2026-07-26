package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.ChangePasswordRequest;
import com.britechnology.edugame.dto.player.CreateGameSessionRequest;
import com.britechnology.edugame.dto.player.CreateGameSessionResponse;
import com.britechnology.edugame.dto.player.PlayerBadgeOverviewItemDTO;
import com.britechnology.edugame.dto.player.PlayerBadgesOverviewDTO;
import com.britechnology.edugame.dto.player.PlayerGameTypePerformanceDTO;
import com.britechnology.edugame.dto.player.PlayerHistorySessionDTO;
import com.britechnology.edugame.dto.player.PlayerOnboardingRequest;
import com.britechnology.edugame.dto.player.PlayerProgressOverviewDTO;
import com.britechnology.edugame.dto.player.PlayerProgressPointDTO;
import com.britechnology.edugame.dto.player.PlayerRewardOverviewItemDTO;
import com.britechnology.edugame.dto.player.PlayerRewardsOverviewDTO;
import com.britechnology.edugame.dto.player.PlayerLeaderboardRanksDTO;
import com.britechnology.edugame.dto.player.SoloLeaderboardEntryDTO;
import com.britechnology.edugame.dto.player.SoloLeaderboardResponseDTO;
import com.britechnology.edugame.dto.player.CompetitiveRoomResultDTO;
import com.britechnology.edugame.dto.player.UpdateProfileRequest;
import com.britechnology.edugame.dto.player.UserDTO;
import com.britechnology.edugame.entity.Badge;
import com.britechnology.edugame.entity.BadgeUtilisateur;
import com.britechnology.edugame.entity.DemandeRecompense;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.Pays;
import com.britechnology.edugame.entity.Recompense;
import com.britechnology.edugame.entity.Region;
import com.britechnology.edugame.entity.Role;
import com.britechnology.edugame.entity.SessionJeu;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.entity.TypeConditionBadge;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.util.AvatarPolicy;
import com.britechnology.edugame.repository.badge.NiveauRepository;
import com.britechnology.edugame.repository.badge.BadgeRepository;
import com.britechnology.edugame.repository.badge.BadgeUtilisateurRepository;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import com.britechnology.edugame.repository.geo.PaysRepository;
import com.britechnology.edugame.repository.geo.RegionRepository;
import com.britechnology.edugame.repository.reward.DemandeRecompenseRepository;
import com.britechnology.edugame.repository.sponsor.RecompenseRepository;
import com.britechnology.edugame.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JeuRepository jeuRepository;
    private final SessionJeuRepository sessionJeuRepository;
    private final PaysRepository paysRepository;
    private final RegionRepository regionRepository;
    private final NiveauRepository niveauRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeUtilisateurRepository badgeUtilisateurRepository;
    private final RecompenseRepository recompenseRepository;
    private final DemandeRecompenseRepository demandeRecompenseRepository;
    private final ScoreCalculatorService scoreCalculatorService;
    private final SmartAdjustmentService smartAdjustmentService;
    private final ProgressionGuardrailService progressionGuardrailService;
    private final SessionLaunchModeValidatorService sessionLaunchModeValidatorService;
    private final RealtimeRoomService realtimeRoomService;

    public UserDTO updateProfile(Authentication authentication, UpdateProfileRequest request) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));

        // Mise à jour des champs modifiables (prénom/nom conservés tels quels, pas de trim)
        if (request.getNom() != null) {
            user.setNom(request.getNom());
        }
        if (request.getPrenom() != null) {
            user.setPrenom(request.getPrenom());
        }
        if (request.getTelephone() != null) {
            user.setTelephone(request.getTelephone());
        }
        AvatarPolicy.applyAvatarUpdate(user, request.getAvatarUrl());

        userRepository.save(user);

        return UserDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .password(null)
                .telephone(user.getTelephone())
                .avatarUrl(AvatarPolicy.publicAvatarUrl(user))
                .role(user.getRole().name())
                .etatCompte(user.getEtatCompte())
                .enabled(user.isEnabled())
                .dateDeNaissance(user.getDateDeNaissance())
                .niveau(user.getNiveau())
                .scoreTotal(user.getScoreTotal())
                .pointsExperience(user.getPointsExperience())
                .xpToNextLevel(xpToNextLevel(user.getNiveau() != null ? Math.max(1, user.getNiveau()) : 1))
                .currentStreakDays(user.getCurrentStreakDays())
                .bestStreakDays(user.getBestStreakDays())
                .lastStreakDate(user.getLastStreakDate())
                .idRegion(user.getRegion() != null ? user.getRegion().getId() : null)
                .idPays(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getId() : null)
                .onboardingCompleted(user.isOnboardingCompleted())
                .idGenre(user.getGenre() != null ? user.getGenre().getId() : null)
                .resetToken(user.getResetToken())
                .resetTokenExpiry(user.getResetTokenExpiry())
                .tokenVerification(user.getTokenVerification())
                .dateExpirationToken(user.getDateExpirationToken())
                .dateDerniereConnexion(user.getDateDerniereConnexion())
                .dateCreation(user.getDateCreation())
                .idParent(user.getParent() != null ? user.getParent().getId() : null)
                .build();
    }

    @Transactional
    public void changePassword(Authentication authentication, ChangePasswordRequest request) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));

        // Vérifier mot de passe actuel
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Mot de passe actuel incorrect");
        }

        // Vérifier que le nouveau mot de passe est différent
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw ApiException.badRequest("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // Encoder nouveau mot de passe
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }

    @Transactional
    public UserDTO completeOnboarding(Authentication authentication, PlayerOnboardingRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Corps de la requête invalide");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));

        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("L'onboarding n'est disponible que pour les joueurs");
        }

        String paysNom = request.getPaysNom() != null ? request.getPaysNom().trim() : null;
        String regionNom = request.getRegionNom() != null ? request.getRegionNom().trim() : null;

        if (paysNom == null || paysNom.isEmpty() || regionNom == null || regionNom.isEmpty()) {
            throw ApiException.badRequest("Pays et région sont requis");
        }

        Pays pays = paysRepository.findFirstByNomIgnoreCase(paysNom)
                .orElseGet(() -> {
                    Pays p = Pays.builder()
                            .nom(paysNom)
                            .codeIso(paysNom.length() >= 2 ? paysNom.substring(0, 2).toUpperCase() : "XX")
                            .build();
                    return paysRepository.save(p);
                });

        Region region = regionRepository.findFirstByPaysIdAndNomIgnoreCase(pays.getId(), regionNom)
                .orElseGet(() -> {
                    Region r = Region.builder().nom(regionNom).pays(pays).build();
                    return regionRepository.save(r);
                });

        user.setRegion(region);
        AvatarPolicy.applyAvatarUpdate(user, request.getAvatarUrl());
        user.setOnboardingCompleted(true);
        userRepository.saveAndFlush(user);

        return buildUserDTO(user);
    }

    @Transactional
    public CreateGameSessionResponse createGameSession(Authentication authentication, CreateGameSessionRequest request) {
        if (request == null || request.getGameId() == null) {
            throw ApiException.badRequest("gameId est requis");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent créer des sessions de jeu");
        }
        Jeu jeu = jeuRepository.findById(request.getGameId())
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        try {
            sessionLaunchModeValidatorService.validate(jeu.getModeJeu(), request.getModeJeu(), request.getRoomCode());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(ex.getMessage());
        }

        EtatSession etat = parseEtatSession(request.getEtatSession());
        String launchMode = normalizeMode(request.getModeJeu());
        String launchRoomCode = normalizeRoomCode(request.getRoomCode());
        Integer roomExpectedPlayers = null;
        if ("EN_LIGNE".equals(launchMode)) {
            RealtimeRoomService.OnlineRoomContext context =
                    realtimeRoomService.validateOnlineSubmission(email, jeu.getId(), launchRoomCode);
            launchRoomCode = context.roomCode();
            roomExpectedPlayers = context.expectedPlayers();
            long roomLockKey = (((long) launchRoomCode.hashCode()) << 32) ^ jeu.getId();
            sessionJeuRepository.lockOnlineRoom(roomLockKey);
            boolean alreadySubmitted = sessionJeuRepository
                    .existsByUtilisateurIdAndJeuIdAndRoomCodeAndModeJeuLance(
                            user.getId(),
                            jeu.getId(),
                            launchRoomCode,
                            "EN_LIGNE"
                    );
            if (alreadySubmitted) {
                throw ApiException.badRequest("Votre score a déjà été enregistré pour cette partie en ligne");
            }
        }
        int requestedDuration = request.getDurationSeconds() != null ? Math.max(0, request.getDurationSeconds()) : 0;
        ScoreCalculatorService.ScoreCalculationResult scoring;
        try {
            scoring = scoreCalculatorService.calculate(
                    jeu.getTypeJeu(),
                    etat,
                    request,
                    requestedDuration,
                    jeu.getDifficulte()
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Session rejected: userId={} gameId={} reason={}", user.getId(), request.getGameId(), ex.getMessage());
            throw ApiException.badRequest(ex.getMessage());
        }

        List<String> anomalyParts = new ArrayList<>();
        if (scoring.anomalyNotes() != null && !scoring.anomalyNotes().isBlank()) anomalyParts.add(scoring.anomalyNotes());

        int baseScore = scoring.baseScore();
        List<SessionJeu> recentSessions = sessionJeuRepository.findTop12ByUtilisateurIdOrderByDateDebutDesc(user.getId());
        SmartAdjustmentService.AdjustmentResult adjustmentResult = smartAdjustmentService.computeBoundedAdjustment(
                user.getId(),
                jeu.getTypeJeu(),
                jeu.getDifficulte(),
                baseScore,
                request,
                recentSessions
        );
        if (adjustmentResult.riskFlags() != null && !adjustmentResult.riskFlags().isBlank()) {
            anomalyParts.add(adjustmentResult.riskFlags());
        }

        int finalScore = clampScoreByType(
                jeu.getTypeJeu(),
                (int) Math.round(baseScore * (1.0 + adjustmentResult.adjustment()))
        );
        if (finalScore != baseScore) anomalyParts.add("score_adjusted");

        int deterministicXp = Math.max(0, scoring.xpFromDeterministic() + (finalScore - baseScore) / 12);
        long recentSameGame = sessionJeuRepository.countByUtilisateurIdAndJeuIdAndDateDebutAfter(
                user.getId(),
                jeu.getId(),
                LocalDateTime.now().minusHours(1)
        );
        ProgressionGuardrailService.GuardrailResult guardrails = progressionGuardrailService.apply(
                jeu.getTypeJeu(),
                etat,
                request,
                deterministicXp,
                recentSameGame
        );
        if (guardrails.guardrailNotes() != null && !guardrails.guardrailNotes().isBlank()) anomalyParts.add(guardrails.guardrailNotes());

        LocalDateTime end = LocalDateTime.now();
        int durationSeconds = scoring.normalizedDurationSeconds();
        LocalDateTime start = end.minusSeconds(durationSeconds);
        int scoreGlobal = finalScore;
        int xpGained = guardrails.xpAfterGuardrails();

        int previousLevel = user.getNiveau() != null ? Math.max(1, user.getNiveau()) : 1;
        int previousXp = user.getPointsExperience() != null ? Math.max(0, user.getPointsExperience()) : 0;
        int previousTotalScore = user.getScoreTotal() != null ? Math.max(0, user.getScoreTotal()) : 0;

        ProgressionResult progression = applyProgression(
                previousLevel,
                previousXp,
                xpGained,
                guardrails.allowLevelUp(),
                true
        );

        // Chaque joueur joue pour son propre compte, y compris face aux adversaires en ligne.
        int newTotalScore = previousTotalScore + scoreGlobal;
        user.setNiveau(progression.newLevel());
        user.setPointsExperience(progression.newXp()); // XP individuel conservé dans les 2 modes
        user.setScoreTotal(newTotalScore);
        userRepository.save(user);

        SessionJeu session = SessionJeu.builder()
                .utilisateur(user)
                .jeu(jeu)
                .dateDebut(start)
                .dateFin(end)
                .scoreGlobal(scoreGlobal)
                .scoreBase(baseScore)
                .scoreFinal(finalScore)
                .aiAdjustment(adjustmentResult.adjustment())
                .aiAdjustmentSource(adjustmentResult.source())
                .aiExplanationCode(adjustmentResult.explanationCode())
                .xpGained(xpGained)
                .durationSeconds(durationSeconds)
                .scoringRulesVersion(scoring.scoringRulesVersion())
                .anomalyNotes(String.join(",", anomalyParts))
                .accuracyPercent(request.getAccuracyPercent())
                .reactionTimeMs(request.getReactionTimeMs())
                .totalQuestions(request.getTotalQuestions())
                .correctAnswers(request.getCorrectAnswers())
                .moves(request.getMoves())
                .matchesCount(request.getMatches())
                .attempts(request.getAttempts())
                .hintsUsed(request.getHintsUsed())
                .totalRounds(request.getTotalRounds())
                .successfulRounds(request.getSuccessfulRounds())
                .modeJeuLance(launchMode)
                .roomCode(launchRoomCode)
                .roomExpectedPlayers(roomExpectedPlayers)
                .niveauAtteint(progression.newLevel())
                .etatSession(etat)
                .build();
        session = sessionJeuRepository.save(session);
        CompetitiveRoomResultDTO roomResult = "EN_LIGNE".equals(launchMode)
                ? realtimeRoomService.broadcastCompetitiveResult(launchRoomCode)
                : null;

        return CreateGameSessionResponse.builder()
                .sessionId(session.getId())
                .scoreGlobal(scoreGlobal)
                .scoreBase(baseScore)
                .scoreFinal(finalScore)
                .scoreMaxPossible(maxScoreByType(jeu.getTypeJeu()))
                .aiAdjustment(adjustmentResult.adjustment())
                .totalScore(newTotalScore)
                .xpGained(xpGained)
                .previousLevel(previousLevel)
                .newLevel(progression.newLevel())
                .previousXp(previousXp)
                .newXp(progression.newXp())
                .xpToNextLevel(progression.xpToNextLevel())
                .durationSeconds(durationSeconds)
                .dateDebut(start)
                .dateFin(end)
                .levelUp(progression.newLevel() > previousLevel)
                .scoringRulesVersion(scoring.scoringRulesVersion())
                .anomalyNotes(String.join(",", anomalyParts))
                .adjustmentSource(adjustmentResult.source())
                .explanationCode(adjustmentResult.explanationCode())
                .roomResult(roomResult)
                .message(roomResult == null
                        ? "Session solo enregistrée"
                        : (roomResult.isComplete()
                            ? "Tous les adversaires ont terminé"
                            : "Score enregistré, en attente des adversaires"))
                .build();
    }

    public List<SoloLeaderboardEntryDTO> getSoloLeaderboard() {
        return userRepository.fetchSoloLeaderboard();
    }

    public SoloLeaderboardResponseDTO getSoloLeaderboardScoped(Authentication authentication, String scopeRaw) {
        User user = resolvePlayer(authentication);
        String scope = scopeRaw == null || scopeRaw.isBlank() ? "GLOBAL" : scopeRaw.trim().toUpperCase();

        List<SoloLeaderboardEntryDTO> entries;
        String scopeLabel;
        switch (scope) {
            case "COUNTRY" -> {
                Region region = user.getRegion();
                if (region == null || region.getPays() == null) {
                    throw ApiException.badRequest("Complète ton profil avec un pays et une région pour voir ce classement.");
                }
                Long paysId = region.getPays().getId();
                entries = userRepository.fetchSoloLeaderboardByPaysId(paysId);
                scopeLabel = region.getPays().getNom();
            }
            case "REGION" -> {
                Region region = user.getRegion();
                if (region == null) {
                    throw ApiException.badRequest("Complète ton profil avec une région pour voir ce classement.");
                }
                entries = userRepository.fetchSoloLeaderboardByRegionId(region.getId());
                scopeLabel = region.getNom();
            }
            default -> {
                scope = "GLOBAL";
                entries = userRepository.fetchSoloLeaderboard();
                scopeLabel = "Mondial";
            }
        }

        Integer currentRank = findRankInEntries(entries, user.getId());
        return SoloLeaderboardResponseDTO.builder()
                .scope(scope)
                .scopeLabel(scopeLabel)
                .entries(entries)
                .currentUserRank(currentRank)
                .currentUserId(user.getId())
                .totalPlayers(entries.size())
                .build();
    }

    public PlayerLeaderboardRanksDTO getMyLeaderboardRanks(Authentication authentication) {
        User user = resolvePlayer(authentication);
        int score = user.getScoreTotal() != null ? user.getScoreTotal() : 0;
        int xp = user.getPointsExperience() != null ? user.getPointsExperience() : 0;
        Long userId = user.getId();

        int globalRank = userRepository.countPlayersAheadGlobal(score, xp, userId);
        int globalTotal = (int) userRepository.countSoloPlayersGlobal();

        Region region = user.getRegion();
        boolean hasRegion = region != null && user.isOnboardingCompleted();
        boolean hasCountry = hasRegion && region.getPays() != null;

        Integer countryRank = null;
        int countryTotal = 0;
        Long paysId = null;
        String paysNom = null;
        if (hasCountry) {
            paysId = region.getPays().getId();
            paysNom = region.getPays().getNom();
            countryRank = userRepository.countPlayersAheadInPays(paysId, score, xp, userId);
            countryTotal = (int) userRepository.countSoloPlayersByPaysId(paysId);
        }

        Integer regionRank = null;
        int regionTotal = 0;
        Long regionId = null;
        String regionNom = null;
        if (hasRegion) {
            regionId = region.getId();
            regionNom = region.getNom();
            regionRank = userRepository.countPlayersAheadInRegion(regionId, score, xp, userId);
            regionTotal = (int) userRepository.countSoloPlayersByRegionId(regionId);
        }

        return PlayerLeaderboardRanksDTO.builder()
                .globalRank(globalRank)
                .globalTotal(globalTotal)
                .countryRankingAvailable(hasCountry)
                .countryRank(countryRank)
                .countryTotal(countryTotal)
                .paysId(paysId)
                .paysNom(paysNom)
                .regionRankingAvailable(hasRegion)
                .regionRank(regionRank)
                .regionTotal(regionTotal)
                .regionId(regionId)
                .regionNom(regionNom)
                .build();
    }

    private User resolvePlayer(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Réservé aux joueurs");
        }
        return user;
    }

    private static Integer findRankInEntries(List<SoloLeaderboardEntryDTO> entries, Long userId) {
        if (userId == null || entries == null) return null;
        for (int i = 0; i < entries.size(); i++) {
            if (userId.equals(entries.get(i).getUserId())) {
                return i + 1;
            }
        }
        return null;
    }

    public PlayerProgressOverviewDTO getProgressOverview(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent consulter la progression");
        }

        List<SessionJeu> recentSessions = sessionJeuRepository.findTop120ByUtilisateurIdOrderByDateDebutDesc(user.getId());
        long totalSessions = sessionJeuRepository.countByUtilisateurIdAndEtatSession(user.getId(), EtatSession.TERMINE);
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        Integer weeklyDurationSum = sessionJeuRepository.sumDurationSecondsSince(user.getId(), weekStart);
        int weeklySeconds = Math.max(
                0,
                weeklyDurationSum != null ? weeklyDurationSum : 0
        );

        int avgSuccessRate = (int) Math.round(
                recentSessions.stream()
                        .map(SessionJeu::getAccuracyPercent)
                        .filter(v -> v != null && v >= 0)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0)
        );

        Map<String, PlayerProgressPointDTO> progressByWeek = new LinkedHashMap<>();
        WeekFields weekFields = WeekFields.ISO;
        recentSessions.stream()
                .filter(s -> s.getDateDebut() != null)
                .forEach(s -> {
                    int week = s.getDateDebut().get(weekFields.weekOfWeekBasedYear());
                    int year = s.getDateDebut().get(weekFields.weekBasedYear());
                    String key = String.format(Locale.ROOT, "S%02d-%d", week, year);
                    PlayerProgressPointDTO point = progressByWeek.getOrDefault(
                            key,
                            PlayerProgressPointDTO.builder().week(key).xp(0).score(0).build()
                    );
                    int xp = point.getXp() != null ? point.getXp() : 0;
                    int score = point.getScore() != null ? point.getScore() : 0;
                    progressByWeek.put(
                            key,
                            PlayerProgressPointDTO.builder()
                                    .week(key)
                                    .xp(xp + (s.getXpGained() != null ? Math.max(0, s.getXpGained()) : 0))
                                    .score(score + (s.getScoreFinal() != null ? s.getScoreFinal() : (s.getScoreGlobal() != null ? s.getScoreGlobal() : 0)))
                                    .build()
                    );
                });
        List<PlayerProgressPointDTO> progressData = progressByWeek.values().stream()
                .sorted(Comparator.comparing(PlayerProgressPointDTO::getWeek))
                .toList();

        Map<TypeJeu, List<SessionJeu>> byType = new LinkedHashMap<>();
        recentSessions.stream()
                .filter(s -> s.getJeu() != null && s.getJeu().getTypeJeu() != null)
                .forEach(s -> byType.computeIfAbsent(s.getJeu().getTypeJeu(), k -> new ArrayList<>()).add(s));

        List<PlayerGameTypePerformanceDTO> performanceByGameType = byType.entrySet().stream()
                .map(entry -> {
                    List<SessionJeu> sessions = entry.getValue();
                    int played = sessions.size();
                    int avgScore = (int) Math.round(
                            sessions.stream()
                                    .mapToInt(s -> s.getScoreFinal() != null ? s.getScoreFinal() : (s.getScoreGlobal() != null ? s.getScoreGlobal() : 0))
                                    .average()
                                    .orElse(0.0)
                    );
                    int successRate = (int) Math.round(
                            sessions.stream()
                                    .map(SessionJeu::getAccuracyPercent)
                                    .filter(v -> v != null && v >= 0)
                                    .mapToInt(Integer::intValue)
                                    .average()
                                    .orElse(0.0)
                    );
                    return PlayerGameTypePerformanceDTO.builder()
                            .type(toProgressTypeLabel(entry.getKey()))
                            .played(played)
                            .avgScore(avgScore)
                            .successRate(successRate)
                            .build();
                })
                .sorted(Comparator.comparing(PlayerGameTypePerformanceDTO::getType))
                .toList();

        return PlayerProgressOverviewDTO.builder()
                .currentLevel(user.getNiveau() != null ? Math.max(1, user.getNiveau()) : 1)
                .avgSuccessRate(Math.max(0, Math.min(100, avgSuccessRate)))
                .totalSessions((int) Math.max(0, totalSessions))
                .weeklyPlaytimeMinutes(Math.max(0, weeklySeconds / 60))
                .skillMath(averageAccuracyForType(byType.get(TypeJeu.QUIZ)))
                .skillLogic(averageAccuracyForType(byType.get(TypeJeu.LOGIQUE)))
                .skillMemory(averageAccuracyForType(byType.get(TypeJeu.MEMOIRE)))
                .skillReflex(averageAccuracyForType(byType.get(TypeJeu.REFLEXE)))
                .progressData(progressData)
                .performanceByGameType(performanceByGameType)
                .build();
    }

    public PlayerBadgesOverviewDTO getBadgesOverview(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent consulter les badges");
        }

        List<Badge> badges = badgeRepository.findAllByOrderByNomAsc();
        Map<Long, java.time.LocalDate> earnedByBadgeId = badgeUtilisateurRepository.findByUtilisateurId(user.getId()).stream()
                .filter(link -> link.getBadge() != null && link.getBadge().getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        link -> link.getBadge().getId(),
                        link -> link.getDateObtention(),
                        (a, b) -> a
                ));

        long totalTerminatedSessions = sessionJeuRepository.countByUtilisateurIdAndEtatSession(user.getId(), EtatSession.TERMINE);
        boolean hasFirstWin = sessionJeuRepository.existsAnyWinByUser(user.getId());
        boolean hasQuizWin = sessionJeuRepository.existsQuizWinByUser(user.getId());
        boolean hasPerfectGame = sessionJeuRepository.existsPerfectGameByUser(user.getId());
        int totalScore = user.getScoreTotal() != null ? Math.max(0, user.getScoreTotal()) : 0;
        int streakDays = user.getCurrentStreakDays() != null ? Math.max(0, user.getCurrentStreakDays()) : 0;

        List<PlayerBadgeOverviewItemDTO> rows = badges.stream()
                .map(badge -> {
                    boolean earned = earnedByBadgeId.containsKey(badge.getId());
                    boolean conditionSatisfied = isBadgeConditionSatisfied(
                            badge,
                            totalScore,
                            totalTerminatedSessions,
                            streakDays,
                            hasFirstWin,
                            hasQuizWin,
                            hasPerfectGame
                    );
                    if (!earned) {
                        earned = false;
                    }
                    return PlayerBadgeOverviewItemDTO.builder()
                            .id(badge.getId())
                            .nom(badge.getNom())
                            .description(badge.getDescription())
                            .icone(badge.getIcone())
                            .unlockCondition(toUnlockConditionLabel(badge))
                            .earned(earned)
                            .claimable(!earned && conditionSatisfied)
                            .earnedDate(earnedByBadgeId.get(badge.getId()))
                            .build();
                })
                .toList();

        int earnedCount = (int) rows.stream().filter(PlayerBadgeOverviewItemDTO::isEarned).count();
        return PlayerBadgesOverviewDTO.builder()
                .total(rows.size())
                .earned(earnedCount)
                .badges(rows)
                .build();
    }

    public List<PlayerHistorySessionDTO> getHistorySessions(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent consulter l'historique");
        }

        return sessionJeuRepository.findTop120ByUtilisateurIdOrderByDateDebutDesc(user.getId()).stream()
                .map(this::toHistorySessionDTO)
                .toList();
    }

    public PlayerRewardsOverviewDTO getRewardsOverview(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent consulter les récompenses");
        }

        int totalScore = user.getScoreTotal() != null ? Math.max(0, user.getScoreTotal()) : 0;
        Map<Long, DemandeRecompense> requestByRewardId = demandeRecompenseRepository
                .findByUtilisateurIdOrderByDateDemandeDesc(user.getId())
                .stream()
                .filter(d -> d.getRecompense() != null && d.getRecompense().getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        d -> d.getRecompense().getId(),
                        d -> d,
                        (a, b) -> a
                ));

        List<PlayerRewardOverviewItemDTO> rows = recompenseRepository.findAllByOrderByIdDesc().stream()
                .filter(r -> !Boolean.FALSE.equals(r.getActive()))
                .map(reward -> toPlayerRewardOverviewItemDTO(reward, requestByRewardId.get(reward.getId()), totalScore))
                .toList();

        int claimableCount = (int) rows.stream().filter(PlayerRewardOverviewItemDTO::isClaimable).count();
        int claimedCount = (int) rows.stream().filter(PlayerRewardOverviewItemDTO::isClaimed).count();

        return PlayerRewardsOverviewDTO.builder()
                .total(rows.size())
                .claimable(claimableCount)
                .claimed(claimedCount)
                .rewards(rows)
                .build();
    }

    @Transactional
    public PlayerRewardOverviewItemDTO claimReward(Authentication authentication, Long rewardId) {
        if (rewardId == null) {
            throw ApiException.badRequest("rewardId est requis");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent réclamer des récompenses");
        }

        Recompense reward = recompenseRepository.findById(rewardId)
                .orElseThrow(() -> ApiException.notFound("Récompense introuvable"));
        if (Boolean.FALSE.equals(reward.getActive())) {
            throw ApiException.badRequest("Cette récompense n'est plus disponible");
        }

        DemandeRecompense existing = demandeRecompenseRepository
                .findFirstByUtilisateurIdAndRecompenseIdOrderByDateDemandeDesc(user.getId(), rewardId)
                .orElse(null);
        if (existing != null && !"REJECTED".equalsIgnoreCase(existing.getStatut())) {
            return toPlayerRewardOverviewItemDTO(
                    reward,
                    existing,
                    user.getScoreTotal() != null ? Math.max(0, user.getScoreTotal()) : 0
            );
        }

        int totalScore = user.getScoreTotal() != null ? Math.max(0, user.getScoreTotal()) : 0;
        int requiredScore = reward.getScoreMin() != null ? Math.max(0, reward.getScoreMin()) : 0;
        if (requiredScore <= 0) {
            throw ApiException.badRequest("Condition de déblocage non configurée pour cette récompense");
        }
        if (totalScore < requiredScore) {
            throw ApiException.badRequest("Score insuffisant pour débloquer cette récompense");
        }

        DemandeRecompense request = DemandeRecompense.builder()
                .utilisateur(user)
                .recompense(reward)
                .statut("PENDING")
                .dateDemande(LocalDate.now())
                .build();
        DemandeRecompense saved = demandeRecompenseRepository.save(request);
        return toPlayerRewardOverviewItemDTO(reward, saved, totalScore);
    }

    @Transactional
    public PlayerBadgeOverviewItemDTO claimBadge(Authentication authentication, Long badgeId) {
        if (badgeId == null) {
            throw ApiException.badRequest("badgeId est requis");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent réclamer des badges");
        }

        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> ApiException.notFound("Badge introuvable"));

        BadgeUtilisateur existing = badgeUtilisateurRepository
                .findFirstByUtilisateurIdAndBadgeId(user.getId(), badgeId)
                .orElse(null);
        if (existing != null) {
            return PlayerBadgeOverviewItemDTO.builder()
                    .id(badge.getId())
                    .nom(badge.getNom())
                    .description(badge.getDescription())
                    .icone(badge.getIcone())
                    .unlockCondition(toUnlockConditionLabel(badge))
                    .earned(true)
                    .claimable(false)
                    .earnedDate(existing.getDateObtention())
                    .build();
        }

        long totalTerminatedSessions = sessionJeuRepository.countByUtilisateurIdAndEtatSession(user.getId(), EtatSession.TERMINE);
        boolean hasFirstWin = sessionJeuRepository.existsAnyWinByUser(user.getId());
        boolean hasQuizWin = sessionJeuRepository.existsQuizWinByUser(user.getId());
        boolean hasPerfectGame = sessionJeuRepository.existsPerfectGameByUser(user.getId());
        int totalScore = user.getScoreTotal() != null ? Math.max(0, user.getScoreTotal()) : 0;
        int streakDays = user.getCurrentStreakDays() != null ? Math.max(0, user.getCurrentStreakDays()) : 0;

        boolean conditionSatisfied = isBadgeConditionSatisfied(
                badge,
                totalScore,
                totalTerminatedSessions,
                streakDays,
                hasFirstWin,
                hasQuizWin,
                hasPerfectGame
        );
        if (!conditionSatisfied) {
            throw ApiException.badRequest("Condition non remplie pour ce badge");
        }

        BadgeUtilisateur link = BadgeUtilisateur.builder()
                .badge(badge)
                .utilisateur(user)
                .dateObtention(LocalDate.now())
                .build();
        badgeUtilisateurRepository.save(link);

        return PlayerBadgeOverviewItemDTO.builder()
                .id(badge.getId())
                .nom(badge.getNom())
                .description(badge.getDescription())
                .icone(badge.getIcone())
                .unlockCondition(toUnlockConditionLabel(badge))
                .earned(true)
                .claimable(false)
                .earnedDate(link.getDateObtention())
                .build();
    }

    private EtatSession parseEtatSession(String raw) {
        if (raw == null || raw.isBlank()) return EtatSession.TERMINE;
        try {
            return EtatSession.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EtatSession.TERMINE;
        }
    }

    private int averageAccuracyForType(List<SessionJeu> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;
        return (int) Math.round(
                sessions.stream()
                        .map(SessionJeu::getAccuracyPercent)
                        .filter(v -> v != null && v >= 0)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0)
        );
    }

    private String toProgressTypeLabel(TypeJeu typeJeu) {
        return switch (typeJeu) {
            case QUIZ -> "Quiz";
            case MEMOIRE -> "Memoire";
            case LOGIQUE -> "Logique";
            case REFLEXE -> "Reflexe";
        };
    }

    private boolean isBadgeConditionSatisfied(
            Badge badge,
            int totalScore,
            long totalSessions,
            int streakDays,
            boolean hasFirstWin,
            boolean hasQuizWin,
            boolean hasPerfectGame
    ) {
        TypeConditionBadge type = badge.getTypeCondition() != null ? badge.getTypeCondition() : TypeConditionBadge.SCORE_MIN;
        Integer rawValue = badge.getScoreCondition();
        int value = rawValue != null ? Math.max(0, rawValue) : 0;
        return switch (type) {
            case SCORE_MIN -> rawValue != null && value > 0 && totalScore >= value;
            case FIRST_WIN -> hasFirstWin;
            case GAMES_PLAYED -> rawValue != null && value > 0 && totalSessions >= value;
            case STREAK_DAYS -> rawValue != null && value > 0 && streakDays >= value;
            case QUIZ_WIN -> hasQuizWin;
            case PERFECT_GAME -> hasPerfectGame;
        };
    }

    private String toUnlockConditionLabel(Badge badge) {
        TypeConditionBadge type = badge.getTypeCondition() != null ? badge.getTypeCondition() : TypeConditionBadge.SCORE_MIN;
        int value = badge.getScoreCondition() != null ? Math.max(0, badge.getScoreCondition()) : 0;
        return switch (type) {
            case SCORE_MIN -> "Score total >= " + value;
            case FIRST_WIN -> "Premiere victoire";
            case GAMES_PLAYED -> value + " parties jouees minimum";
            case STREAK_DAYS -> "Streak de " + value + " jours";
            case QUIZ_WIN -> "Victoire en Quiz";
            case PERFECT_GAME -> "Partie parfaite (100% reussite)";
        };
    }

    private PlayerRewardOverviewItemDTO toPlayerRewardOverviewItemDTO(
            Recompense reward,
            DemandeRecompense request,
            int totalScore
    ) {
        int requiredScore = reward.getScoreMin() != null ? Math.max(0, reward.getScoreMin()) : 0;
        boolean conditionConfigured = requiredScore > 0;
        boolean scoreReached = conditionConfigured && totalScore >= requiredScore;
        boolean hasActiveRequest = request != null && request.getStatut() != null && !"REJECTED".equalsIgnoreCase(request.getStatut());
        boolean claimed = request != null && request.getStatut() != null
                && "APPROVED".equalsIgnoreCase(request.getStatut());
        return PlayerRewardOverviewItemDTO.builder()
                .id(reward.getId())
                .claimId(request != null ? request.getId() : null)
                .nom(reward.getNom())
                .description(reward.getDescription())
                .typeRecompense(reward.getTypeRecompense() != null ? reward.getTypeRecompense().name() : "AUTRE")
                .scoreMin(requiredScore)
                .unlockCondition(conditionConfigured ? ("Score total >= " + requiredScore) : "Condition non configurée")
                .claimable(scoreReached && !hasActiveRequest)
                .claimed(claimed)
                .requestStatus(request != null ? request.getStatut() : null)
                .requestedDate(request != null ? request.getDateDemande() : null)
                .build();
    }

    private PlayerHistorySessionDTO toHistorySessionDTO(SessionJeu session) {
        int score = session.getScoreFinal() != null
                ? session.getScoreFinal()
                : (session.getScoreGlobal() != null ? session.getScoreGlobal() : 0);
        int accuracy = session.getAccuracyPercent() != null ? Math.max(0, session.getAccuracyPercent()) : 0;
        boolean success = session.getEtatSession() == EtatSession.TERMINE && (score > 0 || accuracy >= 60);
        String mode = "EN_LIGNE".equalsIgnoreCase(session.getModeJeuLance()) ? "Online" : "Individual";
        String gameType = session.getJeu() != null && session.getJeu().getTypeJeu() != null
                ? session.getJeu().getTypeJeu().name()
                : "UNKNOWN";
        String status = switch (session.getEtatSession()) {
            case EN_COURS -> "EN_COURS";
            case TERMINE -> "TERMINEE";
            case ABANDONNE -> "ABANDONNEE";
        };

        return PlayerHistorySessionDTO.builder()
                .id(session.getId())
                .gameId(session.getJeu() != null ? session.getJeu().getId() : null)
                .gameTitle(session.getJeu() != null ? session.getJeu().getTitre() : "Jeu")
                .gameType(gameType)
                .dateDebut(session.getDateDebut())
                .dateFin(session.getDateFin())
                .durationSeconds(session.getDurationSeconds())
                .scoreFinal(score)
                .niveauAtteint(session.getNiveauAtteint())
                .reussite(success)
                .statut(status)
                .mode(mode)
                .accuracy(session.getAccuracyPercent())
                .reactionTime(session.getReactionTimeMs())
                .build();
    }

    private UserDTO buildUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .password(null)
                .telephone(user.getTelephone())
                .avatarUrl(AvatarPolicy.publicAvatarUrl(user))
                .role(user.getRole().name())
                .etatCompte(user.getEtatCompte())
                .enabled(user.isEnabled())
                .dateDeNaissance(user.getDateDeNaissance())
                .niveau(user.getNiveau())
                .scoreTotal(user.getScoreTotal())
                .pointsExperience(user.getPointsExperience())
                .xpToNextLevel(xpToNextLevel(user.getNiveau() != null ? Math.max(1, user.getNiveau()) : 1))
                .currentStreakDays(user.getCurrentStreakDays())
                .bestStreakDays(user.getBestStreakDays())
                .lastStreakDate(user.getLastStreakDate())
                .idRegion(user.getRegion() != null ? user.getRegion().getId() : null)
                .regionNom(user.getRegion() != null ? user.getRegion().getNom() : null)
                .idPays(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getId() : null)
                .paysNom(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getNom() : null)
                .onboardingCompleted(user.isOnboardingCompleted())
                .idGenre(user.getGenre() != null ? user.getGenre().getId() : null)
                .resetToken(user.getResetToken())
                .resetTokenExpiry(user.getResetTokenExpiry())
                .tokenVerification(user.getTokenVerification())
                .dateExpirationToken(user.getDateExpirationToken())
                .dateDerniereConnexion(user.getDateDerniereConnexion())
                .dateCreation(user.getDateCreation())
                .idParent(user.getParent() != null ? user.getParent().getId() : null)
                .build();
    }

    private int xpToNextLevel(int level) {
        return niveauRepository.findByNiveau(level)
                .map(cfg -> Math.max(1, cfg.getPointMin() != null ? cfg.getPointMin() : 0))
                // Fallback curve when DB levels are not configured yet.
                // Examples: L1=250, L2=460, L3=720, L4=1030...
                .orElse(Math.max(250, (level * 150) + (level * level * 55)));
    }

    private ProgressionResult applyProgression(
            int currentLevel,
            int currentXp,
            int xpGained,
            boolean allowLevelUp,
            boolean maxOneLevelUpPerSession
    ) {
        int level = Math.max(1, currentLevel);
        int xp = Math.max(0, currentXp) + Math.max(0, xpGained);
        int required = xpToNextLevel(level);

        if (!allowLevelUp) {
            if (xp >= required) xp = Math.max(0, required - 1);
            return new ProgressionResult(level, xp, required);
        }

        boolean levelUpsApplied = false;
        while (xp >= required) {
            xp -= required;
            level += 1;
            levelUpsApplied = true;
            required = xpToNextLevel(level);
            if (maxOneLevelUpPerSession && levelUpsApplied) break;
        }

        return new ProgressionResult(level, xp, required);
    }

    private int maxScoreByType(TypeJeu typeJeu) {
        return switch (typeJeu) {
            case QUIZ -> 400;
            case MEMOIRE -> 350;
            case LOGIQUE -> 300;
            case REFLEXE -> 300;
        };
    }

    private int clampScoreByType(TypeJeu typeJeu, int score) {
        return Math.max(0, Math.min(maxScoreByType(typeJeu), score));
    }

    private String normalizeMode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("INDIVIDUAL".equals(normalized)) return "INDIVIDUEL";
        if ("ONLINE".equals(normalized)) return "EN_LIGNE";
        return normalized;
    }

    private String normalizeRoomCode(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return value.isBlank() ? null : value;
    }

    private record ProgressionResult(int newLevel, int newXp, int xpToNextLevel) {}
}
