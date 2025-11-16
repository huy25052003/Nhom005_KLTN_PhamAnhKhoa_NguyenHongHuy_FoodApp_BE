package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.User;
import org.example.server.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người dùng với ID: " + id));
    }

    @Transactional
    public User updateUserRoles(Long id, Set<String> newRoles) {
        User user = getUserById(id);
        Set<String> normalizedRoles = new HashSet<>();
        for (String role : newRoles) {
            if (role != null && !role.isBlank()) {
                String upperRole = role.trim().toUpperCase();
                if (!upperRole.startsWith("ROLE_")) {
                    normalizedRoles.add("ROLE_" + upperRole);
                } else {
                    normalizedRoles.add(upperRole);
                }
            }
        }

        if (normalizedRoles.isEmpty()) {
            throw new IllegalArgumentException("Người dùng phải có ít nhất một vai trò.");
        }

        user.setRoles(normalizedRoles);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id, String currentAdminUsername) {
        User userToDelete = getUserById(id);

        if (userToDelete.getUsername().equals(currentAdminUsername)) {
            throw new AccessDeniedException("Bạn không thể tự xóa tài khoản của mình.");
        }

        userRepository.deleteById(id);
    }

    public User requestEmailVerification(Long userId, String newEmail) {
        User user = getUserById(userId);

        user.setEmail(newEmail);
        user.setIsEmailVerified(false);

        String code = String.valueOf((int) ((Math.random() * 899999) + 100000));
        user.setEmailVerificationCode(code);

        User savedUser = userRepository.save(user);
        emailService.sendVerificationCode(newEmail, code);

        return savedUser;
    }

    public User verifyEmail(Long userId, String code) {
        User user = getUserById(userId);
        if (code.equals(user.getEmailVerificationCode())) {
            user.setIsEmailVerified(true);
            user.setEmailVerificationCode(null);
            return userRepository.save(user);
        }
        throw new RuntimeException("Mã xác thực không đúng");
    }

}
