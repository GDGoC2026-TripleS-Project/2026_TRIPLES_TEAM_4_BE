package com.gdg.unimatebackend.schedulepoll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePollMetaUpdateRequest {
    private String title;
    private String memo;
    private String alarm;
}