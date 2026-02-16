package com.gdg.unimatebackend.todo.repository;

import com.gdg.unimatebackend.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    Optional<Todo> findByIdAndTeamIdAndUserId(Long id, Long teamId, Long userId);

    List<Todo> findAllByTeamIdAndDateOrderByCreatedAtAsc(Long teamId, LocalDate date);

    boolean existsByTeamIdAndUserIdAndDateAndTitle(Long teamId, Long userId, LocalDate date, String title);
}