package com.example.sikka.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
// @Table(uniqueConstraints = @UniqueConstraint(name = "uk_visit_plan_visit_owner", columnNames = {"visit_id", "owner_visit_member_id"}))
public class VisitPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Plan kind is required")
    @Pattern(regexp = "^(INDIVIDUAL|GROUP)$", message = "Plan kind must be INDIVIDUAL or GROUP")
    @Column(columnDefinition = "VARCHAR(20) not null")
    private String planKind;

    @NotNull(message = "Active status is required")
    @Column(name = "is_active", columnDefinition = "BOOLEAN not null")
    private Boolean isActive = false;

    @Column(columnDefinition = "JSON")
    private String proposedItems;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // @ManyToOne
    // @JoinColumn(name = "visit_id", nullable = false)
    // @JsonIgnore
    // private Visit visit;

    // @ManyToOne
    // @JoinColumn(name = "owner_visit_member_id")
    // @JsonIgnore
    // private VisitMember ownerVisitMember;

     @OneToMany(mappedBy = "visitPlan")
     @JsonIgnore
     private Set<PlanItem> planItems;

     @OneToOne(mappedBy = "visitPlan")
     @JsonIgnore
     private PlanApproval planApproval;
}