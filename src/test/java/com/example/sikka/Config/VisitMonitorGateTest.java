package com.example.sikka.Config;

import com.example.sikka.Repository.VisitRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VisitMonitorGateTest {
    @Test void idleBackendDoesNotPollAndActiveVisitsRemainMonitored() {
        VisitRepository visits=mock(VisitRepository.class);
        when(visits.existsByStatus("IN_PROGRESS")).thenReturn(false,true,false);
        VisitMonitorGate gate=new VisitMonitorGate(visits);
        gate.initialize();
        assertFalse(gate.shouldMonitor(), "No visit and no visitor activity should produce zero monitor queries");
        gate.refreshActiveVisits();
        assertTrue(gate.hasActiveVisit());
        assertTrue(gate.shouldMonitor(), "Keep monitoring while a visit is IN_PROGRESS even if the screen is locked");
        gate.refreshActiveVisits();
        assertFalse(gate.hasActiveVisit());
        assertFalse(gate.shouldMonitor());
        gate.recordVisitRequest();
        assertTrue(gate.shouldMonitor(), "Opening a visit plan temporarily resumes monitoring");
        verify(visits,times(3)).existsByStatus("IN_PROGRESS");
    }
}
