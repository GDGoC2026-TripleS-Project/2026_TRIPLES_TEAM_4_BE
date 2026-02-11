package com.gdg.unimatebackend.calendar.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarMonthResponse {
    private String month;
    private List<CalendarDayCountResponse> dayCounts;
}