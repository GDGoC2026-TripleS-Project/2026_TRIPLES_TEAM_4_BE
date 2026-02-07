package com.gdg.unimatebackend.schedulepoll.repository;

import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollVoteSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulePollVoteSlotRepository extends JpaRepository<SchedulePollVoteSlot, Long> {
}