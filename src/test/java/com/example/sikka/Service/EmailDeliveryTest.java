package com.example.sikka.Service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EmailDeliveryTest {
    @Test
    void deliversThroughHttpsWithoutCallingSmtp() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        EmailService service = new EmailService(smtp);
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "emailApiClient", builder.build());
        ReflectionTestUtils.setField(service, "brevoApiKey", "test-key");
        ReflectionTestUtils.setField(service, "from", "sender@example.test");
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andExpect(method(HttpMethod.POST)).andExpect(header("api-key", "test-key")).andExpect(content().string(org.hamcrest.Matchers.containsString("verification code"))).andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"test\"}"));
        assertTrue(service.sendEmail("recipient@example.test", "verification code", "test content"));
        verifyNoInteractions(smtp);
        server.verify();
    }

    @Test
    void reportsRejectedHttpsDeliveryWithoutFallingBackAndDuplicatingMessages() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        EmailService service = new EmailService(smtp);
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "emailApiClient", builder.build());
        ReflectionTestUtils.setField(service, "brevoApiKey", "test-key");
        ReflectionTestUtils.setField(service, "from", "sender@example.test");
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertFalse(service.sendEmail("recipient@example.test", "verification code", "test content"));
        verifyNoInteractions(smtp);
        server.verify();
    }

    @Test
    void reportsSmtpFailureInsteadOfClaimingDelivery() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        EmailService service = new EmailService(smtp);
        ReflectionTestUtils.setField(service, "from", "sender@example.test");
        doThrow(new MailSendException("unavailable")).when(smtp).send(any(org.springframework.mail.SimpleMailMessage.class));
        assertFalse(service.sendEmail("recipient@example.test", "verification code", "test content"));
    }
}
