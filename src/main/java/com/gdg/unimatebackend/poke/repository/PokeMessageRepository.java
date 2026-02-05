package com.gdg.unimatebackend.poke.repository;

import com.gdg.unimatebackend.poke.entity.PokeMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PokeMessageRepository extends JpaRepository<PokeMessage, Long> {
    // id ASC 기준으로 항상 같은 순서 보장 (=문구 정렬)
    List<PokeMessage> findAllByOrderByIdAsc();
}
