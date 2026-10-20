package com.example.sikka.Repository;

import com.example.sikka.Model.VisitPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitPlanRepository extends JpaRepository<VisitPlan, Integer> {

    VisitPlan findVisitPlanById(Integer id);

}