package org.example.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể thanh toán cho đơn đang PENDING");
        }

        List<ItemData> items = order.getItems().stream()
                .map(it -> ItemData.builder()
                        .name(it.getProduct().getName())
                        .quantity(it.getQuantity())
                        .price(it.getPrice().intValue())
                        .build())
                .collect(Collectors.toList());

        String retUrl = appendOrderId(returnUrl, orderId);
        String canUrl = appendOrderId(cancelUrl, orderId);

        PaymentData paymentData = PaymentData.builder()
                .orderCode(orderId) // dùng orderId trực tiếp (kiểu số)
                .amount(order.getTotal().intValue())
                .description("Thanh toán đơn hàng " + orderId)
                .returnUrl(retUrl)
                .cancelUrl(canUrl)
                .items(items)
                .build();

        CheckoutResponseData res = payOS.createPaymentLink(paymentData);

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
    public void handleWebhook(String webhookBody) {
        try {
            Webhook webhook = objectMapper.readValue(webhookBody, Webhook.class);
            WebhookData data = payOS.verifyPaymentWebhookData(webhook);
            if (data == null) {
                log.warn("Webhook verify thất bại");
                return;
            }
            log.info("WebhookData received: {}", objectMapper.writeValueAsString(data));
            Long orderCode = null;
            try {
                orderCode = Long.valueOf(String.valueOf(data.getOrderCode()));
            } catch (Exception ignore) {}

            if (orderCode == null) {
                log.warn("Không xác định được orderCode từ webhook");
                return;
            }
            Payment payment = paymentRepository.findByPayosOrderId(String.valueOf(orderCode));
            Order order = (payment != null) ? payment.getOrder() : orderRepository.findByIdWithDetails(orderCode).orElse(null);

            if (order == null) {
                log.warn("Không tìm thấy Order cho orderCode={}", orderCode);
                return;
            }
            String payStatus = mapPayosStatus(data);
            if (payment != null) {
                payment.setStatus(payStatus);
                payment.setPayosSignature(webhook.getSignature());
                paymentRepository.save(payment);
            }

            if ("SUCCESS".equalsIgnoreCase(payStatus)) {
                if ("PENDING".equalsIgnoreCase(order.getStatus())) {
                    order.setStatus("CONFIRMED");
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                    clearUserCart(order.getUser());
                } else {
                    log.info("Order {} không ở PENDING ({}), bỏ qua đổi trạng thái",
                            order.getId(), order.getStatus());
                }
            } else if ("FAILED".equalsIgnoreCase(payStatus) || "CANCELLED".equalsIgnoreCase(payStatus)) {
                if ("PENDING".equalsIgnoreCase(order.getStatus())) {
                    order.setStatus("CANCELLED");
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                    for (OrderItem it : order.getItems()) {
                        Product p = it.getProduct();
                        p.setStock(p.getStock() + it.getQuantity());
                        productRepository.save(p);
                    }
                }
            } else {
                log.info("Trạng thái webhook không xác định: {}", payStatus);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý webhook: {}", e.getMessage(), e);
        }
    }

    private String mapPayosStatus(WebhookData data) {
        try {
            String code = data.getCode();
            String desc = data.getDesc();
            if ("00".equals(code) || "success".equalsIgnoreCase(desc)) return "SUCCESS";
        } catch (Exception ignore) {}
        return "FAILED";
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
