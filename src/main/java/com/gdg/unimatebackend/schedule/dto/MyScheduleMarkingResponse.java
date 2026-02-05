package com.gdg.unimatebackend.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyScheduleMarkingResponse {
    private List<LocalDate> markedDates; // 일정이 존재하는 날짜들
}
