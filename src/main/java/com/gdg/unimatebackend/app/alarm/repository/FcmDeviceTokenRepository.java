package com.gdg.unimatebackend.app.alarm.repository;

import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, Long> {

    Optional<FcmDeviceToken> findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(Long userId);

    List<FcmDeviceToken> findAllByUserId(Long userId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO fcm_device_token
          (created_at, updated_at, token, user_id, device_id, platform, is_active)
        VALUES
          (NOW(6), NOW(6), :token, :userId, :deviceId, :platform, b'1')
        ON DUPLICATE KEY UPDATE
          token = VALUES(token),
          is_active = b'1',
          updated_at = NOW(6)
        """, nativeQuery = true)
    int upsertByDevice(
            @Param("userId") Long userId,
            @Param("token") String token,
            @Param("deviceId") String deviceId,
            @Param("platform") String platform
    );

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE fcm_device_token
           SET is_active = b'0',
               updated_at = NOW(6)
         WHERE user_id = :userId
        """, nativeQuery = true)
    int deactivateAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
