package com.gdg.unimatebackend.healthCheck.controller;

import com.gdg.unimatebackend.healthCheck.service.SystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Tag(
        name = "시스템",
        description = "서버 및 인프라 상태 확인 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system")
public class HealthCheckController {

    private final SystemHealthService systemHealthService;

    @Operation(
            summary = "서버 헬스 체크",
            description = """
                    서버 및 데이터베이스 연결 상태를 확인합니다.

                    ✔️ 배포 파이프라인 헬스체크 용도
                    ✔️ status:
                       - UP: 서버 및 DB 정상
                       - DEGRADED: 서버는 정상, DB 문제
                    """
    )
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
