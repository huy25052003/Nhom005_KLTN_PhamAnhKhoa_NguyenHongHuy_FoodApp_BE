package org.example.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.server.entity.Cart;
import org.example.server.entity.Order;
import org.example.server.entity.OrderItem;
import org.example.server.entity.Payment;
import org.example.server.entity.Product;
import org.example.server.entity.User;
import org.example.server.repository.CartRepository;
import org.example.server.repository.OrderRepository;
import org.example.server.repository.PaymentRepository;
import org.example.server.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PayOS payOS;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CartRepository cartRepository,
            @Value("${payos.client-id}") String clientId,
            @Value("${payos.api-key}") String apiKey,
            @Value("${payos.checksum-key}") String checksumKey
    ) {
        this.payOS = new PayOS(clientId, apiKey, checksumKey);
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional
    public String createPaymentLink(Long orderId) throws Exception {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Ðon hàng không t?n t?i"));

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Ch? có th? thanh toán cho don dang PENDING");
        }

        String retUrl = appendOrderId(returnUrl, orderId);
        String canUrl = appendOrderId(cancelUrl, orderId);

        // T?o request thanh toán
        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(orderId)
                .amount(order.getTotal().longValue()) // S?A L?I T?I ÐÂY: intValue() -> longValue()
                .description("Thanh toan don " + orderId)
                .returnUrl(retUrl)
                .cancelUrl(canUrl)
                // .items(...) // T?m th?i b? qua items d? tránh l?i thi?u class ItemData
                .build();

        CreatePaymentLinkResponse res = payOS.paymentRequests().create(request);

        Payment payment = paymentRepository.findByPayosOrderId(String.valueOf(orderId));
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .amount(order.getTotal())
                    .payosOrderId(String.valueOf(orderId))
                    .payosPaymentLinkId(res.getPaymentLinkId())
                    .status("PENDING")
                    .paymentMethod("PAYOS")
                    .build();
        } else {
            payment.setPayosPaymentLinkId(res.getPaymentLinkId());
            payment.setStatus("PENDING");
        }
        paymentRepository.save(payment);

        return res.getCheckoutUrl();
    }

    private static String appendOrderId(String base, Long id) {
        if (base == null || base.isBlank()) return base;
        return base + (base.contains("?") ? "&" : "?") + "orderId=" + id;
    }

    @Transactional
    public void handleWebhook(ObjectNode webhookBody) {
        try {
            // Chuy?n ObjectNode thành chu?i JSON d? verify
            String webhookBodyStr = objectMapper.writeValueAsString(webhookBody);

            // Dùng var d? t? d?ng nh?n ki?u d? li?u tr? v? t? SDK
            var verifiedData = payOS.webhooks().verify(webhookBodyStr);

            log.info("Webhook verified: {}", verifiedData);

            // L?y d? li?u t? verifiedData
            Long orderCode = verifiedData.getOrderCode();
            String code = verifiedData.getCode();
            String desc = verifiedData.getDesc();

            if (orderCode == null) {
                log.warn("Không xác d?nh du?c orderCode t? webhook");
                return;
            }

            Payment payment = paymentRepository.findByPayosOrderId(String.valueOf(orderCode));
            Order order = (payment != null) ? payment.getOrder() : orderRepository.findByIdWithDetails(orderCode).orElse(null);

            if (order == null) {
                log.warn("Không tìm th?y Order cho orderCode={}", orderCode);
                return;
            }

            // Map tr?ng thái
            String payStatus = "FAILED";
            if ("00".equals(code) || "success".equalsIgnoreCase(desc)) {
                payStatus = "SUCCESS";
            }

            if (payment != null) {
                payment.setStatus(payStatus);
                paymentRepository.save(payment);
            }

            if ("SUCCESS".equalsIgnoreCase(payStatus)) {
                if ("PENDING".equalsIgnoreCase(order.getStatus())) {
                    order.setStatus("CONFIRMED");
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                    clearUserCart(order.getUser());
                }
            } else if ("FAILED".equalsIgnoreCase(payStatus) || "CANCELLED".equalsIgnoreCase(payStatus)) {
                if ("PENDING".equalsIgnoreCase(order.getStatus())) {
                    order.setStatus("CANCELLED");
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                    // Hoàn l?i kho
                    for (OrderItem it : order.getItems()) {
                        Product p = it.getProduct();
                        p.setStock(p.getStock() + it.getQuantity());
                        productRepository.save(p);
                    }
                }
            }
        } catch (Exception e) {
            log.error("L?i x? lý webhook: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void clearUserCart(User user) {
        if (user == null) return;
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }
}