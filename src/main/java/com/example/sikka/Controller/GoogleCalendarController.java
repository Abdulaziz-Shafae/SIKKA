package com.example.sikka.Controller;

import com.example.sikka.Model.Account;
import com.example.sikka.Service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/google-calendar")
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    @GetMapping("/manual/{visitId}")
    public ResponseEntity<?> getManualCalendarLink(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(Map.of("url", googleCalendarService.createManualCalendarLink(account.getId(), visitId)));
    }
}
