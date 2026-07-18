/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountDefinitionDtoRaw;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One dated definition of a marketplace turnover discount: the period over which
 * a buyer's turnover is cumulated, the period the resulting discount can be
 * spent, and the threshold ladder that maps turnover to discount.
 *
 * @param cumulatingFromDate first day turnover starts cumulating
 * @param cumulatingToDate last day turnover cumulates, or {@code null} if open-ended
 * @param spendingFromDate first day the earned discount can be spent
 * @param spendingToDate last day the earned discount can be spent, or {@code null}
 * @param createdAt when the definition was created, or {@code null}
 * @param updatedAt when the definition was last changed, or {@code null}
 * @param thresholds the turnover-to-discount ladder (possibly empty)
 *
 * @since 0.3.0
 */
public record TurnoverDiscountDefinition(
        @Nullable LocalDate cumulatingFromDate,
        @Nullable LocalDate cumulatingToDate,
        @Nullable LocalDate spendingFromDate,
        @Nullable LocalDate spendingToDate,
        @Nullable Instant createdAt,
        @Nullable Instant updatedAt,
        List<TurnoverThreshold> thresholds) {

    /** Defensively copies the threshold ladder so the record stays immutable. */
    public TurnoverDiscountDefinition {
        thresholds = List.copyOf(thresholds);
    }

    /**
     * Map the generated definition DTO to the public record.
     *
     * @param raw the generated definition DTO
     * @return the mapped record
     */
    public static TurnoverDiscountDefinition from(TurnoverDiscountDefinitionDtoRaw raw) {
        return new TurnoverDiscountDefinition(
                raw.getCumulatingFromDate(),
                raw.getCumulatingToDate(),
                raw.getSpendingFromDate(),
                raw.getSpendingToDate(),
                raw.getCreatedAt() == null ? null : raw.getCreatedAt().toInstant(),
                raw.getUpdatedAt() == null ? null : raw.getUpdatedAt().toInstant(),
                raw.getThresholds() == null
                        ? List.of()
                        : raw.getThresholds().stream().map(TurnoverThreshold::from).toList());
    }
}
