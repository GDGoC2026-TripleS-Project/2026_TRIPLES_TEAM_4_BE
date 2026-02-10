package com.gdg.unimatebackend.mypage.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyPageQueryRepository {
    // 지금 단계에서는 TeamService 재사용으로 충분해서 비워둬도 OK
    // 추후 "프로필 + 팀 목록 + 팀원수" 같은 걸 한 번에 가져오고 싶으면 여기에 QueryDSL/JPA 커스텀 쿼리 추가
}