package com.example.sikka.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"visit_id", "visitor_profile_id"}))
public class VisitMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Pattern(regexp = "^(INVITED|GOING|MAYBE|NOT_GOING)$",
            message = "rsvp status must be INVITED, GOING, MAYBE, or NOT_GOING")
    @Column(columnDefinition = "varchar(15) not null default 'INVITED'")
    private String rsvpStatus;

    @Column(columnDefinition = "json")
    private String selectedRides;

    @Column(columnDefinition = "datetime")
    private LocalDateTime checkedInAt;


    @Column(columnDefinition = "tinyint")
    private Integer visitRating;

    @Column(columnDefinition = "text")
    private String visitReview;

    @Column(columnDefinition = "datetime")
    private LocalDateTime invitedAt;

    @ManyToOne
    @JoinColumn(name = "visit_id", nullable = false)
    @JsonIgnore
    private Visit visit;

    @ManyToOne
    @JoinColumn(name = "visitor_profile_id", nullable = false)
    @JsonIgnore
    private VisitorProfile visitorProfile;

    @OneToMany(mappedBy = "ownerVisitMember")
    @JsonIgnore
    private Set<VisitPlan> visitPlans;

    @OneToMany(mappedBy = "visitMember")
    @JsonIgnore
    private Set<VisitRecord> visitRecords;


}