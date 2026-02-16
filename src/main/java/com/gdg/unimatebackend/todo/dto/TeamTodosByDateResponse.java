package com.gdg.unimatebackend.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TeamTodosByDateResponse {
    private final Long teamId;
    private final LocalDate date;
    private final List<TodoItemResponse> items;
}