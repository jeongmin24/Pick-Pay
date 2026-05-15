package com.miniproject.server.domain.jwt.dto;

public record JWTResponseDTO(
        String accessToken,
        String refreshToken
) {
}
