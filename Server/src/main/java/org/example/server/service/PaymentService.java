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

        String orderCode = String.valueOf(orderId);
        PaymentData paymentData = PaymentData.builder()
                .orderCode(Long.parseLong(orderCode))
                .amount(order.getTotal().intValue())
                .description("Thanh toán đơn hàng #" + orderId)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .items(itemDataList)
                .build();

        CheckoutResponseData response;
        try {
            response = payOS.createPaymentLink(paymentData);
            logger.info("Tạo link thanh toán thành công cho orderId: {}, orderCode: {}, URL: {}",
                    orderId, orderCode, response.getCheckoutUrl());
        } catch (Exception e) {
            logger.error("Lỗi khi tạo link thanh toán cho orderId {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo link thanh toán: " + e.getMessage());
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotal())
                .payosOrderId(orderCode)
                .payosPaymentLinkId(response.getPaymentLinkId())
                .status("PENDING")
                .paymentMethod("PAYOS")
                .build();

        paymentRepository.save(payment);
        logger.info("Đã lưu payment cho orderId: {}, payosOrderId: {}", orderId, payment.getPayosOrderId());

        return response.getCheckoutUrl();
    }

    public void handleWebhook(String webhookBody) {
        logger.info("Nhận webhook: {}", webhookBody);
        try {
            Webhook webhook = objectMapper.readValue(webhookBody, Webhook.class);
            logger.info("Webhook đã parse: {}", objectMapper.writeValueAsString(webhook));
            WebhookData webhookData = payOS.verifyPaymentWebhookData(webhook);
            logger.info("Kết quả xác minh webhook: {}", webhookData != null ? "Hợp lệ" : "Không hợp lệ");

            if (webhookData != null) {
                JsonNode jsonNode = objectMapper.readTree(webhookBody);
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode == null) {
                    logger.warn("Webhook không hợp lệ: Thiếu node 'data'");
                    return;
                }

                String orderCodeStr = String.valueOf(dataNode.get("orderCode").asText());
                JsonNode codeNode = dataNode.get("code");
                JsonNode descNode = dataNode.get("desc");
                if (codeNode == null || descNode == null) {
                    logger.warn("Webhook không hợp lệ: Thiếu 'code' hoặc 'desc'");
                    return;
                }
                String code = codeNode.asText();
                String desc = descNode.asText();
                String signature = jsonNode.get("signature") != null ? jsonNode.get("signature").asText() : null;
                logger.info("orderCode: {}, code: {}, desc: {}, signature: {}", orderCodeStr, code, desc, signature);

                Payment payment = paymentRepository.findByPayosOrderId(orderCodeStr);
                if (payment != null) {
                    logger.info("Tìm thấy payment ID: {}, Trạng thái hiện tại: {}", payment.getId(), payment.getStatus());
                    payment.setStatus(code.equals("00") && desc.equalsIgnoreCase("success") ? "PAID" : "FAILED");
                    payment.setPayosSignature(signature);
                    paymentRepository.save(payment);
                    logger.info("Đã lưu payment ID: {}, Trạng thái mới: {}", payment.getId(), payment.getStatus());

                    Order order = payment.getOrder();
                    logger.info("Đơn hàng ID: {}, Trạng thái hiện tại: {}", order.getId(), order.getStatus());
                    if (code.equals("00") && desc.equalsIgnoreCase("success")) {
                        if (!"PENDING".equals(order.getStatus())) {
                            logger.warn("Đơn hàng {} không ở trạng thái PENDING, bỏ qua cập nhật", order.getId());
                            return;
                        }
                        order.setStatus("PAID");
                        orderRepository.save(order);
                        logger.info("Đã cập nhật đơn hàng {} thành PAID", order.getId());
                    } else if (code.equals("99") || desc.equalsIgnoreCase("cancelled") || desc.equalsIgnoreCase("failed")) {
                        if (!"PENDING".equals(order.getStatus())) {
                            logger.warn("Đơn hàng {} không ở trạng thái PENDING, bỏ qua hoàn stock", order.getId());
                            return;
                        }
                        order.setStatus("FAILED");
                        orderRepository.save(order);
                        logger.info("Đã cập nhật đơn hàng {} thành FAILED", order.getId());

                        for (OrderItem item : order.getItems()) {
                            Product product = item.getProduct();
                            product.setStock(product.getStock() + item.getQuantity());
                            productRepository.save(product);
                            logger.info("Hoàn stock cho sản phẩm {}: +{}", product.getId(), item.getQuantity());
                        }
                    } else {
                        logger.warn("Trạng thái webhook không được hỗ trợ: code={}, desc={}", code, desc);
                    }
                } else {
                    logger.warn("Không tìm thấy payment cho orderCode: {}", orderCodeStr);
                }
            } else {
                logger.warn("Xác minh webhook thất bại");
            }
        } catch (JsonProcessingException e) {
            logger.error("Lỗi parse JSON webhook: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Lỗi xử lý webhook: {}", e.getMessage(), e);
        }
    }

    public Payment getPaymentByPayosOrderId(String orderCode) {
        logger.info("Tìm payment cho orderCode: {}", orderCode);
        Payment payment = paymentRepository.findByPayosOrderId(orderCode);
        if (payment == null) {
            logger.warn("Không tìm thấy payment cho orderCode: {}", orderCode);
        }
        return payment;
    }

    public void handleCancelRedirect(String orderCode) {
        logger.info("Xử lý redirect hủy cho orderCode: {}", orderCode);
        Payment payment = paymentRepository.findByPayosOrderId(orderCode);
        if (payment != null && "PENDING".equals(payment.getStatus())) {
            payment.setStatus("CANCELLED");
            paymentRepository.save(payment);
            logger.info("Đã cập nhật payment {} thành CANCELLED", payment.getId());

            Order order = payment.getOrder();
            if ("PENDING".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                logger.info("Đã cập nhật đơn hàng {} thành CANCELLED", order.getId());

                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                    logger.info("Hoàn stock cho sản phẩm {}: +{}", product.getId(), item.getQuantity());
                }
            } else {
                logger.warn("Đơn hàng {} không ở trạng thái PENDING, bỏ qua cập nhật", order.getId());
            }
        } else {
            logger.warn("Không tìm thấy payment hoặc trạng thái không hợp lệ cho orderCode: {}", orderCode);
        }
    }
}