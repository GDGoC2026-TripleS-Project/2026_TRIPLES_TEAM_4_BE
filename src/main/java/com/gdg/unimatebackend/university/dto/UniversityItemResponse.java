package com.gdg.unimatebackend.university.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class UniversityItemResponse {
    private Long id;
    private String name;

    public static UniversityItemResponse of(Long id, String name) {
        return UniversityItemResponse.builder()
                .id(id)
                .name(name)
                .build();
    }
}
