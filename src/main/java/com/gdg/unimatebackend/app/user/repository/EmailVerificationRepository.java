package com.gdg.unimatebackend.app.user.repository;

import com.gdg.unimatebackend.app.user.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmailAndCodeAndVerifiedFalse(String email, String code);
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpiredVerifications(LocalDateTime now);
}

