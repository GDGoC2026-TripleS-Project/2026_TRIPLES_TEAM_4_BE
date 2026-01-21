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

    Optional<FcmDeviceToken> findByToken(String token);

    // ✅ user_id가 unique가 아니므로 "Top + Order"로 안전하게 가져오기
    Optional<FcmDeviceToken> findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(Long userId);

    // ✅ 필요하면 전체도 가져올 수 있게
    List<FcmDeviceToken> findAllByUserId(Long userId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO fcm_device_token
          (created_at, updated_at, token, user_id, device_id, platform, is_active)
        VALUES
          (NOW(6), NOW(6), :token, :userId, :deviceId, :platform, b'1')
        ON DUPLICATE KEY UPDATE
          user_id = VALUES(user_id),
          device_id = VALUES(device_id),
          platform = VALUES(platform),
          is_active = b'1',
          updated_at = NOW(6)
        """, nativeQuery = true)
    int upsertByToken(
            @Param("userId") Long userId,
            @Param("token") String token,
            @Param("deviceId") String deviceId,
            @Param("platform") String platform
    );

    // ✅ "유저당 1개" 정책을 유지하려면, 등록 전에 기존 토큰을 비활성화하는 게 깔끔함
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
