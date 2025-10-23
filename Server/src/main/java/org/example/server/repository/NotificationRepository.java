package org.example.server.repository;

import org.example.server.entity.Notification;
import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByRecipientIsNullOrderByCreatedAtDesc();
    List<Notification> findTop50ByRecipientAndReadFlagFalseOrderByCreatedAtDesc(User u);
    long countByRecipientIsNullAndReadFlagFalse();
    long countByRecipientAndReadFlagFalse(User u);
}
