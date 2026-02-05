package com.gdg.unimatebackend.schedule.team.dto;

import com.gdg.unimatebackend.schedule.entity.ScheduleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TeamScheduleCreateRequest {

    @NotBlank
    private String title;

    private String memo;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    @NotNull
    private ScheduleCategory category;

    private String categoryMemo;

    private Integer alarmMinutes;
}
