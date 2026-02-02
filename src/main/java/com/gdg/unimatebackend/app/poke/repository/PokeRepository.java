package com.gdg.unimatebackend.app.poke.repository;

import com.gdg.unimatebackend.app.poke.entity.Poke;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokeRepository extends JpaRepository<Poke, Long> {
}
