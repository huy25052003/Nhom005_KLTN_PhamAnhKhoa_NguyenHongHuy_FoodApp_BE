package org.example.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(String username, Set<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of("roles", roles))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key())
                .compact();
    }

    public String extractUsername(String token) {
        return parseAllClaims(token).getSubject();
    }

    public String[] extractRoles(String token) {
        Object v = parseAllClaims(token).get("roles");
        if (v instanceof java.util.List<?> list) {
            return list.stream().map(Object::toString).toArray(String[]::new);
        }
        return new String[0];
    }

    public boolean isValid(String token, String username) {
        try {
            Claims claims = parseAllClaims(token);
            return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}