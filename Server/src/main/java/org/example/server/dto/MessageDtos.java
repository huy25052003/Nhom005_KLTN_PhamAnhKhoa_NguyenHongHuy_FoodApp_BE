package org.example.server.dto;

import lombok.Data;

@Data
public class MessageDtos {
    private Long conversationId;
    private Long senderId;
    private String content;
}