package org.example.server.dto;

public class AuthDtos {
    public record RegisterRequest(String username, String password) {}
    public record LoginRequest(String username, String password) {}
    public record JwtResponse(String accessToken) {}
}
