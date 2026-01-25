package com.gdg.unimatebackend.app.university.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "university",
        indexes = {
                @Index(name = "idx_university_name", columnList = "name")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255, unique = true)
    private String name;
}
