package org.example.server.controller;

import org.example.server.dto.AuthDtos.*;
import org.example.server.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        auth.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public JwtResponse login(@RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/firebase")
    public ResponseEntity<JwtResponse> loginFirebase(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Token is required");
        return ResponseEntity.ok(auth.loginWithFirebase(token));
    }

    // API: Yêu cầu gửi mã về Email
    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> forgotPasswordRequest(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        auth.requestPasswordReset(email); // Bạn cần sửa AuthService để inject EmailService vào đó
        return ResponseEntity.ok(Map.of("message", "Đã gửi mã xác nhận"));
    }

    // API: Reset bằng Email + OTP
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        auth.resetPasswordWithEmail(body.get("email"), body.get("code"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    // API: Reset bằng Phone (Firebase Token)
    @PostMapping("/forgot-password/phone")
    public ResponseEntity<?> resetPasswordPhone(@RequestBody Map<String, String> body) {
        auth.resetPasswordWithPhone(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }
}