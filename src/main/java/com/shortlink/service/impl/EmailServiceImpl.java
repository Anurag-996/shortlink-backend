package com.shortlink.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.shortlink.exception.EmailSendingException;
import com.shortlink.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Implementation of EmailService managing template interpolation and asynchronous SMTP dispatch.
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String VERIFICATION_TEMPLATE = "verification.html";
    private static final String ACCOUNT_ACTIVATION_TEMPLATE = "account-activation.html";
    private static final String WELCOME_TEMPLATE = "welcome.html";
    private static final String PASSWORD_RESET_TEMPLATE = "password-reset.html";
    private static final String ACCOUNT_DELETION_TEMPLATE = "account-deletion.html";

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.logo-url}")
    private String configuredLogoUrl;

    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, String> variables) {
        try {
            String html = loadTemplate(templateName);

            html = html.replace("{{logoUrl}}", configuredLogoUrl);
            html = html.replace("{{frontendUrl}}", frontendUrl);

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                html = html.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("Email [{}] successfully sent to {}", subject, to);
        } catch (MessagingException | IOException | MailException e) {
            log.error("Failed to send email [{}] to {}", subject, to, e);
            throw new EmailSendingException("Failed to send email to " + to, e);
        }
    }

    @Override
    public void sendVerificationEmail(String to, String name, String verificationLink) {
        sendHtmlEmail(
                to,
                "Verify Your TinyClick Account",
                VERIFICATION_TEMPLATE,
                Map.of(
                        "name", name,
                        "verificationLink", verificationLink
                )
        );
    }

    @Override
    public void sendAccountActivationEmail(String to, String name, String activationLink) {
        sendHtmlEmail(
                to,
                "Reactivate Your TinyClick Account",
                ACCOUNT_ACTIVATION_TEMPLATE,
                Map.of(
                        "name", name,
                        "activationLink", activationLink
                )
        );
    }

    @Override
    public void sendWelcomeEmail(String to, String name) {
        sendHtmlEmail(
                to,
                "Welcome to TinyClick 🚀",
                WELCOME_TEMPLATE,
                Map.of(
                        "name", name,
                        "loginLink", frontendUrl + "/login"
                )
        );
    }

    @Override
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        sendHtmlEmail(
                to,
                "Reset Your TinyClick Password",
                PASSWORD_RESET_TEMPLATE,
                Map.of(
                        "name", name,
                        "resetLink", resetLink
                )
        );
    }

    @Override
    public void sendAccountDeletionEmail(String to, String name, String scheduledDate) {
        sendHtmlEmail(
                to,
                "Account Deletion Scheduled - TinyClick",
                ACCOUNT_DELETION_TEMPLATE,
                Map.of(
                        "name", name,
                        "scheduledDate", scheduledDate
                )
        );
    }

    private String loadTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/email/" + templateName);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
