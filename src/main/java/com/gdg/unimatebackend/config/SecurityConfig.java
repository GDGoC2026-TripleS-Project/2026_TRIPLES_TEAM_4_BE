package com.gdg.unimatebackend.config;

import com.gdg.unimatebackend.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        // ✅ 프리플라이트 허용 (앱/브라우저에서 OPTIONS 먼저 날아올 수 있음)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ 기존 허용
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/system/**").permitAll()

                        // ✅ Swagger 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()

                        // ✅ FCM 디버그 허용
                        .requestMatchers("/api/v1/fcm/debug/**").permitAll()

                        // ✅ (핵심) 실제 존재하는 경로로 오픈해야 401 해결됨
                        // v3/api-docs 기준: POST /api/v1/fcm/token
                        .requestMatchers(HttpMethod.POST, "/api/v1/fcm/token").permitAll()

                        // ✅ (테스트 루프용) 저장된 토큰으로 "나에게 발송"도 테스트 중엔 열어두는게 편함
                        .requestMatchers(HttpMethod.POST, "/api/v1/fcm/test/me").permitAll()

                        // 그 외는 JWT 필요
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
