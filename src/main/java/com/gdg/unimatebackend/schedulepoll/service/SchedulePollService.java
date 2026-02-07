package com.gdg.unimatebackend.schedulepoll.service;

import com.gdg.unimatebackend.schedulepoll.dto.SchedulePollCreateRequest;
import com.gdg.unimatebackend.schedulepoll.dto.SchedulePollCreateResponse;
import com.gdg.unimatebackend.schedulepoll.dto.SchedulePollFixRequest;
import com.gdg.unimatebackend.schedulepoll.dto.SchedulePollMetaUpdateRequest;
import com.gdg.unimatebackend.schedulepoll.dto.SchedulePollVoteUpsertRequest;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePoll;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollDate;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollVote;
import com.gdg.unimatebackend.schedulepoll.exception.SchedulePollErrorCodes;
import com.gdg.unimatebackend.schedulepoll.exception.SchedulePollException;
import com.gdg.unimatebackend.schedulepoll.repository.SchedulePollRepository;
import com.gdg.unimatebackend.schedulepoll.repository.SchedulePollVoteRepository;
import com.gdg.unimatebackend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SchedulePollService {

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollVoteRepository voteRepository;
    private final SchedulePollQueryService queryService;

    // ✅ 추가: 팀 존재/멤버 검증 재사용
    private final TeamService teamService;

    @Transactional
    public SchedulePollCreateResponse create(Long userId, SchedulePollCreateRequest req) {
        // ✅ 팀 존재 + 팀 멤버 검증 (teams/team_member가 비어있으면 여기서 막힘)
        if (req == null || req.getTeamId() == null) {
            throw new SchedulePollException(SchedulePollErrorCodes.INVALID_REQUEST);
        }
        teamService.getTeamMembers(userId, req.getTeamId());

        validateCreate(req);

        int slotMinutes = (req.getSlotMinutes() == null ? 30 : req.getSlotMinutes());

        SchedulePoll poll = SchedulePoll.builder()
                .teamId(req.getTeamId())
                .timezone(req.getTimezone())
                .slotMinutes(slotMinutes)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .title(req.getTitle())
                .memo(req.getMemo())
                .alarm(req.getAlarm())
                .build();

        for (var d : req.getDates()) {
            poll.addDate(new SchedulePollDate(d));
        }

        SchedulePoll saved = schedulePollRepository.save(poll);

        return SchedulePollCreateResponse.builder()
                .pollId(saved.getId())
                .build();
    }

    @Transactional
    public void upsertMyVote(Long userId, Long pollId, SchedulePollVoteUpsertRequest req) {
        SchedulePoll poll = getPollOrThrow(pollId);

        // ✅ 팀 멤버 검증 (poll이 속한 teamId 기준)
        teamService.getTeamMembers(userId, poll.getTeamId());

        if (poll.isLocked()) throw new SchedulePollException(SchedulePollErrorCodes.POLL_LOCKED);

        if (req == null) throw new SchedulePollException(SchedulePollErrorCodes.INVALID_REQUEST);
        validateSlots(req.getSlots());

        SchedulePollVote vote = voteRepository.findBySchedulePoll_IdAndVoterId(pollId, userId)
                .orElseGet(() -> voteRepository.save(new SchedulePollVote(poll, userId)));

        vote.replaceSlots(req.getSlots());
        voteRepository.save(vote);

        // 투표 후 자동픽스 재계산
        queryService.recalculateAutoFixIfPossible(userId, pollId);
    }

    @Transactional
    public void fix(Long userId, Long pollId, SchedulePollFixRequest req) {
        SchedulePoll poll = getPollOrThrow(pollId);

        // ✅ 팀 멤버 검증
        teamService.getTeamMembers(userId, poll.getTeamId());

        if (poll.isLocked()) throw new SchedulePollException(SchedulePollErrorCodes.POLL_LOCKED);
        if (req == null || req.getFixedSlotId() == null) {
            throw new SchedulePollException(SchedulePollErrorCodes.INVALID_REQUEST);
        }

        // TODO(선택): 팀장만 가능 정책이면 여기서 권한 체크 추가
        poll.fixManually(req.getFixedSlotId());
        schedulePollRepository.save(poll);
    }

    @Transactional
    public void updateMeta(Long userId, Long pollId, SchedulePollMetaUpdateRequest req) {
        SchedulePoll poll = getPollOrThrow(pollId);

        // ✅ 팀 멤버 검증
        teamService.getTeamMembers(userId, poll.getTeamId());

        if (req == null) throw new SchedulePollException(SchedulePollErrorCodes.INVALID_REQUEST);

        // TODO(선택): 팀장/생성자만 가능 정책이면 권한 체크 추가
        poll.updateMeta(req.getTitle(), req.getMemo(), req.getAlarm());
        schedulePollRepository.save(poll);
    }

    @Transactional
    public void delete(Long userId, Long pollId) {
        SchedulePoll poll = getPollOrThrow(pollId);

        // ✅ 팀 멤버 검증
        teamService.getTeamMembers(userId, poll.getTeamId());

        // TODO(선택): 팀장/생성자만 가능 정책이면 권한 체크 추가
        schedulePollRepository.delete(poll);
    }

    private SchedulePoll getPollOrThrow(Long pollId) {
        if (pollId == null) throw new SchedulePollException(SchedulePollErrorCodes.INVALID_REQUEST);

        return schedulePollRepository.findById(pollId)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCodes.SCHEDULE_POLL_NOT_FOUND));
    }

    private void validateCreate(SchedulePollCreateRequest req) {
        if (req.getDates() == null || req.getDates().isEmpty()
                || req.getStartTime() == null || req.getEndTime() == null
                || req.getTimezone() == null) {
            throw new SchedulePollException(SchedulePollErrorCodes.INVALID_REQUEST);
        }
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new SchedulePollException(SchedulePollErrorCodes.INVALID_DATE_RANGE);
        }
    }

    private void validateSlots(List<Integer> slots) {
        if (slots == null) throw new SchedulePollException(SchedulePollErrorCodes.INVALID_SLOTS);

        for (Integer s : slots) {
            if (s == null || s < 0) throw new SchedulePollException(SchedulePollErrorCodes.INVALID_SLOTS);
        }
    }
}