package com.gdg.unimatebackend.todo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "todo",
        indexes = {
                @Index(name = "idx_todo_team_date", columnList = "team_id, date"),
                @Index(name = "idx_todo_team_user_date", columnList = "team_id, user_id, date")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // TODO는 "해당 날짜의 할 일"이라서 LocalDate로 고정
    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void markCompleted(LocalDateTime completedAt) {
        if (!this.isCompleted) {
            this.isCompleted = true;
            this.completedAt = completedAt;
        }
    }

    public void markUncompleted() {
        if (this.isCompleted) {
            this.isCompleted = false;
            this.completedAt = null;
        }
    }
}
