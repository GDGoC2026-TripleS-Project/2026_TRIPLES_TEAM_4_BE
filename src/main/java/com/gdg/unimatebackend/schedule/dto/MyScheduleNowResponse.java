package com.gdg.unimatebackend.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyScheduleNowResponse {

    private boolean isBusy;

    // 지금 시각과 겹치는 일정들
    private List<ScheduleTimeRange> schedules;

    @Getter
    @AllArgsConstructor
    public static class ScheduleTimeRange {
        private Long scheduleId;
        private String title;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
    }
}
