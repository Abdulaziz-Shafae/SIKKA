package com.example.sikka.Service;

import com.example.sikka.Model.Account;
import com.example.sikka.Repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailOnlyAccountFlowTest {
    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void verifyingEmailActivatesAccountAndDoesNotContactWhatsapp() throws Exception {
        AccountRepository accounts = mock(AccountRepository.class);
        EmailService email = mock(EmailService.class);
        WhatsAppService whatsapp = mock(WhatsAppService.class);
        Account account = new Account();
        account.setEmail("test@example.com");
        account.setName("New visitor");
        account.setStatus("ACTIVE");
        account.setEmailVerified(false);
        account.setVerificationTokenHash(hash("123456"));
        account.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(accounts.findAccountByEmail("test@example.com")).thenReturn(account);
        VerificationService service = new VerificationService(mock(PasswordEncoder.class), accounts, email, whatsapp);
        assertFalse(account.isEnabled());
        service.verify("test@example.com", "123456");
        assertTrue(account.getEmailVerified());
        assertTrue(account.isEnabled());
        assertNull(account.getVerificationTokenHash());
        verify(accounts).save(account);
        verify(email).sendEmail(eq("test@example.com"), anyString(), anyString());
        verifyNoInteractions(whatsapp);
    }

    @Test
    void passwordResetDeliveryUsesOnlyEmailEvenWhenLegacyPhoneIsVerified() {
        AccountRepository accounts = mock(AccountRepository.class);
        EmailService email = mock(EmailService.class);
        WhatsAppService whatsapp = mock(WhatsAppService.class);
        Account account = new Account();
        account.setEmail("legacy@example.com");
        account.setName("Existing user");
        account.setPhone("966500000000");
        account.setPhoneVerified(true);
        when(accounts.findAccountByEmail(account.getEmail())).thenReturn(account);
        VerificationService service = new VerificationService(mock(PasswordEncoder.class), accounts, email, whatsapp);
        service.sendResetCode(account.getEmail());
        assertNotNull(account.getResetTokenHash());
        verify(email).sendEmail(eq(account.getEmail()), anyString(), anyString());
        verifyNoInteractions(whatsapp);
    }

    @Test
    void unverifiedAccountCannotAuthenticateAsEnabled() {
        Account account = new Account();
        account.setStatus("ACTIVE");
        account.setEmailVerified(false);
        assertFalse(account.isEnabled());
        account.setEmailVerified(true);
        assertTrue(account.isEnabled());
        account.setStatus("BLOCKED");
        assertFalse(account.isEnabled());
    }
}
