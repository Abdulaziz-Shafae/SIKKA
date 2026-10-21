package com.example.sikka.Repository;

import com.example.sikka.Model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Integer> {

    boolean existsByVisit_IdAndStatusAndVerifiedAtIsNotNull(Integer visitId, String status);
}