package com.gdg.unimatebackend.schedulepoll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePollCreateRequest {
    private Long teamId;
    private List<LocalDate> dates;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timezone;
    private Integer slotMinutes; // null이면 기본 30
    private String title;
    private String memo;
    private String alarm;
}
