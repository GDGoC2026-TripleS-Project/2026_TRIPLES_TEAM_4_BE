package com.gdg.unimatebackend.home.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamTodaySchedulesDto {
    private final Long teamId;
    private final String teamName;
    private final String teamColor; // TeamColor enum name()

    private final List<ScheduleItemDto> schedules;
}