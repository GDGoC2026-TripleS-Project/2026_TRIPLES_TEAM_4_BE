package com.gdg.unimatebackend.schedulepoll.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class SlotDefinitionResponse {
    private Integer slotId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
}