package com.gdg.unimatebackend.app.team.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "teams", // ✅ DB 테이블명과 일치
        indexes = {
                @Index(name = "idx_teams_owner", columnList = "owner_user_id")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60) // ✅ DB가 varchar(60)
    private String name;

    // DB에는 description/color 컬럼이 아직 없을 수도 있음.
    // 이미 운영 DB에 없다면, 아래 2개는 "DDL 추가"하거나, 당장 쓰지 않으면 제거해도 됨.
    @Column(length = 300)
    private String description;

    @Column(length = 30)
    private String color;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ teams 컬럼만 사용
    @Column(name = "invite_code", length = 6, unique = true)
    private String inviteCode;

    @Column(name = "invite_code_expires_at")
    private LocalDateTime inviteCodeExpiresAt;

    public void issueInviteCode(String inviteCode, LocalDateTime expiresAt) {
        this.inviteCode = inviteCode;
        this.inviteCodeExpiresAt = expiresAt;
    }

    public void clearInviteCode() {
        this.inviteCode = null;
        this.inviteCodeExpiresAt = null;
    }

    public void update(String name, String description, String color) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
        if (color != null) this.color = color;
    }
}
