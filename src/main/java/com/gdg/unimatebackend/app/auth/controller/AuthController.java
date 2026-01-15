package com.gdg.unimatebackend.app.auth.controller;

import com.gdg.unimatebackend.app.auth.dto.AuthTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.KakaoLoginRequest;
import com.gdg.unimatebackend.app.auth.service.SocialAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 - 카카오", description = "카카오 OAuth 로그인(인가코드/토큰) 기반 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class AuthController {

    private final SocialAuthService socialAuthService;

    @Operation(
            summary = "카카오 Authorize URL 생성",
            description = """
                    카카오 로그인 화면으로 이동할 Authorize URL을 생성합니다.
                    - client_id는 REST API 키를 사용해야 합니다.
                    - redirect_uri는 카카오 콘솔에 등록된 값과 정확히 동일해야 합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorize URL 문자열 반환",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/authorize-url")
    public ResponseEntity<String> authorizeUrl() {
        return ResponseEntity.ok(socialAuthService.buildKakaoAuthorizeUrl());
    }

    @Operation(
            summary = "카카오 OAuth 콜백(인가코드 처리)",
            description = """
                    카카오 로그인 성공 후 redirect_uri로 호출되는 콜백입니다.
                    - code(인가코드)를 받아 토큰 교환 -> 사용자 정보 조회 -> 우리 서비스 JWT 발급까지 수행합니다.
                    - code는 일반적으로 1회성으로 소비됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공(JWT 발급)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(인가코드 누락/형식 오류)"),
            @ApiResponse(responseCode = "500", description = "카카오 연동/서버 내부 오류")
    })
    @GetMapping("/callback")
    public ResponseEntity<AuthTokenResponse> callback(
            @Parameter(description = "카카오에서 전달되는 인가코드(code)", required = true, example = "abc123...")
            @RequestParam("code") String code
    ) {
        return ResponseEntity.ok(socialAuthService.kakaoLoginByCode(code));
    }

    @Operation(
            summary = "액세스 토큰 직접 로그인(테스트용)",
            description = """
                    프론트 없이 빠르게 검증하기 위한 테스트용 엔드포인트입니다.
                    - 카카오 access_token을 직접 받아 사용자 정보 조회 -> 우리 서비스 JWT 발급을 수행합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공(JWT 발급)",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 바디 오류(토큰 누락 등)"),
            @ApiResponse(responseCode = "401", description = "카카오 토큰 인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "카카오 access_token을 포함한 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = KakaoLoginRequest.class))
            )
            @RequestBody KakaoLoginRequest request
    ) {
        return ResponseEntity.ok(socialAuthService.kakaoLogin(request));
    }

    @Operation(
            summary = "인가코드(code) 그대로 출력(테스트용)",
            description = """
                    카카오 콜백으로 받은 code를 그대로 문자열로 반환합니다.
                    - 이 엔드포인트는 code를 '소비'하지 않습니다.
                    - /callback을 1회만 호출하고 싶을 때, code 확인용으로 사용합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "code 문자열 반환",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/code")
    public ResponseEntity<String> codeOnly(
            @Parameter(description = "카카오 인가코드(code)", required = true, example = "abc123...")
            @RequestParam("code") String code
    ) {
        return ResponseEntity.ok(code);
    }
}
