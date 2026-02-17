package com.gdg.unimatebackend.team.repository;

import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.entity.TeamColor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    boolean existsByColor(TeamColor color);

    List<Team> findByEndAtLessThanEqual(LocalDateTime dateTime);
}
