package com.gdg.unimatebackend.home.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WeeklyCalendarDayDto {
    private final LocalDate date;
    private final boolean isToday;
    private final int scheduleCount;
}