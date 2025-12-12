package org.example.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.server.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create/{orderId}")
    public ResponseEntity<String> createPayment(@PathVariable Long orderId) throws Exception {
        String paymentUrl = paymentService.createPaymentLink(orderId);
        return ResponseEntity.ok(paymentUrl);
    }

    // S?a l?i: Nh?n ObjectNode d? tránh l?i "cannot find symbol class Webhook"
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody ObjectNode webhookBody) throws Exception {
        paymentService.handleWebhook(webhookBody);
        return ResponseEntity.ok("Webhook received");
    }
}