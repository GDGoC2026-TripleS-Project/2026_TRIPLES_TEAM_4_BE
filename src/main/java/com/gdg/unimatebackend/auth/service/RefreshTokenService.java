package com.gdg.unimatebackend.auth.service;

import com.gdg.unimatebackend.auth.entity.RefreshToken;
import com.gdg.unimatebackend.auth.repository.RefreshTokenRepository;
import com.gdg.unimatebackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record RotationResult(User user, String refreshToken) {}

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:1209600000}")
    private long refreshExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issueForUser(User user) {
        revokeAllByUserId(user.getId());
        return createAndSave(user);
    }

    @Transactional
    public RotationResult rotate(String rawRefreshToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 refresh token입니다"));

        if (existing.isRevoked()) {
            throw new IllegalArgumentException("이미 폐기된 refresh token입니다");
        }
        if (existing.isExpired(LocalDateTime.now())) {
            existing.revoke();
            refreshTokenRepository.save(existing);
            throw new IllegalArgumentException("만료된 refresh token입니다");
        }
        if (!existing.getUser().getActive()) {
            existing.revoke();
            refreshTokenRepository.save(existing);
            throw new IllegalArgumentException("탈퇴한 계정입니다");
        }

        existing.revoke();
        refreshTokenRepository.save(existing);
        String newRefreshToken = createAndSave(existing.getUser());
        return new RotationResult(existing.getUser(), newRefreshToken);
    }

    @Transactional
    public void revokeAllByUserId(Long userId) {
        var activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        if (activeTokens.isEmpty()) {
            return;
        }
        activeTokens.forEach(RefreshToken::revoke);
        refreshTokenRepository.saveAll(activeTokens);
    }

    private String createAndSave(User user) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(Math.max(1, refreshExpirationMs / 1000)))
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
