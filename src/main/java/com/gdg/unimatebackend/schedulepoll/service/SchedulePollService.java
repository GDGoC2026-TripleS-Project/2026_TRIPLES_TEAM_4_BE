package com.gdg.unimatebackend.schedulepoll.service;

import com.gdg.unimatebackend.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.alarm.service.FcmService;
import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.service.NotificationService;
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
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import com.gdg.unimatebackend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class SchedulePollService {

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollVoteRepository voteRepository;
    private final SchedulePollQueryService queryService;

    // ✅ 추가: 팀 존재/멤버 검증 재사용
    private final TeamService teamService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;
    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final FcmService fcmService;

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
        createMeetingNotifications(userId, saved);

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

    private void createMeetingNotifications(Long creatorUserId, SchedulePoll poll) {
        Team team = teamRepository.findById(poll.getTeamId()).orElse(null);
        if (team == null) {
            log.warn("[SCHEDULE_POLL_NOTI] skip: team not found. pollId={}, teamId={}", poll.getId(), poll.getTeamId());
            return;
        }

        String teamName = team.getName() != null ? team.getName() : "팀";
        String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                ? team.getColor().getHex()
                : "#CCCCCC";

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(poll.getTeamId());
        log.info("[SCHEDULE_POLL_NOTI] start. pollId={}, teamId={}, creatorUserId={}, memberCount={}",
                poll.getId(), poll.getTeamId(), creatorUserId, members.size());
        for (TeamMember member : members) {
            Long receiverId = member.getUserId();
            if (receiverId == null) continue;

            boolean isCreator = receiverId.equals(creatorUserId);
            String type = isCreator ? "MEETING_CREATED" : "MEETING_REQUEST";
            String title = isCreator ? "모임이 생성되었어요!" : "모임 시간 체크요청이 들어왔어요!";
            String body = isCreator
                    ? "체크된 일정을 확인해주세요!"
                    : "해당 팀스페이스로 이동해 모임 시간을 체크해주세요!";

            Notification notification = Notification.builder()
                    .eventKey("SCHEDULE_POLL_CREATE:" + poll.getId() + ":" + receiverId)
                    .type(type)
                    .alarmType("모임시간 체크요청")
                    .teamId(team.getId())
                    .teamName(teamName)
                    .teamColorHex(teamColorHex)
                    .messageTitle(title)
                    .messageBody(body)
                    .senderId(creatorUserId)
                    .receiverId(receiverId)
                    .build();

            Notification saved = notificationService.createNotificationWithReceipt(notification, receiverId);
            log.info("[SCHEDULE_POLL_NOTI] created. pollId={}, notificationId={}, receiverId={}, type={}",
                    poll.getId(), saved.getId(), receiverId, type);

            sendMeetingFcm(saved, receiverId, type, poll.getId());
        }
    }

    private void sendMeetingFcm(Notification notification, Long receiverId, String type, Long pollId) {
        var tokenOpt = fcmDeviceTokenRepository.findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(receiverId);
        if (tokenOpt.isEmpty()) {
            log.info("[SCHEDULE_POLL_NOTI] no token. receiverId={}, notificationId={}", receiverId, notification.getId());
            return;
        }

        LocalDateTime createdAt = notification.getCreatedAt() != null
                ? notification.getCreatedAt()
                : LocalDateTime.now();
        String createdAtText = createdAt.atOffset(ZoneOffset.ofHours(9)).toString();

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", type != null ? type : "");
        data.put("receiverId", String.valueOf(receiverId));
        data.put("teamId", String.valueOf(notification.getTeamId()));
        data.put("pollId", pollId != null ? String.valueOf(pollId) : "");
        data.put("teamName", notification.getTeamName() != null ? notification.getTeamName() : "");
        data.put("teamColorHex", notification.getTeamColorHex() != null ? notification.getTeamColorHex() : "#CCCCCC");
        data.put("alarmType", notification.getAlarmType() != null ? notification.getAlarmType() : "알림");
        data.put("meetingNavigationTarget", "TIMEPICK_STATUS");
        data.put("messageTitle", notification.getMessageTitle() != null ? notification.getMessageTitle() : "");
        data.put("messageBody", notification.getMessageBody() != null ? notification.getMessageBody() : "");
        data.put("createdAt", createdAtText);

        try {
            String pushTitle = notification.getTeamName() != null ? notification.getTeamName() : "팀";
            String pushBody = notification.getMessageTitle() != null ? notification.getMessageTitle() : "모임 알림이 도착했어요.";
            fcmService.sendMessageTo(FcmSendDto.builder()
                    .token(tokenOpt.get().getToken())
                    .title(pushTitle)
                    .body(pushBody)
                    .data(data)
                    .build());
        } catch (Exception e) {
            log.warn("[SCHEDULE_POLL_NOTI] fcm fail. receiverId={}, notificationId={}, reason={}",
                    receiverId, notification.getId(), e.getMessage());
        }
    }
}
