package org.example.server.repository;

import org.example.server.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Conversation findByCustomerId(Long customerId);

}