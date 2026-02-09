package com.gdg.unimatebackend.schedule.team.repository;

import com.gdg.unimatebackend.schedule.team.entity.TeamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeamScheduleRepository extends JpaRepository<TeamSchedule, Long> {

    Optional<TeamSchedule> findByIdAndTeamId(Long id, Long teamId);

    @Query("""
        select s
        from TeamSchedule s
        where s.teamId = :teamId
          and s.startAt < :rangeEnd
          and s.endAt > :rangeStart
        order by s.startAt asc
    """)
    List<TeamSchedule> findOverlaps(
            @Param("teamId") Long teamId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

    List<TeamSchedule> findByStartAtBetween(LocalDateTime startAt, LocalDateTime endAt);

    List<TeamSchedule> findByEndAtBetween(LocalDateTime startAt, LocalDateTime endAt);
}
