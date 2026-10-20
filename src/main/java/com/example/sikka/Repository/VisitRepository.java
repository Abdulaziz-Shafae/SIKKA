package com.example.sikka.Repository;

import com.example.sikka.Model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Integer> {
    Visit findVisitById(Integer id);
}