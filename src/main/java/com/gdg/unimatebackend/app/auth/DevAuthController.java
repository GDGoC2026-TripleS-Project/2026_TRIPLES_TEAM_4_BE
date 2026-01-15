package com.gdg.unimatebackend.app.auth;

import com.gdg.unimatebackend.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final JwtUtil jwtUtil;

    // ⚠️ Step2(진짜 로그인) 들어가면 삭제/비활성화 예정
    @GetMapping("/dev-token")
    public Map<String, Object> devToken(@RequestParam Long userId) {
        String token = jwtUtil.generateToken(userId);
        return Map.of(
                "userId", userId,
                "token", token
        );
    }
}
