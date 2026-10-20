package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.Model.RidePreference;
import com.example.sikka.Service.RidePreferenceService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ride-preference")
@AllArgsConstructor
public class RidePreferenceController {

    private final RidePreferenceService ridePreferenceService;

    @GetMapping("/get/my/preferences/{accountId}")
    public ResponseEntity<?> getMyRidePreferences(@PathVariable Integer accountId){
        return ResponseEntity.status(200).body(ridePreferenceService.getMyRidePreferences(accountId));
    }

    @GetMapping("/get/details/{accountId}/{preferenceId}")
    public ResponseEntity<?> getRidePreferenceById(@PathVariable Integer accountId, @PathVariable Integer preferenceId){
        return ResponseEntity.status(200).body(ridePreferenceService.getRidePreferenceById(accountId, preferenceId));
    }

    @PostMapping("/add/{accountId}/{rideId}")
    public ResponseEntity<?> addRidePreference(@PathVariable Integer accountId, @PathVariable Integer rideId, @RequestBody @Valid RidePreference ridePreference){
        ridePreferenceService.addRidePreference(accountId, rideId, ridePreference);
        return ResponseEntity.status(200).body(new ApiResponse("Ride preference added successfully"));
    }

    @PutMapping("/update/{accountId}/{preferenceId}")
    public ResponseEntity<?> updateRidePreference(@PathVariable Integer accountId, @PathVariable Integer preferenceId, @RequestBody @Valid RidePreference ridePreference){
        ridePreferenceService.updateRidePreference(accountId, preferenceId, ridePreference);
        return ResponseEntity.status(200).body(new ApiResponse("Ride preference updated successfully"));
    }

    @DeleteMapping("/delete/{accountId}/{preferenceId}")
    public ResponseEntity<?> deleteRidePreference(@PathVariable Integer accountId, @PathVariable Integer preferenceId){
        ridePreferenceService.deleteRidePreference(accountId, preferenceId);
        return ResponseEntity.status(200).body(new ApiResponse("Ride preference deleted successfully"));
    }
}