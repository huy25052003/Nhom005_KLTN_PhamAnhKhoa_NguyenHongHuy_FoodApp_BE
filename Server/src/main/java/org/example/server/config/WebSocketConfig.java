package org.example.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS endpoint (cho Vite/Browser)
        registry.addEndpoint("/ws").setAllowedOriginPatterns(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.1.*:*",
                "http://10.0.2.2:*",
                "https://nhom005foodapp.vercel.app",
                "https://*.vercel.app",
                "https://*.onrender.com"
                )
                .withSockJS();
    }

    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker cho client subscribe
        registry.enableSimpleBroker("/topic"); // ví dụ: /topic/admin/orders
        // Prefix khi client SEND (nếu cần xử lý phía server)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
