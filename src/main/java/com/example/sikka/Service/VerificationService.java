package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Account;
import com.example.sikka.Repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final int CODE_MINUTES = 10;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final SecureRandom random = new SecureRandom();

    // creates a new code, stores only its hash, and sends the raw code to the user
    public void sendVerificationCode(Account account) {
        String code = newCode();
        account.setVerificationTokenHash(hash(code));
        account.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(CODE_MINUTES));
        accountRepository.save(account);

        emailService.sendEmail(account.getEmail(), "Sikka verification code", "Your Sikka verification code is " + code + ". It expires in " + CODE_MINUTES + " minutes.");
    }

    // resend: unknown email is ignored silently (we do not reveal which emails exist)
    public void resendVerificationCode(String email) {
        Account account = accountRepository.findAccountByEmail(email);
        if (account == null || Boolean.TRUE.equals(account.getEmailVerified()))
            return;

        // a code sent less than a minute ago is still fresh: ask the user to wait
        if (account.getVerificationExpiresAt() != null && account.getVerificationExpiresAt().isAfter(LocalDateTime.now().plusMinutes(CODE_MINUTES - 1)))
            throw new ApiException("please wait a minute before requesting another code");

        sendVerificationCode(account);
    }

    public void verify(String email, String code) {
        Account account = accountRepository.findAccountByEmail(email);
        if (account == null || account.getVerificationTokenHash() == null || account.getVerificationExpiresAt() == null || account.getVerificationExpiresAt().isBefore(LocalDateTime.now()) || !MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.UTF_8), account.getVerificationTokenHash().getBytes(StandardCharsets.UTF_8)))
            throw new ApiException("invalid or expired code");

        account.setEmailVerified(true);
        account.setVerificationTokenHash(null);
        account.setVerificationExpiresAt(null);
        accountRepository.save(account);
    }
    // forgot password: unknown email is ignored silently (we do not reveal which emails exist)
    public void sendResetCode(String email) {
        Account account = accountRepository.findAccountByEmail(email);
        if (account == null)
            return;

        if (account.getResetExpiresAt() != null && account.getResetExpiresAt().isAfter(LocalDateTime.now().plusMinutes(CODE_MINUTES - 1)))
            throw new ApiException("please wait a minute before requesting another code");

        String code = newCode();
        account.setResetTokenHash(hash(code));
        account.setResetExpiresAt(LocalDateTime.now().plusMinutes(CODE_MINUTES));
        accountRepository.save(account);

        deliver(account, "Sikka password reset code", "Your Sikka password reset code is " + code + ". It expires in " + CODE_MINUTES + " minutes.");
    }

    public void resetPassword(String email, String code, String newPassword) {
        Account account = accountRepository.findAccountByEmail(email);
        if (account == null || account.getResetTokenHash() == null || account.getResetExpiresAt() == null || account.getResetExpiresAt().isBefore(LocalDateTime.now()) || !MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.UTF_8), account.getResetTokenHash().getBytes(StandardCharsets.UTF_8)))
            throw new ApiException("invalid or expired code");

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setResetTokenHash(null);
        account.setResetExpiresAt(null);
        accountRepository.save(account);
    }
    // WhatsApp first (if the user gave a phone); email is the fallback
    private void deliver(Account account, String subject, String text) {
        boolean sent = false;
        if (account.getPhone() != null && !account.getPhone().isBlank())
            sent = whatsAppService.sendMessage(account.getPhone(), text);
        if (!sent)
            emailService.sendEmail(account.getEmail(), subject, text);
    }

    private String newCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException("hashing error");
        }
    }
}