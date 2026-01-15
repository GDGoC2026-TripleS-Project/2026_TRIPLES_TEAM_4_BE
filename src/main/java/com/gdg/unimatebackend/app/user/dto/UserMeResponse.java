package com.gdg.unimatebackend.app.user.dto;

import com.gdg.unimatebackend.app.user.entity.User;

public record UserMeResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
