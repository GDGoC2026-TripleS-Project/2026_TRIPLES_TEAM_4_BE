package com.gdg.unimatebackend.todo.controller;

import com.gdg.unimatebackend.todo.dto.TeamTodosByDateResponse;
import com.gdg.unimatebackend.todo.dto.TodoCompleteRequest;
import com.gdg.unimatebackend.todo.dto.TodoCreateRequest;
import com.gdg.unimatebackend.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
@Tag(name = "TODO", description = "팀원별 TODO 생성/조회/완료 처리 API")
public class TodoController {

    private final TodoService todoService;

    @Operation(
            summary = "내 TODO 생성",
            description = """
                    팀 내에서 나의 TODO를 생성합니다.
                    
                    - 해당 팀의 팀원만 생성 가능합니다.
                    - 같은 날짜에 동일한 title TODO는 중복 생성할 수 없습니다.
                    - 생성 시 기본 상태는 completed=false 입니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{teamId}/my-todos")
    public ResponseEntity<Void> createMyTodo(
            Authentication authentication,

            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,

            @Valid @RequestBody TodoCreateRequest request
    ) {
        Long userId = extractUserId(authentication);
        todoService.createMyTodo(userId, teamId, request);
        return ResponseEntity.ok().build();
    }


    @Operation(
            summary = "팀 TODO 조회 (날짜별)",
            description = """
                    특정 날짜의 팀원 TODO 목록을 조회합니다.
                    
                    - 팀 멤버만 조회 가능합니다.
                    - 팀원의 TODO가 생성된 순서(createdAt ASC)로 반환됩니다.
                    - 각 TODO에는 작성자 정보(닉네임, 프로필 이미지, 팀 색상)가 포함됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{teamId}/todos")
    public ResponseEntity<TeamTodosByDateResponse> getTeamTodosByDate(
            Authentication authentication,

            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,

            @Parameter(description = "조회할 날짜", example = "2026-02-20")
            @RequestParam("date") LocalDate date
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(todoService.getTeamTodosByDate(userId, teamId, date));
    }


    @Operation(
            summary = "내 TODO 완료 상태 변경",
            description = """
                    내 TODO의 완료 상태를 변경합니다.
                    
                    - completed=true → 완료 처리
                    - completed=false → 완료 취소
                    - body 없이 호출 시 기본값 completed=true 처리됩니다.
                    - 본인이 작성한 TODO만 수정 가능합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{teamId}/my-todos/{todoId}")
    public ResponseEntity<Void> updateMyTodoCompleted(
            Authentication authentication,

            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,

            @Parameter(description = "TODO ID", example = "7")
            @PathVariable Long todoId,

            @RequestBody(required = false) TodoCompleteRequest request
    ) {
        Long userId = extractUserId(authentication);

        boolean completed = true;
        if (request != null && request.getCompleted() != null) {
            completed = request.getCompleted();
        }

        todoService.updateMyTodoCompleted(userId, teamId, todoId, completed);
        return ResponseEntity.ok().build();
    }


    // Authentication → userId 추출 (기존 코드 유지)
    private Long extractUserId(Authentication authentication) {

        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (principal == null) {
            throw new IllegalStateException("Unauthenticated");
        }

        if (principal instanceof Long) {
            return (Long) principal;
        }

        try {
            return (Long) principal.getClass().getMethod("getId").invoke(principal);
        } catch (Exception ignored) {
        }

        if (principal instanceof String) {
            return Long.parseLong((String) principal);
        }

        throw new IllegalStateException("Cannot extract userId from principal: " + principal.getClass());
    }
}