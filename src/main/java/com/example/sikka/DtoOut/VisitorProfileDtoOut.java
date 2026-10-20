package com.example.sikka.DtoOut;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisitorProfileDtoOut {
    private Integer id;
    private Integer accountId;
    private String name;
    private LocalDate birthDate;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String thrillPreference;
    private String preferredRideTypes;
    private String dislikedRideFeatures;
    private String foodPreferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}