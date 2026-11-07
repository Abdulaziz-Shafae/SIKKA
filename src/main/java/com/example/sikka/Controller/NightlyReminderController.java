package com.example.sikka.Controller;

import com.example.sikka.Service.NightBeforeVisitReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Called by an external scheduler at 17:00 UTC = 20:00 Asia/Riyadh. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nightly-reminder")
public class NightlyReminderController {
    private final NightBeforeVisitReminderService reminders;

    @Value("${SIKKA_NIGHTLY_REMINDER_KEY:}")
    private String key;

    @PostMapping("/run")
    public ResponseEntity<?> run(@RequestHeader(value = "X-Sikka-Nightly-Key", required = false) String received) {
        // Always require both explicit feature enablement and a strong out-of-band key.
        if (!reminders.isEnabled()) return ResponseEntity.status(404).body(Map.of("message", "Not available"));
        if (key == null || key.length() < 32 || received == null ||
                !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8)))
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        if (!NightBeforeVisitReminderService.isNightlyWindow(
                java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Riyadh"))))
            return ResponseEntity.status(409).body(Map.of("message", "Outside the nightly window"));
        return ResponseEntity.ok(Map.of("processedVisits", reminders.sendTomorrowReminders()));
    }
}
