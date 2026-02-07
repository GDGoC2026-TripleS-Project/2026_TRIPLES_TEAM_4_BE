package com.gdg.unimatebackend.schedulepoll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePollCreateResponse {
    private Long pollId;
}