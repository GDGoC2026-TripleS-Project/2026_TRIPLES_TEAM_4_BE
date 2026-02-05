package com.gdg.unimatebackend.team.dto;

import com.gdg.unimatebackend.team.entity.TeamColor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TeamCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @Size(max = 300)
    private String description;

    @NotNull
    private TeamColor color;

    @NotNull
    private LocalDate startAt;

    @NotNull
    private LocalDate endAt;
}
