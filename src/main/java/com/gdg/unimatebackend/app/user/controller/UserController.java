package com.gdg.unimatebackend.app.user.controller;

import com.gdg.unimatebackend.app.user.dto.ProfileUpsertRequest;
import com.gdg.unimatebackend.app.user.dto.UserResponse;
import com.gdg.unimatebackend.app.user.service.ProfileImageService;
import com.gdg.unimatebackend.app.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;
    private final ProfileImageService profileImageService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    public ResponseEntity<UserResponse> getMyInfo(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "프로필 생성/수정(덮어쓰기)")
    public ResponseEntity<UserResponse> upsertProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpsertRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.upsertProfile(userId, request));
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String url = profileImageService.uploadAndReplaceProfileImage(userId, file);
        return ResponseEntity.ok(Map.of("imageUrl", url));
    }
}
