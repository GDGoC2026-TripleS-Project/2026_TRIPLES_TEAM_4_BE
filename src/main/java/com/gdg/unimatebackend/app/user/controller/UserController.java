package com.gdg.unimatebackend.app.user.controller;

import com.gdg.unimatebackend.app.user.dto.UserMeResponse;
import com.gdg.unimatebackend.app.user.dto.UserUpdateRequest;
import com.gdg.unimatebackend.app.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자 정보를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserMeResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자 정보를 수정합니다(최소 구현: nickname).")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserMeResponse> updateMe(
            Authentication authentication,
            @RequestBody UserUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.updateMe(userId, request));
    }
}
