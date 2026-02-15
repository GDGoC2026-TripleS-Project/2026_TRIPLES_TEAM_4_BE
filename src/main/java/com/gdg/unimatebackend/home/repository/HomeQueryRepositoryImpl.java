package com.gdg.unimatebackend.home.repository;

import com.gdg.unimatebackend.home.dto.*;
import com.gdg.unimatebackend.notification.repository.NotificationReceiptRepository;
import com.gdg.unimatebackend.notification.entity.NotificationReceipt;
import com.gdg.unimatebackend.schedule.entity.MySchedule;
import com.gdg.unimatebackend.schedule.repository.MyScheduleRepository;
import com.gdg.unimatebackend.schedule.team.entity.TeamSchedule;
import com.gdg.unimatebackend.schedule.team.repository.TeamScheduleRepository;
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class HomeQueryRepositoryImpl implements HomeQueryRepository {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamScheduleRepository teamScheduleRepository;
    private final MyScheduleRepository myScheduleRepository;
    private final NotificationReceiptRepository notificationReceiptRepository;

    @Override
    public HomeSummaryResponse fetchHomeSummary(
            Long userId,
            LocalDate date,
            List<Long> teamIds,
            boolean includeMyPersonal
    ) {

        LocalDate baseDate = (date == null) ? LocalDate.now() : date;

        LocalDate weekStart =
                baseDate.minusDays(baseDate.getDayOfWeek().getValue() % 7);

        LocalDate weekEnd = weekStart.plusDays(6);

        List<Long> resolvedTeamIds =
                resolveTeamIds(userId, teamIds);

        List<WeeklyCalendarDayDto> weeklyCalendar =
                buildWeeklyCalendarCounts(
                        userId,
                        weekStart,
                        weekEnd,
                        resolvedTeamIds,
                        includeMyPersonal
                );

        List<TeamTodaySchedulesDto> teamTodaySchedules =
                fetchTeamTodaySchedules(baseDate, resolvedTeamIds);

        List<PersonalScheduleItemDto> personalToday =
                includeMyPersonal
                        ? fetchMyPersonalTodaySchedules(
                        userId,
                        baseDate,
                        resolvedTeamIds
                )
                        : Collections.emptyList();

        TodaySchedulesDto todaySchedules =
                TodaySchedulesDto.builder()
                        .teamSchedules(teamTodaySchedules)
                        .personalSchedules(personalToday)
                        .build();

        List<MyTeamSpaceDto> myTeamSpaces =
                fetchMyTeamSpaces(resolvedTeamIds);

// 기존 repository 메서드 사용해서 unread 존재 여부 계산
        List<NotificationReceipt> receipts =
                notificationReceiptRepository.findAllByUserIdWithNotification(userId);

        boolean hasUnread =
                receipts.stream()
                        .anyMatch(r -> !r.isRead());

        NotificationBadgeDto notification =
                NotificationBadgeDto.builder()
                        .hasUnread(hasUnread)
                        .unreadCount(0)
                        .build();

        return HomeSummaryResponse.builder()
                .date(baseDate)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .weeklyCalendar(weeklyCalendar)
                .todaySchedules(todaySchedules)
                .myTeamSpaces(myTeamSpaces)
                .notification(notification)
                .build();
    }

    /**
     * teamIds가 없으면 내가 속한 팀 목록 조회
     */
    private List<Long> resolveTeamIds(
            Long userId,
            List<Long> teamIds
    ) {

        List<Long> myTeamIds =
                teamMemberRepository
                        .findAllByUserIdOrderByJoinedAtDesc(userId)
                        .stream()
                        .map(TeamMember::getTeamId)
                        .distinct()
                        .collect(Collectors.toList());

        if (teamIds == null || teamIds.isEmpty()) {
            return myTeamIds;
        }

        // 교집합만 허용 (보안 강화)
        return teamIds.stream()
                .filter(myTeamIds::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 주간 캘린더 날짜별 count 계산
     */
    private List<WeeklyCalendarDayDto> buildWeeklyCalendarCounts(
            Long userId,
            LocalDate weekStart,
            LocalDate weekEnd,
            List<Long> teamIds,
            boolean includeMyPersonal
    ) {

        List<LocalDate> dates = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            dates.add(weekStart.plusDays(i));
        }

        Map<LocalDate, Integer> countMap = new HashMap<>();

        for (LocalDate d : dates) {
            countMap.put(d, 0);
        }

        if (!teamIds.isEmpty()) {

            LocalDateTime rangeStart =
                    weekStart.atStartOfDay();

            LocalDateTime rangeEnd =
                    weekEnd.plusDays(1).atStartOfDay();

            for (Long teamId : teamIds) {

                // 팀 일정 count
                List<TeamSchedule> teamSchedules =
                        teamScheduleRepository.findOverlaps(
                                teamId,
                                rangeStart,
                                rangeEnd
                        );

                for (TeamSchedule s : teamSchedules) {

                    LocalDate start =
                            s.getStartAt().toLocalDate();

                    LocalDate end =
                            s.getEndAt().toLocalDate();

                    LocalDate clampedStart =
                            start.isBefore(weekStart)
                                    ? weekStart
                                    : start;

                    LocalDate clampedEnd =
                            end.isAfter(weekEnd)
                                    ? weekEnd
                                    : end;

                    if (clampedEnd.isBefore(clampedStart))
                        continue;

                    LocalDate cur = clampedStart;

                    while (!cur.isAfter(clampedEnd)) {

                        countMap.put(
                                cur,
                                countMap.get(cur) + 1
                        );

                        cur = cur.plusDays(1);
                    }
                }

                // 개인 일정 count
                if (includeMyPersonal) {

                    List<MySchedule> mySchedules =
                            myScheduleRepository.findOverlaps(
                                    teamId,
                                    userId,
                                    rangeStart,
                                    rangeEnd
                            );

                    for (MySchedule s : mySchedules) {

                        LocalDate start =
                                s.getStartAt().toLocalDate();

                        LocalDate end =
                                s.getEndAt().toLocalDate();

                        LocalDate clampedStart =
                                start.isBefore(weekStart)
                                        ? weekStart
                                        : start;

                        LocalDate clampedEnd =
                                end.isAfter(weekEnd)
                                        ? weekEnd
                                        : end;

                        if (clampedEnd.isBefore(clampedStart))
                            continue;

                        LocalDate cur = clampedStart;

                        while (!cur.isAfter(clampedEnd)) {

                            countMap.put(
                                    cur,
                                    countMap.get(cur) + 1
                            );

                            cur = cur.plusDays(1);
                        }
                    }
                }
            }
        }

        LocalDate today = LocalDate.now();

        return dates.stream()
                .map(d ->
                        WeeklyCalendarDayDto.builder()
                                .date(d)
                                .isToday(d.equals(today))
                                .scheduleCount(countMap.get(d))
                                .build()
                )
                .collect(Collectors.toList());
    }

    private List<TeamTodaySchedulesDto> fetchTeamTodaySchedules(
            LocalDate date,
            List<Long> teamIds
    ) {

        if (teamIds.isEmpty())
            return Collections.emptyList();

        LocalDateTime rangeStart =
                date.atStartOfDay();

        LocalDateTime rangeEnd =
                date.plusDays(1).atStartOfDay();

        Map<Long, Team> teamMap =
                teamRepository.findAllById(teamIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Team::getId,
                                t -> t
                        ));

        List<TeamTodaySchedulesDto> result =
                new ArrayList<>();

        for (Long teamId : teamIds) {

            Team team = teamMap.get(teamId);

            if (team == null)
                continue;

            List<TeamSchedule> schedules =
                    teamScheduleRepository.findOverlaps(
                            teamId,
                            rangeStart,
                            rangeEnd
                    );

            if (schedules.isEmpty())
                continue;

            List<ScheduleItemDto> items =
                    schedules.stream()
                            .sorted(Comparator.comparing(
                                    TeamSchedule::getStartAt
                            ))
                            .map(s ->
                                    ScheduleItemDto.builder()
                                            .scheduleId(s.getId())
                                            .title(s.getTitle())
                                            .startAt(s.getStartAt())
                                            .endAt(s.getEndAt())
                                            .build()
                            )
                            .collect(Collectors.toList());

            result.add(
                    TeamTodaySchedulesDto.builder()
                            .teamId(teamId)
                            .teamName(team.getName())
                            .teamColor(team.getColor().getHex())
                            .schedules(items)
                            .build()
            );
        }

        result.sort(
                Comparator.comparing(
                        TeamTodaySchedulesDto::getTeamName,
                        Comparator.nullsLast(String::compareTo)
                )
        );

        return result;
    }

    private List<PersonalScheduleItemDto> fetchMyPersonalTodaySchedules(
            Long userId,
            LocalDate date,
            List<Long> teamIds
    ) {

        if (teamIds.isEmpty())
            return Collections.emptyList();

        LocalDateTime rangeStart =
                date.atStartOfDay();

        LocalDateTime rangeEnd =
                date.plusDays(1).atStartOfDay();

        List<PersonalScheduleItemDto> result =
                new ArrayList<>();

        for (Long teamId : teamIds) {

            List<MySchedule> schedules =
                    myScheduleRepository.findOverlaps(
                            teamId,
                            userId,
                            rangeStart,
                            rangeEnd
                    );

            for (MySchedule s : schedules) {

                result.add(
                        PersonalScheduleItemDto.builder()
                                .teamId(s.getTeamId())
                                .scheduleId(s.getId())
                                .title(s.getTitle())
                                .startAt(s.getStartAt())
                                .endAt(s.getEndAt())
                                .isPrivate(s.isPrivate())
                                .build()
                );
            }
        }

        result.sort(
                Comparator.comparing(
                        PersonalScheduleItemDto::getStartAt,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                )
        );

        return result;
    }

    private List<MyTeamSpaceDto> fetchMyTeamSpaces(
            List<Long> teamIds
    ) {

        if (teamIds.isEmpty())
            return Collections.emptyList();

        return teamRepository.findAllById(teamIds)
                .stream()
                .map(t ->
                        MyTeamSpaceDto.builder()
                                .teamId(t.getId())
                                .teamName(t.getName())
                                .teamColor(t.getColor().getHex())
                                .build()
                )
                .sorted(
                        Comparator.comparing(
                                MyTeamSpaceDto::getTeamName,
                                Comparator.nullsLast(String::compareTo)
                        )
                )
                .collect(Collectors.toList());
    }
}