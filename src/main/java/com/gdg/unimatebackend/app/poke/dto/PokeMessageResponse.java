package com.gdg.unimatebackend.app.poke.dto;

import com.gdg.unimatebackend.app.poke.entity.PokeMessage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PokeMessageResponse {

    private Long messageId;
    private String content;

    public static PokeMessageResponse from(PokeMessage message){
        return PokeMessageResponse.builder()
                .messageId(message.getId())
                .content(message.getContent())
                .build();
    }
}
