package com.example.sikka.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class PlanApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Approval status is required")
    @Pattern(regexp = "^(PENDING|APPROVED|REJECTED)$", message = "Approval status must be PENDING, APPROVED or REJECTED")
    @Column(columnDefinition = "VARCHAR(20) not null")
    private String status = "PENDING";

    private LocalDateTime respondedAt;

    @OneToOne
    @JoinColumn(name = "visit_plan_id", nullable = false, unique = true)
    @JsonIgnore
    private VisitPlan visitPlan;

    // @ManyToOne
    // @JoinColumn(name = "approved_by_account_id", nullable = false)
    // @JsonIgnore
    // private Account approvedByAccount;
}