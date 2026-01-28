package com.gdg.unimatebackend.app.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class TeamJoinRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "초대코드는 숫자 6자리여야 합니다")
    private String inviteCode;
}
