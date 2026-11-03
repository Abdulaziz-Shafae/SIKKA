package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.AccountDtoIn;
import com.example.sikka.DtoIn.RegisterDtoIn;
import com.example.sikka.Service.AccountService;
import jakarta.validation.Valid;
import com.example.sikka.DtoIn.ResetPasswordDtoIn;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.sikka.Service.VisitorProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sikka.Model.Account;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final com.example.sikka.Service.AccountErasureService accountErasureService;


    @GetMapping("/get")
    public ResponseEntity<?> get() {
        return ResponseEntity.status(200).body(accountService.get());
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid AccountDtoIn dto) {
        accountService.add(dto);
        return ResponseEntity.status(200).body(new ApiResponse("added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody @Valid AccountDtoIn dto) {
        accountService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        accountService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("deleted successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDtoIn dto) {
        boolean emailSent = accountService.register(dto);
        return ResponseEntity.status(200).body(new com.example.sikka.DtoOut.RegistrationDtoOut("registered successfully", emailSent));
    }

    @PostMapping("/delete-my-account")
    public ResponseEntity<?> deleteMyAccount(@AuthenticationPrincipal Account account, @RequestBody @Valid com.example.sikka.DtoIn.ConfirmAccountDeletionDtoIn dto, jakarta.servlet.http.HttpServletRequest request) {
        accountErasureService.deleteMyAccount(account.getId(), dto.getPassword());
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.status(200).body(new ApiResponse("Account and personal data deleted"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Account account) {
        return ResponseEntity.status(200).body(accountService.getMe(account.getId()));
    }




}