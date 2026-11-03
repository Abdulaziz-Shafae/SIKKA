package com.example.sikka.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${sikka.email.from:${spring.mail.username:}}")
    private String from;

    @Value("${sikka.email.brevo-api-key:}")
    private String brevoApiKey;

    private RestClient emailApiClient = createEmailApiClient();

    private static RestClient createEmailApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().baseUrl("https://api.brevo.com").requestFactory(factory).build();
    }

    // Send the generated recap image as a private PNG attachment, not a public visitor-data URL.
    public boolean sendEmailWithImage(String to, String subject, String text, byte[] png) {
        if (png == null || png.length == 0 || png.length > 8_000_000) return false;
        try {
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                java.util.Map<String, Object> body = java.util.Map.of("sender", java.util.Map.of("email", from, "name", "Sikka"), "to", java.util.List.of(java.util.Map.of("email", to)), "subject", subject, "textContent", text, "attachment", java.util.List.of(java.util.Map.of("name", "sikka-visit-summary.png", "content", java.util.Base64.getEncoder().encodeToString(png))));
                emailApiClient.post().uri("/v3/smtp/email").header("api-key", brevoApiKey).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
                return true;
            }
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, "Sikka");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            helper.addAttachment("sikka-visit-summary.png", new org.springframework.core.io.ByteArrayResource(png), "image/png");
            mailSender.send(message);
            return true;
        } catch (Exception exception) {
            log.warn("Failed to send visit recap image: {}", exception.getClass().getSimpleName());
            return false;
        }
    }

    // never throws: a failed email must not break the request that triggered it
    public boolean sendEmail(String to, String subject, String text) {
        try {
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                emailApiClient.post().uri("/v3/smtp/email").header("api-key", brevoApiKey).contentType(MediaType.APPLICATION_JSON).body(Map.of("sender", Map.of("email", from, "name", "Sikka"), "to", List.of(Map.of("email", to)), "subject", subject, "textContent", text)).retrieve().toBodilessEntity();
                return true;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}