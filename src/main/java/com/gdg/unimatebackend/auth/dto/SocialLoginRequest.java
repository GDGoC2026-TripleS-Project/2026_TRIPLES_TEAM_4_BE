package com.gdg.unimatebackend.auth.dto;

import com.gdg.unimatebackend.user.entity.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 소셜 로그인 요청 DTO
 * 카카오 로그인에 사용
 */
@Getter
@Setter
public class SocialLoginRequest {
    @NotNull(message = "로그인 제공자는 필수입니다")
    private AuthProvider provider;

    @NotBlank(message = "액세스 토큰은 필수입니다")
    private String accessToken;

    private String email;
    private String nickname;
}

