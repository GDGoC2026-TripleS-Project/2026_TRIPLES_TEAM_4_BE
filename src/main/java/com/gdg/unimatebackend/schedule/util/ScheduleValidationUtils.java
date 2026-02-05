package com.gdg.unimatebackend.schedule.util;

import com.gdg.unimatebackend.schedule.entity.ScheduleCategory;

import java.util.Set;
import java.util.function.Function;

public final class ScheduleValidationUtils {
    private ScheduleValidationUtils() {}

    private static final Set<Integer> ALLOWED_ALARMS = Set.of(0, 10, 30, 60, 1440);

    public static void validateCategoryRequired(
            ScheduleCategory category,
            String categoryMemo,
            Function<String, RuntimeException> exceptionFactory
    ) {
        if (category == null) {
            throw exceptionFactory.apply("category는 필수입니다");
        }
        if (category == ScheduleCategory.OTHER) {
            if (categoryMemo == null || categoryMemo.isBlank()) {
                throw exceptionFactory.apply("categoryMemo는 OTHER일 때 필수입니다");
            }
        }
    }

    public static void validateAlarmMinutes(
            Integer alarmMinutes,
            Function<String, RuntimeException> exceptionFactory
    ) {
        if (alarmMinutes != null && !ALLOWED_ALARMS.contains(alarmMinutes)) {
            throw exceptionFactory.apply("alarmMinutes 값이 올바르지 않습니다");
        }
    }

    public static String normalizeCategoryMemo(ScheduleCategory category, String categoryMemo) {
        if (category == ScheduleCategory.OTHER) {
            return categoryMemo;
        }
        return null;
    }
}
