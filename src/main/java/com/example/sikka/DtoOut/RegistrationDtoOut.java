package com.example.sikka.DtoOut;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistrationDtoOut {
    private String message;
    private boolean verificationEmailSent;
}
