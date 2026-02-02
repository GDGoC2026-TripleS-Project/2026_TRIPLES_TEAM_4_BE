package com.gdg.unimatebackend.app.poke.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PokeQueryRepository {

    private final EntityManager em;

    // 찌르기 대상 조회: 내 팀 + 팀원(본인 제외) >>한번에<<
    public List<Object[]> findTargetRows(Long me) {
        return em.createNativeQuery("""
        SELECT
          t.id AS team_id,
          t.name AS team_name,
          u.id AS user_id,
          u.nickname AS nickname,
          u.profile_image_url AS profile_image_url
        FROM team_member my
        JOIN teams t ON t.id = my.team_id
        JOIN team_member tm ON tm.team_id = my.team_id
        JOIN users u ON u.id = tm.user_id
        WHERE my.user_id = :me
          AND tm.user_id <> :me
        ORDER BY t.id ASC, u.id ASC
    """)
                .setParameter("me", me)
                .getResultList();
    }
}
