package com.gdg.unimatebackend.global.exception;

import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import lombok.Getter;

/**
 * 계정 통합이 필요한 경우 발생하는 예외
 */
@Getter
public class AccountLinkRequiredException extends RuntimeException {

    private final AuthProvider existingProvider;
    private final String email;
    private final int httpStatus = 409; // Conflict

    public AccountLinkRequiredException(AuthProvider existingProvider, String email) {
        super(getMessage(existingProvider));
        this.existingProvider = existingProvider;
        this.email = email;
    }

    private static String getMessage(AuthProvider provider) {
        String providerName = getProviderName(provider);
        return providerName + "로 가입된 계정이 있습니다. 계정을 통합하시겠습니까?";
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

