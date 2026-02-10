package com.gdg.unimatebackend.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MyPageSummaryResponse {

    private MyProfileResponse profile;
    private List<MyTeamCardResponse> activeTeams;
    private List<MyTeamCardResponse> completedTeams;
}