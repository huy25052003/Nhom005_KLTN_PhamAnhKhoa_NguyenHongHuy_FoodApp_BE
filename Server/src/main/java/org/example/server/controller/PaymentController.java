package org.example.server.controller;

import org.example.server.entity.Payment;
import org.example.server.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create/{orderId}")
    public ResponseEntity<String> createPayment(@PathVariable Long orderId) throws Exception {
        logger.info("Tạo link thanh toán cho orderId: {}", orderId);
        String paymentUrl = paymentService.createPaymentLink(orderId);
        return ResponseEntity.ok(paymentUrl);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String webhookBody) {
        logger.info("Nhận webhook request: {}", webhookBody);
        try {
            paymentService.handleWebhook(webhookBody);
            logger.info("Webhook xử lý thành công");
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            logger.error("Lỗi xử lý webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Lỗi xử lý webhook: " + e.getMessage());
        }
    }

    @GetMapping("/success")
    public ResponseEntity<String> success(@RequestParam(required = false) String orderCode) {
        logger.info("Redirect đến success với orderCode: {}", orderCode);
        try {
            if (orderCode == null || orderCode.isEmpty()) {
                logger.warn("Thiếu orderCode trong redirect success");
                return ResponseEntity.badRequest().body("Thiếu thông tin orderCode.");
            }
            Payment payment = paymentService.getPaymentByPayosOrderId(orderCode);
            if (payment != null) {
                logger.info("Payment cho orderCode {}: Trạng thái = {}", orderCode, payment.getStatus());
                return ResponseEntity.ok("Thanh toán thành công. Đang chuyển hướng... OrderCode: " + orderCode + ", Trạng thái: " + payment.getStatus());
            } else {
                logger.warn("Không tìm thấy payment cho orderCode: {}", orderCode);
                return ResponseEntity.ok("Thanh toán thành công nhưng không tìm thấy thông tin payment. OrderCode: " + orderCode);
            }
        } catch (Exception e) {
            logger.error("Lỗi xử lý redirect success cho orderCode {}: {}", orderCode, e.getMessage(), e);
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> cancel(@RequestParam(required = false) String orderCode) {
        logger.info("Redirect đến cancel với orderCode: {}", orderCode);
        try {
            if (orderCode == null || orderCode.isEmpty()) {
                logger.warn("Thiếu orderCode trong redirect cancel");
                return ResponseEntity.badRequest().body("Thiếu thông tin orderCode.");
            }
            paymentService.handleCancelRedirect(orderCode);
            return ResponseEntity.ok("Thanh toán đã hủy. Đang chuyển hướng... OrderCode: " + orderCode);
        } catch (Exception e) {
            logger.error("Lỗi xử lý redirect cancel cho orderCode {}: {}", orderCode, e.getMessage(), e);
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}