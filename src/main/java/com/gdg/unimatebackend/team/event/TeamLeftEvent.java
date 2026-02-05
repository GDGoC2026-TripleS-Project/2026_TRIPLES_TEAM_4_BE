package com.gdg.unimatebackend.team.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TeamLeftEvent {
    private final Long teamId;
    private final Long leftUserId;
}
