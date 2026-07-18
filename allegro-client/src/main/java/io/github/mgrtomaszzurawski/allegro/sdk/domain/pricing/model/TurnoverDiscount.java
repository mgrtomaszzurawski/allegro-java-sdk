/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.NullableTurnoverDiscountDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountDefinitionDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A marketplace turnover discount: the seller rewards a buyer's cumulated
 * turnover with a discount, defined by one or more dated
 * {@link TurnoverDiscountDefinition definitions}.
 *
 * @param marketplaceId the marketplace the discount applies to
 * @param status the discount's lifecycle status, or {@code null} when the
 *     marketplace has no discount configured
 * @param definitions the dated discount definitions (possibly empty)
 *
 * @since 0.3.0
 */
public record TurnoverDiscount(
        String marketplaceId,
        @Nullable Status status,
        List<TurnoverDiscountDefinition> definitions) {

    /** Lifecycle status of a turnover discount — Allegro spec enum values. */
    public enum Status {

        /** The discount is being activated. */
        ACTIVATING,

        /** The discount is active. */
        ACTIVE,

        /** The discount is being deactivated. */
        DEACTIVATING
    }

    /** Defensively copies the definitions so the record stays immutable. */
    public TurnoverDiscount {
        definitions = List.copyOf(definitions);
    }

    /**
     * Map the (non-null) single-discount DTO returned by the write endpoints.
     *
     * @param raw the generated turnover-discount DTO
     * @return the mapped record
     */
    public static TurnoverDiscount from(TurnoverDiscountDtoRaw raw) {
        return new TurnoverDiscount(
                raw.getMarketplaceId(),
                raw.getStatus() == null ? null : Status.valueOf(raw.getStatus().getValue()),
                mapDefinitions(raw.getDefinitions()));
    }

    /**
     * Map one entry of the list DTO (whose fields are individually nullable).
     *
     * @param raw the generated nullable turnover-discount DTO
     * @return the mapped record
     */
    public static TurnoverDiscount fromNullable(NullableTurnoverDiscountDtoRaw raw) {
        return new TurnoverDiscount(
                raw.getMarketplaceId(),
                raw.getStatus() == null ? null : Status.valueOf(raw.getStatus().getValue()),
                mapDefinitions(raw.getDefinitions()));
    }

    private static List<TurnoverDiscountDefinition> mapDefinitions(
            @Nullable List<TurnoverDiscountDefinitionDtoRaw> definitions) {
        return definitions == null
                ? List.of()
                : definitions.stream().map(TurnoverDiscountDefinition::from).toList();
    }
}
