package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.User;
import org.example.server.entity.UserProfile;
import org.example.server.repository.UserProfileRepository;
import org.example.server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;

    @Transactional( )
    public UserProfile getMyProfile(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return profileRepo.findByUser(user)
                .orElseGet(() -> {
                    // chưa có thì trả object rỗng (không lưu) hoặc auto tạo:
                    UserProfile p = new UserProfile();
                    p.setUser(user);
                    return p;
                });
    }

    @Transactional
    public UserProfile upsertMyProfile(Authentication auth, Map<String, Object> body) {
        User user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile p = profileRepo.findByUser(user).orElse(null);
        if (p == null) {
            p = new UserProfile();
            p.setUser(user);
        }
        if (body.containsKey("fullName"))     p.setFullName(toStr(body.get("fullName")));
        if (body.containsKey("heightCm"))     p.setHeightCm(toDouble(body.get("heightCm")));
        if (body.containsKey("weightKg"))     p.setWeightKg(toDouble(body.get("weightKg")));
        if (body.containsKey("gender"))       p.setGender(toStr(body.get("gender")));
        if (body.containsKey("allergies"))    p.setAllergies(toStr(body.get("allergies")));
        if (body.containsKey("dietaryPreference")) p.setDietaryPreference(toStr(body.get("dietaryPreference")));
        if (body.containsKey("targetCalories"))    p.setTargetCalories(toInt(body.get("targetCalories")));
        if (body.containsKey("activityLevel"))     p.setActivityLevel(toStr(body.get("activityLevel")));
        if (body.containsKey("birthDate"))         p.setBirthDate(java.time.LocalDate.parse(toStr(body.get("birthDate"))));
        if (body.containsKey("birthDate")) {
            String dStr = toStr(body.get("birthDate"));
            p.setBirthDate((dStr == null || dStr.isBlank()) ? null : java.time.LocalDate.parse(dStr));
        }
        return profileRepo.save(p);
    }

    private static String toStr(Object v) { return v == null ? null : String.valueOf(v).trim(); }
    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.valueOf(String.valueOf(v));
    }
    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        return Double.valueOf(String.valueOf(v));
    }
}
