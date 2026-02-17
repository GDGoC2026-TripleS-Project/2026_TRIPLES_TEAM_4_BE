package com.gdg.unimatebackend.mypage.service;

import com.gdg.unimatebackend.mypage.dto.MyProfileResponse;
import com.gdg.unimatebackend.mypage.dto.MyTeamCardResponse;
import com.gdg.unimatebackend.team.dto.TeamSummaryResponse;
import com.gdg.unimatebackend.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class MyPageAssembler {

    public MyProfileResponse toProfile(User user) {
        return MyProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public MyTeamCardResponse toTeamCard(TeamSummaryResponse t) {
        return MyTeamCardResponse.builder()
                .teamId(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .memberCount(t.getMemberCount())
                .startAt(t.getStartAt())
                .endAt(t.getEndAt())
                .isCompleted(t.isCompleted())
                .color(t.getColor() != null ? t.getColor().name() : null)
                .colorHex(t.getColorHex())
                .dDay(calcDDay(t.getEndAt(), t.isCompleted()))
                .build();
    }

    private String calcDDay(LocalDateTime endAt, boolean isCompleted) {
        if (endAt == null || isCompleted) {
            return null;
        }

        long diff = ChronoUnit.DAYS.between(LocalDate.now(), endAt.toLocalDate());

        if (diff >= 0) {
            return "D-" + diff;
        }
        return "D+" + Math.abs(diff);
    }
}
