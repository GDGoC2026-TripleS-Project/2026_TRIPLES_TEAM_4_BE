package com.gdg.unimatebackend.schedulepoll.dto;

import com.gdg.unimatebackend.schedulepoll.entity.PollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePollDetailResponse {

    private Long pollId;
    private Long teamId;
    private String timezone;
    private Integer slotMinutes;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<LocalDate> dates;

    private String title;
    private String memo;
    private String alarm;

    private PollStatus status;
    private boolean locked;

    private Integer autoFixedSlotId;
    private Integer fixedSlotId;

    private long totalCount;
    private long votedCount;

    private List<MemberDto> members;
    private List<MemberVoteDto> votesByMember;

    private List<Integer> intersectionSlots;
    private List<Integer> mySlots;

    private List<SlotDefinitionResponse> slotDefinitions;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private Long memberId;   // userId
        private String name;     // nickname
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberVoteDto {
        private Long memberId;         // userId
        private List<Integer> slots;
    }
}