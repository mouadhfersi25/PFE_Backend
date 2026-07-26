package com.stage.auth.authbackend.service.auth;

import lombok.RequiredArgsConstructor;
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
public class EmailService {

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
            // TODO: logger proprement (logger.warn(...)) si tu ajoutes un logger
            System.err.println("Échec d'envoi de l'email de vérification : " + e.getMessage());
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
            System.err.println("Échec d'envoi de l'email de réinitialisation : " + e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendGameApprovedEmail(String toEmail, String gameTitle) {
        String safeTitle = (gameTitle == null || gameTitle.isBlank()) ? "Votre jeu" : gameTitle;
        String gamesLink = frontendUrl + "/educator/games/manage";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
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
            } catch (MailException ignored) {
                System.err.println("Échec d'envoi de l'email d'approbation du jeu : " + ignored.getMessage());
            }
        }
    }

    @Async("taskExecutor")
    public void sendGameRejectedEmail(String toEmail, String gameTitle, String refusalReason) {
        String safeTitle = (gameTitle == null || gameTitle.isBlank()) ? "Votre jeu" : gameTitle;
        String safeReason = (refusalReason == null || refusalReason.isBlank())
                ? "Aucun détail supplémentaire n'a été fourni."
                : refusalReason.trim();
        String gamesLink = frontendUrl + "/educator/games/manage";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
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
            } catch (MailException ignored) {
                System.err.println("Échec d'envoi de l'email de refus du jeu : " + ignored.getMessage());
            }
        }
    }
}
