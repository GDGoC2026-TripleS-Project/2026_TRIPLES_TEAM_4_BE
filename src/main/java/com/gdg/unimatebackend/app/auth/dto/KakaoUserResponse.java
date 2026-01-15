package com.gdg.unimatebackend.app.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,
        @JsonProperty("connected_at") String connectedAt,
        KakaoProperties properties,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public String nickname() {
        if (properties != null && properties.nickname != null) return properties.nickname;
        if (kakaoAccount != null && kakaoAccount.profile != null && kakaoAccount.profile.nickname != null) return kakaoAccount.profile.nickname;
        return "카카오사용자";
    }

    public String profileImageUrl() {
        if (properties != null && properties.profileImage != null) return properties.profileImage;
        if (kakaoAccount != null && kakaoAccount.profile != null && kakaoAccount.profile.profileImageUrl != null) return kakaoAccount.profile.profileImageUrl;
        return null;
    }

    public String email() {
        if (kakaoAccount != null) return kakaoAccount.email;
        return null;
    }

    public static class KakaoProperties {
        public String nickname;

        @JsonProperty("profile_image")
        public String profileImage;

        @JsonProperty("thumbnail_image")
        public String thumbnailImage;
    }

    public static class KakaoAccount {
        public String email;

        @JsonProperty("profile")
        public KakaoProfile profile;

        @JsonProperty("profile_nickname_needs_agreement")
        public Boolean profileNicknameNeedsAgreement;

        @JsonProperty("profile_image_needs_agreement")
        public Boolean profileImageNeedsAgreement;
    }

    public static class KakaoProfile {
        public String nickname;

        @JsonProperty("profile_image_url")
        public String profileImageUrl;

        @JsonProperty("thumbnail_image_url")
        public String thumbnailImageUrl;

        @JsonProperty("is_default_image")
        public Boolean isDefaultImage;
    }
}
