/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The seller's Smart! classification report, as returned by
 * {@code UserAccount.smartClassification()}: whether the account qualifies for
 * Smart!, when that last changed, and the per-condition breakdown.
 *
 * @param fulfilled whether the account currently qualifies for Smart!
 * @param lastChanged when the qualification last changed, or {@code null}
 * @param conditions the individual qualification conditions; never {@code null}
 * @param excludedDeliveryMethodIds delivery-method ids excluded from Smart!;
 *     never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record SmartClassification(
        boolean fulfilled,
        @Nullable OffsetDateTime lastChanged,
        List<Condition> conditions,
        List<String> excludedDeliveryMethodIds) {

    public SmartClassification {
        conditions = List.copyOf(conditions);
        excludedDeliveryMethodIds = List.copyOf(excludedDeliveryMethodIds);
    }

    /**
     * One Smart! qualification condition and whether the account meets it.
     *
     * <p>A condition is either metric-based (a numeric {@code value} measured
     * against a numeric {@code threshold}) or pass/fail. For a pass/fail
     * condition Allegro sends a boolean on the wire where a metric condition
     * sends a number, so {@code value}/{@code threshold} are {@code null} there —
     * the outcome is carried by {@link #fulfilled()}.
     *
     * @param code stable condition code
     * @param name human-readable condition name
     * @param description longer description, or {@code null}
     * @param value the account's current numeric value for the condition, or
     *     {@code null} when the condition is pass/fail rather than metric
     * @param threshold the numeric value required to fulfil it, or {@code null}
     *     when the condition is pass/fail rather than metric
     * @param fulfilled whether the account meets this condition
     * @param required whether this condition is required for qualification
     */
    public record Condition(
            String code,
            @Nullable String name,
            @Nullable String description,
            @Nullable BigDecimal value,
            @Nullable BigDecimal threshold,
            boolean fulfilled,
            boolean required) {
    }
}
