package com.gdg.unimatebackend.team.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TeamUpdateRequest {

    @Size(max = 50)
    private String name;

    @Size(max = 300)
    private String description;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
