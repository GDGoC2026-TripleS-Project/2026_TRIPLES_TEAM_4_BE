package com.gdg.unimatebackend.app.poke.service;

import com.gdg.unimatebackend.app.poke.dto.PokeMessageResponse;
import com.gdg.unimatebackend.app.poke.dto.PokeRequest;
import com.gdg.unimatebackend.app.poke.dto.PokeResponse;
import com.gdg.unimatebackend.app.poke.dto.PokeTargetsResponse;
import com.gdg.unimatebackend.app.poke.entity.Poke;
import com.gdg.unimatebackend.app.poke.entity.PokeMessage;
import com.gdg.unimatebackend.app.poke.repository.PokeMessageRepository;
import com.gdg.unimatebackend.app.poke.repository.PokeQueryRepository;
import com.gdg.unimatebackend.app.poke.repository.PokeRepository;
import com.gdg.unimatebackend.app.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gdg.unimatebackend.app.poke.event.PokeSentEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PokeService {
    private final PokeRepository pokeRepository;
    private final PokeMessageRepository pokeMessageRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PokeQueryRepository pokeQueryRepository;
    private final ApplicationEventPublisher eventPublisher;

    // =========================
    // 찌르기 대상 조회 (/targets)
    // =========================
    @Transactional(readOnly = true)
    public PokeTargetsResponse getPokeTargets(Long me) {

        List<Object[]> rows = pokeQueryRepository.findTargetRows(me);

        Map<Long, String> teamNameMap = new LinkedHashMap<>();
        Map<Long, List<PokeTargetsResponse.Member>> memberMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Long teamId = ((Number) row[0]).longValue();
            String teamName = (String) row[1];

            Long userId = ((Number) row[2]).longValue();
            String nickname = (String) row[3];
            String profileImageUrl = (String) row[4];

            teamNameMap.putIfAbsent(teamId, teamName);

            memberMap
                    .computeIfAbsent(teamId, k -> new ArrayList<>())
                    .add(PokeTargetsResponse.Member.builder()
                            .userId(userId)
                            .nickname(nickname)
                            .profileImageUrl(profileImageUrl)
                            .build());
        }

        List<PokeTargetsResponse.TeamSection> teams = new ArrayList<>();
        for (Long teamId : teamNameMap.keySet()) {
            teams.add(PokeTargetsResponse.TeamSection.builder()
                    .teamId(teamId)
                    .teamName(teamNameMap.get(teamId))
                    .members(memberMap.getOrDefault(teamId, List.of()))
                    .build());
        }

        return PokeTargetsResponse.builder()
                .teams(teams)
                .build();
    }

    // =========================
    // 찌르기 전송 (다건)
    // =========================
    @Transactional
    public PokeResponse sendPokes(Long senderId, PokeRequest request) {

        if (request.getMessageId() == null) {
            throw new IllegalArgumentException("messageId는 필수입니다.");
        }
        if (request.getTargets() == null || request.getTargets().isEmpty()) {
            throw new IllegalArgumentException("targets는 비어 있을 수 없습니다.");
        }
        if (request.getTargets().size() > 50) {
            throw new IllegalArgumentException("targets는 최대 50명까지 가능합니다.");
        }

        // ---- 1) 찌르기 문구 조회 ----
        PokeMessage pokeMessage = pokeMessageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 찌르기 문구입니다."));

        // ---- 2) targets 정리 (null 제거 + dedup + 본인 제외) ----
        int excludedSelfCount = 0;
        Map<String, PokeRequest.Target> uniqueTargets = new LinkedHashMap<>();

        for (PokeRequest.Target t : request.getTargets()) {
            if (t == null || t.getTeamId() == null || t.getUserId() == null) continue;

            if (senderId.equals(t.getUserId())) {
                excludedSelfCount++;
                continue; // 본인 제외(추천)
            }

            String key = t.getTeamId() + ":" + t.getUserId();
            uniqueTargets.putIfAbsent(key, t);
        }

        if (uniqueTargets.isEmpty()) {
            return PokeResponse.builder()
                    .sentCount(0)
                    .excludedSelfCount(excludedSelfCount)
                    .invalidTargets(List.of())
                    .build();
        }

        // ---- 3) 검증 + poke 생성 ----
        // sender가 팀에 속하는지: teamId별 캐싱(쿼리 절약)
        Map<Long, Boolean> senderInTeamCache = new HashMap<>();

        List<Poke> pokesToSave = new ArrayList<>();
        List<PokeResponse.InvalidTarget> invalidTargets = new ArrayList<>();

        for (PokeRequest.Target t : uniqueTargets.values()) {

            Long teamId = t.getTeamId();
            Long targetUserId = t.getUserId();

            // (1) sender가 해당 팀에 속하는지(캐싱)
            boolean senderInTeam = senderInTeamCache.computeIfAbsent(
                    teamId,
                    id -> teamMemberRepository.existsByTeamIdAndUserId(id, senderId)
            );

            if (!senderInTeam) {
                invalidTargets.add(PokeResponse.InvalidTarget.builder()
                        .teamId(teamId)
                        .userId(targetUserId)
                        .reason("NOT_IN_MY_TEAM")
                        .build());
                continue;
            }

            // (2) target이 해당 팀 멤버인지
            boolean targetInTeam = teamMemberRepository.existsByTeamIdAndUserId(teamId, targetUserId);

            if (!targetInTeam) {
                invalidTargets.add(PokeResponse.InvalidTarget.builder()
                        .teamId(teamId)
                        .userId(targetUserId)
                        .reason("NOT_TEAM_MEMBER")
                        .build());
                continue;
            }

            // (3) 유효 → Poke 생성
            pokesToSave.add(Poke.builder()
                    .teamId(teamId)
                    .senderId(senderId)
                    .targetUserId(targetUserId)
                    .pokeMessage(pokeMessage)
                    .build());
        }

        if (!pokesToSave.isEmpty()) {
            pokeRepository.saveAll(pokesToSave);

            // 커밋 이후 FCM 발송 트리거
            var targetUserIds = pokesToSave.stream()
                    .map(Poke::getTargetUserId)
                    .distinct()
                    .toList();

            eventPublisher.publishEvent(new PokeSentEvent(
                    senderId,
                    pokeMessage.getId(),
                    targetUserIds
            ));
        }

        log.info("[POKE] sender={}, sent={}, invalid={}, excludedSelf={}",
                senderId, pokesToSave.size(), invalidTargets.size(), excludedSelfCount);

        return PokeResponse.builder()
                .sentCount(pokesToSave.size())
                .excludedSelfCount(excludedSelfCount)
                .invalidTargets(invalidTargets)
                .build();
    }

    // =========================
    // 찌르기 문구 조회
    // =========================
    @Transactional(readOnly = true)
    public List<PokeMessageResponse> getPokeMessages() {
        return pokeMessageRepository.findAllByOrderByIdAsc().stream()
                .map(PokeMessageResponse::from)
                .toList();
    }
}
