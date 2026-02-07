package com.gdg.unimatebackend.schedulepoll.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "schedule_poll_dates")
public class SchedulePollDate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_poll_id", nullable = false)
    private SchedulePoll schedulePoll;

    @Column(nullable = false)
    private LocalDate date;

    public SchedulePollDate(LocalDate date) {
        this.date = date;
    }

    void setSchedulePoll(SchedulePoll schedulePoll) {
        this.schedulePoll = schedulePoll;
    }
}