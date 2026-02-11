package com.gdg.unimatebackend.calendar.service;

import com.gdg.unimatebackend.calendar.dto.*;
import com.gdg.unimatebackend.calendar.exception.CalendarErrorCodes;
import com.gdg.unimatebackend.calendar.exception.CalendarException;
import com.gdg.unimatebackend.calendar.repository.PersonalScheduleRepository;
import com.gdg.unimatebackend.schedule.dto.MyScheduleResponse;
import com.gdg.unimatebackend.schedule.entity.MySchedule;
import com.gdg.unimatebackend.schedule.service.MyScheduleService;
import com.gdg.unimatebackend.schedule.team.dto.TeamScheduleResponse;
import com.gdg.unimatebackend.schedule.team.service.TeamScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarTeamPort calendarTeamPort;
    private final TeamScheduleService teamScheduleService;
    private final MyScheduleService myScheduleService; // day 조회에서 내 일정 가져올 때 사용
    private final PersonalScheduleRepository personalScheduleRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CalendarMonthResponse getMonth(Long userId, CalendarMonthRequest request) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(request.getMonth(), MONTH_FMT);
        } catch (Exception e) {
            throw new CalendarException(CalendarErrorCodes.INVALID_MONTH_FORMAT);
        }

        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();

        LocalDateTime from = first.atStartOfDay();
        LocalDateTime to = last.plusDays(1).atStartOfDay(); // [from, to)

        List<Long> teamIds = resolveTeamIds(userId, request.getTeamIds());
        if (teamIds == null || teamIds.isEmpty()) {
            throw new CalendarException(CalendarErrorCodes.NO_ACCESSIBLE_TEAM);
        }

        // 권한/유효 팀 체크 + 팀명 매핑
        Map<Long, String> teamNames = calendarTeamPort.getTeamNames(userId, teamIds);
        if (teamNames.size() != teamIds.size()) {
            throw new CalendarException(CalendarErrorCodes.FORBIDDEN_TEAM_ACCESS);
        }

        Map<LocalDate, Integer> countMap = new HashMap<>();

        // 1) 팀 일정 카운트 (기간)
        for (Long teamId : teamIds) {
            List<TeamScheduleResponse> teamSchedules =
                    teamScheduleService.getByRange(userId, teamId, first, last);

            if (teamSchedules == null || teamSchedules.isEmpty()) continue;

            for (TeamScheduleResponse s : teamSchedules) {
                addCountsByOverlap(countMap, s.getStartAt(), s.getEndAt(), first, last);
            }
        }

        // 2) 내 개인 일정 카운트 (includeMyPersonal=true) ✅ 최적화: DB 1번
        if (request.isIncludeMyPersonal()) {
            List<MySchedule> mySchedules =
                    personalScheduleRepository.findMyOverlappingByTeamIds(userId, teamIds, from, to);

            for (MySchedule s : mySchedules) {
                addCountsByOverlap(countMap, s.getStartAt(), s.getEndAt(), first, last);
            }
        }

        // 3) 팀원 개인 일정 카운트 ✅ teamIds 필터 포함
        Set<Long> memberUserIds = calendarTeamPort.getTeamMemberUserIds(userId, teamIds);
        memberUserIds.remove(userId);

        if (!memberUserIds.isEmpty()) {
            List<MySchedule> memberSchedules =
                    personalScheduleRepository.findAllOverlappingByUserIdsAndTeamIds(memberUserIds, teamIds, from, to);

            for (MySchedule s : memberSchedules) {
                // 비공개여도 블록 1개로 카운트 포함
                addCountsByOverlap(countMap, s.getStartAt(), s.getEndAt(), first, last);
            }
        }

        List<CalendarDayCountResponse> dayCounts = countMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> CalendarDayCountResponse.builder()
                        .date(e.getKey().format(DATE_FMT))
                        .count(e.getValue())
                        .build())
                .toList();

        return CalendarMonthResponse.builder()
                .month(request.getMonth())
                .dayCounts(dayCounts)
                .build();
    }

    public CalendarDayResponse getDay(Long userId, CalendarDayRequest request) {
        LocalDate date;
        try {
            date = LocalDate.parse(request.getDate(), DATE_FMT);
        } catch (Exception e) {
            throw new CalendarException(CalendarErrorCodes.INVALID_DATE_FORMAT);
        }

        List<Long> teamIds = resolveTeamIds(userId, request.getTeamIds());
        if (teamIds == null || teamIds.isEmpty()) {
            throw new CalendarException(CalendarErrorCodes.NO_ACCESSIBLE_TEAM);
        }

        Map<Long, String> teamNames = calendarTeamPort.getTeamNames(userId, teamIds);
        if (teamNames.size() != teamIds.size()) {
            throw new CalendarException(CalendarErrorCodes.FORBIDDEN_TEAM_ACCESS);
        }

        // 1) 팀 일정(일별): 팀별 그룹핑
        List<TeamScheduleGroupResponse> teamGroups = new ArrayList<>();

        for (Long teamId : teamIds) {
            List<TeamScheduleResponse> teamSchedules =
                    teamScheduleService.getByDay(userId, teamId, date);

            if (teamSchedules == null || teamSchedules.isEmpty()) continue;

            List<CalendarItemResponse> items = new ArrayList<>();
            for (TeamScheduleResponse s : teamSchedules) {
                items.add(CalendarItemResponse.builder()
                        .scheduleId(s.getId())
                        .type("TEAM")
                        .teamId(teamId)
                        .teamName(teamNames.get(teamId))
                        .title(s.getTitle())
                        .startAt(toStringSafe(s.getStartAt()))
                        .endAt(toStringSafe(s.getEndAt()))
                        .isCompleted(null) // TeamScheduleResponse에 완료 필드 없음
                        .isMasked(false)
                        .build());
            }

            teamGroups.add(TeamScheduleGroupResponse.builder()
                    .teamId(teamId)
                    .teamName(teamNames.get(teamId))
                    .schedules(items)
                    .build());
        }

        // 2) 개인 일정 리스트 (내 것 옵션 + 타인 마스킹)
        List<CalendarItemResponse> personalItems = new ArrayList<>();

        // 2-1) 내 개인 일정 (day에서는 기존 서비스 그대로 사용해도 충분히 빠름)
        if (request.isIncludeMyPersonal()) {
            for (Long teamId : teamIds) {
                List<MyScheduleResponse> myDaySchedules =
                        myScheduleService.getDaySchedules(userId, teamId, date);

                if (myDaySchedules == null || myDaySchedules.isEmpty()) continue;

                for (MyScheduleResponse s : myDaySchedules) {
                    personalItems.add(CalendarItemResponse.builder()
                            .scheduleId(s.getId())
                            .type("PERSONAL")
                            .title(s.getTitle())
                            .startAt(toStringSafe(s.getStartAt()))
                            .endAt(toStringSafe(s.getEndAt()))
                            .isCompleted(null)
                            .isMasked(false)
                            .build());
                }
            }
        }

        // 2-2) 타인 개인 일정(DB) ✅ teamIds 필터 포함
        Set<Long> memberUserIds = calendarTeamPort.getTeamMemberUserIds(userId, teamIds);
        memberUserIds.remove(userId);

        if (!memberUserIds.isEmpty()) {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            List<MySchedule> memberSchedules =
                    personalScheduleRepository.findAllOverlappingByUserIdsAndTeamIds(memberUserIds, teamIds, startOfDay, endOfDay);

            for (MySchedule s : memberSchedules) {
                boolean masked = s.isPrivate();

                personalItems.add(CalendarItemResponse.builder()
                        .scheduleId(s.getId())
                        .type("PERSONAL")
                        .title(masked ? null : s.getTitle())
                        .startAt(toStringSafe(s.getStartAt()))
                        .endAt(toStringSafe(s.getEndAt()))
                        .isCompleted(null)
                        .isMasked(masked)
                        .build());
            }
        }

        // 보기 좋게 정렬(시작 시간 순) - ISO string이라 문자열 정렬도 시간 정렬과 동일
        personalItems.sort(Comparator.comparing(CalendarItemResponse::getStartAt, Comparator.nullsLast(String::compareTo)));

        return CalendarDayResponse.builder()
                .date(request.getDate())
                .teamSchedules(teamGroups)
                .personalSchedules(personalItems)
                .build();
    }

    private List<Long> resolveTeamIds(Long userId, List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return calendarTeamPort.getMyTeamIds(userId);
        }
        return teamIds;
    }

    /**
     * 일정이 여러 날에 걸치면, 겹치는 모든 날짜에 +1
     * overlap 기준: startAt < dayEnd AND endAt > dayStart
     *
     * 하루종일: 00:00 ~ 다음날 00:00
     * endAt이 00:00이면 해당 날짜는 미포함 보정
     */
    private void addCountsByOverlap(
            Map<LocalDate, Integer> countMap,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDate monthStart,
            LocalDate monthEnd
    ) {
        if (startAt == null || endAt == null) return;

        LocalDateTime rangeStart = monthStart.atStartOfDay();
        LocalDateTime rangeEnd = monthEnd.plusDays(1).atStartOfDay();

        // 월 범위와 겹치지 않으면 컷
        if (!(startAt.isBefore(rangeEnd) && endAt.isAfter(rangeStart))) return;

        LocalDate startDate = startAt.toLocalDate();
        LocalDate endDate = endAt.toLocalDate();

        // endAt이 00:00이면 해당 날짜는 미포함 보정
        if (endAt.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            endDate = endDate.minusDays(1);
        }

        if (startDate.isBefore(monthStart)) startDate = monthStart;
        if (endDate.isAfter(monthEnd)) endDate = monthEnd;

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();

            if (startAt.isBefore(dayEnd) && endAt.isAfter(dayStart)) {
                countMap.merge(d, 1, Integer::sum);
            }
        }
    }

    private static String toStringSafe(LocalDateTime dt) {
        return dt == null ? null : dt.toString();
    }
}