package com.gdg.unimatebackend.app.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverUserInfo(
        String resultcode,
        String message,
        Response response
) {
    public record Response(
            String id,
            String nickname,
            @JsonProperty("profile_image") String profileImage,
            String email,
            String name
    ) {}
}
