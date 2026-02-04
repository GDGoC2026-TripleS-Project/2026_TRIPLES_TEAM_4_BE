package com.gdg.unimatebackend.app.poke.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class PokeAlarmTypeMapper {

    @Getter
    @AllArgsConstructor
    public static class PokeAlarmTemplate {
        private final String alarmType;     // 섹션 헤더용 (예: "자료요청")
        private final String messageTitle;  // 카드/푸시 메인 문구
        private final String messageBody;   // 카드 서브 문구
    }

    /**
     * pokeMessageId -> (alarmType, messageTitle, messageBody)
     * 캡쳐 기준 문구로 하드코딩.
     */
    public static PokeAlarmTemplate fromMessageId(Long pokeMessageId) {
        long id = pokeMessageId == null ? -1L : pokeMessageId;

        return switch ((int) id) {
            case 1 -> new PokeAlarmTemplate(
                    "자료요청",
                    "자료를 기다리고 있는 팀원이 있어요👀",
                    "찔러준 친구에게 확인 콕을 남겨주세요!"
            );
            case 2 -> new PokeAlarmTemplate(
                    "마감요청",
                    "혹시 바쁜 일정에 마감일을 잊으신 건 아니죠? ⏰",
                    "찔러준 친구에게 확인 콕을 남겨주세요!"
            );
            case 3 -> new PokeAlarmTemplate(
                    "답변요청",
                    "팀원이 전한 메시지가 답변을 기다리고 있어요 💌",
                    "찔러준 친구에게 확인 콕을 남겨주세요!"
            );
            case 4 -> new PokeAlarmTemplate(
                    "모이기요청",
                    "지금 바로 회의 가능한 시간을 꼭 찍어주세요? 👉",
                    "찔러준 친구에게 확인 콕을 남겨주세요!"
            );
            case 5 -> new PokeAlarmTemplate(
                    "공지확인요청",
                    "놓치면 안 될 중요한 팀 공지가 도착해요 📣",
                    "찔러준 친구에게 확인 콕을 남겨주세요!"
            );
            default -> new PokeAlarmTemplate(
                    "자료요청",
                    "자료를 기다리고 있는 팀원이 있어요👀",
                    "찔러준 친구에게 확인 콕을 남겨주세요!"
            );
        };
    }
}
