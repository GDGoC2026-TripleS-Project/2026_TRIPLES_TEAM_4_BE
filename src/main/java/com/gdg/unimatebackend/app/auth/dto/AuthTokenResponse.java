package com.gdg.unimatebackend.app.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String provider,
        String providerId,
        String nickname,
        String profileImageUrl
) {}
