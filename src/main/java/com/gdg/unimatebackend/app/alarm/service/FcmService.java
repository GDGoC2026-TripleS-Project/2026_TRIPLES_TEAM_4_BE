// ✅ FcmService.java (풀버전)
// - interface에는 @Service 붙이지 말 것!

package com.gdg.unimatebackend.app.alarm.service;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;

import java.io.IOException;

public interface FcmService {

    /**
     * 실제 전송 (토큰 필요)
     */
    String sendMessageTo(FcmSendDto fcmSendDto) throws IOException;

    /**
     * 서버만으로 Access Token 발급 확인용 (토큰 불필요)
     */
    String getAccessTokenForDebug() throws IOException;

    /**
     * 서버만으로 FCM 호출 확인용 (더미 토큰 사용)
     */
    String sendDummyMessageForDebug() throws IOException;
}
