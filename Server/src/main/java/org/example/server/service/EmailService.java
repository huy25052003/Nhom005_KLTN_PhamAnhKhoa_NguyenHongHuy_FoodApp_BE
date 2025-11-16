package org.example.server.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.example.server.entity.Order;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine; // Inject Template Engine

    @Async
    public void sendVerificationCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực FoodApp");

            // Bạn cũng có thể làm template riêng cho verification nếu muốn
            String html = "<h3>Mã xác thực của bạn là: <b style='color:green'>" + code + "</b></h3>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Async
    public void sendOrderConfirmation(String toEmail, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart, "UTF-8" = encoding
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Tạo Context chứa dữ liệu để truyền vào template
            Context context = new Context();
            context.setVariable("order", order);

            // "order-confirmation" là tên file trong thư mục templates (không cần đuôi .html)
            String htmlContent = templateEngine.process("order-confirmation", context);

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng #" + order.getId() + " - FoodApp");
            helper.setText(htmlContent, true); // true = gửi dưới dạng HTML

            mailSender.send(message);
            System.out.println("Đã gửi mail Thymeleaf xác nhận đơn hàng: " + order.getId());

        } catch (MessagingException e) {
            System.err.println("Lỗi gửi mail: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
