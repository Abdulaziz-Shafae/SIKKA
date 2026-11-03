package com.example.sikka.Service;

import com.example.sikka.Model.*;
import com.example.sikka.Repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VisitRecapImageServiceTest {
    @Test
    void renderSevenCompletedRidesAsRealPngWithZigzagLayout() {
        VisitRepository visits = mock(VisitRepository.class);
        VisitMemberRepository members = mock(VisitMemberRepository.class);
        VisitRecordRepository records = mock(VisitRecordRepository.class);
        Visit visit = new Visit();
        visit.setId(55);
        visit.setName("طلعة الشباب");
        visit.setStatus("COMPLETED");
        visit.setVisitDate(LocalDate.of(2026, 11, 4));
        visit.setStartedAt(LocalDateTime.of(2026, 11, 4, 16, 0));
        visit.setCompletedAt(LocalDateTime.of(2026, 11, 4, 22, 40));
        VisitMember member = new VisitMember();
        member.setId(7);
        member.setVisit(visit);
        List<VisitRecord> played = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Ride ride = new Ride();
            ride.setName("لعبة رقم " + (i + 1));
            VisitRecord row = new VisitRecord();
            row.setId(i + 1);
            row.setStatus("COMPLETED");
            row.setRide(ride);
            row.setCompletedAt(visit.getStartedAt().plusMinutes(30L + 40L * i));
            played.add(row);
        }
        when(visits.findVisitById(55)).thenReturn(visit);
        when(members.findVisitMemberById(7)).thenReturn(member);
        when(records.findAllByVisitMember_IdOrderByCreatedAtDescIdDesc(7)).thenReturn(played);
        byte[] png = new VisitRecapImageService(visits, members, records).createCompletedVisitImage(55, 7);
        assertTrue(png.length > 5000);
        assertArrayEquals(new byte[]{(byte)137,80,78,71,13,10,26,10}, java.util.Arrays.copyOfRange(png,0,8));
    }
}
