/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.ConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.Objects;

/**
 * One priced configuration of an additional service in a group create/update request:
 * the price charged for the service. The constraint criteria (which offers the price
 * applies to) are a field-depth follow-up.
 *
 * @param price the price charged for the service
 * @since 0.3.0
 */
public record ServiceConfigurationRequest(Money price) {

    /** Rejects a null price. */
    public ServiceConfigurationRequest {
        Objects.requireNonNull(price, "price");
    }

    /** A configuration charging the given price. */
    public static ServiceConfigurationRequest of(Money price) {
        return new ServiceConfigurationRequest(price);
    }

    /** Project onto the generated Layer-1 request DTO. */
    public ConfigurationRaw toRaw() {
        PriceRaw priceRaw = new PriceRaw();
        priceRaw.setAmount(price.amount());
        priceRaw.setCurrency(price.currency());
        ConfigurationRaw raw = new ConfigurationRaw();
        raw.setPrice(priceRaw);
        return raw;
    }
}
