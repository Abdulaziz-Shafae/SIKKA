package com.example.sikka.Service;

import com.example.sikka.DtoIn.RegisterDtoIn;
import com.example.sikka.Model.Account;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.VisitorProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegistrationEmailVerificationTest {
    @Test
    void registrationCreatesUnverifiedAccountAndSendsEmailOnly() {
        AccountRepository accounts = mock(AccountRepository.class);
        VisitorProfileRepository profiles = mock(VisitorProfileRepository.class);
        VerificationService verification = mock(VerificationService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("password123")).thenReturn("secure-hash");
        when(verification.sendVerificationCode(any(Account.class))).thenReturn(true);
        RegisterDtoIn request = new RegisterDtoIn();
        request.setName("Visitor");
        request.setEmail("visitor@example.com");
        request.setPassword("password123");
        AccountService service = new AccountService(accounts, profiles, encoder, verification);

        boolean delivered = service.register(request);

        assertTrue(delivered);
        org.mockito.ArgumentCaptor<Account> saved = org.mockito.ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(saved.capture());
        assertFalse(saved.getValue().getEmailVerified());
        assertEquals("secure-hash", saved.getValue().getPasswordHash());
        verify(verification).sendVerificationCode(same(saved.getValue()));
        verify(profiles).save(any());
    }

    @Test
    void failedEmailDeliveryKeepsAccountUnverifiedSoResendCanWork() {
        AccountRepository accounts = mock(AccountRepository.class);
        VisitorProfileRepository profiles = mock(VisitorProfileRepository.class);
        VerificationService verification = mock(VerificationService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("secure-hash");
        when(verification.sendVerificationCode(any(Account.class))).thenReturn(false);
        RegisterDtoIn request = new RegisterDtoIn();
        request.setName("Visitor");
        request.setEmail("visitor@example.com");
        request.setPassword("password123");

        assertFalse(new AccountService(accounts, profiles, encoder, verification).register(request));
        org.mockito.ArgumentCaptor<Account> saved = org.mockito.ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(saved.capture());
        assertFalse(saved.getValue().getEmailVerified());
    }
}
