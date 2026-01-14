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

@Tag(name = "시스템", description = "서버 상태/헬스체크 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system")
public class HealthCheckController {

    private final SystemHealthService systemHealthService;

    @GetMapping("/health")
    @Operation(summary = "서버 상태 확인", description = "서버 및 DB 연결 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = systemHealthService.isDatabaseUp();

        Map<String, Object> res = new HashMap<>();
        res.put("status", dbUp ? "UP" : "DEGRADED");
        res.put("db", dbUp ? "UP" : "DOWN");
        res.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(res);
    }
}
