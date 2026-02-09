package com.gdg.unimatebackend.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_team_created", columnList = "team_id,created_at"),
                @Index(name = "idx_notifications_receiver_created", columnList = "receiver_id,created_at"),
                @Index(name = "idx_notifications_schedule", columnList = "team_schedule_id"),
                @Index(name = "idx_notifications_poke", columnList = "poke_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_event_key", columnNames = "event_key")
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 64)
    private String eventKey;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "alarm_type", nullable = false, length = 30)
    private String alarmType;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "team_name", nullable = false, length = 60)
    private String teamName;

    @Column(name = "team_color_hex", nullable = false, length = 10)
    private String teamColorHex;

    @Column(name = "message_title", nullable = false, length = 120)
    private String messageTitle;

    @Column(name = "message_body", nullable = false, length = 1000)
    private String messageBody;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "team_schedule_id")
    private Long teamScheduleId;

    @Column(name = "poke_id")
    private Long pokeId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
