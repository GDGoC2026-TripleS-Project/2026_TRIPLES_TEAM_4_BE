package com.gdg.unimatebackend.home.repository;

import com.gdg.unimatebackend.home.dto.HomeSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface HomeQueryRepository {
    HomeSummaryResponse fetchHomeSummary(Long userId, LocalDate date, List<Long> teamIds, boolean includeMyPersonal);
}