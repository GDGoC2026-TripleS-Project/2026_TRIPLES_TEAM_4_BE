package com.gdg.unimatebackend.app.schedule.repository;

import com.gdg.unimatebackend.app.schedule.entity.MySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MyScheduleRepository extends JpaRepository<MySchedule, Long> {

    Optional<MySchedule> findByIdAndTeamId(Long id, Long teamId);

    /**
     * 기간 겹침(overlap) 조회:
     * start < rangeEnd AND end > rangeStart
     */
    @Query("""
        select s
        from MySchedule s
        where s.teamId = :teamId
          and s.userId = :userId
          and s.startAt < :rangeEnd
          and s.endAt > :rangeStart
        order by s.startAt asc
    """)
    List<MySchedule> findOverlaps(
            @Param("teamId") Long teamId,
            @Param("userId") Long userId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

    /**
     * 지금 시각 기준 Busy 일정 조회:
     * startAt <= now AND endAt > now
     */
    @Query("""
        select s
        from MySchedule s
        where s.teamId = :teamId
          and s.userId = :userId
          and s.startAt <= :now
          and s.endAt > :now
        order by s.startAt asc
    """)
    List<MySchedule> findNowOverlaps(
            @Param("teamId") Long teamId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
}
