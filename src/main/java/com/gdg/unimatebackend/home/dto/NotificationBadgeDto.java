package com.gdg.unimatebackend.home.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationBadgeDto {
    private final boolean hasUnread;
    private final int unreadCount;
}