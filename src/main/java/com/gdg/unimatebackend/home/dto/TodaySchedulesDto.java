package com.gdg.unimatebackend.home.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TodaySchedulesDto {
    private final List<TeamTodaySchedulesDto> teamSchedules;
    private final List<PersonalScheduleItemDto> personalSchedules;
}