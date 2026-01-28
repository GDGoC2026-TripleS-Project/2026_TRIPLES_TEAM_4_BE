package com.gdg.unimatebackend.app.team.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TeamLeftEvent {
    private final Long teamId;
    private final Long leftUserId;
}
