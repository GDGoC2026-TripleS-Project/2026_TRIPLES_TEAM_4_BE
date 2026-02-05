package com.gdg.unimatebackend.schedule.team.dto;

import com.gdg.unimatebackend.schedule.entity.ScheduleCategory;
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

    private ScheduleCategory category;
    private String categoryMemo;
    private Integer alarmMinutes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
