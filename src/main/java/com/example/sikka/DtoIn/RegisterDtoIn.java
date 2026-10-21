package com.example.sikka.DtoIn;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDtoIn {

    @NotEmpty(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @NotEmpty(message = "email is required")
    @Email(message = "invalid email format")
    private String email;

    @NotEmpty(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    @Past(message = "birth date must be in the past")
    private LocalDate birthDate;

    @Positive(message = "height must be positive")
    private BigDecimal heightCm;

    @Positive(message = "weight must be positive")
    private BigDecimal weightKg;

    @Pattern(regexp = "^(LOW|MILD|MEDIUM|HIGH|EXTREME)$", message = "thrill preference must be LOW, MILD, MEDIUM, HIGH or EXTREME")
    private String thrillPreference;

    private String preferredRideTypes;

    private String dislikedRideFeatures;

    private String foodPreferences;
}