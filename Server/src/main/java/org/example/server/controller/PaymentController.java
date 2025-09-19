package org.example.server.controller;

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

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String webhookBody) throws Exception {
        paymentService.handleWebhook(webhookBody);
        return ResponseEntity.ok().build();
    }
}