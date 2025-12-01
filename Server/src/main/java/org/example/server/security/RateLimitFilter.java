package org.example.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Lưu trữ thông tin request của từng IP
    // Key: IP Address, Value: Cặp (Thời gian bắt đầu, Số lượng request)
    private final Map<String, RequestInfo> ipRequestMap = new ConcurrentHashMap<>();

    // CẤU HÌNH GIỚI HẠN
    private static final int MAX_REQUESTS = 100;      // Cho phép tối đa 50 request
    private static final long DURATION = 20 * 1000;  // Trong vòng 1 phút

    private static class RequestInfo {
        long startTime;
        int count;

        public RequestInfo(long startTime, int count) {
            this.startTime = startTime;
            this.count = count;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        ipRequestMap.compute(clientIp, (ip, info) -> {
            if (info == null || (now - info.startTime > DURATION)) {
                // Nếu chưa có hoặc đã qua 1 phút -> Reset bộ đếm
                return new RequestInfo(now, 1);
            } else {
                // Nếu còn trong 1 phút -> Tăng bộ đếm
                info.count++;
                return info;
            }
        });

        RequestInfo info = ipRequestMap.get(clientIp);

        if (info.count > MAX_REQUESTS) {
            // Chặn request nếu vượt quá giới hạn
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            response.getWriter().flush();
            return; // Dừng lại, không cho đi tiếp
        }

        // Cho phép đi tiếp
        filterChain.doFilter(request, response);
    }

    // Hàm lấy IP thật của client (xử lý trường hợp qua proxy/load balancer)
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Dọn dẹp bộ nhớ định kỳ (Optional: để tránh Map bị phình to nếu chạy lâu dài)
    // Bạn có thể dùng @Scheduled để clear map mỗi vài giờ nếu cần.
}