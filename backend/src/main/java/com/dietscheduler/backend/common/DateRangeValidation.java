package com.dietscheduler.backend.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Shared range check for every endpoint that accepts a client-supplied [from, to] date range
 * and then iterates it day-by-day server-side (meal-plan list/auto-generate, shopping-list
 * generate) -- see {@code dietscheduler.limits.max-plan-range-days}. */
public final class DateRangeValidation {

    private DateRangeValidation() {
    }

    public static void requireValidRange(LocalDate from, LocalDate to, int maxDays) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' (" + to + ") must not be before 'from' (" + from + ")");
        }
        long spanDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (spanDays > maxDays) {
            throw new IllegalArgumentException(
                    "Requested range is " + spanDays + " days; the maximum allowed is " + maxDays + " days");
        }
    }

    public static void requireValidLookahead(int days, int maxDays) {
        if (days < 1) {
            throw new IllegalArgumentException("'days' must be at least 1");
        }
        if (days > maxDays) {
            throw new IllegalArgumentException("'days' is " + days + "; the maximum allowed is " + maxDays);
        }
    }
}
