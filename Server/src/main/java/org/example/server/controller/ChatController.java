package org.example.server.controller;
import org.example.server.dto.MessageDtos;
import org.example.server.entity.Conversation;
import org.example.server.entity.Message;
import org.example.server.repository.UserRepository;
import org.example.server.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
@Controller
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatService chatService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserDetailsService userDetailsService; // THÊM DÒNG NÀY
    // ... các method REST khác giữ nguyên ...
    @MessageMapping("/chat.sendMessage")
    public void handleWebSocketMessage(@Payload MessageDtos chatMessage, Principal principal) {
        logger.info("Received WebSocket message: {}", chatMessage);
        if (chatMessage == null || chatMessage.getConversationId() == null ||
                chatMessage.getSenderId() == null || chatMessage.getContent() == null) {
            logger.error("Invalid message payload: {}", chatMessage);
            return;
        }
        if (principal == null) {
            logger.error("Principal is null - no authentication");
            return;
        }
        String username = principal.getName();
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found: {}", username);
            return;
        }
        var user = userOpt.get();
// KIỂM TRA SENDERID == USER.ID
        if (!user.getId().equals(chatMessage.getSenderId())) {
            logger.warn("Sender ID mismatch: expected {}, got {}", user.getId(), chatMessage.getSenderId());
            return;
        }
        try {
// Kiểm tra conversation tồn tại
            Conversation conversation = chatService.getConversation(chatMessage.getConversationId());
            if (conversation == null) {
                logger.error("Conversation {} not found", chatMessage.getConversationId());
                return;
            }
// TẠO Authentication ĐỂ TRUYỀN VÀO SERVICE
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
            }
// Lưu và gửi tin nhắn
            Message message = chatService.sendMessage(
                    chatMessage.getConversationId(),
                    chatMessage.getSenderId(),
                    chatMessage.getContent(),
                    auth
            );
            chatMessage.setContent(message.getContent()); // Cập nhật nếu cần
            String destination = "/topic/conversation/" + chatMessage.getConversationId();
            messagingTemplate.convertAndSend(destination, chatMessage);
            logger.info("Message sent to topic: {}", destination);
        } catch (Exception e) {
            logger.error("Error processing WebSocket message: {}", e.getMessage(), e);
        }
    }
}