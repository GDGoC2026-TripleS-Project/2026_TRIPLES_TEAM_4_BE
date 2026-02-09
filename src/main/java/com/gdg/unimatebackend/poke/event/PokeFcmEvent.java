package com.gdg.unimatebackend.poke.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PokeFcmEvent {
    private final Long notificationId;
    private final String eventKey;
    private final Long senderId;
    private final Long receiverId;
    private final Long pokeId;
    private final Long teamId;
    private final String teamName;
    private final String teamColorHex;
    private final String alarmType;
    private final String messageTitle;
    private final String messageBody;
    private final String pushTitle;
    private final String pushBody;
    private final LocalDateTime createdAt;
}
