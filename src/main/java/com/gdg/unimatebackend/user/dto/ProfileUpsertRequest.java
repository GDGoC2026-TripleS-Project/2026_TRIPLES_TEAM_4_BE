package com.gdg.unimatebackend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpsertRequest {

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(max = 10, message = "사용자명은 10자 이하로 입력해주세요")
    private String nickname;

    @NotNull(message = "학교 선택은 필수입니다")
    private Long universityId;

    // 이미지 URL은 선택(업로드 안 하면 null)
    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하로 입력해주세요")
    private String profileImageUrl;
}
