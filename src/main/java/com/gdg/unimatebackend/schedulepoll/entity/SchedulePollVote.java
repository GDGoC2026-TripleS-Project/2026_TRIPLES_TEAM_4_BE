package com.gdg.unimatebackend.schedulepoll.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "schedule_poll_votes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_poll_voter", columnNames = {"schedule_poll_id", "voter_id"})
        }
)
public class SchedulePollVote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_poll_id", nullable = false)
    private SchedulePoll schedulePoll;

    @Column(name = "voter_id", nullable = false)
    private Long voterId; // ✅ userId

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchedulePollVoteSlot> slots = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public SchedulePollVote(SchedulePoll poll, Long voterId) {
        this.schedulePoll = poll;
        this.voterId = voterId;
        this.updatedAt = LocalDateTime.now();
    }

    public void replaceSlots(List<Integer> newSlotIds) {
        this.slots.clear();
        for (Integer slotId : newSlotIds) {
            this.slots.add(new SchedulePollVoteSlot(this, slotId));
        }
        this.updatedAt = LocalDateTime.now();
    }
}