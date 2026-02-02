package com.gdg.unimatebackend.app.schedule.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "my_schedule",
        indexes = {
                @Index(name = "idx_my_schedule_team_user", columnList = "team_id, user_id"),
                @Index(name = "idx_my_schedule_start_end", columnList = "start_at, end_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="team_id", nullable = false)
    private Long teamId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(name="memo", length = 500)
    private String memo;

    @Column(name="start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name="end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name="is_private", nullable = false)
    private boolean isPrivate;

    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(
            String title,
            String memo,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean isPrivate
    ) {
        this.title = title;
        this.memo = memo;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isPrivate = isPrivate;
    }
}
