package com.example.sikka.Service;

import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.List;

/**
 * External trigger only. No @Scheduled timer runs in the sleeping backend.
 * No messages can be sent unless explicitly enabled and triggered with the private key.
 */
@Service
@RequiredArgsConstructor
public class NightBeforeVisitReminderService {
    private static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");
    private final VisitRepository visits;
    private final NotificationEventService notifications;

    @Value("${sikka.notifications.night-before-enabled:false}")
    private boolean enabled;

    public boolean isEnabled() { return enabled; }

    public static boolean isNightlyWindow(ZonedDateTime instant) {
        LocalTime time = instant.withZoneSameInstant(RIYADH).toLocalTime();
        return !time.isBefore(LocalTime.of(20, 0)) && time.isBefore(LocalTime.of(20, 30));
    }

    public int sendTomorrowReminders() {
        if (!enabled) throw new IllegalStateException("Night-before reminders are disabled");
        ZonedDateTime now = ZonedDateTime.now(RIYADH);
        if (!isNightlyWindow(now)) throw new IllegalStateException("Night-before reminders only run at 20:00 Asia/Riyadh");
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        int processed = 0;
        // The data query is made only on the external trigger, not every minute.
        for (int page = 0; page < 100; page++) {
            List<Integer> ids = visits.findReadyApprovedVisitsOn(tomorrow, PageRequest.of(page, 100));
            if (ids.isEmpty()) break;
            for (Integer visitId : ids) {
                notifications.notifyNightBeforeVisit(visitId, tomorrow);
                processed++;
            }
            if (ids.size() < 100) break;
        }
        return processed;
    }
}
