package com.gdg.unimatebackend.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MyProfileResponse {

    private Long userId;
    private String nickname;
    private String email;
    private String profileImageUrl;
}