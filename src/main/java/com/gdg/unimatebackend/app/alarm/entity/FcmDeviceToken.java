package com.gdg.unimatebackend.app.alarm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fcm_device_token",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fcm_user_device_platform",
                        columnNames = {"user_id", "device_id", "platform"}
                )
        },
        indexes = {
                @Index(name = "idx_fcm_device_token_user", columnList = "user_id"),
                @Index(name = "idx_fcm_device_token_active", columnList = "is_active"),
                @Index(name = "idx_fcm_token", columnList = "token")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FcmDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(length = 32)
    private String platform; // ANDROID / IOS 등

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.isActive = true;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
