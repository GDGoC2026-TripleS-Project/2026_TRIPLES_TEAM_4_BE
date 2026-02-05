package com.gdg.unimatebackend.schedule.dto;

import com.gdg.unimatebackend.schedule.entity.ScheduleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyScheduleUpdateRequest {

    @NotBlank
    private String title;

    private String memo;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    @NotNull
    private Boolean isPrivate;

    private ScheduleCategory category;

    private String categoryMemo;

    private Integer alarmMinutes;
}
