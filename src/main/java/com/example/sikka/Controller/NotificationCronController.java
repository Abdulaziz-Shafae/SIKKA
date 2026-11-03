package com.example.sikka.Controller;

import com.example.sikka.Service.NotificationReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/notification-cron")
public class NotificationCronController {
    private final NotificationReminderService notificationReminderService;

    @Value("${sikka.notifications.cron-key:}")
    private String cronKey;

    // Set SIKKA_NOTIFICATION_CRON_KEY on Railway and configure a 5-minute external POST cron.
    @PostMapping("/run")
    public ResponseEntity<?> runDueReminderScan(@RequestHeader(value = "X-SIKKA-CRON-KEY", required = false) String suppliedKey) {
        if (cronKey == null || cronKey.isBlank() || suppliedKey == null || !MessageDigest.isEqual(cronKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        int scanned = notificationReminderService.dispatchDueReminders();
        return ResponseEntity.ok(Map.of("scannedVisits", scanned));
    }
}
