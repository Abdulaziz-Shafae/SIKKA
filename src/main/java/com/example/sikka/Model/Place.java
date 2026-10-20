package com.example.sikka.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Place name cannot be blank")
    @Size(max = 150, message = "Place name must not exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Place kind cannot be blank")
    @Pattern(
            regexp = "^(RESTAURANT|MEETING_POINT|RESTROOM|OTHER)$",
            message = "Place kind must be RESTAURANT, MEETING_POINT, RESTROOM or OTHER"
    )
    @Column(nullable = false, length = 20)
    private String kind;

    @Digits(integer = 4, fraction = 6, message = "Invalid map X coordinate")
    @Column(precision = 10, scale = 6)
    private BigDecimal mapX;

    @Digits(integer = 4, fraction = 6, message = "Invalid map Y coordinate")
    @Column(precision = 10, scale = 6)
    private BigDecimal mapY;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "place")
    private Set<PlanItem> planItems = new HashSet<>();

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}