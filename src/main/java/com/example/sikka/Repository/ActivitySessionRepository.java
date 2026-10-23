package com.example.sikka.Repository;

import com.example.sikka.Model.ActivitySession;
import com.example.sikka.Model.VisitMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivitySessionRepository extends JpaRepository<ActivitySession, Integer> {

    ActivitySession findActivitySessionById(Integer activitySessionId);

    List<ActivitySession> findAllByVisitMember_IdOrderByIdDesc(Integer visitMemberId);

    List<ActivitySession> findAllByVisitMember_IdAndPlanItem_IdOrderByIdAsc(Integer visitMemberId, Integer planItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ActivitySession a where a.id = :activitySessionId")
    ActivitySession findActivitySessionByIdForUpdate(@Param("activitySessionId") Integer activitySessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from VisitMember m where m.id = :visitMemberId")
    VisitMember findVisitMemberByIdForActivityUpdate(@Param("visitMemberId") Integer visitMemberId);

}