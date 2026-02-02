package com.gdg.unimatebackend.app.poke.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poke {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //찌르기가 발생한 팀
    @Column(nullable = false)
    private Long teamId;

    // 찌른 사람 (본인)
    @Column(nullable = false)
    private Long senderId;

    // 찌름 당한 사람 (대상자)
    @Column(nullable = false)
    private Long targetUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poke_message_id", nullable = false)
    private PokeMessage pokeMessage;

    @Builder
    public Poke(Long teamId, Long senderId, Long targetUserId, PokeMessage pokeMessage){
        this.teamId = teamId;
        this.senderId = senderId;
        this.targetUserId = targetUserId;
        this.pokeMessage = pokeMessage;
    }
}
