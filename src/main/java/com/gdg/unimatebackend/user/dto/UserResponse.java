package com.gdg.unimatebackend.user.dto;

import com.gdg.unimatebackend.user.entity.AuthProvider;
import com.gdg.unimatebackend.user.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private Boolean emailVerified;
    private AuthProvider provider;
    private String providerId;
    private Boolean active;
    private String profileImageUrl;
    private String name;
    private LocalDate birthDate;
    private Gender gender;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long universityId;
    private String universityName;
    private Boolean profileCompleted;
}

