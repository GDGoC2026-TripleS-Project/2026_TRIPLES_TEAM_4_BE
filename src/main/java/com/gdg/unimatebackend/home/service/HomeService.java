package com.gdg.unimatebackend.home.service;

import com.gdg.unimatebackend.home.dto.HomeSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface HomeService {

    HomeSummaryResponse getHomeSummary(
            Long userId,
            LocalDate date,
            List<Long> teamIds,
            boolean includeMyPersonal
    );
}