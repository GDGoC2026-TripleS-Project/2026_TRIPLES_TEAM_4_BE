package com.gdg.unimatebackend.notification.service;

import com.gdg.unimatebackend.notification.dto.NotificationItemResponse;
import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.entity.NotificationReceipt;
import com.gdg.unimatebackend.notification.repository.NotificationReceiptRepository;
import com.gdg.unimatebackend.schedulepoll.entity.PollStatus;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePoll;
import com.gdg.unimatebackend.schedulepoll.repository.SchedulePollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationReceiptRepository notificationReceiptRepository;
    private final SchedulePollRepository schedulePollRepository;

    private static final Set<String> ACTION_TYPES = Set.of("POKE", "MEETING_REQUEST");
    private static final Set<String> MEETING_TYPES = Set.of("MEETING_CREATED", "MEETING_REQUEST");
    private static final String SCHEDULE_POLL_EVENT_PREFIX = "SCHEDULE_POLL_CREATE:";

    @Transactional(readOnly = true)
    public List<NotificationItemResponse> getMyNotifications(Long userId) {
        return notificationReceiptRepository.findAllByUserIdWithNotification(userId).stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public NotificationItemResponse markRead(Long notificationId, Long userId) {
        NotificationReceipt receipt = notificationReceiptRepository
                .findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다"));

        boolean action = isActionType(receipt.getNotification());
        if (action && !receipt.isCompleted()) {
            throw new IllegalArgumentException("actionDone 이후에만 읽음 처리할 수 있습니다");
        }

        receipt.markRead(LocalDateTime.now());
        return toItem(receipt);
    }

    @Transactional
    public int readAllNonAction(Long userId) {
        List<NotificationReceipt> receipts = notificationReceiptRepository.findAllByUserIdWithNotification(userId);
        int updated = 0;
        LocalDateTime now = LocalDateTime.now();
        for (NotificationReceipt r : receipts) {
            boolean action = isActionType(r.getNotification());
            if (action) continue;
            if (!r.isRead()) {
                r.markRead(now);
                updated++;
            }
        }
        return updated;
    }

    @Transactional
    public NotificationItemResponse markActionDone(Long notificationId, Long userId) {
        NotificationReceipt receipt = notificationReceiptRepository
                .findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다"));

        receipt.markCompleted(LocalDateTime.now());
        return toItem(receipt);
    }

    private boolean isActionType(Notification notification) {
        if (notification == null) return false;
        String type = notification.getType();
        return type != null && ACTION_TYPES.contains(type);
    }

    private NotificationItemResponse toItem(NotificationReceipt r) {
        Notification n = r.getNotification();
        boolean action = isActionType(n);
        MeetingNavigationInfo meetingNavigation = resolveMeetingNavigation(n);
        return NotificationItemResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .alarmType(n.getAlarmType())
                .teamId(n.getTeamId())
                .teamName(n.getTeamName())
                .teamColorHex(n.getTeamColorHex())
                .messageTitle(n.getMessageTitle())
                .messageBody(n.getMessageBody())
                .createdAt(n.getCreatedAt())
                .senderId(n.getSenderId())
                .receiverId(n.getReceiverId())
                .teamScheduleId(n.getTeamScheduleId())
                .pokeId(n.getPokeId())
                .meetingPollId(meetingNavigation.pollId)
                .meetingNavigationTarget(meetingNavigation.navigationTarget)
                .isRead(r.isRead())
                .processedAt(r.getProcessedAt())
                .action(action)
                .actionDone(r.isCompleted())
                .build();
    }

    private MeetingNavigationInfo resolveMeetingNavigation(Notification notification) {
        if (notification == null || notification.getType() == null || !MEETING_TYPES.contains(notification.getType())) {
            return MeetingNavigationInfo.empty();
        }

        Long pollId = extractSchedulePollId(notification.getEventKey());
        if (pollId == null) {
            return MeetingNavigationInfo.empty();
        }

        SchedulePoll poll = schedulePollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            return new MeetingNavigationInfo(pollId, null);
        }

        String target = toNavigationTarget(poll.getStatus());
        return new MeetingNavigationInfo(pollId, target);
    }

    private String toNavigationTarget(PollStatus status) {
        if (status == null) return null;
        return switch (status) {
            case OPEN -> "TIMEPICK_STATUS";
            case AUTO_FIXED -> "TIMEPICK_RESULT";
            case MANUALLY_FIXED -> "EDIT_TIMEPICK";
        };
    }

    private Long extractSchedulePollId(String eventKey) {
        if (eventKey == null || !eventKey.startsWith(SCHEDULE_POLL_EVENT_PREFIX)) {
            return null;
        }

        String suffix = eventKey.substring(SCHEDULE_POLL_EVENT_PREFIX.length());
        int separatorIndex = suffix.indexOf(':');
        String pollIdText = separatorIndex >= 0 ? suffix.substring(0, separatorIndex) : suffix;

        try {
            return Long.parseLong(pollIdText);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static class MeetingNavigationInfo {
        private final Long pollId;
        private final String navigationTarget;

        private MeetingNavigationInfo(Long pollId, String navigationTarget) {
            this.pollId = pollId;
            this.navigationTarget = navigationTarget;
        }

        private static MeetingNavigationInfo empty() {
            return new MeetingNavigationInfo(null, null);
        }
    }
}
