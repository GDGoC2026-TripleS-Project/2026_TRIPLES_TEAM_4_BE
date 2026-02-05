package com.gdg.unimatebackend.schedule.dto;

import com.gdg.unimatebackend.schedule.entity.ScheduleCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyScheduleResponse {
    private Long id;
    private Long teamId;
    private Long userId;

    private String title;
    private String memo;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private boolean isPrivate;

    private ScheduleCategory category;
    private String categoryMemo;
    private Integer alarmMinutes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
