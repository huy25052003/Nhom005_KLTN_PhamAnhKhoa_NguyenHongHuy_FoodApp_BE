package org.example.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS endpoint (cho Vite/Browser)
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker cho client subscribe
        registry.enableSimpleBroker("/topic"); // ví dụ: /topic/admin/orders
        // Prefix khi client SEND (nếu cần xử lý phía server)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
