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
import java.time.LocalTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "varchar(150)")
    private String name;

    @Column(columnDefinition = "date not null")
    private LocalDate visitDate;

    @Column(columnDefinition = "int not null")
    private Integer maxParticipants;

    @Column(columnDefinition = "time")
    private LocalTime plannedStartTime;

    @Column(columnDefinition = "time")
    private LocalTime plannedEndTime;

    @Pattern(regexp = "^(DRAFT|OPEN|PLANNING|READY|IN_PROGRESS|COMPLETED|CANCELLED)$",
            message = "invalid visit status")
    @Column(columnDefinition = "varchar(20) not null default 'DRAFT'")
    private String status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "group_id", unique = true)
    @JsonIgnore
    private VisitGroup group;

    @ManyToOne
    @JoinColumn(name = "organizer_account_id", nullable = false)
    @JsonIgnore
    private Account organizer;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "visit")
    @JsonIgnore
    private Set<VisitMember> members;

    @OneToMany(mappedBy = "visit")
    @JsonIgnore
    private Set<VisitPlan> visitPlans;

    @OneToMany(mappedBy = "visit")
    @JsonIgnore
    private Set<Notification> notifications;

    @OneToMany(mappedBy = "visit")
    @JsonIgnore
    private Set<PaymentOrder> paymentOrders;
}