package com.gdg.unimatebackend.schedulepoll.repository;

import com.gdg.unimatebackend.schedulepoll.entity.SchedulePoll;
import com.gdg.unimatebackend.schedulepoll.entity.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface SchedulePollRepository extends JpaRepository<SchedulePoll, Long> {

    /**
     * 팀 캘린더(월/기간 마킹)용: "전체(회의)"로 표시할 모이기 목록
     * - MANUALLY_FIXED: 팀장이 확정한 회의 (locked=true && fixedSlotId != null)
     * - AUTO_FIXED: 전원 투표 완료로 자동 픽스된 회의 (autoFixedSlotId != null)
     */
    @Query("""
        select distinct p
        from SchedulePoll p
        join p.dates d
        where p.teamId = :teamId
          and (
                (p.status = com.gdg.unimatebackend.schedulepoll.entity.PollStatus.MANUALLY_FIXED
                 and p.locked = true
                 and p.fixedSlotId is not null)
             or (p.status = com.gdg.unimatebackend.schedulepoll.entity.PollStatus.AUTO_FIXED
                 and p.autoFixedSlotId is not null)
              )
          and d.date between :from and :to
        """)
    List<SchedulePoll> findFixedByTeamIdAndRange(Long teamId, LocalDate from, LocalDate to);

    /**
     * 팀 캘린더(하루 클릭)용: 해당 날짜의 "전체(회의)" 모이기 목록
     * - MANUALLY_FIXED / AUTO_FIXED 모두 포함
     */
    @Query("""
        select distinct p
        from SchedulePoll p
        join p.dates d
        where p.teamId = :teamId
          and (
                (p.status = com.gdg.unimatebackend.schedulepoll.entity.PollStatus.MANUALLY_FIXED
                 and p.locked = true
                 and p.fixedSlotId is not null)
             or (p.status = com.gdg.unimatebackend.schedulepoll.entity.PollStatus.AUTO_FIXED
                 and p.autoFixedSlotId is not null)
              )
          and d.date = :date
        """)
    List<SchedulePoll> findFixedByTeamIdAndDay(Long teamId, LocalDate date);
}

