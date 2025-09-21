package org.example.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.ttl:3600000}")
    private long defaultTtlMillis;

    private Key getSigningKey() {
        // JJWT 0.9.x dùng SecretKeySpec + SignatureAlgorithm
        return new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS512.getJcaName()
        );
    }

    /** Parse claims (JJWT 0.9.x API) */
    public Claims extractAllClaims(String token) throws JwtException {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isValid(String token, String expectedUsername) {
        try {
            Claims c = extractAllClaims(token);
            if (expectedUsername != null && !expectedUsername.equals(c.getSubject())) return false;
            Date exp = c.getExpiration();
            return exp == null || exp.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Đọc roles robust (List, array, string, JSON string) */
    public String[] extractRoles(String token) {
        try {
            Object r = extractAllClaims(token).get("roles");
            if (r == null) return new String[0];

            if (r instanceof List<?> list) {
                return list.stream().map(String::valueOf).toArray(String[]::new);
            }
            if (r instanceof String s) {
                String v = s.trim();
                if (v.startsWith("[") && v.endsWith("]")) {
                    return new ObjectMapper().readValue(v, String[].class);
                }
                return new String[]{ v };
            }
            if (r.getClass().isArray()) {
                Object[] arr = (Object[]) r;
                return Arrays.stream(arr).map(String::valueOf).toArray(String[]::new);
            }
        } catch (Exception ignored) {}
        return new String[0];
    }

    /** Giữ tương thích: generate với roles[] + TTL chỉ định */
    public String generate(String username, String[] roles, long ttlMillis) {
        long now = System.currentTimeMillis();

        // Chuẩn hoá prefix ROLE_
        List<String> norm = Arrays.stream(roles == null ? new String[0] : roles)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .distinct()
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", norm)         // lưu List<String> => dễ parse
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMillis))
                .signWith(SignatureAlgorithm.HS512, getSigningKey())
                .compact();
    }

    /** NEW: overload nhận Collection (Set/List), TTL mặc định từ cấu hình */
    public String generate(String username, Collection<String> roles) {
        String[] arr = (roles == null) ? new String[0] : roles.toArray(new String[0]);
        return generate(username, arr, defaultTtlMillis);
    }

    /** NEW: overload nhận Set (gọi sang Collection) — để khớp call site hiện tại */
    public String generate(String username, Set<String> roles) {
        return generate(username, (Collection<String>) roles);
    }
}
