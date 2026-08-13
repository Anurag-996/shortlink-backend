package com.shortlink.service;

import java.util.Map;

// Service interface for sending transactional emails across the TinyClick platform.
public interface EmailService {

    void sendHtmlEmail(String to, String subject, String templateName, Map<String, String> variables);

    void sendVerificationEmail(String to, String name, String verificationLink);

    void sendAccountActivationEmail(String to, String name, String activationLink);

    void sendWelcomeEmail(String to, String name);

    void sendPasswordResetEmail(String to, String name, String resetLink);

    void sendAccountDeletionEmail(String to, String name, String scheduledDate);
}
