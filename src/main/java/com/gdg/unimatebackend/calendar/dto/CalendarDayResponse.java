package com.gdg.unimatebackend.calendar.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayResponse {
    private String date;
    private List<TeamScheduleGroupResponse> teamSchedules;
    private List<CalendarItemResponse> personalSchedules;
}