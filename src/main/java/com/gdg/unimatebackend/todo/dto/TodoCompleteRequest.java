package com.gdg.unimatebackend.todo.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Builder;

@Getter
public class TodoCompleteRequest {

    private final Boolean completed;

    @JsonCreator
    @Builder
    public TodoCompleteRequest(@JsonProperty("completed") Boolean completed) {
        this.completed = completed;
    }
}