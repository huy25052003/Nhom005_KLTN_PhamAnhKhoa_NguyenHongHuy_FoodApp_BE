package org.example.server.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.example.server.dto.AuthDtos.*;
import org.example.server.entity.User;
import org.example.server.repository.UserRepository;
import org.example.server.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authManager,
                       JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .roles(Set.of("ROLE_USER"))
                .build();
        userRepo.save(user);
    }

    public JwtResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String jwt = jwtService.generate(user.getUsername(), user.getRoles());
        return new JwtResponse(jwt);
    }

    // --- HÀM MỚI CHO FIREBASE GOOGLE LOGIN ---
    public JwtResponse loginWithFirebase(String idToken) {
        try {
            // 1. Verify Token với Google
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decodedToken.getEmail();
            String uid = decodedToken.getUid();
             String name = decodedToken.getName(); // Nếu muốn lưu tên

            // 2. Tìm User trong DB (ưu tiên dùng email làm username)
            String username = (email != null && !email.isBlank()) ? email : uid;

            User user = userRepo.findByUsername(username).orElse(null);

            if (user == null) {
                // 3. Nếu chưa có -> Tự động tạo User mới
                user = User.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Mật khẩu ngẫu nhiên
                        .isEmailVerified(true) // Google đã xác thực rồi
                        .roles(Set.of("ROLE_USER"))
                        .build();
                userRepo.save(user);
            }

            // 4. Tạo JWT của hệ thống mình trả về cho Frontend
            String jwt = jwtService.generate(user.getUsername(), user.getRoles());
            return new JwtResponse(jwt);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xác thực Google: " + e.getMessage());
        }
    }
}