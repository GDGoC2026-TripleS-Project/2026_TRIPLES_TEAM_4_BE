package com.gdg.unimatebackend.calendar.controller;

import com.gdg.unimatebackend.calendar.dto.CalendarDayRequest;
import com.gdg.unimatebackend.calendar.dto.CalendarDayResponse;
import com.gdg.unimatebackend.calendar.dto.CalendarMonthRequest;
import com.gdg.unimatebackend.calendar.dto.CalendarMonthResponse;
import com.gdg.unimatebackend.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/month")
    public ResponseEntity<CalendarMonthResponse> getMonth(
            Authentication authentication,
            @RequestParam String month,                    // YYYY-MM
            @RequestParam(required = false) List<Long> teamIds,
            @RequestParam boolean includeMyPersonal
    ) {
        Long userId = (Long) authentication.getPrincipal();

        CalendarMonthRequest request = CalendarMonthRequest.builder()
                .month(month)
                .teamIds(teamIds)
                .includeMyPersonal(includeMyPersonal)
                .build();

        return ResponseEntity.ok(calendarService.getMonth(userId, request));
    }

    @GetMapping("/day")
    public ResponseEntity<CalendarDayResponse> getDay(
            Authentication authentication,
            @RequestParam String date,                     // YYYY-MM-DD
            @RequestParam(required = false) List<Long> teamIds,
            @RequestParam boolean includeMyPersonal
    ) {
        Long userId = (Long) authentication.getPrincipal();

        CalendarDayRequest request = CalendarDayRequest.builder()
                .date(date)
                .teamIds(teamIds)
                .includeMyPersonal(includeMyPersonal)
                .build();

        return ResponseEntity.ok(calendarService.getDay(userId, request));
    }
}