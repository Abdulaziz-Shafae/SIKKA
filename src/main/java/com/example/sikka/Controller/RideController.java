package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.Model.Ride;
import com.example.sikka.Service.RideService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ride")
@AllArgsConstructor
public class RideController {

    private final RideService rideService;

    @GetMapping("/get")
    public ResponseEntity<?> getRides(){
        return ResponseEntity.status(200).body(rideService.getRides());
    }

    @GetMapping("/get/{rideId}")
    public ResponseEntity<?> getRideById(@PathVariable Integer rideId){
        return ResponseEntity.status(200).body(rideService.getRideById(rideId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addRide(@RequestBody @Valid Ride ride){
        rideService.addRide(ride);
        return ResponseEntity.status(200).body(new ApiResponse("Ride added successfully"));
    }

    @PutMapping("/update/{rideId}")
    public ResponseEntity<?> updateRide(@PathVariable Integer rideId, @RequestBody @Valid Ride ride){
        rideService.updateRide(rideId, ride);
        return ResponseEntity.status(200).body(new ApiResponse("Ride updated successfully"));
    }

    @DeleteMapping("/delete/{rideId}")
    public ResponseEntity<?> deleteRide(@PathVariable Integer rideId){
        rideService.deleteRide(rideId);
        return ResponseEntity.status(200).body(new ApiResponse("Ride deleted successfully"));
    }
}