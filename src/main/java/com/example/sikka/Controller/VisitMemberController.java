package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
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

    @GetMapping("/get")
    public ResponseEntity<?> get() {
        return ResponseEntity.status(200).body(visitMemberService.get());
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid VisitMemberDtoIn dto) {
        visitMemberService.add(dto);
        return ResponseEntity.status(200).body(new ApiResponse("added successfully"));
    }

    @PostMapping("/join/{code}/{visitorProfileId}")
    public ResponseEntity<?> join(@PathVariable String code, @PathVariable Integer visitorProfileId) {
        visitMemberService.join(code, visitorProfileId);
        return ResponseEntity.status(200).body(new ApiResponse("joined successfully"));
    }

    @PutMapping("/update-rsvp/{id}/{rsvp}")
    public ResponseEntity<?> updateRsvp(@PathVariable Integer id, @PathVariable String rsvp) {
        visitMemberService.updateRsvp(id, rsvp);
        return ResponseEntity.status(200).body(new ApiResponse("rsvp updated successfully"));
    }

    @PutMapping("/update-selected-rides/{id}")
    public ResponseEntity<?> updateSelectedRides(@PathVariable Integer id, @RequestBody String selectedRides) {
        visitMemberService.updateSelectedRides(id, selectedRides);
        return ResponseEntity.status(200).body(new ApiResponse("selected rides updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        visitMemberService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("deleted successfully"));
    }
}