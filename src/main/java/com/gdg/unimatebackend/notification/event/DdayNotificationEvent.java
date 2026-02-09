package com.gdg.unimatebackend.notification.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DdayNotificationEvent {
    private final Long notificationId;
    private final Long receiverId;
    private final Long teamId;
    private final String teamName;
    private final String teamColorHex;
    private final Long teamScheduleId;
    private final int dday;
    private final String messageTitle;
    private final String messageBody;
    private final LocalDateTime createdAt;
}
