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

import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authManager,
                       JwtService jwtService, EmailService emailService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
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
        String login = req.username();
        String phoneFormatted = login;
        if (login != null && login.matches("^0\\d{9}$")) {
            phoneFormatted = "+84" + login.substring(1);
        }

        User user = userRepo.findByLoginIdentifier(login, phoneFormatted)
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

    // 1. Yêu cầu gửi mã qua Email
    public void requestPasswordReset(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

        // Tạo mã 6 số
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        user.setPasswordResetCode(code);
        userRepo.save(user);
         emailService.sendPasswordResetCode(email, code);
    }

    // 2. Đặt lại mật khẩu bằng OTP Email
    public void resetPasswordWithEmail(String email, String code, String newPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getPasswordResetCode() == null || !user.getPasswordResetCode().equals(code)) {
            throw new IllegalArgumentException("Mã xác thực không đúng hoặc đã hết hạn");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null); // Xóa mã sau khi dùng
        userRepo.save(user);
    }

    // 3. Đặt lại mật khẩu bằng Firebase Phone Token
    public void resetPasswordWithPhone(String firebaseToken, String newPassword) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseToken);
            String phone = (String) decodedToken.getClaims().get("phone_number");

            User user = userRepo.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("Số điện thoại này chưa đăng ký tài khoản nào"));

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepo.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xác thực: " + e.getMessage());
        }
    }
}