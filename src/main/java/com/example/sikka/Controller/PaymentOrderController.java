package com.example.sikka.Controller;

import com.example.sikka.Service.PaymentOrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-order")
@AllArgsConstructor
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    @GetMapping("/get/my/orders/{accountId}")
    public ResponseEntity<?> getMyPaymentOrders(@PathVariable Integer accountId){
        return ResponseEntity.status(200).body(paymentOrderService.getMyPaymentOrders(accountId));
    }

    @GetMapping("/get/details/{accountId}/{paymentOrderId}")
    public ResponseEntity<?> getPaymentOrderById(@PathVariable Integer accountId, @PathVariable Integer paymentOrderId){
        return ResponseEntity.status(200).body(paymentOrderService.getPaymentOrderById(accountId, paymentOrderId));
    }

    @GetMapping("/get/visit/{accountId}/{visitId}")
    public ResponseEntity<?> getVisitPaymentOrders(@PathVariable Integer accountId, @PathVariable Integer visitId){
        return ResponseEntity.status(200).body(paymentOrderService.getVisitPaymentOrders(accountId, visitId));
    }

    @PostMapping("/add/{accountId}/{visitId}")
    public ResponseEntity<?> addPaymentOrder(@PathVariable Integer accountId, @PathVariable Integer visitId){
        return ResponseEntity.status(200).body(paymentOrderService.addPaymentOrder(accountId, visitId));
    }
}