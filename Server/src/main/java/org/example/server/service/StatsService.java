// org/example/server/service/StatsService.java
package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Product;
import org.example.server.repository.StatsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsRepository repo;

    private LocalDateTime[] range(String from, String to) {
        LocalDate f = (from == null || from.isBlank()) ? LocalDate.now().minusDays(6) : LocalDate.parse(from);
        LocalDate t = (to   == null || to.isBlank())   ? LocalDate.now()               : LocalDate.parse(to);
        return new LocalDateTime[]{ f.atStartOfDay(), t.plusDays(1).atStartOfDay() };
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        if (v instanceof BigDecimal bd) return bd.longValue();
        return Long.parseLong(String.valueOf(v));
    }

    public Map<String,Object> overview(String from, String to) {
        var r = range(from, to);
        List<Object[]> rows = repo.overview(r[0], r[1]);      // ✅ giờ trả List<Object[]>
        Object[] row = rows.isEmpty() ? new Object[]{0,0,0} : rows.get(0);

        long revenue = toLong(row[0]);
        long orders  = toLong(row[1]);
        long items   = toLong(row[2]);
        double aov   = orders == 0 ? 0.0 : (double) revenue / orders;

        return Map.of("revenue", revenue, "orders", orders, "items", items, "avgOrderValue", aov);
    }

    public List<Map<String,Object>> revenueSeries(String from, String to) {
        var r = range(from, to);
        List<Map<String,Object>> out = new ArrayList<>();
        for (Object[] row : repo.revenueByDay(r[0], r[1])) {
            // row[0] thường là java.sql.Date hoặc String 'yyyy-MM-dd'
            String d = String.valueOf(row[0]);
            long revenue = toLong(row[1]);
            out.add(Map.of("date", d, "revenue", revenue));
        }
        return out;
    }

    public List<Map<String,Object>> topProducts(String from, String to, int limit) {
        var r = range(from, to);
        List<Map<String,Object>> out = new ArrayList<>();
        for (Object[] row : repo.topProducts(r[0], r[1])) {
            out.add(new LinkedHashMap<>() {{
                put("productId", toLong(row[0]));
                put("name", row[1]);
                put("quantity", toLong(row[2]));
                put("revenue", toLong(row[3]));
            }});
            if (out.size() >= limit) break;
        }
        return out;
    }

    public List<Map<String,Object>> ordersByStatus(String from, String to) {
        var r = range(from, to);
        List<Map<String,Object>> out = new ArrayList<>();
        for (Object[] row : repo.ordersByStatus(r[0], r[1])) {
            out.add(Map.of(
                    "status", String.valueOf(row[0]),
                    "count",  toLong(row[1])
            ));
        }
        return out;
    }

    public List<Map<String,Object>> lowStock(int threshold, int limit) {
        List<Product> list = repo.lowStock(threshold);
        List<Map<String,Object>> out = new ArrayList<>();
        for (Product p : list) {
            out.add(Map.of("id", p.getId(), "name", p.getName(), "stock", p.getStock()));
            if (out.size() >= limit) break;
        }
        return out;
    }
}
