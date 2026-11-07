package com.example.sikka.Config;

import com.example.sikka.Repository.VisitRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Avoid background SQL in an idle park, but keep monitors running throughout
 * an IN_PROGRESS visit even if visitors lock their phones.
 */
@Component
@RequiredArgsConstructor
public class VisitMonitorGate {
    private static final long RECENT_VISIT_ACTIVITY_MS = Duration.ofMinutes(10).toMillis();
    private final VisitRepository visits;
    private final AtomicLong lastVisitRequest = new AtomicLong(0);
    private volatile boolean visitInProgress;

    @PostConstruct
    public void initialize() {
        refreshActiveVisits();
    }

    public void refreshActiveVisits() {
        visitInProgress = visits.existsByStatus("IN_PROGRESS");
    }

    public void recordVisitRequest() {
        lastVisitRequest.set(System.currentTimeMillis());
    }

    public boolean shouldMonitor() {
        return visitInProgress || System.currentTimeMillis() - lastVisitRequest.get() < RECENT_VISIT_ACTIVITY_MS;
    }

    public boolean hasActiveVisit() {
        return visitInProgress;
    }
}
