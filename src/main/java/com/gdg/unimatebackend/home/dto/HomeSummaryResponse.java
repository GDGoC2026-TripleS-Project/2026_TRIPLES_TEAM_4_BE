package com.gdg.unimatebackend.home.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class HomeSummaryResponse {
    private final LocalDate date;
    private final LocalDate weekStart;
    private final LocalDate weekEnd;

    private final List<WeeklyCalendarDayDto> weeklyCalendar;

    private final TodaySchedulesDto todaySchedules;

    private final List<MyTeamSpaceDto> myTeamSpaces;

    private final NotificationBadgeDto notification;
}