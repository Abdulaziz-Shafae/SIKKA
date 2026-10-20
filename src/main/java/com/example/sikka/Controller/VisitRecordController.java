package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.CreateVisitRecordDtoIn;
import com.example.sikka.DtoIn.RateVisitRecordDtoIn;
import com.example.sikka.DtoIn.UpdateVisitRecordDtoIn;
import com.example.sikka.DtoOut.VisitRecordDtoOut;
import com.example.sikka.Service.VisitRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visit-record")
public class VisitRecordController {

    private final VisitRecordService visitRecordService;

    @GetMapping("/get/my-records/{accountId}/{visitId}")
    public ResponseEntity<List<VisitRecordDtoOut>> getMyVisitRecords(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitRecordService.getMyVisitRecords(accountId, visitId));
    }

    @PostMapping("/create/{accountId}/{visitId}")
    public ResponseEntity<ApiResponse> createVisitRecords(@PathVariable Integer accountId, @PathVariable Integer visitId, @RequestBody @Valid CreateVisitRecordDtoIn createVisitRecordDtoIn) {
        visitRecordService.createVisitRecords(accountId, visitId, createVisitRecordDtoIn);
        return ResponseEntity.status(201).body(new ApiResponse("Visit records created successfully"));
    }

    @PutMapping("/update/status/{accountId}")
    public ResponseEntity<ApiResponse> updateVisitRecordStatus(@PathVariable Integer accountId, @RequestBody @Valid UpdateVisitRecordDtoIn updateVisitRecordDtoIn) {
        visitRecordService.updateVisitRecordStatus(accountId, updateVisitRecordDtoIn);
        return ResponseEntity.status(200).body(new ApiResponse("Visit record status updated successfully"));
    }

    @PutMapping("/rate/{accountId}")
    public ResponseEntity<ApiResponse> rateVisitRecord(@PathVariable Integer accountId, @RequestBody @Valid RateVisitRecordDtoIn rateVisitRecordDtoIn) {
        visitRecordService.rateVisitRecord(accountId, rateVisitRecordDtoIn);
        return ResponseEntity.status(200).body(new ApiResponse("Ride rating saved successfully"));
    }
}