package com.gdg.unimatebackend.app.user.repository;

import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import com.gdg.unimatebackend.app.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    // 소셜 로그인 관련 메서드
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);
    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);
}

