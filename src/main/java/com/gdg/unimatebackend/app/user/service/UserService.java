package com.gdg.unimatebackend.app.user.service;

import com.gdg.unimatebackend.app.auth.service.AuthService;
import com.gdg.unimatebackend.app.user.dto.UserResponse;
import com.gdg.unimatebackend.app.user.dto.UserUpdateRequest;
import com.gdg.unimatebackend.app.user.entity.User;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public UserResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        return convertToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 닉네임 변경 요청이 있고, 현재 닉네임과 다르면 중복 체크
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            String oldNickname = user.getNickname();
            authService.checkNicknameDuplicate(request.getNickname());
            user.updateNickname(request.getNickname());
            log.info("사용자 {} 닉네임 변경: {} -> {}", userId, oldNickname, request.getNickname());
        }

        // 프로필 정보 업데이트 (이름, 생년월일, 성별, 연락처)
        user.updateProfile(request.getName(), request.getBirthDate(), request.getGender(), request.getPhoneNumber());

        return convertToResponse(user);
    }

    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .emailVerified(user.getEmailVerified())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .active(user.getActive())
                .profileImageUrl(user.getProfileImageUrl())
                .name(user.getName())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

