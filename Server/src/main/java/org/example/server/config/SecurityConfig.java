package org.example.server.config;

import org.example.server.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Các endpoint công khai (ưu tiên cao nhất)
                        .requestMatchers("/api/auth/**", "/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers("/api/payments/webhook", "/pay/result", "/pay/cancel", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/*/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/reviews").permitAll() // Đảm bảo permitAll
                        .requestMatchers(HttpMethod.GET, "/api/products/*/reviews/avg").permitAll()
                        .requestMatchers("/api/favorites/**").permitAll()
                        .requestMatchers("/api/account/**").authenticated()


                        // Các endpoint yêu cầu quyền ADMIN
                        .requestMatchers("/api/conversations/**").hasAnyRole("USER", "ADMIN", "SUPPORT")
                        .requestMatchers("/api/messages/**").hasAnyRole("USER", "ADMIN", "SUPPORT")
                        .requestMatchers("/api/products/**").hasRole("ADMIN")
                        .requestMatchers("/api/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/stats/**").hasRole("ADMIN")
                        .requestMatchers("/api/files/upload").hasRole("ADMIN")
                        .requestMatchers("/api/promotions/preview").hasRole("ADMIN")
                        // .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN") // Sửa ở dưới
                        // .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasRole("ADMIN") // Sửa ở dưới
                        .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/topic/**", "/queue/**").permitAll()

                        // API Bếp (Kitchen)
                        .requestMatchers("/api/kitchen/**").hasAnyRole("ADMIN", "KITCHEN")

                        // API Đơn hàng
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasAnyRole("ADMIN", "KITCHEN")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasAnyRole("ADMIN", "KITCHEN")
                        // ===== THAY ĐỔI CHO KITCHEN (KẾT THÚC) =====

                        // Các endpoint yêu cầu xác thực (authenticated)
                        .requestMatchers("/api/users/me/**").authenticated()
                        .requestMatchers("/api/orders/my").authenticated()
                        .requestMatchers("/api/orders/*/cancel").authenticated()
                        .requestMatchers("/api/shipping/me").authenticated()
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders/*").authenticated() // Logic service sẽ check (admin, kitchen, owner)
                        .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/products/*/reviews/*").authenticated()

                        // Tất cả các request khác yêu cầu xác thực
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.1.*:*",
                "http://10.0.2.2:*",
                "https://nhom005foodapp.vercel.app",
                "https://*.vercel.app",
                "https://*.onrender.com",
                "https://unscaled-obtect-irvin.ngrok-free.dev"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "ngrok-skip-browser-warning"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}