package com.securehandoff.securehandoff.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    public void sendTrusteeEscalationAlert(String trusteeEmail, String ownerEmail, Long releaseRequestId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(trusteeEmail);
            message.setSubject("SecureHandoff: Action needed — " + ownerEmail + " has missed a check-in");
            message.setText(
                    "You've been designated as a trustee for " + ownerEmail + " on SecureHandoff.\n\n"
                    + "They have missed their scheduled check-in. If you believe this is a genuine emergency "
                    + "(and not a false alarm), please log in and confirm release request #" + releaseRequestId
                    + " (POST /api/release/" + releaseRequestId + "/confirm).\n\n"
                    + "Access will only be granted once enough trustees independently confirm."
            );
            mailSender.send(message);
        } catch (Exception e) {
            // In dev without real SMTP credentials this will fail — log rather than crash
            // the escalation flow, since the confirmation can still happen via the app/API.
            log.warn("Failed to send trustee alert email to {}: {}", trusteeEmail, e.getMessage());
        }
    }

    public void sendInviteEmail(String trusteeEmail, String ownerName, String inviteToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(trusteeEmail);
            message.setSubject("SecureHandoff: " + ownerName + " has invited you as a trustee");
            message.setText(
                    "Register or log in to SecureHandoff with this email address, then accept using this invite code:\n\n"
                    + inviteToken
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send invite email to {}: {}", trusteeEmail, e.getMessage());
        }
    }
}
