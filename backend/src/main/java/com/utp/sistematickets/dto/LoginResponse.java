package com.utp.sistema_tickets.dto;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, String subject,
                            String role, Instant expiresAt) {
}
