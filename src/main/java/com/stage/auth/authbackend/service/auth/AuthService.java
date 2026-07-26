package com.stage.auth.authbackend.service.auth;

import com.stage.auth.authbackend.dto.auth.*;
import com.stage.auth.authbackend.entity.EtatCompte;
import com.stage.auth.authbackend.entity.Role;
import com.stage.auth.authbackend.entity.User;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.user.UserRepository;
import com.stage.auth.authbackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;
    private static final int VERIFY_TOKEN_EXPIRY_HOURS = 24;

    public void register(RegisterRequest request) {

        String rawEmail = request.getEmail();
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw ApiException.badRequest("L'email est requis");
        }
        String email = rawEmail.trim().toLowerCase();

        // Si l'email existe déjà :
        // - si le compte est déjà activé => refuser
        // - sinon => renvoyer un nouveau lien de vérification (utile si token invalide/perdu)
        if (userRepository.existsByEmail(email)) {
            User existing = userRepository.findByEmail(email)
                    .orElseThrow(() -> ApiException.badRequest("Cet email est déjà utilisé"));

            if (existing.isEnabled()) {
                throw ApiException.badRequest("Cet email est déjà utilisé");
            }

            String newToken = UUID.randomUUID().toString();
            LocalDateTime newExpiry = LocalDateTime.now().plusHours(VERIFY_TOKEN_EXPIRY_HOURS);

            existing.setTokenVerification(newToken);
            existing.setDateExpirationToken(newExpiry);
            userRepository.save(existing);

            try {
                emailService.sendVerificationEmail(existing.getEmail(), newToken);
            } catch (Exception ex) {
                System.out.println("⚠️ Email de vérification non renvoyé: " + ex.getMessage());
            }
            return;
        }

        LocalDate dateNaissance = request.getDateDeNaissance();
        int age = Period.between(dateNaissance, LocalDate.now()).getYears();
        if (age < 7) {
            throw ApiException.badRequest("L'âge minimum requis est de 7 ans");
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
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : null)
                .role(Role.JOUEUR)
                .etatCompte(EtatCompte.ACTIF)
                .enabled(false)
                .tokenVerification(token)
                .dateExpirationToken(expiry)
                .niveau(1)
                .scoreTotal(0)
                .pointsExperience(0)
                .build();

        userRepository.save(user);

        // ✅ ne pas casser le register si email KO
        try {
            emailService.sendVerificationEmail(user.getEmail(), token);
        } catch (Exception ex) {
            // log seulement (tu peux mettre un logger)
            System.out.println("⚠️ Email non envoyé: " + ex.getMessage());
        }
    }


    public void verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw ApiException.badRequest("Token de vérification manquant");
        }

        String trimmedToken = token.trim();

        User user = userRepository.findByTokenVerification(trimmedToken)
                .orElseThrow(() -> ApiException.badRequest("Token de vérification invalide"));

        // Idempotent: si le compte est déjà activé, on répond OK (utile en dev/StrictMode)
        if (user.isEnabled()) {
            return;
        }

        if (user.getDateExpirationToken() == null || user.getDateExpirationToken().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Token expiré");
        }

        user.setEnabled(true);
        // IMPORTANT: ne pas mettre tokenVerification à null, sinon un 2e appel (dev/StrictMode)
        // renverra "token invalide". On garde le token pour rendre l'opération idempotente.
        user.setDateExpirationToken(null);

        userRepository.save(user);
    }

    public AuthResponse login(AuthRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        // compte activé par email ?
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Veuillez vérifier votre email avant de vous connecter");
        }

        // Vérification etatCompte
        if (user.getEtatCompte() != EtatCompte.ACTIF) {
            throw ApiException.unauthorized("Account is not active");
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
                .orElseThrow(() -> ApiException.notFound("User not found with this email"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS));
        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), resetToken);
    }

    public void resetPassword(ResetPasswordRequest request) {

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> ApiException.badRequest("Invalid reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

}
