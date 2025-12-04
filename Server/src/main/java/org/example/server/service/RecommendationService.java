package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Product;
import org.example.server.entity.User;
import org.example.server.entity.UserProfile;
import org.example.server.repository.ProductRepository;
import org.example.server.repository.UserProfileRepository;
import org.example.server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public List<Product> getRecommendedProducts(Authentication auth) {
        String username = auth.getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy Profile, nếu chưa có thì trả về rỗng (Client sẽ xử lý hiển thị form nhập)
        UserProfile profile = profileRepo.findByUser(user).orElse(null);
        if (profile == null) {
            return Collections.emptyList();
        }

        // 1. Xác định Calo mục tiêu mỗi ngày (Daily Target)
        int dailyCalories;
        if (profile.getTargetCalories() != null && profile.getTargetCalories() > 0) {
            dailyCalories = profile.getTargetCalories();
        } else {
            // Nếu user chưa set mục tiêu, tự tính TDEE
            dailyCalories = calculateTDEE(profile);
        }

        // 2. Chia cho 3 bữa ăn chính để ra Calo mục tiêu cho 1 bữa
        // Ví dụ: Cần 1800 calo/ngày => Mỗi bữa nên ăn khoảng 600 calo
        int mealTarget = dailyCalories / 3;

        // 3. Gợi ý các món ăn phù hợp (ví dụ: <= target + 10% buffer)
        int maxCalPerMeal = (int) (mealTarget * 1.1);

        // Lấy các món có calo phù hợp
        List<Product> recommendations = productRepo.findByActiveTrueAndCaloriesLessThanEqual(maxCalPerMeal);

        // Nếu không có món nào (do set calo quá thấp), lấy tạm 5 món ít calo nhất
        if (recommendations.isEmpty()) {
            // Logic dự phòng: Bạn có thể query sort by calories ASC limit 5
            // Ở đây mình trả về rỗng để đơn giản
        }

        // Random hoặc lấy tối đa 4-8 món để hiển thị
        Collections.shuffle(recommendations);
        return recommendations.stream().limit(4).toList();
    }

    /**
     * Tính TDEE dựa trên công thức Mifflin-St Jeor
     */
    private int calculateTDEE(UserProfile p) {
        if (p.getWeightKg() == null || p.getHeightCm() == null || p.getBirthDate() == null) {
            return 2000; // Mặc định nếu thiếu thông tin
        }

        int age = Period.between(p.getBirthDate(), LocalDate.now()).getYears();
        double bmr;

        // Công thức Mifflin-St Jeor
        if ("MALE".equalsIgnoreCase(p.getGender())) {
            bmr = (10 * p.getWeightKg()) + (6.25 * p.getHeightCm()) - (5 * age) + 5;
        } else { // FEMALE
            bmr = (10 * p.getWeightKg()) + (6.25 * p.getHeightCm()) - (5 * age) - 161;
        }

        // Nhân hệ số vận động (Activity Level)
        double multiplier = switch (p.getActivityLevel() != null ? p.getActivityLevel() : "MODERATE") {
            case "SEDENTARY" -> 1.2;      // Ít vận động
            case "LIGHT" -> 1.375;        // Nhẹ
            case "MODERATE" -> 1.55;      // Vừa
            case "ACTIVE" -> 1.725;       // Năng động
            default -> 1.2;
        };

        return (int) (bmr * multiplier);
    }
}