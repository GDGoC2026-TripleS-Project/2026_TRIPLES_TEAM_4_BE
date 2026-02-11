package com.gdg.unimatebackend.calendar.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamScheduleGroupResponse {
    private Long teamId;
    private String teamName;
    private List<CalendarItemResponse> schedules;
}