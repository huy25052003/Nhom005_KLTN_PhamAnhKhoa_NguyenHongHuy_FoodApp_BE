package org.example.server.controller;

import org.example.server.dto.MessageDtos;
import org.example.server.entity.Conversation;
import org.example.server.entity.Message;
import org.example.server.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    // API để bắt đầu hoặc lấy cuộc trò chuyện
    @PostMapping("/api/conversations/start")
    @ResponseBody
    public Conversation startConversation(@RequestParam Long customerId, Authentication auth) {
        return chatService.getOrCreateConversation(customerId, auth);
    }

    // API để lấy danh sách tin nhắn của một cuộc trò chuyện
    @GetMapping("/api/conversations/{conversationId}/messages")
    @ResponseBody
    public List<Message> getMessages(@PathVariable Long conversationId) {
        return chatService.getMessages(conversationId);
    }

    // API để gửi tin nhắn (REST endpoint cho test hoặc tích hợp)
    @PostMapping("/api/conversations/{conversationId}/send")
    @ResponseBody
    public Message sendMessage(@PathVariable Long conversationId, @RequestParam Long senderId,
                               @RequestParam String content, Authentication auth) {
        return chatService.sendMessage(conversationId, senderId, content, auth);
    }

    // API để đánh dấu tin nhắn đã đọc
    @PostMapping("/api/messages/{messageId}/read")
    @ResponseBody
    public void markAsRead(@PathVariable Long messageId, Authentication auth) {
        chatService.markAsRead(messageId, auth);
    }

    // Xử lý tin nhắn qua WebSocket
    @MessageMapping("/chat.sendMessage")
    public void handleWebSocketMessage(@Payload MessageDtos chatMessage, Authentication auth) {
        Message message = chatService.sendMessage(chatMessage.getConversationId(), chatMessage.getSenderId(),
                chatMessage.getContent(), auth);
        chatMessage.setContent(message.getContent()); // Đảm bảo nội dung đồng bộ
        messagingTemplate.convertAndSend("/topic/conversation/" + chatMessage.getConversationId(), chatMessage);
    }
}