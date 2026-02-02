package com.gdg.unimatebackend.app.schedule.team.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TeamMemberBusyNowResponse {

    private List<MemberBusy> members;

    @Getter
    @AllArgsConstructor
    public static class MemberBusy {
        private Long userId;
        private boolean isBusy;
    }
}
