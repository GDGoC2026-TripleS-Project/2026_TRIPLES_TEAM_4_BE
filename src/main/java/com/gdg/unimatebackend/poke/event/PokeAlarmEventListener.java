package com.gdg.unimatebackend.poke.event;

import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.service.NotificationService;
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PokeAlarmEventListener {

    private final TeamRepository teamRepository;
    private final NotificationService notificationService;
    private final EntityManager em;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            fallbackExecution = true
    )
    public void handle(PokeSentEvent event) {
        log.info("[POKE][NOTI] listener fired. senderId={}, messageId={}, targets={}",
                event.getSenderId(), event.getPokeMessageId(), event.getTargetUserIds());
        // ✅ 캡쳐 문구 템플릿
        PokeAlarmTypeMapper.PokeAlarmTemplate template =
                PokeAlarmTypeMapper.fromMessageId(event.getPokeMessageId());

        String alarmType = template.getAlarmType();          // 섹션 헤더
        String messageTitle = template.getMessageTitle();    // 카드/푸시 메인
        String messageBody = template.getMessageBody();      // 카드 서브

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromTime = now.minusMinutes(30);

        // 최근 30분 내 동일 sender + messageId + targets 의 poke 중 최신만 팀/타겟별로 1개씩
        List<Object[]> rows = em.createQuery("""
                        select p.id, p.teamId, p.targetUserId, p.createdAt
                        from Poke p
                        where p.senderId = :senderId
                          and p.pokeMessage.id = :messageId
                          and p.targetUserId in :targetUserIds
                          and p.createdAt >= :fromTime
                        order by p.createdAt desc
                """, Object[].class)
                .setParameter("senderId", event.getSenderId())
                .setParameter("messageId", event.getPokeMessageId())
                .setParameter("targetUserIds", event.getTargetUserIds())
                .setParameter("fromTime", fromTime)
                .getResultList();

        if (rows.isEmpty()) {
            log.warn("[POKE][NOTI] no recent pokes. senderId={}, messageId={}, targets={}",
                    event.getSenderId(), event.getPokeMessageId(), event.getTargetUserIds());
            return;
        }

        // teamId:targetUserId 기준으로 최신 1개만 남김
        Map<String, PokeRow> latestByTeamTarget = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Long pokeId = (Long) row[0];
            Long teamId = (Long) row[1];
            Long targetUserId = (Long) row[2];
            LocalDateTime createdAt = (LocalDateTime) row[3];

            String key = teamId + ":" + targetUserId;
            latestByTeamTarget.putIfAbsent(key, new PokeRow(pokeId, teamId, targetUserId, createdAt));
        }

        // teamId로 그룹핑
        Map<Long, List<PokeRow>> byTeam = new LinkedHashMap<>();
        for (PokeRow row : latestByTeamTarget.values()) {
            byTeam.computeIfAbsent(row.teamId, k -> new ArrayList<>()).add(row);
        }

        // 팀 정보 조회
        Map<Long, Team> teamMap = new HashMap<>();
        teamRepository.findAllById(byTeam.keySet())
                .forEach(t -> teamMap.put(t.getId(), t));

        // 팀 단위로 Notification N개 + receipt N개 (FCM은 AFTER_COMMIT 이벤트로 분리)
        for (Map.Entry<Long, List<PokeRow>> entry : byTeam.entrySet()) {
            Long teamId = entry.getKey();
            Team team = teamMap.get(teamId);
            if (team == null) {
                log.warn("[POKE][NOTI] team not found. teamId={}", teamId);
                continue;
            }

            String teamName = safeTeamName(team.getName());
            String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                    ? team.getColor().getHex()
                    : "";

            // ✅ 시스템 푸시에서 보이는 제목/본문 규칙
            // title: 팀명(체리시)
            // body : 메인 문구(자료를 기다리고...)
            String pushTitle = teamName;
            String pushBody = messageTitle; // 길면 여기만 보이는 게 보통이라 메인 문구를 body로

            for (PokeRow row : entry.getValue()) {
                Long receiverId = row.targetUserId;
                Long pokeId = row.pokeId;

                // eventKey: receiver 단위로 고유
                String eventKey = "POKE:" + pokeId + ":" + receiverId;

                // ✅ DB 저장(수신자 단위)
                Notification notification = Notification.builder()
                        .eventKey(eventKey)
                        .type("POKE")
                        .alarmType(alarmType)
                        .teamId(teamId)
                        .teamName(teamName)
                        .teamColorHex(teamColorHex)
                        .messageTitle(messageTitle)
                        .messageBody(messageBody)
                        .senderId(event.getSenderId())
                        .receiverId(receiverId)
                        .pokeId(pokeId)
                        .build();

                Notification saved = notificationService.createNotificationWithReceipt(notification, receiverId);

                LocalDateTime createdAt = saved.getCreatedAt() != null ? saved.getCreatedAt() : now;

                eventPublisher.publishEvent(PokeFcmEvent.builder()
                        .notificationId(saved.getId())
                        .eventKey(saved.getEventKey())
                        .senderId(event.getSenderId())
                        .receiverId(receiverId)
                        .pokeId(pokeId)
                        .teamId(teamId)
                        .teamName(teamName)
                        .teamColorHex(teamColorHex)
                        .alarmType(alarmType)
                        .messageTitle(messageTitle)
                        .messageBody(messageBody)
                        .pushTitle(pushTitle)
                        .pushBody(pushBody)
                        .createdAt(createdAt)
                        .build());
            }
        }
    }

    private String safeTeamName(String teamName) {
        if (teamName == null || teamName.isBlank()) return "팀";
        return teamName;
    }

    private static class PokeRow {
        private final Long pokeId;
        private final Long teamId;
        private final Long targetUserId;
        private final LocalDateTime createdAt;

        private PokeRow(Long pokeId, Long teamId, Long targetUserId, LocalDateTime createdAt) {
            this.pokeId = pokeId;
            this.teamId = teamId;
            this.targetUserId = targetUserId;
            this.createdAt = createdAt;
        }
    }
}
