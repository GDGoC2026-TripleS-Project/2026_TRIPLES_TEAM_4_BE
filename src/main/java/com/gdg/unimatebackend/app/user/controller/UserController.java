package com.gdg.unimatebackend.app.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        // Authentication이 null이면 (보안 설정상) 보통 여기까지 오기 전에 401이지만,
        // 혹시라도 들어오면 500 대신 안전하게 응답
        if (authentication == null || !authentication.isAuthenticated()) {
            return Map.of(
                    "authenticated", false,
                    "userId", null,
                    "principalType", null,
                    "principal", null
            );
        }

        Object principal = authentication.getPrincipal();

        Long userId = null;

        // ✅ principal을 Long으로 못 박지 말고 타입별로 안전 처리
        if (principal instanceof Long l) {
            userId = l;
        } else if (principal instanceof String s) {
            // 예: "1" 같은 문자열로 들어오는 경우 대비
            try {
                userId = Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                // "anonymousUser" 같은 값이면 userId는 null 유지
            }
        }

        return Map.of(
                "authenticated", true,
                "userId", userId,
                "principalType", principal == null ? null : principal.getClass().getName(),
                "principal", String.valueOf(principal)
        );
    }
}
