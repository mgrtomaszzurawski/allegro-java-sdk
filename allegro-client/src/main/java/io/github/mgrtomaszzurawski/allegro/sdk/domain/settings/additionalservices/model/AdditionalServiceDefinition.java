/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryDefinitionResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * An additional-service definition available to the seller (a catalog entry the
 * seller instantiates when building a group): its id (e.g. {@code CARRY_IN}), the
 * buyer-facing name, and the maximum price allowed.
 *
 * @param id definition id, or {@code null}
 * @param name buyer-facing name, or {@code null}
 * @param maxPrice maximum allowed price, or {@code null}
 *
 * @since 0.3.0
 */
public record AdditionalServiceDefinition(
        @Nullable String id,
        @Nullable String name,
        @Nullable Money maxPrice) {

    /** Map the generated Layer-1 DTO. */
    public static AdditionalServiceDefinition from(CategoryDefinitionResponseRaw raw) {
        return new AdditionalServiceDefinition(raw.getId(), raw.getName(), maxPrice(raw.getMaxPrice()));
    }

    private static @Nullable Money maxPrice(@Nullable PriceRaw raw) {
        if (raw == null || raw.getAmount() == null || raw.getCurrency() == null) {
            return null;
        }
        return Money.of(raw.getAmount(), raw.getCurrency());
    }
}
