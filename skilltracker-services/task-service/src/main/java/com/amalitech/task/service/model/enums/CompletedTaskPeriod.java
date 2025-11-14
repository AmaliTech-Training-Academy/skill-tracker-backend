package com.amalitech.task.service.model.enums;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

public enum CompletedTaskPeriod {
    ALL_PERIODS,
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    OLDER;

    /**
     * Parses a string to the corresponding enum, defaulting to ALL_PERIODS.
     */
    public static CompletedTaskPeriod fromString(String text) {
        if (text == null) {
            return ALL_PERIODS;
        }

        return Stream.of(CompletedTaskPeriod.values())
                .filter(e -> e.name().equalsIgnoreCase(text))
                .findFirst()
                .orElse(ALL_PERIODS);
    }

    /**
     * Calculates the start boundary for the time filter.
     * @return The LocalDateTime to filter "greater than or equal to".
     * Returns null if no start boundary is needed (ALL_PERIODS, OLDER).
     */
    public LocalDateTime getStartDateTime() {
        LocalDate today = LocalDate.now();
        return switch (this) {
            case TODAY -> today.atStartOfDay();
            case YESTERDAY -> today.minusDays(1).atStartOfDay();
            case LAST_7_DAYS -> today.minusDays(7).atStartOfDay();
            case LAST_30_DAYS -> today.minusDays(30).atStartOfDay();
            default -> null;
        };
    }

    /**
     * Calculates the end boundary for the time filter.
     * @return The LocalDateTime to filter "less than".
     * Returns null if no end boundary is needed (ALL_PERIODS, etc.).
     */
    public LocalDateTime getEndDateTime() {
        LocalDate today = LocalDate.now();
        return switch (this) {
            case YESTERDAY -> today.atStartOfDay();
            case OLDER -> today.minusDays(30).atStartOfDay();
            default -> null;
        };
    }
}