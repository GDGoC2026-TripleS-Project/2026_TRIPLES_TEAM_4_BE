package com.gdg.unimatebackend.todo.service;

import com.gdg.unimatebackend.todo.dto.TeamTodosByDateResponse;
import com.gdg.unimatebackend.todo.dto.TodoCreateRequest;

import java.time.LocalDate;

public interface TodoService {

    void createMyTodo(Long userId, Long teamId, TodoCreateRequest request);

    TeamTodosByDateResponse getTeamTodosByDate(Long userId, Long teamId, LocalDate date);

    void updateMyTodoCompleted(Long userId, Long teamId, Long todoId, boolean completed);
}