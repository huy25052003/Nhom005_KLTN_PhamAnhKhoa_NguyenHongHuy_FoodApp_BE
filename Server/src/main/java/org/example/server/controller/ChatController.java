package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.dto.MessageDtos;
import org.example.server.entity.Conversation;
import org.example.server.entity.Message;
import org.example.server.repository.UserRepository;
import org.example.server.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController // <-- Bắt buộc để trả về JSON
@RequiredArgsConstructor
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    // === REST API (Frontend gọi cái này khi mở chat) ===

    @PostMapping("/api/conversations")
    public ResponseEntity<Conversation> createConversation(
            @RequestParam(required = false) Long customerId,
            Authentication auth) {
        return ResponseEntity.ok(chatService.getOrCreateConversation(customerId, auth));
    }

    @GetMapping("/api/conversations")
    public ResponseEntity<List<Conversation>> getAllConversations() {
        return ResponseEntity.ok(chatService.getAllConversations());
    }

    @GetMapping("/api/conversations/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(id));
    }

    // === SOCKET HANDLER (Frontend gọi cái này khi bấm Gửi) ===

    @MessageMapping("/chat.sendMessage")
    public void handleWebSocketMessage(@Payload MessageDtos chatMessage, Principal principal) {
        if (principal == null) {
            logger.error("Rejected: Principal is null");
            return;
        }
        if (chatMessage == null || chatMessage.getConversationId() == null) return;

        try {
            String username = principal.getName();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
            }

            Message savedMsg = chatService.sendMessage(
                    chatMessage.getConversationId(),
                    chatMessage.getSenderId(),
                    chatMessage.getContent(),
                    auth
            );

            // Convert sang DTO để tránh lỗi lặp JSON
            MessageDtos responseDto = MessageDtos.builder()
                    .id(savedMsg.getId())
                    .conversationId(savedMsg.getConversation().getId())
                    .senderId(savedMsg.getSender().getId())
                    .senderName(savedMsg.getSender().getUsername())
                    .content(savedMsg.getContent())
                    .createdAt(savedMsg.getCreatedAt().toString())
                    .build();

            String destination = "/topic/conversation/" + chatMessage.getConversationId();
            messagingTemplate.convertAndSend(destination, responseDto);

        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);
        }
    }
}