package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.UserProfile;
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

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(profileService.getMyProfile(auth));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfile> updateMyProfile(Authentication auth, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(profileService.upsertMyProfile(auth, body));
    }

}
