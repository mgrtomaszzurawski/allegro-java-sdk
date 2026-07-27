/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * One priced configuration of an additional service: the {@link ServiceConstraint}
 * it applies under and the {@link Money} price charged.
 *
 * @param constraint the constraint this configuration applies under, or {@code null}
 * @param price the price charged, or {@code null}
 *
 * @since 0.3.0
 */
public record ServiceConfiguration(
        @Nullable ServiceConstraint constraint,
        @Nullable Money price) {

    /** Map the generated Layer-1 DTO. */
    public static ServiceConfiguration from(ConfigurationRaw raw) {
        return new ServiceConfiguration(ServiceConstraint.from(raw.getConstraintCriteria()), price(raw.getPrice()));
    }

    private static @Nullable Money price(@Nullable PriceRaw raw) {
        if (raw == null || raw.getAmount() == null || raw.getCurrency() == null) {
            return null;
        }
        return Money.of(raw.getAmount(), raw.getCurrency());
    }
}
