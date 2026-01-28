package com.gdg.unimatebackend.app.team.repository;

import com.gdg.unimatebackend.app.team.entity.TeamMember;
import com.gdg.unimatebackend.app.team.entity.TeamColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findAllByUserIdOrderByJoinedAtDesc(Long userId);

    List<TeamMember> findAllByTeamIdOrderByJoinedAtAsc(Long teamId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    long countByTeamId(Long teamId);

    void deleteAllByTeamId(Long teamId);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);

    @Query("select distinct tm.displayColor from TeamMember tm where tm.userId = :userId")
    List<TeamColor> findDistinctDisplayColorsByUserId(Long userId);
}
