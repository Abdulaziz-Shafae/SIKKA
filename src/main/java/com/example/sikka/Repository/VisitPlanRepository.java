package com.example.sikka.Repository;

import com.example.sikka.Model.VisitPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface VisitPlanRepository extends JpaRepository<VisitPlan, Integer> {

    VisitPlan findVisitPlanById(Integer id);

    List<VisitPlan> findAllByVisit_Id(Integer visitId);

    boolean existsByVisit_IdAndPlanKind(Integer visitId, String planKind);

    boolean existsByVisit_IdAndOwnerVisitMember_IdAndPlanKind(Integer visitId, Integer visitMemberId, String planKind);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from VisitPlan p where p.id = :visitPlanId")
    VisitPlan findVisitPlanByIdForUpdate(@Param("visitPlanId") Integer visitPlanId);

    @Query("SELECT DISTINCT vp FROM VisitPlan vp JOIN vp.planItems pi JOIN pi.ride r WHERE vp.planKind = 'GROUP' AND vp.isActive = true AND vp.visit.status = 'IN_PROGRESS' AND r.id = :rideId")
    List<VisitPlan> findActiveGroupPlansAffectedByRide(@Param("rideId") Integer rideId);

}