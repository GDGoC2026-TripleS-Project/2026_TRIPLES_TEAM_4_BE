package com.gdg.unimatebackend.app.university.controller;

import com.gdg.unimatebackend.app.university.dto.UniversityItemResponse;
import com.gdg.unimatebackend.app.university.repository.UniversityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/universities")
@Tag(name = "학교", description = "학교 검색/추천 API")
public class UniversityController {

    private final UniversityRepository universityRepository;

    @GetMapping("/search")
    @Operation(summary = "학교 검색(자동완성)", description = "q로 학교명을 앞글자 기준으로 검색하여 추천 리스트를 반환합니다")
    public List<UniversityItemResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        String query = (q == null) ? "" : q.trim();
        if (query.isEmpty()) return List.of();

        int safeLimit = Math.min(Math.max(limit, 1), 20);

        return universityRepository
                .findByNameStartingWithIgnoreCaseOrderByNameAsc(query, PageRequest.of(0, safeLimit))
                .stream()
                .map(u -> UniversityItemResponse.of(u.getId(), u.getName()))
                .toList();
    }
}
