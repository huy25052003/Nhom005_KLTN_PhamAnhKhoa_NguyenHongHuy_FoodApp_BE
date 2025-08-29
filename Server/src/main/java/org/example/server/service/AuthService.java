package org.example.server.service;

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
                .roles(Set.of("ROLE_USER")) // mặc định user mới
                .build();
        userRepo.save(user);
    }

    public JwtResponse login(LoginRequest req) {
        // Xác thực username/password
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        // Lấy user + roles từ DB
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Sinh JWT có claim "roles"
        String jwt = jwtService.generate(user.getUsername(), user.getRoles());
        return new JwtResponse(jwt);
    }
}
