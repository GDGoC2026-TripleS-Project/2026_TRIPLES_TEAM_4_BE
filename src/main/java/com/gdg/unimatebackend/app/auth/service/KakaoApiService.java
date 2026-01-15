package com.gdg.unimatebackend.app.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gdg.unimatebackend.app.auth.dto.KakaoUserResponse;
import com.gdg.unimatebackend.global.exception.KakaoApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoApiService implements InitializingBean {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String userInfoUri;

    @Override
    public void afterPropertiesSet() {
        log.info("Kakao user-info-uri = {}", userInfoUri);
    }

    public KakaoUserResponse getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // ✅ 요청할 속성을 명시 (동의항목이 켜져 있으면 내려옴)
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("property_keys", "[\"properties.nickname\",\"properties.profile_image\",\"kakao_account.profile\",\"kakao_account.email\"]");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.POST,
                    entity,
                    KakaoUserResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new KakaoApiException("카카오 사용자 정보 조회 실패");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new KakaoApiException("유효하지 않은 카카오 액세스 토큰", e, 401);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new KakaoApiException("카카오 API 권한 오류(403). 플랫폼/카카오로그인 활성화/도메인 설정 확인", e, 403);
        } catch (HttpClientErrorException e) {
            throw new KakaoApiException("카카오 API 오류: " + e.getStatusCode(), e, e.getStatusCode().value());
        } catch (Exception e) {
            throw new KakaoApiException("카카오 사용자 정보 조회 중 오류", e);
        }
    }
}
