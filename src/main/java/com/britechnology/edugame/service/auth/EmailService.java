package com.britechnology.edugame.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String UTF_8 = "UTF-8";
    private static final String DEFAULT_GAME_TITLE = "Votre jeu";
    private static final String GAMES_MANAGE_PATH = "/educator/games/manage";
    private static final String DEFAULT_NO_DETAILS_MESSAGE = "Aucun détail supplémentaire n'a été fourni.";
    private static final String GREETING_PREFIX = "Bonjour ";

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async("taskExecutor")
    public void sendVerificationEmail(String toEmail, String token) {

        String verificationLink = frontendUrl + "/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Vérification de votre compte");
        message.setText(
                "Bonjour,\n\n" +
                        "Cliquez sur le lien suivant pour activer votre compte :\n\n" +
                        verificationLink +
                        "\n\nCe lien expirera prochainement."
        );

        // En environnement de dev, on ne veut pas que l'échec d'envoi mail bloque l'inscription
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Échec d'envoi de l'email de vérification vers {}", toEmail, e);
        }
    }

    @Async("taskExecutor")
    public void sendResetPasswordEmail(String toEmail, String token) {

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Réinitialisation de mot de passe");
        message.setText(
                "Cliquez sur le lien suivant pour réinitialiser votre mot de passe :\n\n" +
                        resetLink
        );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Échec d'envoi de l'email de réinitialisation vers {}", toEmail, e);
        }
    }

    @Async("taskExecutor")
    public void sendGameApprovedEmail(String toEmail, String gameTitle) {
        String safeTitle = (gameTitle == null || gameTitle.isBlank()) ? DEFAULT_GAME_TITLE : gameTitle;
        String gamesLink = frontendUrl + GAMES_MANAGE_PATH;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8);
            helper.setTo(toEmail);
            helper.setSubject("Votre demande de jeu a été approuvée");
            helper.setText("""
                    <div style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                        <div style="padding:20px 24px;background:linear-gradient(90deg,#8b5cf6,#06b6d4);color:#ffffff;">
                          <h2 style="margin:0;font-size:22px;line-height:1.2;">Demande approuvée avec succès</h2>
                          <p style="margin:8px 0 0 0;font-size:14px;opacity:.95;">Votre contenu est maintenant validé par l'administration.</p>
                        </div>
                        <div style="padding:22px 24px;">
                          <p style="margin:0 0 12px 0;">Bonjour,</p>
                          <p style="margin:0 0 16px 0;">
                            Excellente nouvelle ! Votre demande de jeu a été <strong>approuvée</strong>.
                          </p>
                          <div style="margin:0 0 18px 0;padding:12px 14px;border:1px solid #dbeafe;background:#eff6ff;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:4px;">Jeu concerné</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                          </div>
                          <a href="%s" style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;padding:11px 16px;border-radius:10px;font-size:14px;">
                            Voir mes jeux
                          </a>
                          <p style="margin:18px 0 0 0;font-size:13px;color:#6b7280;">
                            Merci pour votre contribution,<br/>
                            <strong style="color:#374151;">L'équipe EduGame</strong>
                          </p>
                        </div>
                      </div>
                    </div>
                    """.formatted(safeTitle, gamesLink), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // Fallback texte si le rendu HTML échoue
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(toEmail);
            fallback.setSubject("Votre demande de jeu a été approuvée");
            fallback.setText(
                    "Bonjour,\n\n" +
                            "Votre demande de jeu a été approuvée avec succès.\n\n" +
                            "Jeu concerné : " + safeTitle + "\n\n" +
                            "Consulter mes jeux : " + gamesLink + "\n\n" +
                            "Cordialement,\n" +
                            "L'équipe EduGame"
            );
            try {
                mailSender.send(fallback);
            } catch (Exception ignored) {
                log.warn("Échec d'envoi de l'email d'approbation du jeu vers {}", toEmail, ignored);
            }
        }
    }

    @Async("taskExecutor")
    public void sendChildAccountCreatedEmail(String toEmail, String childFirstName, String plainPassword, String parentDisplayName) {
        String safeFirstName = (childFirstName == null || childFirstName.isBlank()) ? "Champion" : childFirstName.trim();
        String safeParentName = (parentDisplayName == null || parentDisplayName.isBlank()) ? "Ton parent" : parentDisplayName.trim();
        String loginLink = frontendUrl + "/login";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8);
            helper.setTo(toEmail);
            helper.setSubject("Ton compte EduGame a été créé 🎉");
            helper.setText("""
                    <div style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                        <div style="padding:20px 24px;background:linear-gradient(90deg,#0284c7,#22d3ee);color:#ffffff;">
                          <h2 style="margin:0;font-size:22px;line-height:1.2;">Bienvenue sur EduGame, %s !</h2>
                          <p style="margin:8px 0 0 0;font-size:14px;opacity:.95;">%s vient de créer ton compte joueur.</p>
                        </div>
                        <div style="padding:22px 24px;">
                          <p style="margin:0 0 12px 0;">Bonjour %s,</p>
                          <p style="margin:0 0 16px 0;">
                            Voici tes identifiants de connexion pour accéder à tes jeux et suivre ta progression :
                          </p>
                          <div style="margin:0 0 18px 0;padding:14px 16px;border:1px solid #dbeafe;background:#eff6ff;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:2px;">Adresse e-mail</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                            <span style="display:block;font-size:12px;color:#6b7280;margin:10px 0 2px 0;">Mot de passe</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                          </div>
                          <a href="%s" style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;padding:11px 16px;border-radius:10px;font-size:14px;">
                            Me connecter
                          </a>
                          <p style="margin:18px 0 0 0;font-size:13px;color:#6b7280;">
                            Conseil : tu peux changer ton mot de passe une fois connecté, dans ton profil.
                          </p>
                          <p style="margin:14px 0 0 0;font-size:13px;color:#6b7280;">
                            À bientôt,<br/>
                            <strong style="color:#374151;">L'équipe EduGame</strong>
                          </p>
                        </div>
                      </div>
                    </div>
                    """.formatted(safeFirstName, safeParentName, safeFirstName, toEmail, plainPassword, loginLink), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(toEmail);
            fallback.setSubject("Ton compte EduGame a été créé");
            fallback.setText(
                    GREETING_PREFIX + safeFirstName + ",\n\n" +
                            safeParentName + " vient de créer ton compte joueur EduGame.\n\n" +
                            "Adresse e-mail : " + toEmail + "\n" +
                            "Mot de passe : " + plainPassword + "\n\n" +
                            "Connexion : " + loginLink + "\n\n" +
                            "À bientôt,\n" +
                            "L'équipe EduGame"
            );
            try {
                mailSender.send(fallback);
            } catch (Exception ignored) {
                log.warn("Échec d'envoi de l'email de création de compte joueur vers {}", toEmail, ignored);
            }
        }
    }

    @Async("taskExecutor")
    public void sendStaffAccountCreatedEmail(String toEmail, String firstName, String plainPassword, String roleLabel) {
        String safeFirstName = (firstName == null || firstName.isBlank()) ? "Bonjour" : firstName.trim();
        String safeRoleLabel = (roleLabel == null || roleLabel.isBlank()) ? "collaborateur" : roleLabel.trim();
        String loginLink = frontendUrl + "/login";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8);
            helper.setTo(toEmail);
            helper.setSubject("Votre compte " + safeRoleLabel + " EduGame a été créé");
            helper.setText("""
                    <div style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                        <div style="padding:20px 24px;background:linear-gradient(90deg,#8b5cf6,#06b6d4);color:#ffffff;">
                          <h2 style="margin:0;font-size:22px;line-height:1.2;">Bienvenue sur EduGame</h2>
                          <p style="margin:8px 0 0 0;font-size:14px;opacity:.95;">Un administrateur vient de créer votre compte %s.</p>
                        </div>
                        <div style="padding:22px 24px;">
                          <p style="margin:0 0 12px 0;">Bonjour %s,</p>
                          <p style="margin:0 0 16px 0;">
                            Voici vos identifiants de connexion pour accéder à votre espace %s :
                          </p>
                          <div style="margin:0 0 18px 0;padding:14px 16px;border:1px solid #dbeafe;background:#eff6ff;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:2px;">Adresse e-mail</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                            <span style="display:block;font-size:12px;color:#6b7280;margin:10px 0 2px 0;">Mot de passe</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                          </div>
                          <a href="%s" style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;padding:11px 16px;border-radius:10px;font-size:14px;">
                            Me connecter
                          </a>
                          <p style="margin:18px 0 0 0;font-size:13px;color:#6b7280;">
                            Conseil : vous pouvez changer votre mot de passe une fois connecté, dans votre profil.
                          </p>
                          <p style="margin:14px 0 0 0;font-size:13px;color:#6b7280;">
                            Cordialement,<br/>
                            <strong style="color:#374151;">L'équipe EduGame</strong>
                          </p>
                        </div>
                      </div>
                    </div>
                    """.formatted(safeRoleLabel, safeFirstName, safeRoleLabel, toEmail, plainPassword, loginLink), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(toEmail);
            fallback.setSubject("Votre compte " + safeRoleLabel + " EduGame a été créé");
            fallback.setText(
                    GREETING_PREFIX + safeFirstName + ",\n\n" +
                            "Un administrateur vient de créer votre compte " + safeRoleLabel + " EduGame.\n\n" +
                            "Adresse e-mail : " + toEmail + "\n" +
                            "Mot de passe : " + plainPassword + "\n\n" +
                            "Connexion : " + loginLink + "\n\n" +
                            "Cordialement,\n" +
                            "L'équipe EduGame"
            );
            try {
                mailSender.send(fallback);
            } catch (Exception ignored) {
                log.warn("Échec d'envoi de l'email de création de compte {} vers {}", safeRoleLabel, toEmail, ignored);
            }
        }
    }

    @Async("taskExecutor")
    public void sendGameRejectedEmail(String toEmail, String gameTitle, String refusalReason) {
        String safeTitle = (gameTitle == null || gameTitle.isBlank()) ? DEFAULT_GAME_TITLE : gameTitle;
        String safeReason = (refusalReason == null || refusalReason.isBlank())
                ? DEFAULT_NO_DETAILS_MESSAGE
                : refusalReason.trim();
        String gamesLink = frontendUrl + GAMES_MANAGE_PATH;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8);
            helper.setTo(toEmail);
            helper.setSubject("Votre demande de jeu a été refusée");
            helper.setText("""
                    <div style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                        <div style="padding:20px 24px;background:linear-gradient(90deg,#ef4444,#f97316);color:#ffffff;">
                          <h2 style="margin:0;font-size:22px;line-height:1.2;">Demande refusée</h2>
                          <p style="margin:8px 0 0 0;font-size:14px;opacity:.95;">Merci de corriger le jeu puis de le soumettre à nouveau.</p>
                        </div>
                        <div style="padding:22px 24px;">
                          <p style="margin:0 0 12px 0;">Bonjour,</p>
                          <p style="margin:0 0 16px 0;">
                            Votre demande de validation a été <strong>refusée</strong> après revue.
                          </p>
                          <div style="margin:0 0 10px 0;padding:12px 14px;border:1px solid #fee2e2;background:#fff1f2;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:4px;">Jeu concerné</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                          </div>
                          <div style="margin:0 0 18px 0;padding:12px 14px;border:1px solid #fed7aa;background:#fff7ed;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:6px;">Motif de refus</span>
                            <p style="margin:0;font-size:14px;line-height:1.45;color:#7c2d12;white-space:pre-wrap;">%s</p>
                          </div>
                          <a href="%s" style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;padding:11px 16px;border-radius:10px;font-size:14px;">
                            Corriger mon jeu
                          </a>
                        </div>
                      </div>
                    </div>
                    """.formatted(safeTitle, safeReason, gamesLink), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(toEmail);
            fallback.setSubject("Votre demande de jeu a été refusée");
            fallback.setText(
                    "Bonjour,\n\n" +
                            "Votre demande de jeu a été refusée.\n\n" +
                            "Jeu concerné : " + safeTitle + "\n" +
                            "Motif : " + safeReason + "\n\n" +
                            "Corriger le jeu : " + gamesLink + "\n\n" +
                            "Cordialement,\n" +
                            "L'équipe EduGame"
            );
            try {
                mailSender.send(fallback);
            } catch (Exception ignored) {
                log.warn("Échec d'envoi de l'email de refus du jeu vers {}", toEmail, ignored);
            }
        }
    }

    @Async("taskExecutor")
    public void sendGameDeactivatedEmail(String toEmail, String gameTitle, String deactivationReason) {
        String safeTitle = (gameTitle == null || gameTitle.isBlank()) ? DEFAULT_GAME_TITLE : gameTitle;
        String safeReason = (deactivationReason == null || deactivationReason.isBlank())
                ? DEFAULT_NO_DETAILS_MESSAGE
                : deactivationReason.trim();
        String gamesLink = frontendUrl + GAMES_MANAGE_PATH;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8);
            helper.setTo(toEmail);
            helper.setSubject("Votre jeu a été désactivé suite à un signalement");
            helper.setText("""
                    <div style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                        <div style="padding:20px 24px;background:linear-gradient(90deg,#ef4444,#f97316);color:#ffffff;">
                          <h2 style="margin:0;font-size:22px;line-height:1.2;">Jeu désactivé</h2>
                          <p style="margin:8px 0 0 0;font-size:14px;opacity:.95;">Un signalement joueur a été validé par l'administration.</p>
                        </div>
                        <div style="padding:22px 24px;">
                          <p style="margin:0 0 12px 0;">Bonjour,</p>
                          <p style="margin:0 0 16px 0;">
                            Suite à un signalement d'un joueur jugé fondé, votre jeu a été <strong>désactivé</strong>
                            et n'est plus accessible aux joueurs en attendant votre correction.
                          </p>
                          <div style="margin:0 0 10px 0;padding:12px 14px;border:1px solid #fee2e2;background:#fff1f2;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:4px;">Jeu concerné</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                          </div>
                          <div style="margin:0 0 18px 0;padding:12px 14px;border:1px solid #fed7aa;background:#fff7ed;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:6px;">Détails de la désactivation</span>
                            <p style="margin:0;font-size:14px;line-height:1.45;color:#7c2d12;white-space:pre-wrap;">%s</p>
                          </div>
                          <a href="%s" style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;padding:11px 16px;border-radius:10px;font-size:14px;">
                            Corriger mon jeu
                          </a>
                          <p style="margin:18px 0 0 0;font-size:13px;color:#6b7280;">
                            Une fois les corrections faites, vous pourrez demander une nouvelle validation.
                          </p>
                        </div>
                      </div>
                    </div>
                    """.formatted(safeTitle, safeReason, gamesLink), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(toEmail);
            fallback.setSubject("Votre jeu a été désactivé suite à un signalement");
            fallback.setText(
                    "Bonjour,\n\n" +
                            "Suite à un signalement joueur validé par l'administration, votre jeu a été désactivé.\n\n" +
                            "Jeu concerné : " + safeTitle + "\n" +
                            "Détails : " + safeReason + "\n\n" +
                            "Corriger le jeu : " + gamesLink + "\n\n" +
                            "Cordialement,\n" +
                            "L'équipe EduGame"
            );
            try {
                mailSender.send(fallback);
            } catch (Exception ignored) {
                log.warn("Échec d'envoi de l'email de désactivation du jeu vers {}", toEmail, ignored);
            }
        }
    }

    /**
     * Email envoyé à l'éducateur lorsque l'admin accepte sa demande de réactivation :
     * le jeu corrigé redevient actif et visible des joueurs.
     */
    @Async("taskExecutor")
    public void sendReactivationAcceptedEmail(String toEmail, String gameTitle) {
        String safeTitle = (gameTitle == null || gameTitle.isBlank()) ? DEFAULT_GAME_TITLE : gameTitle;
        String gamesLink = frontendUrl + GAMES_MANAGE_PATH;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8);
            helper.setTo(toEmail);
            helper.setSubject("Votre jeu a été réactivé");
            helper.setText("""
                    <div style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                        <div style="padding:20px 24px;background:linear-gradient(90deg,#8b5cf6,#06b6d4);color:#ffffff;">
                          <h2 style="margin:0;font-size:22px;line-height:1.2;">Jeu réactivé avec succès</h2>
                          <p style="margin:8px 0 0 0;font-size:14px;opacity:.95;">Votre correction a été validée par l'administration.</p>
                        </div>
                        <div style="padding:22px 24px;">
                          <p style="margin:0 0 12px 0;">Bonjour,</p>
                          <p style="margin:0 0 16px 0;">
                            Bonne nouvelle ! Votre demande de réactivation a été <strong>acceptée</strong>.
                            Le jeu est de nouveau <strong>actif</strong> et visible par les joueurs.
                          </p>
                          <div style="margin:0 0 18px 0;padding:12px 14px;border:1px solid #dbeafe;background:#eff6ff;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:4px;">Jeu concerné</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                          </div>
                          <a href="%s" style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;padding:11px 16px;border-radius:10px;font-size:14px;">
                            Voir mes jeux
                          </a>
                        </div>
                      </div>
                    </div>
                    """.formatted(safeTitle, gamesLink), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(toEmail);
            fallback.setSubject("Votre jeu a été réactivé");
            fallback.setText(
                    "Bonjour,\n\n" +
                            "Votre demande de réactivation a été acceptée. Le jeu est de nouveau actif.\n\n" +
                            "Jeu concerné : " + safeTitle + "\n\n" +
                            "Voir mes jeux : " + gamesLink + "\n\n" +
                            "Cordialement,\n" +
                            "L'équipe EduGame"
            );
            try {
                mailSender.send(fallback);
            } catch (Exception ignored) {
                log.warn("Échec d'envoi de l'email de réactivation acceptée vers {}", toEmail, ignored);
            }
        }
    }

    /**
     * Email envoyé à l'éducateur lorsque l'admin refuse sa demande de réactivation :
     * le jeu reste désactivé, avec le motif de refus fourni.
     */
    @Async("taskExecutor")
    public void sendReactivationRejectedEmail(String toEmail, String gameTitle, String rejectionReason) {
        String safeTitle = (gameTitle == null || gameTitle.isBlank()) ? DEFAULT_GAME_TITLE : gameTitle;
        String safeReason = (rejectionReason == null || rejectionReason.isBlank())
                ? DEFAULT_NO_DETAILS_MESSAGE
                : rejectionReason.trim();
        String gamesLink = frontendUrl + GAMES_MANAGE_PATH;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8);
            helper.setTo(toEmail);
            helper.setSubject("Votre demande de réactivation a été refusée");
            helper.setText("""
                    <div style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                        <div style="padding:20px 24px;background:linear-gradient(90deg,#ef4444,#f97316);color:#ffffff;">
                          <h2 style="margin:0;font-size:22px;line-height:1.2;">Réactivation refusée</h2>
                          <p style="margin:8px 0 0 0;font-size:14px;opacity:.95;">Le jeu reste désactivé en attendant une nouvelle correction.</p>
                        </div>
                        <div style="padding:22px 24px;">
                          <p style="margin:0 0 12px 0;">Bonjour,</p>
                          <p style="margin:0 0 16px 0;">
                            Votre demande de réactivation a été <strong>refusée</strong> après revue.
                            Le jeu reste désactivé et invisible des joueurs.
                          </p>
                          <div style="margin:0 0 10px 0;padding:12px 14px;border:1px solid #fee2e2;background:#fff1f2;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:4px;">Jeu concerné</span>
                            <strong style="font-size:15px;color:#111827;">%s</strong>
                          </div>
                          <div style="margin:0 0 18px 0;padding:12px 14px;border:1px solid #fed7aa;background:#fff7ed;border-radius:10px;">
                            <span style="display:block;font-size:12px;color:#6b7280;margin-bottom:6px;">Motif du refus</span>
                            <p style="margin:0;font-size:14px;line-height:1.45;color:#7c2d12;white-space:pre-wrap;">%s</p>
                          </div>
                          <a href="%s" style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;padding:11px 16px;border-radius:10px;font-size:14px;">
                            Corriger à nouveau mon jeu
                          </a>
                          <p style="margin:18px 0 0 0;font-size:13px;color:#6b7280;">
                            Vous pouvez modifier le jeu puis redemander une réactivation.
                          </p>
                        </div>
                      </div>
                    </div>
                    """.formatted(safeTitle, safeReason, gamesLink), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(toEmail);
            fallback.setSubject("Votre demande de réactivation a été refusée");
            fallback.setText(
                    "Bonjour,\n\n" +
                            "Votre demande de réactivation a été refusée. Le jeu reste désactivé.\n\n" +
                            "Jeu concerné : " + safeTitle + "\n" +
                            "Motif : " + safeReason + "\n\n" +
                            "Corriger le jeu : " + gamesLink + "\n\n" +
                            "Cordialement,\n" +
                            "L'équipe EduGame"
            );
            try {
                mailSender.send(fallback);
            } catch (Exception ignored) {
                log.warn("Échec d'envoi de l'email de réactivation refusée vers {}", toEmail, ignored);
            }
        }
    }

    /**
     * Email envoyé à un utilisateur lorsque l'admin suspend son compte : il ne peut plus se
     * connecter (message dédié affiché côté login) tant que le compte n'est pas réactivé.
     */
    @Async("taskExecutor")
    public void sendAccountSuspendedEmail(String toEmail, String firstName) {
        String safeName = (firstName == null || firstName.isBlank()) ? "" : firstName.trim();
        String greeting = safeName.isEmpty() ? "Bonjour," : GREETING_PREFIX + safeName + ",";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8);
            helper.setTo(toEmail);
            helper.setSubject("Votre compte EduGame a été suspendu");
            helper.setText("""
                    <div style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                        <div style="padding:20px 24px;background:linear-gradient(90deg,#ef4444,#f97316);color:#ffffff;">
                          <h2 style="margin:0;font-size:22px;line-height:1.2;">Compte suspendu</h2>
                          <p style="margin:8px 0 0 0;font-size:14px;opacity:.95;">Votre accès à EduGame a été temporairement suspendu.</p>
                        </div>
                        <div style="padding:22px 24px;">
                          <p style="margin:0 0 12px 0;">%s</p>
                          <p style="margin:0 0 16px 0;">
                            Votre compte a été <strong>suspendu</strong> par l'administration. Vous ne pouvez plus vous
                            connecter à EduGame tant que votre compte n'a pas été réactivé.
                          </p>
                          <div style="margin:0 0 18px 0;padding:12px 14px;border:1px solid #fed7aa;background:#fff7ed;border-radius:10px;">
                            <p style="margin:0;font-size:14px;line-height:1.45;color:#7c2d12;">
                              Si vous pensez qu'il s'agit d'une erreur, ou pour toute question concernant cette suspension,
                              merci de contacter notre support.
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                    """.formatted(greeting), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(toEmail);
            fallback.setSubject("Votre compte EduGame a été suspendu");
            fallback.setText(
                    greeting + "\n\n" +
                            "Votre compte a été suspendu par l'administration. Vous ne pouvez plus vous connecter à EduGame " +
                            "tant que votre compte n'a pas été réactivé.\n\n" +
                            "Si vous pensez qu'il s'agit d'une erreur, merci de contacter notre support.\n\n" +
                            "Cordialement,\n" +
                            "L'équipe EduGame"
            );
            try {
                mailSender.send(fallback);
            } catch (Exception ignored) {
                log.warn("Échec d'envoi de l'email de suspension de compte vers {}", toEmail, ignored);
            }
        }
    }
}
