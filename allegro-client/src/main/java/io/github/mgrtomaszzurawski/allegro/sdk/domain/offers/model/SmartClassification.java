/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.SmartOfferClassificationReportClassificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SmartOfferClassificationReportConditionsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SmartOfferClassificationReportRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An offer's Allegro Smart! classification report
 * ({@code offers().smartClassification(offerId)}): whether the offer qualifies
 * for Smart!, and the per-condition breakdown of why.
 *
 * <p>The report's per-delivery-method lists ({@code smartDeliveryMethods} and the
 * per-condition {@code passed}/{@code failedDeliveryMethods}) are marked deprecated
 * in the Allegro spec and are deliberately not surfaced here — a new typed surface
 * should not lead consumers onto fields the API owner is removing.
 *
 * @param fulfilled                    whether the offer currently qualifies for Smart!
 * @param scheduledForReclassification whether Allegro will re-evaluate the offer
 * @param lastChanged                  when the classification last changed, or {@code null}
 * @param conditions                   the individual Smart! conditions and their state
 * @since 0.2.0
 */
public record SmartClassification(
        boolean fulfilled,
        boolean scheduledForReclassification,
        @Nullable OffsetDateTime lastChanged,
        List<Condition> conditions) {

    public SmartClassification {
        conditions = List.copyOf(conditions);
    }

    /** Project the generated Smart! report onto the consumer record. */
    public static SmartClassification from(SmartOfferClassificationReportRaw raw) {
        SmartOfferClassificationReportClassificationRaw classification = raw.getClassification();
        List<SmartOfferClassificationReportConditionsInnerRaw> rawConditions = raw.getConditions();
        List<Condition> conditions = rawConditions == null
                ? List.of()
                : rawConditions.stream().map(Condition::from).toList();
        return new SmartClassification(
                classification != null && Boolean.TRUE.equals(classification.getFulfilled()),
                Boolean.TRUE.equals(raw.getScheduledForReclassification()),
                classification == null ? null : classification.getLastChanged(),
                conditions);
    }

    /**
     * One Smart! qualification condition and whether the offer meets it.
     *
     * @param code        stable condition code
     * @param name        human-readable condition name
     * @param description condition detail, or {@code null} when absent
     * @param fulfilled   whether the offer meets this condition
     */
    public record Condition(
            @Nullable String code,
            @Nullable String name,
            @Nullable String description,
            boolean fulfilled) {

        /** Project a generated condition onto the consumer record. */
        public static Condition from(SmartOfferClassificationReportConditionsInnerRaw raw) {
            return new Condition(
                    raw.getCode(),
                    raw.getName(),
                    raw.getDescription(),
                    Boolean.TRUE.equals(raw.getFulfilled()));
        }
    }
}
