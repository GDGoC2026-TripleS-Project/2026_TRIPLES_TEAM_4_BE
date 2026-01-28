package com.gdg.unimatebackend.app.team.event;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
import com.gdg.unimatebackend.app.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import com.gdg.unimatebackend.app.team.entity.Team;
import com.gdg.unimatebackend.app.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.app.team.repository.TeamRepository;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamMemberFcmNotifier {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final FcmService fcmService;

    /**
     * ✅ 신규 가입 알림: 신규가입(INSERT) 성공 커밋 이후에만 실행
     * - 본인 제외
     * - 유저당 활성 토큰 1개만
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTeamJoined(TeamJoinedEvent event) {
        Long teamId = event.getTeamId();
        Long joinedUserId = event.getJoinedUserId();

        String joinedNickname = userRepository.findById(joinedUserId)
                .map(u -> (u.getNickname() == null || u.getNickname().isBlank()) ? "새 팀원" : u.getNickname())
                .orElse("새 팀원");

        String teamName = teamRepository.findById(teamId)
                .map(Team::getName)
                .orElse("팀");

        // 현재 팀 멤버 목록(커밋 이후이므로 신규 가입자 포함)
        List<Long> receiverUserIds = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId).stream()
                .map(m -> m.getUserId())
                .filter(uid -> !uid.equals(joinedUserId))
                .distinct()
                .toList();

        for (Long receiverId : receiverUserIds) {
            Optional<FcmDeviceToken> tokenOpt =
                    fcmDeviceTokenRepository.findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(receiverId);

            if (tokenOpt.isEmpty()) continue;

            try {
                fcmService.sendMessageTo(FcmSendDto.builder()
                        .token(tokenOpt.get().getToken())
                        .title("팀 참가")
                        .body(joinedNickname + "님이 " + teamName + "에 참가했어요.")
                        .build());
            } catch (Exception e) {
                // 푸시 실패해도 비즈니스 트랜잭션은 이미 커밋 완료. 로그만 남김.
                log.warn("FCM(join) failed. teamId={}, receiverId={}, reason={}", teamId, receiverId, e.getMessage());
            }
        }
    }

    /**
     * ✅ 탈퇴 알림: 탈퇴(DELETE) 성공 커밋 이후에만 실행
     * - 남아있는 팀원들에게 발송 (탈퇴자는 이미 팀에서 빠져있음)
     * - 유저당 활성 토큰 1개만
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTeamLeft(TeamLeftEvent event) {
        Long teamId = event.getTeamId();
        Long leftUserId = event.getLeftUserId();

        String leftNickname = userRepository.findById(leftUserId)
                .map(u -> (u.getNickname() == null || u.getNickname().isBlank()) ? "팀원" : u.getNickname())
                .orElse("팀원");

        String teamName = teamRepository.findById(teamId)
                .map(Team::getName)
                .orElse("팀");

        // 커밋 이후이므로 이미 leftUserId는 team_member에서 삭제된 상태
        List<Long> receiverUserIds = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId).stream()
                .map(m -> m.getUserId())
                .distinct()
                .toList();

        for (Long receiverId : receiverUserIds) {
            Optional<FcmDeviceToken> tokenOpt =
                    fcmDeviceTokenRepository.findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(receiverId);

            if (tokenOpt.isEmpty()) continue;

            try {
                fcmService.sendMessageTo(FcmSendDto.builder()
                        .token(tokenOpt.get().getToken())
                        .title("팀 탈퇴")
                        .body(leftNickname + "님이 " + teamName + "에서 나갔어요.")
                        .build());
            } catch (Exception e) {
                log.warn("FCM(leave) failed. teamId={}, receiverId={}, reason={}", teamId, receiverId, e.getMessage());
            }
        }
    }
}
