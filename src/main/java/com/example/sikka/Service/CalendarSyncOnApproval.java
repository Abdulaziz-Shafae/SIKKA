package com.example.sikka.Service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CalendarSyncOnApproval {
    private static final Logger log = LoggerFactory.getLogger(CalendarSyncOnApproval.class);
    private final GoogleCalendarOAuthService calendarService;

    public record ApprovedVisitChanged(Integer accountId, Integer visitId) {}
    public record CancelledVisitChanged(Integer accountId, Integer visitId) {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void removeCancelled(CancelledVisitChanged event) {
        try {
            if (calendarService.connected(event.accountId())) calendarService.removeCancelledVisit(event.accountId(), event.visitId());
        } catch (RuntimeException exception) {
            log.warn("Google Calendar cancellation cleanup failed; visit cancellation remains saved: {}", event.visitId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sync(ApprovedVisitChanged event) {
        try {
            if (calendarService.connected(event.accountId())) calendarService.syncVisit(event.accountId(), event.visitId());
        } catch (RuntimeException exception) {
            log.warn("Google Calendar sync failed after visit plan approval; visit remains unchanged: {}", event.visitId(), exception);
        }
    }
}
