package com.gdg.unimatebackend.app.auth.dto;

import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenResponse {
    private String token;
    private Long userId;
    private AuthProvider provider;
    private String email;
    private String nickname;
}
