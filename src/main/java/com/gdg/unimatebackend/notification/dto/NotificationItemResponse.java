package com.gdg.unimatebackend.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationItemResponse {
    private Long id;
    private String type;
    private String alarmType;
    private Long teamId;
    private String teamName;
    private String teamColorHex;
    private String messageTitle;
    private String messageBody;
    private LocalDateTime createdAt;
    private Long senderId;
    private Long receiverId;
    private Long teamScheduleId;
    private Long pokeId;
    private Long meetingPollId;
    private String meetingNavigationTarget;

    private boolean isRead;
    private boolean action;
    private boolean actionDone;
    private LocalDateTime processedAt;
}
