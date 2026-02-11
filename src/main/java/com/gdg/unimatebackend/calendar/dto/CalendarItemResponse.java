package com.gdg.unimatebackend.calendar.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarItemResponse {

    private Long scheduleId;
    private String type;      // "TEAM" | "PERSONAL"

    private Long teamId;      // TEAM일 때만
    private String teamName;  // TEAM일 때만

    private String title;     // 타인 비공개면 null
    private String startAt;   // ISO String
    private String endAt;     // ISO String

    private Boolean isCompleted; // 팀 일정이면 사용 가능, 개인일정은 null이어도 됨
    private boolean isMasked;    // 타인 비공개 개인일정 마스킹 여부
}