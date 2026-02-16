package com.gdg.unimatebackend.todo.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoItemResponse {

    private final Long todoId;

    private final Long userId;
    private final String nickname;
    private final String profileImageUrl;
    private final String displayColorHex;

    private final String title;
    private final boolean completed;
}