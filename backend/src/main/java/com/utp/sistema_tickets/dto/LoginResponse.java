package com.utp.sistema_tickets.dto;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, String subject,
                            Long userId, String userName, String role, Instant expiresAt) {
}
