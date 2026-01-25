package com.gdg.unimatebackend.app.user.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.gdg.unimatebackend.app.user.entity.User;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileImageService {

    private final AmazonS3 amazonS3;
    private final UserRepository userRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    @Value("${aws.s3.base-url:}")
    private String baseUrl;

    @Transactional
    public String uploadAndReplaceProfileImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다");
        }

        String contentType = (file.getContentType() == null) ? "" : file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        //  1) 기존 이미지 삭제 (실패해도 업로드는 진행)
        String oldKey = user.getProfileImageKey();
        if (oldKey != null && !oldKey.isBlank()) {
            try {
                amazonS3.deleteObject(bucket, oldKey);
                log.info("[S3] old profile image deleted. key={}", oldKey);
            } catch (Exception e) {
                // 로그만 남기고 계속 진행
                log.warn("[S3] old profile image delete failed. key={}, reason={}", oldKey, e.getMessage());
            }
        }

        // 2) 새 파일 업로드
        String ext = guessExt(file.getOriginalFilename(), contentType);
        String newKey = "users/" + userId + "/profile/" + UUID.randomUUID() + ext;

        try (InputStream is = file.getInputStream()) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(contentType);
            meta.setContentLength(file.getSize());
            amazonS3.putObject(bucket, newKey, is, meta);
            log.info("[S3] new profile image uploaded. key={}", newKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("S3 업로드 실패: " + e.getMessage());
        }

        // 3) URL 생성 + DB 저장(덮어쓰기)
        String imageUrl = buildUrl(newKey);
        user.updateProfileImageKey(newKey);
        user.updateProfileImageUrl(imageUrl);

        return imageUrl;
    }

    private String buildUrl(String key) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return trimmed + "/" + key;
        }
        return amazonS3.getUrl(bucket, key).toString();
    }

    private String guessExt(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }
        if (contentType.equals("image/png")) return ".png";
        if (contentType.equals("image/jpeg")) return ".jpg";
        if (contentType.equals("image/webp")) return ".webp";
        return "";
    }
}
