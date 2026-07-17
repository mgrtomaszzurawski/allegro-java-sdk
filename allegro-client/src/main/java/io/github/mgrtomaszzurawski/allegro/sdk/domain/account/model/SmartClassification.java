/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.SmartDeliveryMethodRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SmartSellerClassificationReportClassificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SmartSellerClassificationReportConditionsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SmartSellerClassificationReportRaw;
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

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static SmartClassification from(SmartSellerClassificationReportRaw raw) {
        SmartSellerClassificationReportClassificationRaw classification = raw.getClassification();
        boolean fulfilled = classification != null && Boolean.TRUE.equals(classification.getFulfilled());
        OffsetDateTime lastChanged = classification == null ? null : classification.getLastChanged();

        List<SmartSellerClassificationReportConditionsInnerRaw> rawConditions = raw.getConditions();
        List<Condition> conditions = rawConditions == null
                ? List.of()
                : rawConditions.stream().map(Condition::from).toList();

        List<SmartDeliveryMethodRaw> excluded = raw.getExcludedDeliveryMethods();
        List<String> excludedIds = excluded == null
                ? List.of()
                : excluded.stream().map(SmartDeliveryMethodRaw::getId).toList();

        return new SmartClassification(fulfilled, lastChanged, conditions, excludedIds);
    }

    /**
     * One Smart! qualification condition and whether the account meets it.
     *
     * @param code stable condition code
     * @param name human-readable condition name
     * @param description longer description, or {@code null}
     * @param value the account's current value for the condition, or {@code null}
     * @param threshold the value required to fulfil it, or {@code null}
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

        static Condition from(SmartSellerClassificationReportConditionsInnerRaw raw) {
            return new Condition(
                    raw.getCode(),
                    raw.getName(),
                    raw.getDescription(),
                    raw.getValue(),
                    raw.getThreshold(),
                    Boolean.TRUE.equals(raw.getFulfilled()),
                    Boolean.TRUE.equals(raw.getRequired()));
        }
    }
}
