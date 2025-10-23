package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Notification;
import org.example.server.entity.User;
import org.example.server.repository.NotificationRepository;
import org.example.server.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {
    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    // Lấy 50 bản ghi gần nhất (broadcast cho tất cả admin)
    @GetMapping
    public List<Notification> list() {
        return notificationRepo.findTop50ByRecipientIsNullOrderByCreatedAtDesc();
    }

    // Đếm unread (broadcast)
    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(Authentication auth) {
        long c = notificationRepo.countByRecipientIsNullAndReadFlagFalse();
        return Map.of("unread", c);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        return notificationRepo.findById(id)
                .map(n -> {
                    n.setReadFlag(true);
                    notificationRepo.save(n);
                    return ResponseEntity.ok(Map.of("ok", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/read-all")
    public Map<String, Object> readAll() {
        var list = notificationRepo.findTop50ByRecipientIsNullOrderByCreatedAtDesc();
        list.forEach(n -> n.setReadFlag(true));
        notificationRepo.saveAll(list);
        return Map.of("ok", true);
    }
}
