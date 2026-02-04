package com.gdg.unimatebackend.app.poke.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = {
        @Index(
                name = "idx_poke_cooldown",
                columnList = "team_id,sender_id,target_user_id,poke_message_id,created_at"
        )
})
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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Poke(Long teamId, Long senderId, Long targetUserId, PokeMessage pokeMessage){
        this.teamId = teamId;
        this.senderId = senderId;
        this.targetUserId = targetUserId;
        this.pokeMessage = pokeMessage;
    }
}
