package com.gdg.unimatebackend.calendar.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CalendarTeamPort {

    List<Long> getMyTeamIds(Long userId);

    Map<Long, String> getTeamNames(Long userId, List<Long> teamIds);

    Set<Long> getTeamMemberUserIds(Long userId, List<Long> teamIds);
}