package com.gdg.unimatebackend.team.event;

import com.gdg.unimatebackend.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.alarm.entity.FcmDeviceToken;
import com.gdg.unimatebackend.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.alarm.service.FcmService;
import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.service.NotificationService;
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import com.gdg.unimatebackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamMemberFcmNotifier {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final FcmService fcmService;
    private final NotificationService notificationService;

    /**
     * ✅ 신규 가입 알림: 신규가입(INSERT) 성공 커밋 이후에만 실행
     * - 본인 제외
     * - 유저당 활성 토큰 1개만
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTeamJoined(TeamJoinedEvent event) {
        Long teamId = event.getTeamId();
        Long joinedUserId = event.getJoinedUserId();
        String joinTraceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        String joinedNickname = userRepository.findById(joinedUserId)
                .map(u -> (u.getNickname() == null || u.getNickname().isBlank()) ? "새 팀원" : u.getNickname())
                .orElse("새 팀원");

        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return;
        String teamName = team.getName() != null ? team.getName() : "팀";
        String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                ? team.getColor().getHex()
                : "#CCCCCC";

        // 현재 팀 멤버 목록(커밋 이후이므로 신규 가입자 포함)
        List<Long> receiverUserIds = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId).stream()
                .map(m -> m.getUserId())
                .filter(uid -> !uid.equals(joinedUserId))
                .distinct()
                .toList();

        for (Long receiverId : receiverUserIds) {
            Notification notification = Notification.builder()
                    .eventKey(buildJoinEventKey(teamId, joinedUserId, receiverId, joinTraceId))
                    .type("TEAM_MEMBER_JOINED")
                    .alarmType("팀플 참여 알림")
                    .teamId(teamId)
                    .teamName(teamName)
                    .teamColorHex(teamColorHex)
                    .messageTitle("새 팀원이 참여했어요!")
                    .messageBody(joinedNickname + "님이 " + teamName + "에 참가했어요.")
                    .senderId(joinedUserId)
                    .receiverId(receiverId)
                    .build();
            Notification saved = notificationService.createNotificationWithReceipt(notification, receiverId);

            Optional<FcmDeviceToken> tokenOpt =
                    fcmDeviceTokenRepository.findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(receiverId);

            if (tokenOpt.isEmpty()) continue;

            try {
                Map<String, String> data = toDataMap(saved, receiverId);
                fcmService.sendMessageTo(FcmSendDto.builder()
                        .token(tokenOpt.get().getToken())
                        .title("팀플 참여 알림")
                        .body(joinedNickname + "님이 " + teamName + "에 참가했어요.")
                        .data(data)
                        .build());
            } catch (Exception e) {
                // 푸시 실패해도 비즈니스 트랜잭션은 이미 커밋 완료. 로그만 남김.
                log.warn("FCM(join) failed. teamId={}, receiverId={}, reason={}", teamId, receiverId, e.getMessage());
            }
        }

        Notification joinedUserNotification = Notification.builder()
                .eventKey(buildJoinSelfEventKey(teamId, joinedUserId, joinTraceId))
                .type("TEAM_JOINED")
                .alarmType("팀플 참여 알림")
                .teamId(teamId)
                .teamName(teamName)
                .teamColorHex(teamColorHex)
                .messageTitle("팀에 참여했어요!")
                .messageBody(teamName + " 팀플에 참여가 완료되었어요.")
                .senderId(joinedUserId)
                .receiverId(joinedUserId)
                .build();
        Notification joinedSaved = notificationService.createNotificationWithReceipt(joinedUserNotification, joinedUserId);

        Optional<FcmDeviceToken> joinedToken =
                fcmDeviceTokenRepository.findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(joinedUserId);
        if (joinedToken.isPresent()) {
            try {
                Map<String, String> data = toDataMap(joinedSaved, joinedUserId);
                fcmService.sendMessageTo(FcmSendDto.builder()
                        .token(joinedToken.get().getToken())
                        .title("팀플 참여 알림")
                        .body(teamName + " 팀플에 참여가 완료되었어요.")
                        .data(data)
                        .build());
            } catch (Exception e) {
                log.warn("FCM(join-self) failed. teamId={}, receiverId={}, reason={}", teamId, joinedUserId, e.getMessage());
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

        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return;
        String teamName = team.getName() != null ? team.getName() : "팀";
        String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                ? team.getColor().getHex()
                : "#CCCCCC";

        // 커밋 이후이므로 이미 leftUserId는 team_member에서 삭제된 상태
        List<Long> receiverUserIds = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId).stream()
                .map(m -> m.getUserId())
                .distinct()
                .toList();

        for (Long receiverId : receiverUserIds) {
            Notification notification = Notification.builder()
                    .eventKey("TEAM_LEFT:" + teamId + ":" + leftUserId + ":" + receiverId)
                    .type("TEAM_MEMBER_LEFT")
                    .alarmType("팀플 탈퇴 알림")
                    .teamId(teamId)
                    .teamName(teamName)
                    .teamColorHex(teamColorHex)
                    .messageTitle("팀원이 나갔어요")
                    .messageBody(leftNickname + "님이 " + teamName + "에서 나갔어요.")
                    .senderId(leftUserId)
                    .receiverId(receiverId)
                    .build();
            Notification saved = notificationService.createNotificationWithReceipt(notification, receiverId);

            Optional<FcmDeviceToken> tokenOpt =
                    fcmDeviceTokenRepository.findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(receiverId);

            if (tokenOpt.isEmpty()) continue;

            try {
                Map<String, String> data = toDataMap(saved, receiverId);
                fcmService.sendMessageTo(FcmSendDto.builder()
                        .token(tokenOpt.get().getToken())
                        .title("팀플 탈퇴 알림")
                        .body(leftNickname + "님이 " + teamName + "에서 나갔어요.")
                        .data(data)
                        .build());
            } catch (Exception e) {
                log.warn("FCM(leave) failed. teamId={}, receiverId={}, reason={}", teamId, receiverId, e.getMessage());
            }
        }
    }

    private Map<String, String> toDataMap(Notification notification, Long receiverId) {
        LocalDateTime createdAt = notification.getCreatedAt() != null
                ? notification.getCreatedAt()
                : LocalDateTime.now();
        String createdAtText = createdAt.atOffset(ZoneOffset.ofHours(9)).toString();

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", notification.getType() != null ? notification.getType() : "");
        data.put("receiverId", String.valueOf(receiverId));
        data.put("teamId", String.valueOf(notification.getTeamId()));
        data.put("teamName", notification.getTeamName() != null ? notification.getTeamName() : "");
        data.put("teamColorHex", notification.getTeamColorHex() != null ? notification.getTeamColorHex() : "#CCCCCC");
        data.put("alarmType", notification.getAlarmType() != null ? notification.getAlarmType() : "알림");
        data.put("messageTitle", notification.getMessageTitle() != null ? notification.getMessageTitle() : "");
        data.put("messageBody", notification.getMessageBody() != null ? notification.getMessageBody() : "");
        data.put("createdAt", createdAtText);
        return data;
    }

    private String buildJoinEventKey(Long teamId, Long joinedUserId, Long receiverId, String trace) {
        return "TJ:"
                + toBase36(teamId) + ":"
                + toBase36(joinedUserId) + ":"
                + toBase36(receiverId) + ":"
                + trace;
    }

    private String buildJoinSelfEventKey(Long teamId, Long joinedUserId, String trace) {
        return "TJS:"
                + toBase36(teamId) + ":"
                + toBase36(joinedUserId) + ":"
                + trace;
    }

    private String toBase36(Long value) {
        if (value == null) return "0";
        return Long.toUnsignedString(value, 36);
    }
}
