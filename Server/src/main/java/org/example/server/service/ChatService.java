package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Conversation;
import org.example.server.entity.Message;
import org.example.server.entity.User;
import org.example.server.repository.ConversationRepository;
import org.example.server.repository.MessageRepository;
import org.example.server.repository.UserRepository;
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

    // Lấy hoặc tạo cuộc trò chuyện giữa admin và khách hàng
    @Transactional
    public Conversation getOrCreateConversation(Long customerId, Authentication auth) {
        // Giả sử admin được lấy từ authentication (có role ADMIN)
        User admin = userRepository.findByUsername(auth.getName())
                .filter(user -> user.getRoles().contains("ADMIN"))
                .orElseThrow(() -> new SecurityException("Chỉ admin mới có thể khởi tạo chat"));

        // Kiểm tra xem khách hàng đã có cuộc trò chuyện chưa
        Conversation existingConversation = conversationRepository.findByCustomerId(customerId);
        if (existingConversation != null) {
            return existingConversation;
        }

        // Tạo cuộc trò chuyện mới
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));

        Conversation conversation = Conversation.builder()
                .admin(admin)
                .customer(customer)
                .build();
        return conversationRepository.save(conversation);
    }

    // Lấy danh sách tin nhắn của một cuộc trò chuyện
    @Transactional(readOnly = true)
    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }

    // Gửi tin nhắn và cập nhật trạng thái
    @Transactional
    public Message sendMessage(Long conversationId, Long senderId, String content, Authentication auth) {
        // Kiểm tra quyền gửi tin nhắn
        User sender = userRepository.findById(senderId)
                .filter(user -> user.getUsername().equals(auth.getName()) || user.getRoles().contains("ADMIN"))
                .orElseThrow(() -> new SecurityException("Không có quyền gửi tin nhắn"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();
        return messageRepository.save(message);
    }

    // Đánh dấu tin nhắn đã đọc
    @Transactional
    public void markAsRead(Long messageId, Authentication auth) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Tin nhắn không tồn tại"));
        if (!message.getConversation().getCustomer().getUsername().equals(auth.getName()) &&
                !message.getConversation().getAdmin().getUsername().equals(auth.getName())) {
            throw new SecurityException("Không có quyền cập nhật trạng thái tin nhắn");
        }
        message.setRead(true);
        messageRepository.save(message);
    }
}