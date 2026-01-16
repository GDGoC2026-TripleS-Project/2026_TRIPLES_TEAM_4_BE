package com.gdg.unimatebackend.app.auth.dto;

import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLinkResponse {
    private boolean needsConfirmation;
    private String message;
    private AuthProvider existingProvider;
    private String email;

    public static AccountLinkResponse needsConfirmation(AuthProvider existingProvider, String email) {
        String providerName = getProviderName(existingProvider);
        return AccountLinkResponse.builder()
                .needsConfirmation(true)
                .message(providerName + "로 가입된 계정이 있습니다. 계정을 통합하시겠습니까?")
                .existingProvider(existingProvider)
                .email(email)
                .build();
    }

    private static String getProviderName(AuthProvider provider) {
        switch (provider) {
            case EMAIL:
                return "이메일";
            case KAKAO:
                return "카카오";
            default:
                return provider.name();
        }
    }
}

