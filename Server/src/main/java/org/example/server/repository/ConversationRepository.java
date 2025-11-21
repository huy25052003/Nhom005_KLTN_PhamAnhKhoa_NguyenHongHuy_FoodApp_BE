package org.example.server.repository;

import org.example.server.entity.Conversation;
import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Conversation findByCustomer(User customer);
    boolean existsByCustomer(User customer);
    Conversation findByCustomer_Id(Long customerId);

}