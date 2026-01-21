package com.gdg.unimatebackend.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // System / Swagger
        if (matcher.match("/api/system/**", uri)
                || matcher.match("/swagger-ui/**", uri)
                || matcher.match("/v3/api-docs/**", uri)
                || matcher.match("/swagger-ui.html", uri)
                || matcher.match("/error", uri)) {
            return true;
        }

        // Auth 중 "공개"만 JWT 필터 제외
        if (matcher.match("/api/auth/**", uri)) {
            // GET 공개
            if (HttpMethod.GET.matches(method)) {
                return matcher.match("/api/auth/naver/authorize-url", uri)
                        || matcher.match("/api/auth/kakao/authorize-url", uri)
                        || matcher.match("/api/auth/naver/callback", uri)
                        || matcher.match("/api/auth/kakao/callback", uri)
                        || matcher.match("/api/auth/nickname/check", uri);
            }

            // POST 공개
            if (HttpMethod.POST.matches(method)) {
                return matcher.match("/api/auth/signup", uri)
                        || matcher.match("/api/auth/login", uri)
                        || matcher.match("/api/auth/password/reset", uri)
                        || matcher.match("/api/auth/email/find", uri)
                        || matcher.match("/api/auth/email/verification/send", uri)
                        || matcher.match("/api/auth/email/verification/confirm", uri)
                        || matcher.match("/api/auth/social/login", uri);
            }

            // 나머지 auth(DELETE /account, POST /password/change, POST /logout)는 필터 적용
            return false;
        }

        return false;
    }
}
