package com.example.sikka.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    @Column(columnDefinition = "varchar(100) not null")
    private String name;

    @NotEmpty(message = "email is required")
    @Email(message = "invalid email format")
    @Column(columnDefinition = "varchar(150) not null unique")
    private String email;

    @Column(columnDefinition = "varchar(255) not null")
    private String passwordHash;

    @Pattern(regexp = "^(USER|ADMIN)$", message = "role must be USER or ADMIN")
    @Column(columnDefinition = "varchar(10) not null default 'USER'")
    private String role;

    @Pattern(regexp = "^(ACTIVE|SUSPENDED|BLOCKED)$", message = "status must be ACTIVE, SUSPENDED, or BLOCKED")
    @Column(columnDefinition = "varchar(10) not null default 'ACTIVE'")
    private String status;

    @Column(columnDefinition = "boolean not null default false")
    private Boolean emailVerified = false;

    @Column(columnDefinition = "varchar(255)")
    private String verificationTokenHash;

    @Column(columnDefinition = "datetime")
    private LocalDateTime verificationExpiresAt;

    @Column(columnDefinition = "varchar(255)")
    private String resetTokenHash;

    @Column(columnDefinition = "datetime")
    private LocalDateTime resetExpiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "account")
    private VisitorProfile visitorProfile;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organizer")
    private Set<VisitGroup> organizedGroups;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organizer")
    private Set<Visit> organizedVisits;
}