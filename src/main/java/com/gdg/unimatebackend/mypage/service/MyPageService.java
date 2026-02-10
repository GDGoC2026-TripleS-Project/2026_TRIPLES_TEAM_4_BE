package com.gdg.unimatebackend.mypage.service;

import com.gdg.unimatebackend.mypage.dto.MyPageSummaryResponse;
import com.gdg.unimatebackend.mypage.dto.MyProfileResponse;
import com.gdg.unimatebackend.mypage.dto.MyTeamCardResponse;
import com.gdg.unimatebackend.mypage.exception.MyPageErrorCodes;
import com.gdg.unimatebackend.mypage.exception.MyPageException;
import com.gdg.unimatebackend.team.dto.TeamSummaryResponse;
import com.gdg.unimatebackend.team.service.TeamService;
import com.gdg.unimatebackend.user.entity.User;
import com.gdg.unimatebackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final TeamService teamService;
    private final MyPageAssembler myPageAssembler;

    @Transactional(readOnly = true)
    public MyPageSummaryResponse getMyPageSummary(Long userId) {
        if (userId == null) {
            throw new MyPageException(
                    MyPageErrorCodes.UNAUTHORIZED,
                    "인증 정보가 없습니다",
                    401
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MyPageException(
                        MyPageErrorCodes.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다",
                        404
                ));

        // 팀 요약 목록 (teams 도메인 API 재사용)
        List<TeamSummaryResponse> myTeams = teamService.getMyTeams(userId);

        List<MyTeamCardResponse> activeTeams = new ArrayList<>();
        List<MyTeamCardResponse> completedTeams = new ArrayList<>();

        for (TeamSummaryResponse t : myTeams) {
            MyTeamCardResponse card = myPageAssembler.toTeamCard(t);

            if (t.isCompleted()) {
                completedTeams.add(card);
            } else {
                activeTeams.add(card);
            }
        }

        MyProfileResponse profile = myPageAssembler.toProfile(user);

        return MyPageSummaryResponse.builder()
                .profile(profile)
                .activeTeams(activeTeams)
                .completedTeams(completedTeams)
                .build();
    }
}
