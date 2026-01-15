package com.gdg.unimatebackend.app.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal(); // ✅ Step1 설계: principal = userId
        return Map.of(
                "authenticated", true,
                "userId", userId
        );
    }
}
