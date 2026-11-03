package com.example.sikka.DtoIn;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmAccountDeletionDtoIn {
    @NotBlank(message = "Current password is required to delete your account")
    private String password;
}
