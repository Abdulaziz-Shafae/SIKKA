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