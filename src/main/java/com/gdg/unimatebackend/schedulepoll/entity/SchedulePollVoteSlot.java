package com.gdg.unimatebackend.schedulepoll.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "schedule_poll_vote_slots")
public class SchedulePollVoteSlot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private SchedulePollVote vote;

    @Column(nullable = false)
    private Integer slotId;

    public SchedulePollVoteSlot(SchedulePollVote vote, Integer slotId) {
        this.vote = vote;
        this.slotId = slotId;
    }
}