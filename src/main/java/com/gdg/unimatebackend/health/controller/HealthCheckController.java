package com.gdg.unimatebackend.health.controller;

import com.gdg.unimatebackend.health.service.SystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "시스템", description = "서버상태/헬스체크 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system")
public class HealthCheckController {

    private final SystemHealthService systemHealthService;

    @Operation(
            summary = "서버 헬스 체크",
            description = """
                    서버 및 DB 연결 상태를 확인합니다.
                    - status: UP(정상) / DEGRADED(DB 이상)
                    - 배포 파이프라인 헬스체크(Health URL) 용도로 사용합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "헬스 체크 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = systemHealthService.isDatabaseUp();

        Map<String, Object> res = new HashMap<>();
        res.put("status", dbUp ? "UP" : "DEGRADED");
        res.put("db", dbUp ? "UP" : "DOWN");
        res.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(res);
    }
}
