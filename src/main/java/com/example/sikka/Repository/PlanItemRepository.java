package com.example.sikka.Repository;

import com.example.sikka.Model.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanItemRepository extends JpaRepository<PlanItem, Integer> {

    PlanItem findPlanItemById(Integer id);

}