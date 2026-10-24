package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.FamilyMemberDtoIn;
import com.example.sikka.Service.FamilyMemberService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/family-member")
@AllArgsConstructor
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    @GetMapping("/get/my/{accountId}")
    public ResponseEntity<?> getMyFamilyMembers(@PathVariable Integer accountId){
        return ResponseEntity.status(200).body(
                familyMemberService.getMyFamilyMembers(accountId)
        );
    }

    @GetMapping("/get/details/{accountId}/{familyMemberId}")
    public ResponseEntity<?> getFamilyMemberById(
            @PathVariable Integer accountId,
            @PathVariable Integer familyMemberId){
        return ResponseEntity.status(200).body(
                familyMemberService.getFamilyMemberById(accountId, familyMemberId)
        );
    }

    @PostMapping("/add/{accountId}")
    public ResponseEntity<?> addFamilyMember(
            @PathVariable Integer accountId,
            @RequestBody @Valid FamilyMemberDtoIn dto){
        familyMemberService.addFamilyMember(accountId, dto);
        return ResponseEntity.status(200).body(
                new ApiResponse("Family member added successfully")
        );
    }

    @PutMapping("/update/{accountId}/{familyMemberId}")
    public ResponseEntity<?> updateFamilyMember(
            @PathVariable Integer accountId,
            @PathVariable Integer familyMemberId,
            @RequestBody @Valid FamilyMemberDtoIn dto){
        familyMemberService.updateFamilyMember(accountId, familyMemberId, dto);
        return ResponseEntity.status(200).body(
                new ApiResponse("Family member updated successfully")
        );
    }

    @DeleteMapping("/delete/{accountId}/{familyMemberId}")
    public ResponseEntity<?> deleteFamilyMember(@PathVariable Integer accountId, @PathVariable Integer familyMemberId) {
        familyMemberService.deleteFamilyMember(accountId, familyMemberId);
        return ResponseEntity.status(200).body(new ApiResponse("Family member deleted successfully"));
    }

}