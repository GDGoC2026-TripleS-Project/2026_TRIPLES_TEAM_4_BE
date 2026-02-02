package com.gdg.unimatebackend.app.schedule.team.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamScheduleResponse {
    private Long id;
    private Long teamId;
    private Long createdBy;

    private String title;
    private String memo;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
