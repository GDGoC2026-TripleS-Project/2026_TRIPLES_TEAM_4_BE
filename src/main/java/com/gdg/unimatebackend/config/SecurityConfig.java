package com.gdg.unimatebackend.config;

import com.gdg.unimatebackend.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        // ✅ 기존 허용 경로
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/system/**").permitAll()

                        // ✅ FCM 디버그/테스트용 경로 허용 (여기 추가)
                        // - 지금 Postman에서 401 뜨는 문제 해결
                        // - 서버만으로 access-token 발급/더미 전송 확인 가능
                        .requestMatchers("/api/v1/fcm/debug/**").permitAll()
                        // 선택: 실제 전송도 인증 없이 임시로 열고 싶으면 아래 주석 해제
                        // .requestMatchers("/api/v1/fcm/send").permitAll()

                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();

        // 로컬 개발
        c.addAllowedOriginPattern("http://localhost:*");
        c.addAllowedOriginPattern("http://127.0.0.1:*");

        // 운영 (duckdns)
        c.addAllowedOriginPattern("https://seok-hwan1.duckdns.org");
        c.addAllowedOriginPattern("http://seok-hwan1.duckdns.org");

        // 선택: 프론트 URL이 환경변수로 들어오는 경우
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            c.addAllowedOriginPattern(frontendUrl);
        }

        c.addAllowedHeader("*");
        c.addAllowedMethod("*");
        c.setAllowCredentials(true);
        c.setExposedHeaders(Arrays.asList("Authorization"));
        c.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
