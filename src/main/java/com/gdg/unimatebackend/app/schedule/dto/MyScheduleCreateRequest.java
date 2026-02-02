package com.gdg.unimatebackend.app.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyScheduleCreateRequest {

    @NotBlank
    private String title;

    private String memo;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    @NotNull
    private Boolean isPrivate;
}
