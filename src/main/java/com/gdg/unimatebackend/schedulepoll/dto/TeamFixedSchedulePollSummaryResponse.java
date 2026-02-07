package com.gdg.unimatebackend.schedulepoll.dto;

import com.gdg.unimatebackend.schedulepoll.entity.PollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamFixedSchedulePollSummaryResponse {
    private Long pollId;
    private String title;
    private PollStatus status;
    private Integer fixedSlotId;
    private List<LocalDate> dates;
}