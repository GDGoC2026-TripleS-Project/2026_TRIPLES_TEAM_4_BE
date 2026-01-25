package com.gdg.unimatebackend.app.user.service;

import com.gdg.unimatebackend.app.auth.service.AuthService;
import com.gdg.unimatebackend.app.university.entity.University;
import com.gdg.unimatebackend.app.university.repository.UniversityRepository;
import com.gdg.unimatebackend.app.user.dto.ProfileUpsertRequest;
import com.gdg.unimatebackend.app.user.dto.UserResponse;
import com.gdg.unimatebackend.app.user.entity.User;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final AuthService authService;

    @Transactional
    public UserResponse upsertProfile(Long userId, ProfileUpsertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 닉네임 변경이면 중복 체크
        if (!request.getNickname().equals(user.getNickname())) {
            authService.checkNicknameDuplicate(request.getNickname());
            user.updateNickname(request.getNickname());
        }

        // 학교 FK 세팅
        University uni = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new IllegalArgumentException("학교를 찾을 수 없습니다"));
        user.updateUniversity(uni);

        // 이미지 URL은 덮어쓰기(선택)
        if (request.getProfileImageUrl() != null) {
            user.updateProfileImageUrl(request.getProfileImageUrl());
        }

        return convertToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        return convertToResponse(user);
    }

    private UserResponse convertToResponse(User user) {
        Long uniId = (user.getUniversity() == null) ? null : user.getUniversity().getId();
        String uniName = (user.getUniversity() == null) ? null : user.getUniversity().getName();

        boolean completed =
                user.getNickname() != null && !user.getNickname().isBlank()
                        && uniId != null;

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .emailVerified(user.getEmailVerified())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .active(user.getActive())
                .profileImageUrl(user.getProfileImageUrl())
                .universityId(uniId)
                .universityName(uniName)
                .profileCompleted(completed)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
