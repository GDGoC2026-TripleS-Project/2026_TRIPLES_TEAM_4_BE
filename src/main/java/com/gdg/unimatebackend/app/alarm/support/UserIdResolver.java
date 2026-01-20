package com.gdg.unimatebackend.app.alarm.support;

import org.springframework.stereotype.Component;

@Component
public class UserIdResolver {

    /**
     * TODO: JWT 연동되면 여기만 교체하면 됨.
     * 지금은 테스트 편하게 "X-USER-ID"로 받는 방식 사용.
     */
    public Long resolveOrThrow(String headerUserId) {
        if (headerUserId == null || headerUserId.isBlank()) {
            throw new IllegalArgumentException("X-USER-ID header is required for now.");
        }
        return Long.parseLong(headerUserId);
    }
}
