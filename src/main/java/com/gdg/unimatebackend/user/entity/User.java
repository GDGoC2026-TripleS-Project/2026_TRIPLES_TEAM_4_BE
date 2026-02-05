package com.gdg.unimatebackend.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", length = 255)
    private String password;  // 소셜 로그인 사용자는 null 가능

    @Column(name="nickname", unique = true, length=50)
    private String nickname;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.EMAIL;  // 로그인 제공자 (EMAIL, KAKAO)

    @Column(name = "provider_id", length = 100)
    private String providerId;  // 소셜 로그인 제공자의 사용자 ID (예: 카카오 ID)

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "name", length = 50)
    private String name;  // 이름

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;  // 생년월일

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;  // 성별

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;  // 연락처

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updatePassword(String password) {
        this.password = password;
    }

    public void deactivate() {
        this.active = false;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private com.gdg.unimatebackend.university.entity.University university;

    public void updateUniversity(com.gdg.unimatebackend.university.entity.University university) {
        this.university = university;
    }

    @Column(name = "profile_image_key", length = 300)
    private String profileImageKey;

    public void updateProfileImageKey(String key) {
        this.profileImageKey = key;
    }

    /**
     * 닉네임 업데이트
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 프로필 이미지 URL 업데이트
     */
    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 프로필 이미지 삭제
     */
    public void deleteProfileImage() {
        this.profileImageUrl = null;
        this.profileImageKey = null;
    }

    /**
     * 프로필 정보 업데이트
     */
    public void updateProfile(String name, java.time.LocalDate birthDate, Gender gender, String phoneNumber) {
        if (name != null) {
            this.name = name;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }
}

