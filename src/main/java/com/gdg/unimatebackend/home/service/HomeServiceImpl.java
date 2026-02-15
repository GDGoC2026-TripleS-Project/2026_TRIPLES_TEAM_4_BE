package com.gdg.unimatebackend.home.service;

import com.gdg.unimatebackend.home.dto.HomeSummaryResponse;
import com.gdg.unimatebackend.home.repository.HomeQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final HomeQueryRepository homeQueryRepository;

    @Override
    public HomeSummaryResponse getHomeSummary(Long userId, LocalDate date, List<Long> teamIds, boolean includeMyPersonal) {
        return homeQueryRepository.fetchHomeSummary(userId, date, teamIds, includeMyPersonal);
    }
}