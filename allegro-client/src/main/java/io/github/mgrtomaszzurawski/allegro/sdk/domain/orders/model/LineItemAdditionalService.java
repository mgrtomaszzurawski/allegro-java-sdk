/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormAdditionalServiceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * An additional service the buyer selected for a {@link LineItem} (e.g. gift wrap
 * or carry-in): the service definition it instantiates, its name, the price
 * charged, and the quantity ordered.
 *
 * @param definitionId the additional-service definition id, or {@code null} when not set
 * @param name the service name, or {@code null} when not set
 * @param price the price charged for the service, or {@code null} when not set
 * @param quantity the quantity ordered, or {@code null} when not set
 *
 * @since 0.8.0
 */
public record LineItemAdditionalService(
        @Nullable String definitionId,
        @Nullable String name,
        @Nullable Money price,
        @Nullable Integer quantity) {

    /** Map the generated Layer-1 DTO. */
    public static LineItemAdditionalService from(CheckoutFormAdditionalServiceRaw raw) {
        return new LineItemAdditionalService(
                raw.getDefinitionId(),
                raw.getName(),
                Prices.money(raw.getPrice()),
                raw.getQuantity());
    }
}
