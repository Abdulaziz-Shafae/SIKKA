package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.VisitDtoIn;
import com.example.sikka.Service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.sikka.DtoIn.CreateVisitDtoIn;
import com.example.sikka.DtoIn.ReuseVisitDtoIn;

@RestController
@RequestMapping("/api/v1/visit")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping("/get")
    public ResponseEntity<?> get() {
        return ResponseEntity.status(200).body(visitService.get());
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid VisitDtoIn dto) {
        visitService.add(dto);
        return ResponseEntity.status(200).body(new ApiResponse("added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody @Valid VisitDtoIn dto) {
        visitService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        visitService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("deleted successfully"));
    }

    @PostMapping("/create/{accountId}")
    public ResponseEntity<?> create(@PathVariable Integer accountId, @RequestBody @Valid CreateVisitDtoIn dto) {
        String code = visitService.create(accountId, dto);
        if (code == null)
            return ResponseEntity.status(200).body(new ApiResponse("visit created"));
        return ResponseEntity.status(200).body(new ApiResponse("visit created, invite code: " + code));
    }

    @PostMapping("/reuse/{accountId}/{visitId}")
    public ResponseEntity<?> reuse(@PathVariable Integer accountId, @PathVariable Integer visitId,
                                   @RequestBody @Valid ReuseVisitDtoIn dto) {
        String code = visitService.reuse(accountId, visitId, dto);
        if (code == null)
            return ResponseEntity.status(200).body(new ApiResponse("visit created from the previous one"));
        return ResponseEntity.status(200).body(new ApiResponse("visit created from the previous one, invite code: " + code));
    }

    @GetMapping("/my-visits/{accountId}")
    public ResponseEntity<?> myVisits(@PathVariable Integer accountId) {
        return ResponseEntity.status(200).body(visitService.myVisits(accountId));
    }

    @GetMapping("/details/{accountId}/{visitId}")
    public ResponseEntity<?> details(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitService.getVisitDetails(accountId, visitId));
    }

    @PutMapping("/update-visit/{accountId}/{visitId}")
    public ResponseEntity<?> updateVisit(@PathVariable Integer accountId, @PathVariable Integer visitId,
                                         @RequestBody @Valid CreateVisitDtoIn dto) {
        visitService.updateByOrganizer(accountId, visitId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("visit updated successfully"));
    }

    @PutMapping("/cancel/{accountId}/{visitId}")
    public ResponseEntity<?> cancel(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitService.cancel(accountId, visitId);
        return ResponseEntity.status(200).body(new ApiResponse("visit cancelled"));
    }

    @PutMapping("/start/{accountId}/{visitId}")
    public ResponseEntity<?> start(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitService.startVisit(accountId, visitId);
        return ResponseEntity.status(200).body(new ApiResponse("visit started"));
    }

    @PutMapping("/complete/{accountId}/{visitId}")
    public ResponseEntity<?> complete(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitService.completeVisit(accountId, visitId);
        return ResponseEntity.status(200).body(new ApiResponse("visit completed"));
    }
}