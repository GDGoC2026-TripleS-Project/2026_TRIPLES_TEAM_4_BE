package com.gdg.unimatebackend.app.team.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TeamJoinedEvent {
    private final Long teamId;
    private final Long joinedUserId;
}
