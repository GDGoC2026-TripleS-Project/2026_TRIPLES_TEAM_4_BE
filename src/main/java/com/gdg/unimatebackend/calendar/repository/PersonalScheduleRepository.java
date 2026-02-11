package com.gdg.unimatebackend.calendar.repository;

import com.gdg.unimatebackend.schedule.entity.MySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface PersonalScheduleRepository extends JpaRepository<MySchedule, Long> {

    @Query("""
        select s
        from MySchedule s
        where s.userId = :userId
          and s.teamId in :teamIds
          and s.startAt < :to
          and s.endAt > :from
    """)
    List<MySchedule> findMyOverlappingByTeamIds(
            @Param("userId") Long userId,
            @Param("teamIds") List<Long> teamIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        select s
        from MySchedule s
        where s.userId in :userIds
          and s.teamId in :teamIds
          and s.startAt < :to
          and s.endAt > :from
    """)
    List<MySchedule> findAllOverlappingByUserIdsAndTeamIds(
            @Param("userIds") Set<Long> userIds,
            @Param("teamIds") List<Long> teamIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}