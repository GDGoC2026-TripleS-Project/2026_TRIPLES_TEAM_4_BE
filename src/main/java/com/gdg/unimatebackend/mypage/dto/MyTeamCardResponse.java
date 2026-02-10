package com.gdg.unimatebackend.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class MyTeamCardResponse {

    private Long teamId;
    private String name;
    private String description;

    private Long memberCount;

    private LocalDate startAt;
    private LocalDate endAt;

    private Boolean isCompleted;

    // 팀 카드 스타일용(팀 대표색/내 표시색 등 프로젝트 정책에 맞춰 사용)
    private String color;     // enum name (ex: GREEN)
    private String colorHex;  // ex: #AABBCC

    // 와이어프레임의 "마감 D-3" 같은 표시용
    private String dDay;      // ex: D-3
}