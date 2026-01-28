package com.gdg.unimatebackend.app.team.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeamColor {
    RED("#EF4444"),
    YELLOW("#F59E0B"),
    GREEN("#22C55E"),
    BLUE("#3B82F6"),
    PURPLE("#8B5CF6");

    private final String hex;
}
