package com.gdg.unimatebackend.schedulepoll.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "schedule_polls")
public class SchedulePoll {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private Integer slotMinutes;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private String title;
    private String memo;
    private String alarm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PollStatus status;

    @Column(nullable = false)
    private boolean locked;

    private Integer autoFixedSlotId;
    private Integer fixedSlotId;

    @OneToMany(mappedBy = "schedulePoll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchedulePollDate> dates = new ArrayList<>();

    @OneToMany(mappedBy = "schedulePoll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchedulePollVote> votes = new ArrayList<>();

    @Builder
    public SchedulePoll(Long teamId, String timezone, Integer slotMinutes, LocalTime startTime, LocalTime endTime,
                        String title, String memo, String alarm) {
        this.teamId = teamId;
        this.timezone = timezone;
        this.slotMinutes = slotMinutes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.memo = memo;
        this.alarm = alarm;
        this.status = PollStatus.OPEN;
        this.locked = false;
    }

    public void addDate(SchedulePollDate date) {
        this.dates.add(date);
        date.setSchedulePoll(this);
    }

    public void updateMeta(String title, String memo, String alarm) {
        if (title != null) this.title = title;
        if (memo != null) this.memo = memo;
        if (alarm != null) this.alarm = alarm;
    }

    public void setAutoFixed(Integer slotId) {
        this.autoFixedSlotId = slotId;
        this.status = PollStatus.AUTO_FIXED;
    }

    public void fixManually(Integer fixedSlotId) {
        this.fixedSlotId = fixedSlotId;
        this.status = PollStatus.MANUALLY_FIXED;
        this.locked = true;
    }
}
