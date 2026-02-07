package com.gdg.unimatebackend.schedulepoll.repository;

import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchedulePollVoteRepository extends JpaRepository<SchedulePollVote, Long> {

    Optional<SchedulePollVote> findBySchedulePoll_IdAndVoterId(Long pollId, Long voterId);

    List<SchedulePollVote> findAllBySchedulePoll_Id(Long pollId);

    long countBySchedulePoll_Id(Long pollId);
}
