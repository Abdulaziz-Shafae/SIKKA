package com.example.sikka.Repository;

import com.example.sikka.Model.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanItemRepository extends JpaRepository<PlanItem, Integer> {

    PlanItem findPlanItemById(Integer id);

    List<PlanItem> findAllByVisitPlan_Id(Integer visitPlanId);


}
