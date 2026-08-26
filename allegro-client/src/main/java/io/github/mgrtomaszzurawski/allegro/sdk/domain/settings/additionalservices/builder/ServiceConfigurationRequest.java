/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.ConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One priced configuration of an additional service in a group create/update request:
 * the price charged for the service and the {@link ServiceConstraintRequest constraint}
 * under which that price applies (which country, and — for delivery-realised services —
 * which delivery methods). Allegro requires a matching constraint for a service to be
 * accepted, so prefer {@link #of(Money, ServiceConstraintRequest)}.
 *
 * @param price the price charged for the service
 * @param constraint the constraint under which the price applies, or {@code null}
 * @since 0.3.0
 */
public record ServiceConfigurationRequest(Money price, @Nullable ServiceConstraintRequest constraint) {

    /** Rejects a null price. */
    public ServiceConfigurationRequest {
        Objects.requireNonNull(price, "price");
    }

    /** A configuration charging the given price with no constraint. */
    public static ServiceConfigurationRequest of(Money price) {
        return new ServiceConfigurationRequest(price, null);
    }

    /** A configuration charging the given price under the given constraint. */
    public static ServiceConfigurationRequest of(Money price, ServiceConstraintRequest constraint) {
        return new ServiceConfigurationRequest(price, constraint);
    }

    /** Project onto the generated Layer-1 request DTO. */
    public ConfigurationRaw toRaw() {
        PriceRaw priceRaw = new PriceRaw();
        priceRaw.setAmount(price.amount());
        priceRaw.setCurrency(price.currency());
        ConfigurationRaw raw = new ConfigurationRaw();
        raw.setPrice(priceRaw);
        if (constraint != null) {
            raw.setConstraintCriteria(constraint.toRaw());
        }
        return raw;
    }
}
