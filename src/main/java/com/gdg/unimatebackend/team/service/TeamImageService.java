package com.gdg.unimatebackend.team.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.entity.TeamRole;
import com.gdg.unimatebackend.team.exception.TeamErrorCodes;
import com.gdg.unimatebackend.team.exception.TeamException;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamImageService {

    private final AmazonS3 amazonS3;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    @Value("${aws.s3.base-url:}")
    private String baseUrl;

    @Transactional
    public String uploadAndReplaceTeamImage(Long userId, Long teamId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다");
        }

        String contentType = (file.getContentType() == null) ? "" : file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.TEAM_NOT_FOUND, "팀을 찾을 수 없습니다", 404
                ));

        requireLeader(team, userId);

        String oldKey = team.getImageKey();
        if (oldKey != null && !oldKey.isBlank()) {
            try {
                amazonS3.deleteObject(bucket, oldKey);
                log.info("[S3] old team image deleted. key={}", oldKey);
            } catch (Exception e) {
                log.warn("[S3] old team image delete failed. key={}, reason={}", oldKey, e.getMessage());
            }
        }

        String ext = guessExt(file.getOriginalFilename(), contentType);
        String newKey = "teams/" + teamId + "/image/" + UUID.randomUUID() + ext;

        try (InputStream is = file.getInputStream()) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(contentType);
            meta.setContentLength(file.getSize());
            amazonS3.putObject(bucket, newKey, is, meta);
            log.info("[S3] new team image uploaded. key={}", newKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("S3 업로드 실패: " + e.getMessage());
        }

        String imageUrl = buildUrl(newKey);
        team.updateImage(newKey, imageUrl);
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

    private void requireLeader(Team team, Long userId) {
        Long teamId = team.getId();

        if (team.getOwnerUserId() != null && !Objects.equals(team.getOwnerUserId(), userId)) {
            throw new TeamException(TeamErrorCodes.FORBIDDEN, "팀장만 가능한 작업입니다", 403);
        }

        TeamMember me = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamErrorCodes.NOT_A_MEMBER, "팀 멤버가 아닙니다", 403));

        if (me.getRole() != TeamRole.LEADER) {
            throw new TeamException(TeamErrorCodes.FORBIDDEN, "팀장만 가능한 작업입니다", 403);
        }
    }
}
