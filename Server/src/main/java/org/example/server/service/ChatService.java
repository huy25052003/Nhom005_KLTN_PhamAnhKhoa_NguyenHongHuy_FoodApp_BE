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
        User initiator = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        if (initiator.getRoles().contains("ROLE_USER")) {
            // Khách hàng tạo cuộc trò chuyện, chọn admin mặc định
            User admin = userRepository.findByRolesContaining("ADMIN")
                    .stream().findFirst()
                    .orElseThrow(() -> new SecurityException("Không tìm thấy admin"));
            Conversation existingConversation = conversationRepository.findByCustomerId(initiator.getId());
            if (existingConversation != null) {
                return existingConversation;
            }
            Conversation conversation = Conversation.builder()
                    .admin(admin)
                    .customer(initiator)
                    .build();
            return conversationRepository.save(conversation);
        }

        if (initiator.getRoles().contains("ADMIN")) {
            // Admin tạo cuộc trò chuyện với customerId được chỉ định
            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
            Conversation existingConversation = conversationRepository.findByCustomerId(customerId);
            if (existingConversation != null) {
                return existingConversation;
            }
            Conversation conversation = Conversation.builder()
                    .admin(initiator)
                    .customer(customer)
                    .build();
            return conversationRepository.save(conversation);
        }

        throw new SecurityException("Không có quyền khởi tạo cuộc trò chuyện");
    }

    // Lấy danh sách tin nhắn của một cuộc trò chuyện
    @Transactional(readOnly = true)
    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }

    // Lấy thông tin cuộc trò chuyện theo ID
    @Transactional(readOnly = true)
    public Conversation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElse(null); // Trả về null nếu không tìm thấy
    }

    // Gửi tin nhắn và cập nhật trạng thái
    @Transactional
    public Message sendMessage(Long conversationId, Long senderId, String content, Authentication auth) {
        // Kiểm tra quyền gửi tin nhắn
        User sender = userRepository.findById(senderId)
                .filter(user -> user.getUsername().equals(auth.getName()) &&
                        (user.getRoles().contains("ADMIN") || user.getRoles().contains("ROLE_USER")))
                .orElseThrow(() -> new SecurityException("Không có quyền gửi tin nhắn"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        if (!conversation.getAdmin().getId().equals(senderId) && !conversation.getCustomer().getId().equals(senderId)) {
            throw new SecurityException("Chỉ admin hoặc khách hàng trong cuộc trò chuyện mới có thể gửi tin nhắn");
        }

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