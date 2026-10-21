package com.example.sikka.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class VisitorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    @Column(columnDefinition = "varchar(100) not null")
    private String name;

    @Past(message = "birth date must be in the past")
    @Column(columnDefinition = "date")
    private LocalDate birthDate;

    @Positive(message = "height must be positive")
    @Column(columnDefinition = "decimal(5,2)")
    private Double heightCm;

    @Positive(message = "weight must be positive")
    @Column(columnDefinition = "decimal(6,2)")
    private Double weightKg;

    @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", message = "thrill preference must be LOW, MEDIUM, or HIGH")
    @Column(columnDefinition = "varchar(30)")
    private String thrillPreference;

    @Column(columnDefinition = "json")
    private String preferredRideTypes;

    @Column(columnDefinition = "json")
    private String dislikedRideFeatures;

    @Column(columnDefinition = "json")
    private String foodPreferences;

    @Column(columnDefinition = "json")
    private String healthData;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @JsonIgnore
    private Account account;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "visitorProfile")
    private Set<VisitMember> visitMembers;
}