package org.example.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server.entity.Order;
import org.example.server.entity.OrderItem;
import org.example.server.entity.Payment;
import org.example.server.entity.Product;
import org.example.server.repository.OrderRepository;
import org.example.server.repository.PaymentRepository;
import org.example.server.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PayOS payOS;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
                          ProductRepository productRepository,
                          @Value("${payos.client-id}") String clientId,
                          @Value("${payos.api-key}") String apiKey,
                          @Value("${payos.checksum-key}") String checksumKey) {
        this.payOS = new PayOS(clientId, apiKey, checksumKey);
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.objectMapper = new ObjectMapper();
    }

    public String createPaymentLink(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể thanh toán cho đơn hàng đang chờ xử lý (PENDING)");
        }

        List<ItemData> itemDataList = order.getItems().stream()
                .map(item -> ItemData.builder()
                        .name(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice().intValue())
                        .build())
                .collect(Collectors.toList());

        // Sửa orderCode thành orderId (số) thay vì chuỗi "ORDER_" + orderId
        PaymentData paymentData = PaymentData.builder()
                .orderCode(orderId) // Sử dụng orderId trực tiếp
                .amount(order.getTotal().intValue())
                .description("Thanh toán đơn hàng #" + orderId)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .items(itemDataList)
                .build();

        CheckoutResponseData response;
        try {
            response = payOS.createPaymentLink(paymentData);
        } catch (Exception e) {
            logger.error("Lỗi khi tạo link thanh toán cho orderId {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Không thể tạo link thanh toán: " + e.getMessage());
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotal())
                .payosOrderId(String.valueOf(orderId)) // Lưu orderId dưới dạng chuỗi
                .payosPaymentLinkId(response.getPaymentLinkId())
                .status("PENDING")
                .paymentMethod("PAYOS")
                .build();

        paymentRepository.save(payment);

        return response.getCheckoutUrl();
    }

    public void handleWebhook(String webhookBody) {
        try {
            Webhook webhook = objectMapper.readValue(webhookBody, Webhook.class);
            WebhookData webhookData = payOS.verifyPaymentWebhookData(webhook);
            if (webhookData != null) {
                logger.info("WebhookData received: {}", objectMapper.writeValueAsString(webhookData));

                // Lấy dữ liệu từ webhookBody để xử lý linh hoạt
                JsonNode jsonNode = objectMapper.readTree(webhookBody);
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode == null) {
                    logger.warn("Dữ liệu webhook không hợp lệ: thiếu node 'data'");
                    return;
                }

                // Lấy orderCode dưới dạng chuỗi và chuyển sang Long nếu cần
                String orderCodeStr = dataNode.get("orderCode").asText();
                Long orderCode = null;
                try {
                    orderCode = Long.parseLong(orderCodeStr.replaceAll("[^0-9]", "")); // Lọc số từ chuỗi
                } catch (NumberFormatException e) {
                    logger.warn("orderCode không hợp lệ: {}, bỏ qua", orderCodeStr);
                    return;
                }

                String status = dataNode.get("status").asText();
                String signature = jsonNode.get("signature") != null ? jsonNode.get("signature").asText() : null;

                Payment payment = paymentRepository.findByPayosOrderId(orderCodeStr); // Tìm theo chuỗi gốc
                if (payment != null) {
                    payment.setStatus(status);
                    payment.setPayosSignature(signature);
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    if ("SUCCESS".equals(status)) {
                        if (!"PENDING".equals(order.getStatus())) {
                            logger.warn("Đơn hàng {} không ở trạng thái PENDING, bỏ qua cập nhật", order.getId());
                            return;
                        }
                        order.setStatus("PAID");
                        orderRepository.save(order);
                    } else if ("CANCELLED".equals(status) || "FAILED".equals(status)) {
                        if (!"PENDING".equals(order.getStatus())) {
                            logger.warn("Đơn hàng {} không ở trạng thái PENDING, bỏ qua hoàn stock", order.getId());
                            return;
                        }
                        order.setStatus("FAILED");
                        orderRepository.save(order);

                        for (OrderItem item : order.getItems()) {
                            Product product = item.getProduct();
                            product.setStock(product.getStock() + item.getQuantity());
                            productRepository.save(product);
                        }
                    }
                } else {
                    logger.warn("Webhook không hợp lệ hoặc không tìm thấy payment cho orderCode: {}", orderCodeStr);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Lỗi phân tích JSON webhook: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Lỗi xử lý webhook: {}", e.getMessage());
        }
    }
}