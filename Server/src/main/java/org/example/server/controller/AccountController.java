package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.User;
import org.example.server.repository.UserRepository;
import org.example.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/email/request")
    public ResponseEntity<User> requestVerification(
            Authentication authentication,
            @RequestParam String email) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(userService.requestEmailVerification(user.getId(), email));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<User> verifyEmail(
            Authentication authentication,
            @RequestParam String code) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(userService.verifyEmail(user.getId(), code));
    }
}