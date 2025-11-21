package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.User;
import org.example.server.repository.UserRepository;
import org.example.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/email/request")
    public ResponseEntity<User> requestVerification(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        return ResponseEntity.ok(userService.requestEmailVerification(user.getId(), email));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<User> verifyEmail(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String code = body.get("code");
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code is required");
        }

        return ResponseEntity.ok(userService.verifyEmail(user.getId(), code));
    }
}