package com.example.sikka.Repository;

import com.example.sikka.Model.VisitRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitRecordRepository extends JpaRepository<VisitRecord, Integer> {

    VisitRecord findVisitRecordById(Integer id);

    List<VisitRecord> findAllByVisitMember_IdOrderByCreatedAtDescIdDesc(Integer visitMemberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from VisitRecord r where r.id = :visitRecordId")
    VisitRecord findVisitRecordByIdForUpdate(@Param("visitRecordId") Integer visitRecordId);

    boolean existsByVisitMember_IdAndRide_IdAndStatus(Integer memberId, Integer rideId, String status);

}