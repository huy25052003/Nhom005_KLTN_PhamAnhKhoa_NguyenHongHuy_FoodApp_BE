package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.User;
import org.example.server.repository.StatsRepository;
import org.example.server.repository.UserRepository;
import org.example.server.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DailyReport {

    private final StatsRepository statsRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // Chạy lúc 23:00 hàng ngày
    @Scheduled(cron = "0 0 23 * * ?")
    public void sendDailyReport() {
        System.out.println("⏰ Đang tạo báo cáo chi tiết...");

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // 1. Lấy tổng quan (Doanh thu, Tổng đơn)
        List<Object[]> stats = statsRepository.overview(start, end);
        BigDecimal revenue = BigDecimal.ZERO;
        Long orders = 0L;

        if (!stats.isEmpty()) {
            Object[] row = stats.get(0);
            revenue = (row[0] != null) ? new BigDecimal(row[0].toString()) : BigDecimal.ZERO;
            orders = (row[1] != null) ? Long.parseLong(row[1].toString()) : 0L;
        }

        // 2. Lấy chi tiết từng món bán ra (Sử dụng hàm có sẵn topProducts)
        List<Object[]> productRows = statsRepository.topProducts(start, end);
        List<Map<String, Object>> productList = new ArrayList<>();

        for (Object[] row : productRows) {
            // row[0]=id, row[1]=name, row[2]=qty, row[3]=revenue
            Map<String, Object> item = new HashMap<>();
            item.put("name", row[1]);
            item.put("qty", row[2]);

            BigDecimal itemRev = (row[3] != null) ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            item.put("revenue", String.format("%,.0f đ", itemRev)); // Format tiền

            productList.add(item);
        }

        // 3. Gửi mail cho Admin
        List<User> admins = userRepository.findByRolesContaining("ADMIN");
        String dateStr = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        for (User admin : admins) {
            if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                emailService.sendDailyReport(admin.getEmail(), dateStr, revenue, orders, productList);
                System.out.println("✅ Đã gửi báo cáo chi tiết cho: " + admin.getEmail());
            }
        }
    }
}
