package com.securehandoff.securehandoff.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationService {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;

    public NotificationService(
            @Value("${app.mail.api-key:}") String apiKey,
            @Value("${app.mail.from:}") String fromEmail) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public void sendTrusteeEscalationAlert(String trusteeEmail, String ownerEmail, Long releaseRequestId) {
        send(trusteeEmail,
                "SecureHandoff: Action needed — " + ownerEmail + " has missed a check-in",
                "You've been designated as a trustee for " + ownerEmail + " on SecureHandoff.\n\n"
                        + "They have missed their scheduled check-in. If you believe this is a genuine emergency "
                        + "(and not a false alarm), please log in and confirm release request #" + releaseRequestId
                        + " (POST /api/release/" + releaseRequestId + "/confirm).\n\n"
                        + "Access will only be granted once enough trustees independently confirm.");
    }

    public void sendInviteEmail(String trusteeEmail, String ownerName, String inviteToken) {
        send(trusteeEmail,
                "SecureHandoff: " + ownerName + " has invited you as a trustee",
                "Register or log in to SecureHandoff with this email address, then accept using this invite code:\n\n"
                        + inviteToken);
    }

    /** Sends through Brevo's HTTPS API. Failures are logged and never break the calling flow. */
    private void send(String to, String subject, String text) {
        if (apiKey.isBlank() || fromEmail.isBlank()) {
            log.warn("Email is not configured (app.mail.api-key / app.mail.from); skipping email to {}", to);
            return;
        }
        try {
            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", "SecureHandoff", "email", fromEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "textContent", text);

            restClient.post()
                    .uri(BREVO_URL)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}