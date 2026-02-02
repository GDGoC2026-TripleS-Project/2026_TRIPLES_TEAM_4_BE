package com.gdg.unimatebackend.app.poke.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PokeRequest {

    private Long messageId;
    private List<Target> targets;

    @Getter
    @NoArgsConstructor
    public static class Target {
        private Long teamId;
        private Long userId;
    }
}
