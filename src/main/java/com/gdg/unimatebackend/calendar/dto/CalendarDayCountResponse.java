package com.gdg.unimatebackend.calendar.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayCountResponse {
    private String date; // YYYY-MM-DD
    private int count;
}