package com.example.sikka.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class VisitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Record status is required")
    @Pattern(regexp = "^(COMPLETED|SKIPPED)$", message = "Record status must be COMPLETED or SKIPPED")
    @Column(columnDefinition = "VARCHAR(20) not null")
    private String status;

    private LocalDateTime completedAt;

    private LocalDateTime skippedAt;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    @Column(columnDefinition = "TINYINT")
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    private LocalDateTime correctedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

     @ManyToOne
     @JoinColumn(name = "visit_member_id", nullable = false)
     @JsonIgnore
     private VisitMember visitMember;

     @ManyToOne
     @JoinColumn(name = "ride_id", nullable = false)
     @JsonIgnore
     private Ride ride;

     @ManyToOne
     @JoinColumn(name = "recorded_by_account_id", nullable = false)
     @JsonIgnore
     private Account recordedByAccount;
}