package com.gdg.unimatebackend.app.university.repository;

import com.gdg.unimatebackend.app.university.entity.University;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniversityRepository extends JpaRepository<University, Long> {

    // "성" 입력 시 "성공회대학교", "성신여대"처럼 '앞글자 매칭' 우선 추천
    List<University> findByNameStartingWithIgnoreCaseOrderByNameAsc(String q, Pageable pageable);
}
