package com.gdg.unimatebackend.todo.service;

import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.todo.dto.TeamTodosByDateResponse;
import com.gdg.unimatebackend.todo.dto.TodoCreateRequest;
import com.gdg.unimatebackend.todo.dto.TodoItemResponse;
import com.gdg.unimatebackend.todo.entity.Todo;
import com.gdg.unimatebackend.todo.exception.TodoException;
import com.gdg.unimatebackend.todo.repository.TodoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void createMyTodo(Long userId, Long teamId, TodoCreateRequest request) {

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw TodoException.notTeamMember();
        }

        if (request == null || request.getDate() == null) {
            throw TodoException.invalidDate();
        }

        String title = request.getTitle() == null ? null : request.getTitle().trim();
        if (title == null || title.isBlank()) {
            // @Valid로 걸리긴 하지만 방어
            throw new IllegalArgumentException("title is required");
        }

        boolean duplicate =
                todoRepository.existsByTeamIdAndUserIdAndDateAndTitle(teamId, userId, request.getDate(), title);

        if (duplicate) {
            throw TodoException.duplicateTodo();
        }

        Todo todo = Todo.builder()
                .teamId(teamId)
                .userId(userId)
                .date(request.getDate())
                .title(title)
                .isCompleted(false)
                .completedAt(null)
                .build();

        todoRepository.save(todo);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamTodosByDateResponse getTeamTodosByDate(Long userId, Long teamId, LocalDate date) {

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw TodoException.notTeamMember();
        }

        if (date == null) {
            throw TodoException.invalidDate();
        }

        // 팀원 목록(색상 포함)
        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);

        // teamMember -> userId list
        Set<Long> memberUserIds = members.stream()
                .map(TeamMember::getUserId)
                .collect(Collectors.toSet());

        // users 테이블에서 닉네임/프로필 가져오기 (User 엔티티 필요 없이 native로)
        Map<Long, UserMini> userMap = fetchUsersMini(memberUserIds);

        // 팀원의 displayColorHex 맵
        Map<Long, String> colorMap = new HashMap<>();
        for (TeamMember m : members) {
            String hex = null;
            try {
                // TeamMember에 displayColor가 있다는 전제(너 repo에 displayColor 쿼리도 있으니 거의 확실)
                hex = m.getDisplayColor() != null ? m.getDisplayColor().getHex() : null;
            } catch (Exception ignored) {
                // displayColor 구조가 다르면 여기서 null로만 내려감 (컴파일은 TeamMember에 getter가 있어야 함)
                hex = null;
            }
            colorMap.put(m.getUserId(), hex);
        }

        // 해당 날짜 TODO
        List<Todo> todos = todoRepository.findAllByTeamIdAndDateOrderByCreatedAtAsc(teamId, date);

        List<TodoItemResponse> items = new ArrayList<>();
        for (Todo t : todos) {
            UserMini u = userMap.get(t.getUserId());

            items.add(
                    TodoItemResponse.builder()
                            .todoId(t.getId())
                            .userId(t.getUserId())
                            .nickname(u == null ? null : u.nickname)
                            .profileImageUrl(u == null ? null : u.profileImageUrl)
                            .displayColorHex(colorMap.get(t.getUserId()))
                            .title(t.getTitle())
                            .completed(t.isCompleted())
                            .build()
            );
        }

        return TeamTodosByDateResponse.builder()
                .teamId(teamId)
                .date(date)
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public void updateMyTodoCompleted(Long userId, Long teamId, Long todoId, boolean completed) {

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw TodoException.notTeamMember();
        }

        Todo todo = todoRepository.findByIdAndTeamIdAndUserId(todoId, teamId, userId)
                .orElseThrow(TodoException::todoNotFound);

        if (completed) {
            todo.markCompleted(LocalDateTime.now());
        } else {
            todo.markUncompleted();
        }
        // dirty checking
    }

    // ===== users native query helper (내부용) =====
    private Map<Long, UserMini> fetchUsersMini(Set<Long> userIds) {

        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // IN (:ids) 를 native query에서 쓰기 위해 List로
        List<Long> ids = new ArrayList<>(userIds);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select id, nickname, profile_image_url from users where id in (:ids)"
                )
                .setParameter("ids", ids)
                .getResultList();

        Map<Long, UserMini> map = new HashMap<>();
        for (Object[] r : rows) {
            Long id = r[0] == null ? null : ((Number) r[0]).longValue();
            String nickname = r[1] == null ? null : String.valueOf(r[1]);
            String profileUrl = r[2] == null ? null : String.valueOf(r[2]);
            if (id != null) map.put(id, new UserMini(nickname, profileUrl));
        }
        return map;
    }

    private static class UserMini {
        final String nickname;
        final String profileImageUrl;

        UserMini(String nickname, String profileImageUrl) {
            this.nickname = nickname;
            this.profileImageUrl = profileImageUrl;
        }
    }
}