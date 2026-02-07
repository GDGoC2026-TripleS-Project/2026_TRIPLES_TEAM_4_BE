package com.gdg.unimatebackend.schedulepoll.service;

import com.gdg.unimatebackend.schedulepoll.dto.SchedulePollDetailResponse;
import com.gdg.unimatebackend.schedulepoll.dto.SlotDefinitionResponse;
import com.gdg.unimatebackend.schedulepoll.dto.TeamFixedSchedulePollSummaryResponse;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePoll;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollDate;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollVote;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollVoteSlot;
import com.gdg.unimatebackend.schedulepoll.exception.SchedulePollErrorCodes;
import com.gdg.unimatebackend.schedulepoll.exception.SchedulePollException;
import com.gdg.unimatebackend.schedulepoll.repository.SchedulePollRepository;
import com.gdg.unimatebackend.schedulepoll.repository.SchedulePollVoteRepository;
import com.gdg.unimatebackend.team.dto.TeamMemberResponse;
import com.gdg.unimatebackend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class SchedulePollQueryService {

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollVoteRepository voteRepository;
    private final SchedulePollAutoFixService autoFixService;

    private final TeamService teamService;

    @Transactional(readOnly = true)
    public SchedulePollDetailResponse getDetail(Long userId, Long pollId) {
        SchedulePoll poll = schedulePollRepository.findById(pollId)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCodes.SCHEDULE_POLL_NOT_FOUND));

        List<TeamMemberResponse> members = teamService.getTeamMembers(userId, poll.getTeamId());
        long totalCount = members.size();

        List<SchedulePollVote> votes = voteRepository.findAllBySchedulePoll_Id(pollId);
        long votedCount = votes.size();

        List<SchedulePollDetailResponse.MemberVoteDto> votesByMember = votes.stream()
                .map(v -> SchedulePollDetailResponse.MemberVoteDto.builder()
                        .memberId(v.getVoterId())
                        .slots(v.getSlots().stream()
                                .map(SchedulePollVoteSlot::getSlotId)
                                .distinct()
                                .sorted()
                                .toList())
                        .build())
                .toList();

        List<Integer> intersection = autoFixService.computeIntersectionSlots(votes, totalCount);

        List<Integer> mySlots = votes.stream()
                .filter(v -> Objects.equals(v.getVoterId(), userId))
                .findFirst()
                .map(v -> v.getSlots().stream()
                        .map(SchedulePollVoteSlot::getSlotId)
                        .distinct()
                        .sorted()
                        .toList())
                .orElse(List.of());

        List<SchedulePollDetailResponse.MemberDto> memberDtos = members.stream()
                .map(m -> SchedulePollDetailResponse.MemberDto.builder()
                        .memberId(m.getUserId())
                        .name(m.getNickname())
                        .build())
                .toList();

        // ✅ dates 정렬 + slotDefinitions 생성
        List<LocalDate> sortedDates = poll.getDates().stream()
                .map(SchedulePollDate::getDate)
                .sorted()
                .toList();

        List<SlotDefinitionResponse> slotDefinitions = buildSlotDefinitions(
                poll.getSlotMinutes(),
                poll.getStartTime(),
                poll.getEndTime(),
                sortedDates
        );

        return SchedulePollDetailResponse.builder()
                .pollId(poll.getId())
                .teamId(poll.getTeamId())
                .timezone(poll.getTimezone())
                .slotMinutes(poll.getSlotMinutes())
                .startTime(poll.getStartTime())
                .endTime(poll.getEndTime())
                .dates(sortedDates)
                .title(poll.getTitle())
                .memo(poll.getMemo())
                .alarm(poll.getAlarm())
                .status(poll.getStatus())
                .locked(poll.isLocked())
                .autoFixedSlotId(poll.getAutoFixedSlotId())
                .fixedSlotId(poll.getFixedSlotId())
                .totalCount(totalCount)
                .votedCount(votedCount)
                .members(memberDtos)
                .votesByMember(votesByMember)
                .intersectionSlots(intersection)
                .mySlots(mySlots)
                .slotDefinitions(slotDefinitions) // ✅ 추가
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeamFixedSchedulePollSummaryResponse> getTeamFixedRange(Long userId, Long teamId, LocalDate from, LocalDate to) {
        teamService.getTeamMembers(userId, teamId);
        return schedulePollRepository.findFixedByTeamIdAndRange(teamId, from, to)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamFixedSchedulePollSummaryResponse> getTeamFixedDay(Long userId, Long teamId, LocalDate date) {
        teamService.getTeamMembers(userId, teamId);
        return schedulePollRepository.findFixedByTeamIdAndDay(teamId, date)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public void recalculateAutoFixIfPossible(Long userId, Long pollId) {
        SchedulePoll poll = schedulePollRepository.findById(pollId)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCodes.SCHEDULE_POLL_NOT_FOUND));

        if (poll.isLocked()) return;

        List<TeamMemberResponse> members = teamService.getTeamMembers(userId, poll.getTeamId());
        long totalCount = members.size();
        long votedCount = voteRepository.countBySchedulePoll_Id(pollId);

        if (totalCount > 0 && votedCount == totalCount) {
            List<SchedulePollVote> votes = voteRepository.findAllBySchedulePoll_Id(pollId);
            List<Integer> intersection = autoFixService.computeIntersectionSlots(votes, totalCount);
            Integer earliest = autoFixService.pickEarliestSlot(intersection);
            if (earliest != null) {
                poll.setAutoFixed(earliest);
                schedulePollRepository.save(poll);
            }
        }
    }

    private TeamFixedSchedulePollSummaryResponse toSummary(SchedulePoll poll) {
        return TeamFixedSchedulePollSummaryResponse.builder()
                .pollId(poll.getId())
                .title(poll.getTitle())
                .status(poll.getStatus())
                .fixedSlotId(poll.getFixedSlotId())
                .dates(poll.getDates().stream().map(SchedulePollDate::getDate).sorted().toList())
                .build();
    }

    /**
     * ✅ slotId ↔ (date, startTime, endTime) 매핑표 생성
     *
     * 규칙:
     * - dates는 정렬된 리스트를 받는다고 가정
     * - 각 날짜마다 startTime~endTime을 slotMinutes 단위로 쪼갠다
     * - slotId는 0부터 순차 증가
     * - endTime을 넘는 마지막 조각은 만들지 않는다
     */
    private List<SlotDefinitionResponse> buildSlotDefinitions(
            Integer slotMinutes,
            LocalTime startTime,
            LocalTime endTime,
            List<LocalDate> dates
    ) {
        if (slotMinutes == null || slotMinutes <= 0 || startTime == null || endTime == null || dates == null) {
            return List.of();
        }

        List<SlotDefinitionResponse> defs = new ArrayList<>();
        int slotId = 0;

        for (LocalDate d : dates) {
            LocalTime t = startTime;
            while (t.isBefore(endTime)) {
                LocalTime next = t.plusMinutes(slotMinutes);
                if (next.isAfter(endTime)) break;

                defs.add(SlotDefinitionResponse.builder()
                        .slotId(slotId)
                        .date(d)
                        .startTime(t)
                        .endTime(next)
                        .build());

                slotId++;
                t = next;
            }
        }

        return defs;
    }
}
