package com.gdg.unimatebackend.app.team.repository;

import com.gdg.unimatebackend.app.team.entity.TeamMember;
import com.gdg.unimatebackend.app.team.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    List<TeamMember> findAllByTeamIdOrderByJoinedAtAsc(Long teamId);

    List<TeamMember> findAllByUserIdOrderByJoinedAtDesc(Long userId);

    long countByTeamId(Long teamId);

    long countByTeamIdAndRole(Long teamId, TeamRole role);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);

    void deleteAllByTeamId(Long teamId);
}
