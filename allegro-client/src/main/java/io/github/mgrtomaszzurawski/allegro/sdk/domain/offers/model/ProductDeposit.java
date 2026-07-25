/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductDepositRaw;
import java.util.Objects;
import java.util.UUID;

/**
 * A returnable-packaging deposit on a {@linkplain ProductSetElement product-set element} — a
 * reference to a deposit the seller registered (its {@link #id() id}) plus how many units of it
 * apply ({@link #quantity() quantity}). The same immutable value is used both ways: attach one
 * to a product-set element on a write, or read one back from an {@link Offer}.
 *
 * @param id       the registered deposit id (a UUID, required)
 * @param quantity how many units of the deposit apply (at least 1)
 * @since 0.6.0
 */
public record ProductDeposit(String id, int quantity) {

    private static final String ERR_QUANTITY = "quantity must be at least 1";
    private static final int DEFAULT_QUANTITY = 1;

    /** Canonical constructor: the deposit id is required and the quantity must be positive. */
    public ProductDeposit {
        Objects.requireNonNull(id, "id");
        if (quantity < DEFAULT_QUANTITY) {
            throw new IllegalArgumentException(ERR_QUANTITY);
        }
    }

    /** A single unit of the given registered deposit. */
    public static ProductDeposit of(String id) {
        return new ProductDeposit(id, DEFAULT_QUANTITY);
    }

    /** {@code quantity} units of the given registered deposit. */
    public static ProductDeposit of(String id, int quantity) {
        return new ProductDeposit(id, quantity);
    }

    /** Project a generated response deposit onto the consumer value. */
    public static ProductDeposit from(ProductDepositRaw raw) {
        UUID rawId = raw.getId();
        return new ProductDeposit(
                Objects.requireNonNull(rawId == null ? null : rawId.toString(), "id"),
                raw.getQuantity() == null ? DEFAULT_QUANTITY : raw.getQuantity());
    }

    /** The generated request deposit: the deposit id and its quantity. */
    public ProductDepositRaw toRaw() {
        return new ProductDepositRaw().id(UUID.fromString(id)).quantity(quantity);
    }
}
