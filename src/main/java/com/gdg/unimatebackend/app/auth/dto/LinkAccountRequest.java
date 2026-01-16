package com.gdg.unimatebackend.app.auth.dto;

import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkAccountRequest {
    @NotNull(message = "기존 로그인 제공자는 필수입니다")
    private AuthProvider existingProvider;

    @NotBlank(message = "이메일은 필수입니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다 (이메일 회원가입 시)")
    private String password;  // 이메일 회원가입 시 비밀번호

    private String nickname;  // 회원가입 시 닉네임 (선택)
}

