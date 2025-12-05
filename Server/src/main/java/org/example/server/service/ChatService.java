package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Conversation;
import org.example.server.entity.Message;
import org.example.server.entity.User;
import org.example.server.repository.ConversationRepository;
import org.example.server.repository.MessageRepository;
import org.example.server.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public Conversation getOrCreateConversation(Long customerId, Authentication auth) {
        User initiator = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new SecurityException("User not found"));

        // Khách hàng muốn chat
        if (initiator.getRoles().contains("ROLE_USER")) {
            Conversation existing = conversationRepository.findByCustomer(initiator);
            if (existing != null) return existing;

            User supportAgent = userRepository.findByRolesContaining("SUPPORT").stream().findFirst()
                    .orElseGet(() -> userRepository.findByRolesContaining("ADMIN").stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("No support agent found")));

            Conversation conversation = Conversation.builder()
                    .admin(supportAgent)
                    .customer(initiator)
                    .build();
            return conversationRepository.save(conversation);
        }

        // Admin/Support muốn chat
        if (initiator.getRoles().contains("ROLE_ADMIN") || initiator.getRoles().contains("ROLE_SUPPORT")) {
            if (customerId == null) throw new IllegalArgumentException("Customer ID required");

            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

            Conversation existing = conversationRepository.findByCustomer(customer);
            if (existing != null) return existing;

            return conversationRepository.save(Conversation.builder()
                    .admin(initiator)
                    .customer(customer)
                    .build());
        }
        throw new SecurityException("Invalid role");
    }

    @Transactional(readOnly = true)
    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getAllConversations() {
        return conversationRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @Transactional
    public Message sendMessage(Long conversationId, Long senderId, String content, Authentication auth) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        if (!sender.getUsername().equals(auth.getName())) {
            throw new SecurityException("User mismatch");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        // Update thời gian để sort
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return messageRepository.save(message);
    }
}