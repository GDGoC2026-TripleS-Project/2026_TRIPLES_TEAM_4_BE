package com.gdg.unimatebackend.university.repository;

import com.gdg.unimatebackend.university.entity.University;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UniversityRepository extends JpaRepository<University, Long> {

    // 앞글자(prefix) 매칭: "성" -> "성공회대학교", "성신여자대학교" 등
    @Query("""
        select u
        from University u
        where lower(u.name) like lower(concat(:q, '%'))
        order by u.name asc
    """)
    Page<University> searchPrefix(@Param("q") String q, Pageable pageable);
}
