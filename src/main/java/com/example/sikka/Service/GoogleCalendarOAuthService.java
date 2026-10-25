package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.*;
import com.example.sikka.Repository.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoogleCalendarOAuthService {
    private final GoogleCalendarConnectionRepository connectionRepository;
    private final GoogleCalendarEventRepository eventRepository;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final VisitPlanRepository visitPlanRepository;
    private final RestClient client = RestClient.create();
    private final SecureRandom random = new SecureRandom();

    @Value("${sikka.google.client-id:}")
    private String clientId;
    @Value("${sikka.google.client-secret:}")
    private String clientSecret;
    @Value("${sikka.google.redirect-uri:http://localhost:8080/api/v1/google-calendar/oauth/callback}")
    private String redirectUri;
    @Value("${sikka.google.encryption-key:}")
    private String encryptionKey;

    private void configured() {
        if (clientId.isBlank() || clientSecret.isBlank() || encryptionKey.isBlank()) {
            throw new ApiException("Google Calendar OAuth is not configured");
        }
        if (Base64.getDecoder().decode(encryptionKey).length != 32) {
            throw new ApiException("Calendar encryption key must contain 32 bytes");
        }
    }

    public String authorizationUrl(Integer accountId, HttpSession session) {
        configured();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        session.setAttribute("sikka_google_state", state);
        session.setAttribute("sikka_google_account", accountId);
        String scope = "https://www.googleapis.com/auth/calendar.events";
        return "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=" + enc(clientId) + "&redirect_uri=" + enc(redirectUri) + "&scope=" + enc(scope) + "&access_type=offline&prompt=consent&state=" + enc(state);
    }

    public void callback(Integer accountId, String state, String code, HttpSession session) {
        configured();
        Object savedState = session.getAttribute("sikka_google_state");
        Object savedAccount = session.getAttribute("sikka_google_account");
        session.removeAttribute("sikka_google_state");
        session.removeAttribute("sikka_google_account");
        if (!(savedState instanceof String) || !(savedAccount instanceof Integer) || !Objects.equals(savedState, state) || !Objects.equals(savedAccount, accountId)) {
            throw new ApiException("Invalid Google Calendar authorization state");
        }
        if (code == null || code.isBlank()) {
            throw new ApiException("Google authorization code is missing");
        }
        Map<?, ?> tokens = client.post().uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("code=" + enc(code) + "&client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret) + "&redirect_uri=" + enc(redirectUri) + "&grant_type=authorization_code")
                .retrieve().body(Map.class);
        Object refreshToken = tokens == null ? null : tokens.get("refresh_token");
        if (!(refreshToken instanceof String token) || token.isBlank()) {
            throw new ApiException("Google did not return a refresh token");
        }
        GoogleCalendarConnection connection = connectionRepository.findById(accountId).orElseGet(GoogleCalendarConnection::new);
        Account account = accountRepository.findAccountById(accountId);
        if (account == null) throw new ApiException("Account not found");
        connection.setAccount(account);
        connection.setEncryptedRefreshToken(encrypt(token));
        connectionRepository.save(connection);
    }

    public boolean connected(Integer accountId) {
        return connectionRepository.existsById(accountId);
    }

    public void disconnect(Integer accountId) {
        eventRepository.deleteAllByAccount_Id(accountId);
        connectionRepository.deleteById(accountId);
    }

    public String syncVisit(Integer accountId, Integer visitId) {
        configured();
        GoogleCalendarConnection connection = connectionRepository.findById(accountId).orElseThrow(() -> new ApiException("Connect Google Calendar first"));
        Visit visit = visitRepository.findVisitById(visitId);
        if (visit == null) throw new ApiException("Visit not found");
        boolean organizer = visit.getOrganizer() != null && accountId.equals(visit.getOrganizer().getId());
        boolean member = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId) != null;
        if (!organizer && !member) throw new ApiException("You are not a member of this visit");
        if ("CANCELLED".equals(visit.getStatus())) throw new ApiException("Cancelled visits cannot be synced");

        VisitPlan plan = visitPlanRepository.findAllByVisit_IdAndPlanKind(visitId, "GROUP").stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && p.getPlanApproval() != null && "APPROVED".equals(p.getPlanApproval().getStatus()))
                .max(Comparator.comparing(VisitPlan::getCreatedAt)).orElseThrow(() -> new ApiException("An approved group plan is required"));

        List<PlanItem> items = plan.getPlanItems().stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(PlanItem::getStartsAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        LocalTime arrival = visit.getPlannedStartTime() != null ? visit.getPlannedStartTime() : visit.getExpectedArrivalTime();
        LocalDateTime start = arrival != null ? visit.getVisitDate().atTime(arrival) : items.stream().map(PlanItem::getStartsAt).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
        LocalDateTime end = visit.getPlannedEndTime() != null ? visit.getVisitDate().atTime(visit.getPlannedEndTime()) : items.stream().map(PlanItem::getEndsAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        if (start == null || end == null) throw new ApiException("Visit start and end times are required");
        if (!end.isAfter(start) && visit.getPlannedEndTime() != null) end = end.plusDays(1);
        if (!end.isAfter(start)) throw new ApiException("Visit end must be after start");
        String description = "جدول زيارتك من سكة | Sikka\n" + items.stream()
                .filter(p -> p.getStartsAt() != null)
                .map(p -> p.getStartsAt().toLocalTime() + " - " + (p.getTitle() == null ? p.getActivityKind() : p.getTitle()))
                .collect(Collectors.joining("\n"));
        Map<String, Object> event = Map.of(
                "summary", visit.getName() == null ? "زيارة سكة" : visit.getName(),
                "description", description,
                "start", Map.of("dateTime", start.atZone(ZoneId.of("Asia/Riyadh")).toOffsetDateTime().toString(), "timeZone", "Asia/Riyadh"),
                "end", Map.of("dateTime", end.atZone(ZoneId.of("Asia/Riyadh")).toOffsetDateTime().toString(), "timeZone", "Asia/Riyadh")
        );
        String token = accessToken(decrypt(connection.getEncryptedRefreshToken()));
        Optional<GoogleCalendarEvent> existing = eventRepository.findByAccount_IdAndVisit_Id(accountId, visitId);
        Map<?, ?> response;
        if (existing.isPresent()) {
            response = client.patch().uri("https://www.googleapis.com/calendar/v3/calendars/primary/events/" + enc(existing.get().getGoogleEventId()))
                    .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON).body(event).retrieve().body(Map.class);
        } else {
            response = client.post().uri("https://www.googleapis.com/calendar/v3/calendars/primary/events")
                    .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON).body(event).retrieve().body(Map.class);
            if (response == null || !(response.get("id") instanceof String eventId)) throw new ApiException("Google event was not created");
            GoogleCalendarEvent record = new GoogleCalendarEvent();
            record.setAccount(connection.getAccount());
            record.setVisit(visit);
            record.setGoogleEventId(eventId);
            eventRepository.save(record);
        }
        return response != null && response.get("htmlLink") instanceof String link ? link : "Synced";
    }

    private String accessToken(String refreshToken) {
        Map<?, ?> response = client.post().uri("https://oauth2.googleapis.com/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret) + "&refresh_token=" + enc(refreshToken) + "&grant_type=refresh_token")
                .retrieve().body(Map.class);
        if (response == null || !(response.get("access_token") instanceof String token)) throw new ApiException("Google token refresh failed");
        return token;
    }

    private String encrypt(String token) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Base64.getDecoder().decode(encryptionKey), "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            byte[] all = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, all, 0, iv.length);
            System.arraycopy(encrypted, 0, all, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(all);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt Google credentials", e);
        }
    }

    private String decrypt(String data) {
        try {
            byte[] all = Base64.getDecoder().decode(data);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Base64.getDecoder().decode(encryptionKey), "AES"), new GCMParameterSpec(128, Arrays.copyOfRange(all, 0, 12)));
            return new String(cipher.doFinal(Arrays.copyOfRange(all, 12, all.length)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt Google credentials", e);
        }
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
