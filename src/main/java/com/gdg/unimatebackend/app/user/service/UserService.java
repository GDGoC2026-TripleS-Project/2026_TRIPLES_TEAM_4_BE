package com.gdg.unimatebackend.app.user.service;

import com.gdg.unimatebackend.app.user.dto.UserMeResponse;
import com.gdg.unimatebackend.app.user.dto.UserUpdateRequest;
import com.gdg.unimatebackend.app.user.entity.User;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserMeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found. id=" + userId));
        return UserMeResponse.from(user);
    }

    @Transactional
    public UserMeResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found. id=" + userId));

        user.updateProfile(request.nickname()); // ✅ 엔티티 기존 메서드 재사용
        return UserMeResponse.from(user);
    }
}
