package com.gdg.unimatebackend.schedule.team.service;

import com.gdg.unimatebackend.schedule.entity.ScheduleCategory;
import com.gdg.unimatebackend.schedule.team.dto.*;
import com.gdg.unimatebackend.schedule.team.entity.TeamSchedule;
import com.gdg.unimatebackend.schedule.team.exception.TeamScheduleErrorCodes;
import com.gdg.unimatebackend.schedule.team.exception.TeamScheduleException;
import com.gdg.unimatebackend.schedule.team.repository.TeamScheduleRepository;
import com.gdg.unimatebackend.schedule.util.ScheduleValidationUtils;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.entity.TeamRole;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamScheduleService {

    private final TeamScheduleRepository teamScheduleRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public TeamScheduleResponse create(Long userId, Long teamId, TeamScheduleCreateRequest request) {
        TeamMember me = requireMember(teamId, userId);
        validateRange(request.getStartAt(), request.getEndAt());
        ScheduleValidationUtils.validateCategoryRequired(
                request.getCategory(),
                request.getCategoryMemo(),
                msg -> new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, msg, 400)
        );
        ScheduleValidationUtils.validateAlarmMinutes(
                request.getAlarmMinutes(),
                msg -> new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, msg, 400)
        );

        String normalizedMemo = ScheduleValidationUtils.normalizeCategoryMemo(
                request.getCategory(),
                request.getCategoryMemo()
        );

        TeamSchedule saved = teamScheduleRepository.save(
                TeamSchedule.builder()
                        .teamId(teamId)
                        .createdBy(userId)
                        .title(request.getTitle())
                        .memo(request.getMemo())
                        .startAt(request.getStartAt())
                        .endAt(request.getEndAt())
                        .category(request.getCategory())
                        .categoryMemo(normalizedMemo)
                        .alarmMinutes(request.getAlarmMinutes())
                        .build()
        );

        return toResponse(saved);
    }

    @Transactional
    public TeamScheduleResponse update(Long userId, Long teamId, Long scheduleId, TeamScheduleUpdateRequest request) {
        TeamMember me = requireMember(teamId, userId);
        validateRange(request.getStartAt(), request.getEndAt());

        TeamSchedule schedule = teamScheduleRepository.findByIdAndTeamId(scheduleId, teamId)
                .orElseThrow(() -> new TeamScheduleException(
                        TeamScheduleErrorCodes.SCHEDULE_NOT_FOUND, "일정을 찾을 수 없습니다", 404
                ));

        requireWriterOrLeader(me, schedule, userId);

        ScheduleCategory newCategory = request.getCategory() != null ? request.getCategory() : schedule.getCategory();
        String newCategoryMemo = request.getCategory() != null ? request.getCategoryMemo() : schedule.getCategoryMemo();

        if (request.getCategory() != null) {
            ScheduleValidationUtils.validateCategoryRequired(
                    request.getCategory(),
                    request.getCategoryMemo(),
                    msg -> new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, msg, 400)
            );
        }
        ScheduleValidationUtils.validateAlarmMinutes(
                request.getAlarmMinutes(),
                msg -> new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, msg, 400)
        );

        String normalizedMemo = ScheduleValidationUtils.normalizeCategoryMemo(newCategory, newCategoryMemo);
        Integer newAlarmMinutes = request.getAlarmMinutes() != null ? request.getAlarmMinutes() : schedule.getAlarmMinutes();

        schedule.update(
                request.getTitle(),
                request.getMemo(),
                request.getStartAt(),
                request.getEndAt(),
                newCategory,
                normalizedMemo,
                newAlarmMinutes
        );

        return toResponse(schedule);
    }

    @Transactional
    public void delete(Long userId, Long teamId, Long scheduleId) {
        TeamMember me = requireMember(teamId, userId);

        TeamSchedule schedule = teamScheduleRepository.findByIdAndTeamId(scheduleId, teamId)
                .orElseThrow(() -> new TeamScheduleException(
                        TeamScheduleErrorCodes.SCHEDULE_NOT_FOUND, "일정을 찾을 수 없습니다", 404
                ));

        requireWriterOrLeader(me, schedule, userId);

        teamScheduleRepository.delete(schedule);
    }

    @Transactional(readOnly = true)
    public java.util.List<TeamScheduleResponse> getByRange(Long userId, Long teamId, LocalDate from, LocalDate to) {
        requireMember(teamId, userId);

        if (from == null || to == null) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, "from/to는 필수입니다", 400);
        }
        if (to.isBefore(from)) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, "to는 from보다 빠를 수 없습니다", 400);
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay();

        return teamScheduleRepository.findOverlaps(teamId, rangeStart, rangeEnd)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<TeamScheduleResponse> getByDay(Long userId, Long teamId, LocalDate date) {
        requireMember(teamId, userId);

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return teamScheduleRepository.findOverlaps(teamId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ===== helpers =====

    private TeamMember requireMember(Long teamId, Long userId) {
        Optional<TeamMember> me = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        if (me.isEmpty()) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.NOT_A_MEMBER, "팀 멤버만 접근할 수 있습니다", 403);
        }
        return me.get();
    }

    private void requireWriterOrLeader(TeamMember me, TeamSchedule schedule, Long userId) {
        boolean isWriter = Objects.equals(schedule.getCreatedBy(), userId);
        boolean isLeader = me.getRole() == TeamRole.LEADER;

        if (!isWriter && !isLeader) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.FORBIDDEN, "작성자 또는 팀장만 수정/삭제할 수 있습니다", 403);
        }
    }

    private void validateRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, "startAt/endAt은 필수입니다", 400);
        }
        if (!endAt.isAfter(startAt)) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, "endAt은 startAt보다 이후여야 합니다", 400);
        }
    }

    private TeamScheduleResponse toResponse(TeamSchedule s) {
        return TeamScheduleResponse.builder()
                .id(s.getId())
                .teamId(s.getTeamId())
                .createdBy(s.getCreatedBy())
                .title(s.getTitle())
                .memo(s.getMemo())
                .startAt(s.getStartAt())
                .endAt(s.getEndAt())
                .category(s.getCategory())
                .categoryMemo(s.getCategoryMemo())
                .alarmMinutes(s.getAlarmMinutes())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
