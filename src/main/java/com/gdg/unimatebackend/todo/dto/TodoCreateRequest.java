package com.gdg.unimatebackend.todo.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TodoCreateRequest {

    @NotNull
    private final LocalDate date;

    @NotBlank
    private final String title;

    @JsonCreator
    @Builder
    public TodoCreateRequest(
            @JsonProperty("date") LocalDate date,
            @JsonProperty("title") String title
    ) {
        this.date = date;
        this.title = title;
    }
}