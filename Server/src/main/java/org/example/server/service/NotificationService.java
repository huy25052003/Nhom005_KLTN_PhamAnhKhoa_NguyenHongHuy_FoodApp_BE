package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Notification;
import org.example.server.entity.Order;
import org.example.server.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepo;
    private final SimpMessagingTemplate simp;

    @Transactional(readOnly = true)
    public Notification newOrderNotify(Order order) {
        Notification n = Notification.builder()
                .type("NEW_ORDER")
                .title("Đơn hàng mới #" + order.getId())
                .message("Người dùng " + order.getUser().getUsername() + " vừa đặt hàng. Tổng: " + order.getTotal())
                .orderId(order.getId())
                .build();
        Notification saved = notificationRepo.save(n);

        simp.convertAndSend("/topic/admin/orders", saved);
        return saved;
    }
    @Transactional(readOnly = true)
    public Notification paymentPaidNotify(Order order) {
        Notification n = Notification.builder()
                .type("PAYMENT_PAID")
                .title("Thanh toán thành công #" + order.getId())
                .message("Đơn #" + order.getId() + " đã được thanh toán. Trạng thái: " + order.getStatus())
                .orderId(order.getId())
                .build();
        Notification saved = notificationRepo.save(n);

        simp.convertAndSend("/topic/admin/orders", saved);
        return saved;
    }
    @Transactional(readOnly = true)
    public Notification paymentFailedNotify(Order order, String reason) {
        Notification n = Notification.builder()
                .type("PAYMENT_FAILED")
                .title("Thanh toán thất bại #" + order.getId())
                .message("Đơn #" + order.getId() + " thanh toán thất bại: " + reason)
                .orderId(order.getId())
                .build();
        Notification saved = notificationRepo.save(n);
        simp.convertAndSend("/topic/admin/orders", saved);
        return saved;
    }
    @Transactional(readOnly = true)
    public void notifyKitchenOfNewOrder(Order order) {
        simp.convertAndSend("/topic/kitchen/new-order", order);
    }
}
