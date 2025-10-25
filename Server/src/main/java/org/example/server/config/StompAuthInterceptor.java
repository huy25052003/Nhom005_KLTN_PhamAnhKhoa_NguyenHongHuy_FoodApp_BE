package org.example.server.config;

import org.example.server.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthInterceptor.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    // LOG XÁC NHẬN CLASS ĐƯỢC LOAD
    static {
        System.out.println("=== STOMP AUTH INTERCEPTOR LOADED - NEW VERSION (ALLOW CONNECT) ===");
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            log.debug("STOMP CONNECT - Authorization header: {}", authHeader);

            // CHO PHÉP KẾT NỐI DÙ KHÔNG CÓ TOKEN (ĐỂ TEST)
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("STOMP CONNECT: No token provided, allowing anonymous connection for testing");
                // Vẫn cho phép kết nối
            } else {
                String token = authHeader.substring(7).trim();
                try {
                    String username = jwtService.extractUsername(token);
                    if (username != null && jwtService.isValid(token, username)) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("STOMP CONNECT success: user={}", username);
                    } else {
                        log.warn("STOMP CONNECT: Invalid token, continuing without auth");
                    }
                } catch (Exception e) {
                    log.error("STOMP CONNECT: JWT error: {}", e.getMessage());
                    // Không throw, vẫn cho phép kết nối
                }
            }

            // QUAN TRỌNG: TRẢ VỀ message ĐỂ CHO PHÉP KẾT NỐI
            return message;
        }

        // Các command khác (SEND, SUBSCRIBE): vẫn giữ nguyên xử lý JWT nếu cần
        return message;
    }
}