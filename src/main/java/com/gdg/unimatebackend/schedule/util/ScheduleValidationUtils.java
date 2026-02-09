package com.gdg.unimatebackend.schedule.util;

import com.gdg.unimatebackend.schedule.entity.ScheduleCategory;

import java.util.function.Function;

public final class ScheduleValidationUtils {
    private ScheduleValidationUtils() {}

    private static final int MIN_ALARM_MINUTES = 0;
    private static final int MAX_ALARM_MINUTES = 1440;

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
        if (alarmMinutes != null
                && (alarmMinutes < MIN_ALARM_MINUTES || alarmMinutes > MAX_ALARM_MINUTES)) {
            throw exceptionFactory.apply("alarmMinutes 값이 올바르지 않습니다 (0~1440)");
        }
    }

    public static String normalizeCategoryMemo(ScheduleCategory category, String categoryMemo) {
        if (category == ScheduleCategory.OTHER) {
            return categoryMemo;
        }
        return null;
    }
}
