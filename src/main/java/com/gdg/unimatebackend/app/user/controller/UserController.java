package com.gdg.unimatebackend.app.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(
        name = "사용자",
        description = "로그인 사용자 정보 조회 API"
)
@RestController
public class UserController {

    @Operation(
            summary = "내 정보 조회",
            description = """
                    현재 인증된 사용자의 정보를 반환합니다.

                    ✔️ Authorization 헤더에 JWT 토큰 필요
                    ✔️ 인증 실패 시 authenticated=false 반환
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {

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
        if (principal instanceof Long l) {
            userId = l;
        } else if (principal instanceof String s) {
            try {
                userId = Long.parseLong(s);
            } catch (NumberFormatException ignored) {
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
