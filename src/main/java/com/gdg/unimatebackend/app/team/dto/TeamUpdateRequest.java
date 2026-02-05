package com.gdg.unimatebackend.app.team.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TeamUpdateRequest {

    @Size(max = 50)
    private String name;

    @Size(max = 300)
    private String description;

    private LocalDate startAt;
    private LocalDate endAt;
}
