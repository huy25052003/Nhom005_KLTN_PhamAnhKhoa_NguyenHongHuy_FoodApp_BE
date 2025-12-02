package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.UserProfile;
import org.example.server.repository.UserRepository;
import org.example.server.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {
    private final ProfileService profileService;
    private final UserRepository userRepo;

    @GetMapping("")
    public ResponseEntity<?> getMe(Authentication auth) {
        return userRepo.findByUsername(auth.getName())
                .map(u -> Map.of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "email", u.getEmail() != null ? u.getEmail() : "",
                        "isEmailVerified", Boolean.TRUE.equals(u.getIsEmailVerified()),
                        // --- THÊM 2 DÒNG NÀY ---
                        "phone", u.getPhone() != null ? u.getPhone() : "",
                        "isPhoneVerified", Boolean.TRUE.equals(u.getIsPhoneVerified()),
                        // -----------------------
                        "roles", u.getRoles()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(profileService.getMyProfile(auth));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfile> updateMyProfile(Authentication auth, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(profileService.upsertMyProfile(auth, body));
    }
}