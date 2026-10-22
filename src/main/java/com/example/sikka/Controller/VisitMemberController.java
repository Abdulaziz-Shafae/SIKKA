package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.JoinDtoIn;
import com.example.sikka.DtoIn.ReviewDtoIn;
import com.example.sikka.DtoIn.SelectedRidesDtoIn;
import com.example.sikka.DtoIn.VisitMemberDtoIn;
import com.example.sikka.Service.VisitMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visit-member")
@RequiredArgsConstructor
public class VisitMemberController {

    private final VisitMemberService visitMemberService;

    // ---------- CRUD ----------

    @GetMapping("/get")
    public ResponseEntity<?> get() {
        return ResponseEntity.status(200).body(visitMemberService.get());
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid VisitMemberDtoIn dto) {
        visitMemberService.add(dto);
        return ResponseEntity.status(200).body(new ApiResponse("added successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        visitMemberService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("deleted successfully"));
    }

    // ---------- Flow 1 extras ----------

    @PostMapping("/join/{accountId}")
    public ResponseEntity<?> join(@PathVariable Integer accountId, @RequestBody @Valid JoinDtoIn dto) {
        visitMemberService.join(accountId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("joined successfully"));
    }

    @PutMapping("/update-rsvp/{accountId}/{visitId}/{rsvp}")
    public ResponseEntity<?> updateRsvp(@PathVariable Integer accountId, @PathVariable Integer visitId,
                                        @PathVariable String rsvp) {
        visitMemberService.updateRsvp(accountId, visitId, rsvp);
        return ResponseEntity.status(200).body(new ApiResponse("rsvp updated successfully"));
    }

    @PutMapping("/update-selected-rides/{accountId}/{visitId}")
    public ResponseEntity<?> updateSelectedRides(@PathVariable Integer accountId, @PathVariable Integer visitId,
                                                 @RequestBody @Valid SelectedRidesDtoIn dto) {
        visitMemberService.updateSelectedRides(accountId, visitId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("selected rides updated successfully"));
    }

    @PutMapping("/check-in/{accountId}/{visitId}")
    public ResponseEntity<?> checkIn(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitMemberService.checkIn(accountId, visitId);
        return ResponseEntity.status(200).body(new ApiResponse("checked in successfully"));
    }

    @PutMapping("/review/{accountId}/{visitId}")
    public ResponseEntity<?> review(@PathVariable Integer accountId, @PathVariable Integer visitId,
                                    @RequestBody @Valid ReviewDtoIn dto) {
        visitMemberService.review(accountId, visitId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("review saved successfully"));
    }

    @DeleteMapping("/leave/{accountId}/{visitId}")
    public ResponseEntity<?> leave(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitMemberService.leave(accountId, visitId);
        return ResponseEntity.status(200).body(new ApiResponse("left the visit successfully"));
    }

    @DeleteMapping("/remove/{organizerId}/{visitId}/{memberId}")
    public ResponseEntity<?> remove(@PathVariable Integer organizerId, @PathVariable Integer visitId,
                                    @PathVariable Integer memberId) {
        visitMemberService.remove(organizerId, visitId, memberId);
        return ResponseEntity.status(200).body(new ApiResponse("member removed successfully"));
    }

    @GetMapping("/summary/{accountId}/{visitId}")
    public ResponseEntity<?> summary(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitMemberService.getSummary(accountId, visitId));
    }
}