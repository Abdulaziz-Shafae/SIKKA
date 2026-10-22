package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.Model.PrivacyConsent;
import com.example.sikka.Service.PrivacyConsentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/privacy-consent")
@AllArgsConstructor
public class PrivacyConsentController {

    private final PrivacyConsentService privacyConsentService;

    @GetMapping("/get/my/consents/{accountId}")
    public ResponseEntity<?> getMyPrivacyConsents(@PathVariable Integer accountId){
        return ResponseEntity.status(200).body(privacyConsentService.getMyPrivacyConsents(accountId));
    }

    @GetMapping("/get/details/{accountId}/{consentId}")
    public ResponseEntity<?> getPrivacyConsentById(@PathVariable Integer accountId, @PathVariable Integer consentId){
        return ResponseEntity.status(200).body(privacyConsentService.getPrivacyConsentById(accountId, consentId));
    }

    @PostMapping("/add/{accountId}")
    public ResponseEntity<?> addPrivacyConsent(@PathVariable Integer accountId, @RequestBody @Valid PrivacyConsent privacyConsent){
        privacyConsentService.addPrivacyConsent(accountId, privacyConsent);
        return ResponseEntity.status(200).body(new ApiResponse("Privacy consent added successfully"));
    }

    @PutMapping("/revoke/{accountId}/{consentId}")
    public ResponseEntity<?> revokePrivacyConsent(@PathVariable Integer accountId, @PathVariable Integer consentId){
        privacyConsentService.revokePrivacyConsent(accountId, consentId);
        return ResponseEntity.status(200).body(new ApiResponse("Privacy consent revoked successfully"));
    }
}