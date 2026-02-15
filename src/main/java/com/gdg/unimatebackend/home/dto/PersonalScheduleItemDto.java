package com.gdg.unimatebackend.home.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PersonalScheduleItemDto {
    private final Long teamId;
    private final Long scheduleId;
    private final String title;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final boolean isPrivate;
}