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

        UserProfile profile = profileRepo.findByUser(user).orElse(null);
        if (profile == null) return Collections.emptyList();

        // 1. Tính toán Calo mục tiêu
        int dailyCalories;
        if (profile.getTargetCalories() != null && profile.getTargetCalories() > 0) {
            dailyCalories = profile.getTargetCalories();
        } else {
            dailyCalories = calculateTDEE(profile);
        }

        // 2. Tính giới hạn calo cho 1 bữa (Chia 3 + buffer 10%)
        int maxCalPerMeal = (int) ((dailyCalories / 3) * 1.1);

        // 3. Tìm sản phẩm phù hợp
        List<Product> recommendations = productRepo.findByActiveTrueAndCaloriesLessThanEqual(maxCalPerMeal);

        if (recommendations.isEmpty()) {
            recommendations = productRepo.findByActiveTrue();
        }

        Collections.shuffle(recommendations);
        return recommendations.stream().limit(4).toList();
    }

    private int calculateTDEE(UserProfile p) {
        if (p.getWeightKg() == null || p.getHeightCm() == null || p.getBirthDate() == null) {
            return 2000;
        }

        int age = Period.between(p.getBirthDate(), LocalDate.now()).getYears();
        double bmr;

        // Mifflin-St Jeor
        if ("MALE".equalsIgnoreCase(p.getGender())) {
            bmr = (10 * p.getWeightKg()) + (6.25 * p.getHeightCm()) - (5 * age) + 5;
        } else {
            bmr = (10 * p.getWeightKg()) + (6.25 * p.getHeightCm()) - (5 * age) - 161;
        }

        double multiplier = switch (p.getActivityLevel() != null ? p.getActivityLevel() : "MODERATE") {
            case "SEDENTARY" -> 1.2;
            case "LIGHT" -> 1.375;
            case "MODERATE" -> 1.55;
            case "ACTIVE" -> 1.725;
            default -> 1.2;
        };

        int maintenance = (int) (bmr * multiplier);

        // --- ĐIỀU CHỈNH THEO MỤC TIÊU ---
        String goal = p.getGoal() != null ? p.getGoal() : "MAINTAIN";
        return switch (goal) {
            case "LOSE" -> Math.max(1200, maintenance - 500); // Giảm cân
            case "GAIN" -> maintenance + 500;                 // Tăng cân
            default -> maintenance;                           // Giữ cân
        };
    }
}