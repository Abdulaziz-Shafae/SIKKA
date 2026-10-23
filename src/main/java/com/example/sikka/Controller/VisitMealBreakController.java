package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.VisitMealBreakDtoIn;
import com.example.sikka.Service.VisitMealBreakService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.sikka.DtoIn.SharedMealBreakDtoIn;

@RestController
@RequestMapping("/api/v1/visit-meal-break")
@AllArgsConstructor
public class VisitMealBreakController {

    private final VisitMealBreakService visitMealBreakService;

    @GetMapping("/get/visit/{accountId}/{visitId}")
    public ResponseEntity<?> getVisitMealBreaks(
            @PathVariable Integer accountId,
            @PathVariable Integer visitId){
        return ResponseEntity.status(200).body(
                visitMealBreakService.getVisitMealBreaks(accountId, visitId)
        );
    }

    @PostMapping("/add/{accountId}/{visitId}/{visitMemberId}")
    public ResponseEntity<?> addVisitMealBreak(
            @PathVariable Integer accountId,
            @PathVariable Integer visitId,
            @PathVariable Integer visitMemberId,
            @RequestBody @Valid VisitMealBreakDtoIn dto){
        visitMealBreakService.addVisitMealBreak(accountId, visitId, visitMemberId, dto);
        return ResponseEntity.status(200).body(
                new ApiResponse("Visit meal break added successfully")
        );
    }

    @PutMapping("/update/{accountId}/{visitId}/{mealBreakId}")
    public ResponseEntity<?> updateVisitMealBreak(
            @PathVariable Integer accountId,
            @PathVariable Integer visitId,
            @PathVariable Integer mealBreakId,
            @RequestBody @Valid VisitMealBreakDtoIn dto){
        visitMealBreakService.updateVisitMealBreak(accountId, visitId, mealBreakId, dto);
        return ResponseEntity.status(200).body(
                new ApiResponse("Visit meal break updated successfully")
        );
    }

    @DeleteMapping("/delete/{accountId}/{visitId}/{mealBreakId}")
    public ResponseEntity<?> deleteVisitMealBreak(
            @PathVariable Integer accountId,
            @PathVariable Integer visitId,
            @PathVariable Integer mealBreakId){
        visitMealBreakService.deleteVisitMealBreak(accountId, visitId, mealBreakId);
        return ResponseEntity.status(200).body(
                new ApiResponse("Visit meal break deleted successfully")
        );
    }

    @GetMapping("/get/my/{accountId}/{visitId}")
    public ResponseEntity<?> getMyMealBreaks(
            @PathVariable Integer accountId,
            @PathVariable Integer visitId){
        return ResponseEntity.status(200).body(
                visitMealBreakService.getMyMealBreaks(accountId, visitId)
        );
    }

    @PostMapping("/add/shared/{accountId}/{visitId}")
    public ResponseEntity<?> addSharedMealBreak(
            @PathVariable Integer accountId,
            @PathVariable Integer visitId,
            @RequestBody @Valid SharedMealBreakDtoIn dto){
        visitMealBreakService.addSharedMealBreak(accountId, visitId, dto);
        return ResponseEntity.status(200).body(
                new ApiResponse("Shared meal break added successfully")
        );
    }

    @DeleteMapping("/delete/shared/{accountId}/{visitId}/{breakKey}")
    public ResponseEntity<?> deleteSharedMealBreak(
            @PathVariable Integer accountId,
            @PathVariable Integer visitId,
            @PathVariable String breakKey){
        visitMealBreakService.deleteSharedMealBreak(accountId, visitId, breakKey);
        return ResponseEntity.status(200).body(
                new ApiResponse("Shared meal break deleted successfully")
        );
    }

    @PutMapping("/update/shared/{accountId}/{visitId}/{breakKey}")
    public ResponseEntity<?> updateSharedMealBreak(
            @PathVariable Integer accountId,
            @PathVariable Integer visitId,
            @PathVariable String breakKey,
            @RequestBody @Valid VisitMealBreakDtoIn dto){
        visitMealBreakService.updateSharedMealBreak(accountId, visitId, breakKey, dto);
        return ResponseEntity.status(200).body(
                new ApiResponse("Shared meal break updated successfully")
        );
    }
}