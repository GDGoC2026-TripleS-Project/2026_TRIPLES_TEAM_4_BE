package com.gdg.unimatebackend.poke.controller;

import com.gdg.unimatebackend.poke.dto.PokeMessageResponse;
import com.gdg.unimatebackend.poke.dto.PokeRequest;
import com.gdg.unimatebackend.poke.dto.PokeResponse;
import com.gdg.unimatebackend.poke.dto.PokeTargetsResponse;
import com.gdg.unimatebackend.poke.service.PokeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pokes")
@Tag(name = "찌르기", description = "팀원 찌르기 API")
public class PokeController {

    private final PokeService pokeService;

    // =========================
    // 찌르기 전송 (다건)
    // =========================
    @Operation(
            summary = "찌르기 전송",
            description = "여러 팀/여러 팀원을 선택한 문구로 찌릅니다. (본인 포함 시 제외 처리)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찌르기 처리 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "같은 팀원이 아님"),
            @ApiResponse(responseCode = "404", description = "찌르기 문구 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<PokeResponse> sendPokes(
            Authentication authentication,
            @Valid @RequestBody PokeRequest request
    ) {
        Long senderId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(pokeService.sendPokes(senderId, request));
    }

    // =========================
    // 찌르기 대상 조회
    // =========================
    @Operation(
            summary = "찌르기 대상 조회",
            description = "내가 속한 팀과 각 팀의 팀원 목록을 조회합니다. (본인 제외)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/targets")
    public ResponseEntity<PokeTargetsResponse> getTargets(Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(pokeService.getPokeTargets(me));
    }

    // =========================
    // 찌르기 문구 조회
    // =========================
    @Operation(
            summary = "찌르기 문구 조회",
            description = "사용자가 선택할 수 있는 찌르기 문구 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/messages")
    public ResponseEntity<List<PokeMessageResponse>> getMessages() {
        return ResponseEntity.ok(pokeService.getPokeMessages());
    }
}
