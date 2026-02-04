package com.gdg.unimatebackend.app.poke.repository;

import com.gdg.unimatebackend.app.poke.entity.Poke;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PokeRepository extends JpaRepository<Poke, Long> {
    Optional<Poke> findTopByTeamIdAndSenderIdAndTargetUserIdAndPokeMessageIdOrderByCreatedAtDesc(
            Long teamId,
            Long senderId,
            Long targetUserId,
            Long pokeMessageId
    );
}
