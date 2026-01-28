package com.gdg.unimatebackend.app.team.dto;

import com.gdg.unimatebackend.app.team.entity.TeamColor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class TeamCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @Size(max = 300)
    private String description;

    @NotNull
    private TeamColor color;
}
