package com.britechnology.edugame.service.auth;

import com.britechnology.edugame.dto.auth.*;
import com.britechnology.edugame.entity.EtatCompte;
import com.britechnology.edugame.entity.Genre;
import com.britechnology.edugame.entity.Role;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.user.UserRepository;
import com.britechnology.edugame.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;
    private static final int VERIFY_TOKEN_EXPIRY_HOURS = 24;

    public void register(RegisterRequest request) {

        String rawEmail = request.getEmail();
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw ApiException.badRequest("L'e-mail est requis");
        }
        String email = rawEmail.trim().toLowerCase();

        // Si l'e-mail existe déjà :
        // - si le compte est déjà activé => refuser
        // - sinon => renvoyer un nouveau lien de vérification (utile si token invalide/perdu)
        if (userRepository.existsByEmail(email)) {
            User existing = userRepository.findByEmail(email)
                    .orElseThrow(() -> ApiException.badRequest("Cet e-mail est déjà utilisé"));

            if (existing.isEnabled()) {
                throw ApiException.badRequest("Cet e-mail est déjà utilisé");
            }

            String newToken = UUID.randomUUID().toString();
            LocalDateTime newExpiry = LocalDateTime.now().plusHours(VERIFY_TOKEN_EXPIRY_HOURS);

            existing.setTokenVerification(newToken);
            existing.setDateExpirationToken(newExpiry);
            userRepository.save(existing);

            try {
                emailService.sendVerificationEmail(existing.getEmail(), newToken);
            } catch (Exception ex) {
                log.warn("Email de vérification non renvoyé pour {}", existing.getEmail(), ex);
            }
            return;
        }

        LocalDate dateNaissance = request.getDateDeNaissance();
        int age = Period.between(dateNaissance, LocalDate.now()).getYears();
        if (age < 18) {
            throw ApiException.badRequest("L'âge minimum pour un compte parent est de 18 ans");
        }

        String cin = request.getCin() != null ? request.getCin().trim() : "";
        if (cin.isEmpty()) {
            throw ApiException.badRequest("Le CIN est requis");
        }
        if (userRepository.existsByCin(cin)) {
            throw ApiException.badRequest("Ce CIN est déjà utilisé");
        }

        // Token de vérification
        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(VERIFY_TOKEN_EXPIRY_HOURS);

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .dateDeNaissance(request.getDateDeNaissance())
                .telephone(request.getTelephone())
                .paysPreference(request.getPaysNom() != null ? request.getPaysNom().trim() : null)
                .cin(cin)
                .genre(Genre.parse(request.getGenre()))
                .role(Role.PARENT)
                .etatCompte(EtatCompte.ACTIF)
                .enabled(false)
                .tokenVerification(token)
                .dateExpirationToken(expiry)
                .build();

        userRepository.save(user);

        // ✅ ne pas casser le register si email KO
        try {
            emailService.sendVerificationEmail(user.getEmail(), token);
        } catch (Exception ex) {
            log.warn("Email de vérification non envoyé pour {}", user.getEmail(), ex);
        }
    }


    public void verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw ApiException.badRequest("Jeton de vérification manquant");
        }

        String trimmedToken = token.trim();

        User user = userRepository.findByTokenVerification(trimmedToken)
                .orElseThrow(() -> ApiException.badRequest("Jeton de vérification invalide"));

        // Idempotent: si le compte est déjà activé, on répond OK (utile en dev/StrictMode)
        if (user.isEnabled()) {
            return;
        }

        if (user.getDateExpirationToken() == null || user.getDateExpirationToken().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Jeton expiré");
        }

        user.setEnabled(true);
        // IMPORTANT: ne pas mettre tokenVerification à null, sinon un 2e appel (dev/StrictMode)
        // renverra "token invalide". On garde le token pour rendre l'opération idempotente.
        user.setDateExpirationToken(null);

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("E-mail ou mot de passe invalide"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockedUntil()));
            throw ApiException.unauthorized(
                    "Trop de tentatives échouées. Réessayez dans " + minutesLeft + " minute(s).");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.registerFailedAttempt(user.getId());
            throw ApiException.unauthorized("E-mail ou mot de passe invalide");
        }

        // Compte suspendu par l'admin : message dédié, prioritaire sur le check "enabled" ci-dessous
        // (la suspension met aussi enabled=false, il ne faut donc pas afficher le message de
        // vérification d'e-mail à un utilisateur suspendu).
        if (user.getEtatCompte() == EtatCompte.SUSPENDU) {
            throw ApiException.unauthorized(
                    "Votre compte a été suspendu par l'administration. Veuillez contacter le support pour plus d'informations.");
        }

        // compte activé par email ?
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Veuillez vérifier votre e-mail avant de vous connecter");
        }

        // Vérification etatCompte (garde-fou pour un futur état non ACTIF/SUSPENDU)
        if (user.getEtatCompte() != EtatCompte.ACTIF) {
            throw ApiException.unauthorized("Le compte n'est pas actif");
        }

        if ((user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        applyStreakPolicy(user);
        user.setDateDerniereConnexion(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }

    private void applyStreakPolicy(User user) {
        if (user.getRole() != Role.JOUEUR) return;

        LocalDate today = LocalDate.now();
        Integer currentStreak = user.getCurrentStreakDays() != null ? Math.max(0, user.getCurrentStreakDays()) : 0;
        Integer bestStreak = user.getBestStreakDays() != null ? Math.max(0, user.getBestStreakDays()) : 0;
        LocalDate lastStreakDate = user.getLastStreakDate();

        if (lastStreakDate == null) {
            currentStreak = 1;
            lastStreakDate = today;
        } else {
            long daysDelta = ChronoUnit.DAYS.between(lastStreakDate, today);
            if (daysDelta == 1) {
                currentStreak += 1;
                lastStreakDate = today;
            } else if (daysDelta > 1) {
                currentStreak = 1;
                lastStreakDate = today;
            }
        }

        bestStreak = Math.max(bestStreak, currentStreak);
        user.setCurrentStreakDays(currentStreak);
        user.setBestStreakDays(bestStreak);
        user.setLastStreakDate(lastStreakDate);
    }

    public void forgotPassword(ForgotPasswordRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Aucun utilisateur trouvé avec cet e-mail"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS));
        userRepository.save(user);

        try {
            emailService.sendResetPasswordEmail(user.getEmail(), resetToken);
        } catch (Exception ex) {
            log.warn("Email de reset non envoyé pour {}", user.getEmail(), ex);
        }
    }

    public void resetPassword(ResetPasswordRequest request) {

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> ApiException.badRequest("Jeton de réinitialisation invalide"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Le jeton de réinitialisation a expiré");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

}
