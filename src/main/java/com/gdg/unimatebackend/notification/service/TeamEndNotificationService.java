package com.gdg.unimatebackend.notification.service;

import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamEndNotificationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    @Transactional
    public void notifyIfEnded(Team team, Long senderUserId) {
        if (team == null || team.getId() == null || team.getEndAt() == null) return;
        LocalDate today = LocalDate.now(KST);
        if (team.getEndAt().isAfter(today)) return;
        createTeamEndNotifications(team, senderUserId);
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    @Transactional
    public void generateForEndedTeams() {
        LocalDate today = LocalDate.now(KST);
        List<Team> teams = teamRepository.findByEndAtLessThanEqual(today);
        for (Team team : teams) {
            createTeamEndNotifications(team, 0L);
        }
    }

    private void createTeamEndNotifications(Team team, Long senderUserId) {
        String teamName = team.getName() != null ? team.getName() : "팀";
        String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                ? team.getColor().getHex()
                : "#CCCCCC";

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(team.getId());
        for (TeamMember member : members) {
            Long receiverId = member.getUserId();
            if (receiverId == null) continue;

            Notification notification = Notification.builder()
                    .eventKey("TEAM_END:" + team.getId() + ":" + receiverId + ":" + team.getEndAt())
                    .type("TEAM_END")
                    .alarmType("팀플 종료 알림")
                    .teamId(team.getId())
                    .teamName(teamName)
                    .teamColorHex(teamColorHex)
                    .messageTitle("팀플이 종료되었습니다!")
                    .messageBody("그동안 고생하셨어요🎉")
                    .senderId(senderUserId)
                    .receiverId(receiverId)
                    .build();

            notificationService.createNotificationWithReceipt(notification, receiverId);
        }
    }
}
