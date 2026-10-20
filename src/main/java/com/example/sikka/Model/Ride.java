package com.example.sikka.Model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Ride name cannot be blank")
    @Size(max = 150, message = "Ride name must not exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 60, message = "Category must not exceed 60 characters")
    @Column(length = 60)
    private String category;

    @Pattern(
            regexp = "^(LOW|MEDIUM|HIGH)$",
            message = "Thrill level must be LOW, MEDIUM or HIGH"
    )
    @Column(length = 20)
    private String thrillLevel;

    @NotBlank(message = "Status cannot be blank")
    @Pattern(
            regexp = "^(OPEN|CLOSED|UNAVAILABLE)$",
            message = "Status must be OPEN, CLOSED or UNAVAILABLE"
    )
    @Column(nullable = false, length = 20)
    private String status = "UNAVAILABLE";

    @PositiveOrZero(message = "Minimum height cannot be negative")
    @Digits(integer = 3, fraction = 2, message = "Invalid minimum height")
    @Column(precision = 5, scale = 2)
    private BigDecimal minHeightCm;

    @PositiveOrZero(message = "Maximum height cannot be negative")
    @Digits(integer = 3, fraction = 2, message = "Invalid maximum height")
    @Column(precision = 5, scale = 2)
    private BigDecimal maxHeightCm;

    @PositiveOrZero(message = "Minimum age cannot be negative")
    private Integer minAgeYears;

    @PositiveOrZero(message = "Maximum age cannot be negative")
    private Integer maxAgeYears;

    @PositiveOrZero(message = "Minimum weight cannot be negative")
    @Digits(integer = 4, fraction = 2, message = "Invalid minimum weight")
    @Column(precision = 6, scale = 2)
    private BigDecimal minWeightKg;

    @PositiveOrZero(message = "Maximum weight cannot be negative")
    @Digits(integer = 4, fraction = 2, message = "Invalid maximum weight")
    @Column(precision = 6, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    @PositiveOrZero(message = "Waiting time cannot be negative")
    private Integer waitMinutes;

    @PastOrPresent(message = "Waiting time observation cannot be in the future")
    private LocalDateTime waitObservedAt;

    @Size(max = 500, message = "Source URL must not exceed 500 characters")
    @Column(length = 500)
    private String sourceUrl;

    @Size(max = 255, message = "Wait source must not exceed 255 characters")
    @Column(length = 255)
    private String waitSource;

    @Digits(integer = 4, fraction = 6, message = "Invalid map X coordinate")
    @Column(precision = 10, scale = 6)
    private BigDecimal mapX;

    @Digits(integer = 4, fraction = 6, message = "Invalid map Y coordinate")
    @Column(precision = 10, scale = 6)
    private BigDecimal mapY;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ride")
    private Set<PlanItem> planItems = new HashSet<>();

    @OneToMany(mappedBy = "ride")
    private Set<VisitRecord> visitRecords = new HashSet<>();

    @OneToMany(mappedBy = "ride")
    private Set<RidePreference> ridePreferences = new HashSet<>();

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}