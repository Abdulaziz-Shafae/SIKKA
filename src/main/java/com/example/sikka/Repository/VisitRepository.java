package com.example.sikka.Repository;

import com.example.sikka.Model.Visit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Integer> {

    Visit findVisitById(Integer id);

    @Query(value = "select * from visit where id = :visitId for update", nativeQuery = true)
    Visit findVisitByIdForUpdate(@Param("visitId") Integer visitId);

    List<Visit> findAllByOrganizer_Id(Integer accountId);
}