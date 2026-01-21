package com.gdg.unimatebackend.app.alarm.repository;

import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, Long> {

    Optional<FcmDeviceToken> findByToken(String token);

    Optional<FcmDeviceToken> findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(Long userId);

    void deleteByUserId(Long userId);

    Optional<FcmDeviceToken> findByUserId(Long userId);
}
