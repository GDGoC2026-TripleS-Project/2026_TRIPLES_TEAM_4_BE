package com.gdg.unimatebackend.calendar.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarMonthRequest {
    private String month;              // YYYY-MM
    private List<Long> teamIds;        // 없으면 "내가 속한 팀 전체"
    private boolean includeMyPersonal; // 내 개인 일정 포함 여부
}