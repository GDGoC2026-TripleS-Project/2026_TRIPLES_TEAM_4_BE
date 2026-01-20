package com.gdg.unimatebackend.app.alarm.repository;

import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, Long> {

    Optional<FcmDeviceToken> findByToken(String token);

    // "내 토큰" 하나를 최신으로 가져오고 싶으면 updatedAt DESC 방식도 가능
    Optional<FcmDeviceToken> findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(Long userId);
}
