package com.gdg.unimatebackend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "알림 목록 아이템")
public class NotificationItemResponse {
    @Schema(description = "알림 ID", example = "301")
    private Long id;
    @Schema(description = "알림 유형", example = "MEETING_REQUEST")
    private String type;
    @Schema(description = "클라이언트용 알람 타입", example = "meeting_request")
    private String alarmType;
    @Schema(description = "연관 팀 ID", example = "12")
    private Long teamId;
    @Schema(description = "팀 이름", example = "체리시")
    private String teamName;
    @Schema(description = "팀 컬러 HEX", example = "#F488D4")
    private String teamColorHex;
    @Schema(description = "알림 제목", example = "모임 시간 체크요청이 들어왔어요!")
    private String messageTitle;
    @Schema(description = "알림 본문", example = "해당 팀스페이스로 이동해 모임 시간을 체크해주세요!")
    private String messageBody;
    @Schema(description = "알림 생성 시각")
    private LocalDateTime createdAt;
    @Schema(description = "발신자 유저 ID", example = "100")
    private Long senderId;
    @Schema(description = "수신자 유저 ID", example = "101")
    private Long receiverId;
    @Schema(description = "연관 팀 일정 ID")
    private Long teamScheduleId;
    @Schema(description = "연관 찌르기 ID")
    private Long pokeId;
    @Schema(description = "연관 모임 투표 ID")
    private Long meetingPollId;
    @Schema(description = "모임 알림 이동 대상 화면 키", example = "TIMEPICK_STATUS")
    private String meetingNavigationTarget;

    @Schema(description = "읽음 여부", example = "false")
    private boolean isRead;
    @Schema(description = "버튼 액션 필요 여부", example = "true")
    private boolean action;
    @Schema(description = "액션 완료 여부", example = "false")
    private boolean actionDone;
    @Schema(description = "읽음 처리 시각")
    private LocalDateTime processedAt;
}
