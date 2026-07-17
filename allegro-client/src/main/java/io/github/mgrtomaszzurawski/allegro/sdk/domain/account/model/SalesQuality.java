/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.SalesQualityForDayRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalesQualityHistoryResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalesQualityMetricRaw;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The seller's sales-quality history, as returned by {@code UserAccount.salesQuality()}
 * — one {@link Day} per reported day, each with its overall score and the
 * component {@link Metric}s.
 *
 * @param days the per-day results; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record SalesQuality(List<Day> days) {

    public SalesQuality {
        days = List.copyOf(days);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static SalesQuality from(SalesQualityHistoryResponseRaw raw) {
        List<SalesQualityForDayRaw> quality = raw.getQuality();
        if (quality == null) {
            return new SalesQuality(List.of());
        }
        return new SalesQuality(quality.stream().map(Day::from).toList());
    }

    /**
     * Sales quality for one day.
     *
     * @param resultFor the day these results describe
     * @param score the overall score achieved
     * @param maxScore the maximum achievable score
     * @param grade a letter/category grade, or {@code null} when not provided
     * @param metrics the component metrics; never {@code null}, possibly empty
     */
    public record Day(
            LocalDate resultFor,
            @Nullable BigDecimal score,
            @Nullable BigDecimal maxScore,
            @Nullable String grade,
            List<Metric> metrics) {

        public Day {
            metrics = List.copyOf(metrics);
        }

        static Day from(SalesQualityForDayRaw raw) {
            List<SalesQualityMetricRaw> rawMetrics = raw.getMetrics();
            List<Metric> metrics = rawMetrics == null
                    ? List.of()
                    : rawMetrics.stream().map(Metric::from).toList();
            return new Day(raw.getResultFor(), raw.getScore(), raw.getMaxScore(),
                    raw.getGrade(), metrics);
        }
    }

    /**
     * One component metric of a day's sales quality.
     *
     * @param code stable metric code
     * @param name human-readable metric name
     * @param score the score achieved for this metric
     * @param maxScore the maximum achievable score for this metric
     */
    public record Metric(
            String code,
            @Nullable String name,
            @Nullable BigDecimal score,
            @Nullable BigDecimal maxScore) {

        static Metric from(SalesQualityMetricRaw raw) {
            return new Metric(raw.getCode(), raw.getName(), raw.getScore(), raw.getMaxScore());
        }
    }
}
