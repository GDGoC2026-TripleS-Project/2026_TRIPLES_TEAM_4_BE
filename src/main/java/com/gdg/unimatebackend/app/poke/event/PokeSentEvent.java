package com.gdg.unimatebackend.app.poke.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PokeSentEvent {
    private final Long senderId;
    private final Long pokeMessageId;
    private final List<Long> targetUserIds;
}
