package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.CreateVisitRecordDtoIn;
import com.example.sikka.DtoIn.RateVisitRecordDtoIn;
import com.example.sikka.DtoIn.UpdateVisitRecordDtoIn;
import com.example.sikka.Service.VisitRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visit-record")
public class VisitRecordController {

    private final VisitRecordService visitRecordService;

    @GetMapping("/get/my-records/{accountId}/{visitId}")
    public ResponseEntity<?> getMyVisitRecords(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitRecordService.getMyVisitRecords(accountId, visitId));
    }

    @PostMapping("/complete/{accountId}/{visitId}")
    public ResponseEntity<?> completeVisitRecords(@PathVariable Integer accountId, @PathVariable Integer visitId, @RequestBody @Valid CreateVisitRecordDtoIn dto) {
        visitRecordService.createVisitRecords(accountId, visitId, dto, 1);
        return ResponseEntity.status(201).body(new ApiResponse("Visit records created successfully"));
    }

    @PostMapping("/skip/{accountId}/{visitId}")
    public ResponseEntity<?> skipVisitRecords(@PathVariable Integer accountId, @PathVariable Integer visitId, @RequestBody @Valid CreateVisitRecordDtoIn dto) {
        visitRecordService.createVisitRecords(accountId, visitId, dto, 2);
        return ResponseEntity.status(201).body(new ApiResponse("Visit records created successfully"));
    }

    @PutMapping("/update/complete/{accountId}")
    public ResponseEntity<?> correctVisitRecordToCompleted(@PathVariable Integer accountId, @RequestBody @Valid UpdateVisitRecordDtoIn dto) {
        visitRecordService.updateVisitRecordStatus(accountId, dto, 1);
        return ResponseEntity.status(200).body(new ApiResponse("Visit record status updated successfully"));
    }

    @PutMapping("/update/skip/{accountId}")
    public ResponseEntity<?> correctVisitRecordToSkipped(@PathVariable Integer accountId, @RequestBody @Valid UpdateVisitRecordDtoIn dto) {
        visitRecordService.updateVisitRecordStatus(accountId, dto, 2);
        return ResponseEntity.status(200).body(new ApiResponse("Visit record status updated successfully"));
    }

    @PutMapping("/rate/{accountId}")
    public ResponseEntity<?> rateVisitRecord(@PathVariable Integer accountId, @RequestBody @Valid RateVisitRecordDtoIn rateVisitRecordDtoIn) {
        visitRecordService.rateVisitRecord(accountId, rateVisitRecordDtoIn);
        return ResponseEntity.status(200).body(new ApiResponse("Ride rating saved successfully"));
    }

    @GetMapping("/visit-summary/{accountId}/{visitId}")
    public ResponseEntity<?> getMyVisitSummary(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitRecordService.getMyVisitSummary(accountId, visitId));
    }

    @GetMapping("/progress/{accountId}/{visitId}")
    public ResponseEntity<?> getMyVisitProgress(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitRecordService.getMyVisitProgress(accountId, visitId));
    }
}
