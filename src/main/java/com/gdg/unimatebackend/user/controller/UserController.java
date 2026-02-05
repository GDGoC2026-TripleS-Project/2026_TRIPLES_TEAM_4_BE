package com.gdg.unimatebackend.user.controller;

import com.gdg.unimatebackend.user.dto.ProfileUpsertRequest;
import com.gdg.unimatebackend.user.dto.UserResponse;
import com.gdg.unimatebackend.user.service.ProfileImageService;
import com.gdg.unimatebackend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @PostMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "프로필 이미지 업로드",
            description = """
            사용자의 프로필 이미지를 업로드합니다.

            - multipart/form-data 형식의 이미지 파일을 업로드합니다.
            - 업로드된 이미지는 AWS S3에 저장됩니다.
            - 기존 프로필 이미지가 존재하는 경우, 이전 이미지는 S3에서 삭제됩니다.
            - DB에는 최신 이미지 URL만 저장됩니다.
            - 응답으로 접근 가능한 public 이미지 URL을 반환합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로필 이미지 업로드 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                {
                                  "imageUrl": "https://seok-hwan-bucket.s3.ap-northeast-2.amazonaws.com/users/123/profile.png"
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (파일이 없거나 형식이 올바르지 않음)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    public ResponseEntity<Map<String, String>> upload(
            @Parameter(
                    description = "업로드할 프로필 이미지 파일 (multipart/form-data)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart("file") MultipartFile file,

            @Parameter(hidden = true)
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String url = profileImageService.uploadAndReplaceProfileImage(userId, file);
        return ResponseEntity.ok(Map.of("imageUrl", url));
    }
}
