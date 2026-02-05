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
        name = "notification_receipts",
        indexes = {
                @Index(name = "idx_receipts_user_created", columnList = "user_id,created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_user", columnNames = "notification_id,user_id")
        }
)
public class NotificationReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void markCompleted(LocalDateTime completedAt) {
        if (!this.isCompleted) {
            this.isCompleted = true;
            this.completedAt = completedAt;
        }
    }

    public void markRead(LocalDateTime processedAt) {
        if (!this.isRead) {
            this.isRead = true;
            this.processedAt = processedAt;
        }
    }
}
