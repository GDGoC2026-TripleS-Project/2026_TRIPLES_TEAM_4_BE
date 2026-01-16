package com.gdg.unimatebackend.app.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gdg.unimatebackend.global.exception.KakaoApiException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 API 호출 서비스
 * 카카오 액세스 토큰을 검증하고 사용자 정보를 가져옵니다.
 */
@Service
@Slf4j
public class KakaoApiService implements InitializingBean {

    private final RestTemplate restTemplate;
    private final String kakaoApiBaseUrl = "https://kapi.kakao.com";

    @Value("${kakao.rest-api-key:}")
    private String restApiKey;

    public KakaoApiService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void afterPropertiesSet() {
        if (restApiKey == null || restApiKey.isEmpty()) {
            log.warn("카카오 REST API 키가 설정되지 않았습니다. 카카오 로그인이 작동하지 않을 수 있습니다.");
        } else {
            log.info("카카오 REST API 키가 설정되었습니다. (키: {}...)", restApiKey.substring(0, Math.min(8, restApiKey.length())));
        }
    }

    /**
     * 카카오 액세스 토큰으로 사용자 정보 조회
     *
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 사용자 정보
     * @throws RuntimeException 토큰이 유효하지 않거나 API 호출 실패 시
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 이메일을 포함한 사용자 정보 요청 (동의항목에 포함된 경우)
            // property_keys 파라미터를 사용하여 필요한 정보만 명시적으로 요청
            String url = kakaoApiBaseUrl + "/v2/user/me?property_keys=[\"kakao_account.email\"]";
            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    KakaoUserInfo.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("카카오 액세스 토큰이 유효하지 않습니다: {}", e.getMessage());
            throw new KakaoApiException("유효하지 않은 카카오 액세스 토큰입니다", e, 401);
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("카카오 API 호출 권한 오류 (403): {}", e.getMessage());
            log.error("응답 본문: {}", e.getResponseBodyAsString());
            throw new KakaoApiException(
                    "카카오 로그인 권한이 없습니다. 카카오 개발자 콘솔에서 플랫폼 등록 및 카카오 로그인 제품 활성화를 확인해주세요.",
                    e,
                    403
            );
        } catch (HttpClientErrorException e) {
            log.error("카카오 API HTTP 오류 ({}): {}", e.getStatusCode(), e.getMessage());
            log.error("응답 본문: {}", e.getResponseBodyAsString());
            throw new KakaoApiException(
                    "카카오 API 호출 중 오류가 발생했습니다: " + e.getStatusCode(),
                    e,
                    e.getStatusCode().value()
            );
        } catch (Exception e) {
            log.error("카카오 API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new KakaoApiException("카카오 사용자 정보 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 카카오 API 응답 DTO
     */
    @Getter
    @Setter
    public static class KakaoUserInfo {
        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @JsonProperty("properties")
        private KakaoProperties properties;

        @Getter
        @Setter
        public static class KakaoAccount {
            private String email;
            private Boolean emailNeedsAgreement;
            private Boolean isEmailValid;
            private Boolean isEmailVerified;

            @JsonProperty("profile")
            private KakaoProfile profile;

            @Getter
            @Setter
            public static class KakaoProfile {
                private String nickname;
                @JsonProperty("profile_image_url")
                private String profileImageUrl;
            }
        }

        @Getter
        @Setter
        public static class KakaoProperties {
            private String nickname;
            @JsonProperty("profile_image")
            private String profileImage;
        }

        /**
         * 이메일 추출 (kakao_account.email 우선, 없으면 null)
         */
        public String getEmail() {
            if (kakaoAccount != null && kakaoAccount.getEmail() != null) {
                return kakaoAccount.getEmail();
            }
            return null;
        }

        /**
         * 닉네임 추출 (properties.nickname 우선, 없으면 kakao_account.profile.nickname)
         */
        public String getNickname() {
            if (properties != null && properties.getNickname() != null) {
                return properties.getNickname();
            }
            if (kakaoAccount != null && kakaoAccount.getProfile() != null
                    && kakaoAccount.getProfile().getNickname() != null) {
                return kakaoAccount.getProfile().getNickname();
            }
            return "카카오사용자";
        }
    }
}

