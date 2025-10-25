package org.example.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        // Bỏ qua filter cho endpoint đăng nhập và WebSocket
        if (path.startsWith("/api/auth/") || path.startsWith("/chat-ws")) {
            log.debug("Skipping JWT validation for path: {}", path);
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                String username = jwtService.extractUsername(token);
                if (username != null && jwtService.isValid(token, username)) {
                    String[] roles = jwtService.extractRoles(token);
                    Collection<SimpleGrantedAuthority> authorities =
                            (roles == null ? java.util.List.<SimpleGrantedAuthority>of()
                                    : Arrays.stream(roles)
                                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList()));

                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("JWT OK -> user={}, roles={}", username, authorities);
                } else {
                    log.debug("JWT invalid or no username");
                }
            } catch (Exception ex) {
                log.warn("JWT parse error: {}", ex.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}