package com.example.sikka.Repository;

import com.example.sikka.Model.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitRecordRepository extends JpaRepository<VisitRecord, Integer> {

    VisitRecord findVisitRecordById(Integer id);

}