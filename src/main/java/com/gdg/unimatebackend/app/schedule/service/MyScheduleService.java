package com.gdg.unimatebackend.app.schedule.service;

import com.gdg.unimatebackend.app.schedule.dto.*;
import com.gdg.unimatebackend.app.schedule.entity.MySchedule;
import com.gdg.unimatebackend.app.schedule.exception.MyScheduleErrorCodes;
import com.gdg.unimatebackend.app.schedule.exception.MyScheduleException;
import com.gdg.unimatebackend.app.schedule.repository.MyScheduleRepository;
import com.gdg.unimatebackend.app.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MyScheduleService {

    private final MyScheduleRepository myScheduleRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public MyScheduleResponse create(Long userId, Long teamId, MyScheduleCreateRequest request) {
        requireMember(teamId, userId);
        validateRange(request.getStartAt(), request.getEndAt());

        MySchedule saved = myScheduleRepository.save(
                MySchedule.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .title(request.getTitle())
                        .memo(request.getMemo())
                        .startAt(request.getStartAt())
                        .endAt(request.getEndAt())
                        .isPrivate(Boolean.TRUE.equals(request.getIsPrivate()))
                        .build()
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MyScheduleMarkingResponse getMarkedDates(Long userId, Long teamId, LocalDate from, LocalDate to) {
        requireMember(teamId, userId);

        if (from == null || to == null) {
            throw new MyScheduleException(MyScheduleErrorCodes.INVALID_RANGE, "from/to는 필수입니다", 400);
        }
        if (to.isBefore(from)) {
            throw new MyScheduleException(MyScheduleErrorCodes.INVALID_RANGE, "to는 from보다 빠를 수 없습니다", 400);
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay();

        List<MySchedule> overlaps = myScheduleRepository.findOverlaps(teamId, userId, rangeStart, rangeEnd);

        Set<LocalDate> days = new HashSet<>();
        for (MySchedule s : overlaps) {
            LocalDateTime sStart = max(s.getStartAt(), rangeStart);
            LocalDateTime sEnd = min(s.getEndAt(), rangeEnd);

            LocalDate startDay = sStart.toLocalDate();
            LocalDate endDay = sEnd.minusNanos(1).toLocalDate();

            LocalDate cur = startDay;
            while (!cur.isAfter(endDay)) {
                if (!cur.isBefore(from) && !cur.isAfter(to)) days.add(cur);
                cur = cur.plusDays(1);
            }
        }

        List<LocalDate> sorted = days.stream().sorted().toList();
        return new MyScheduleMarkingResponse(sorted);
    }

    @Transactional
    public MyScheduleResponse update(Long userId, Long teamId, Long scheduleId, MyScheduleUpdateRequest request) {
        requireMember(teamId, userId);
        validateRange(request.getStartAt(), request.getEndAt());

        MySchedule schedule = myScheduleRepository.findByIdAndTeamId(scheduleId, teamId)
                .orElseThrow(() -> new MyScheduleException(
                        MyScheduleErrorCodes.SCHEDULE_NOT_FOUND, "일정을 찾을 수 없습니다", 404
                ));

        requireOwner(schedule, userId);

        schedule.update(
                request.getTitle(),
                request.getMemo(),
                request.getStartAt(),
                request.getEndAt(),
                Boolean.TRUE.equals(request.getIsPrivate())
        );

        return toResponse(schedule);
    }

    @Transactional
    public void delete(Long userId, Long teamId, Long scheduleId) {
        requireMember(teamId, userId);

        MySchedule schedule = myScheduleRepository.findByIdAndTeamId(scheduleId, teamId)
                .orElseThrow(() -> new MyScheduleException(
                        MyScheduleErrorCodes.SCHEDULE_NOT_FOUND, "일정을 찾을 수 없습니다", 404
                ));

        requireOwner(schedule, userId);
        myScheduleRepository.delete(schedule);
    }

    // ====== helpers ======

    private void requireMember(Long teamId, Long userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new MyScheduleException(MyScheduleErrorCodes.NOT_A_MEMBER, "팀 멤버만 접근할 수 있습니다", 403);
        }
    }

    private void requireOwner(MySchedule schedule, Long userId) {
        if (!Objects.equals(schedule.getUserId(), userId)) {
            throw new MyScheduleException(MyScheduleErrorCodes.FORBIDDEN, "본인 소유의 개인 일정만 수정/삭제할 수 있습니다", 403);
        }
    }

    private void validateRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new MyScheduleException(MyScheduleErrorCodes.INVALID_RANGE, "startAt/endAt은 필수입니다", 400);
        }
        if (!endAt.isAfter(startAt)) {
            throw new MyScheduleException(MyScheduleErrorCodes.INVALID_RANGE, "endAt은 startAt보다 이후여야 합니다", 400);
        }
    }

    private MyScheduleResponse toResponse(MySchedule s) {
        return MyScheduleResponse.builder()
                .id(s.getId())
                .teamId(s.getTeamId())
                .userId(s.getUserId())
                .title(s.getTitle())
                .memo(s.getMemo())
                .startAt(s.getStartAt())
                .endAt(s.getEndAt())
                .isPrivate(s.isPrivate())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    /**
     * 지금 기준 Busy/Idle (단일 규칙)
     * Busy if startAt <= now < endAt
     */
    @Transactional(readOnly = true)
    public MyScheduleNowResponse getNowStatus(Long userId, Long teamId) {
        requireMember(teamId, userId);

        LocalDateTime now = LocalDateTime.now();

        List<MySchedule> overlaps =
                myScheduleRepository.findNowOverlaps(teamId, userId, now);

        boolean isBusy = !overlaps.isEmpty();

        List<MyScheduleNowResponse.ScheduleTimeRange> ranges =
                overlaps.stream()
                        .map(s -> new MyScheduleNowResponse.ScheduleTimeRange(
                                s.getId(),
                                s.getTitle(),
                                s.getStartAt(),
                                s.getEndAt()
                        ))
                        .toList();

        return new MyScheduleNowResponse(isBusy, ranges);
    }

    @Transactional(readOnly = true)
    public List<MyScheduleResponse> getDaySchedules(Long userId, Long teamId, LocalDate date) {
        requireMember(teamId, userId);

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return myScheduleRepository
                .findOverlaps(teamId, userId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}
