package org.example.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDtos {
    private Long id;             // Add ID
    private Long conversationId;
    private Long senderId;
    private String senderName;   // Add Sender Name
    private String content;
    private String createdAt;    // Add Timestamp string
}