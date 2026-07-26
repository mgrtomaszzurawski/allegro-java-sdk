/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnShipping;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.HandlingUnit;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The body for amending an already-submitted Advance Ship Notice
 * ({@code advanceShipNotices().updateSubmitted(...)}). Every field is optional —
 * only the parts you set are sent, so an empty update touches nothing. Any
 * product line supplied is quantity-checked to the spec's {@code 1}..{@code 1000000}
 * bounds fail-fast.
 *
 * <p>Amend how the goods reach the warehouse with {@link Builder#shipping} — a writable
 * {@link AsnShipping} method (courier, own transport, or third party). The read-only
 * {@link AsnShipping.AlreadyInWarehouse} method is rejected.
 *
 * @since 0.4.0
 */
public final class SubmittedAsnUpdate {

    private static final String ERR_SHIPPING_READ_ONLY =
            "ALREADY_IN_WAREHOUSE shipping is read-only and cannot be declared on an update";

    private final List<AsnItem> items;
    private final @Nullable HandlingUnit handlingUnit;
    private final @Nullable BigDecimal declaredVolumeInCc;
    private final @Nullable AsnShipping shipping;

    private SubmittedAsnUpdate(Builder builder) {
        this.items = List.copyOf(builder.items);
        this.handlingUnit = builder.handlingUnit;
        this.declaredVolumeInCc = builder.declaredVolumeInCc;
        this.shipping = builder.shipping;
    }

    /** The product lines to send (never {@code null}; empty leaves them unchanged). */
    public List<AsnItem> items() {
        return items;
    }

    /** The packing to send, or {@code null} to leave it unchanged. */
    public @Nullable HandlingUnit handlingUnit() {
        return handlingUnit;
    }

    /** The declared volume to send, or {@code null} to leave it unchanged. */
    public @Nullable BigDecimal declaredVolumeInCc() {
        return declaredVolumeInCc;
    }

    /** The shipping to send, or {@code null} to leave it unchanged. */
    public @Nullable AsnShipping shipping() {
        return shipping;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this update. */
    public Builder toBuilder() {
        return new Builder()
                .items(items)
                .handlingUnit(handlingUnit)
                .declaredVolumeInCc(declaredVolumeInCc)
                .shipping(shipping);
    }

    /** Fluent builder for {@link SubmittedAsnUpdate}. */
    public static final class Builder {

        private final List<AsnItem> items = new ArrayList<>();
        private @Nullable HandlingUnit handlingUnit;
        private @Nullable BigDecimal declaredVolumeInCc;
        private @Nullable AsnShipping shipping;

        /** Add a product line by product id and unit quantity. */
        public Builder addItem(String productId, int quantity) {
            this.items.add(AsnItems.checked(productId, BigDecimal.valueOf(quantity)));
            return this;
        }

        /** Replace the product lines with the given collection (each validated). */
        public Builder items(List<AsnItem> items) {
            this.items.clear();
            for (AsnItem item : items) {
                this.items.add(AsnItems.checked(item.productId(), item.quantity()));
            }
            return this;
        }

        /** Amend how the goods are packed. */
        public Builder handlingUnit(@Nullable HandlingUnit handlingUnit) {
            this.handlingUnit = handlingUnit;
            return this;
        }

        /** Amend the declared volume in cubic centimetres. */
        public Builder declaredVolumeInCc(@Nullable BigDecimal declaredVolumeInCc) {
            this.declaredVolumeInCc = declaredVolumeInCc;
            return this;
        }

        /**
         * Amend how the goods reach the warehouse. Rejects the read-only
         * {@link AsnShipping.AlreadyInWarehouse} method, which is never sent.
         */
        public Builder shipping(@Nullable AsnShipping shipping) {
            if (shipping instanceof AsnShipping.AlreadyInWarehouse) {
                throw new IllegalArgumentException(ERR_SHIPPING_READ_ONLY);
            }
            this.shipping = shipping;
            return this;
        }

        /** Build the update. */
        public SubmittedAsnUpdate build() {
            return new SubmittedAsnUpdate(this);
        }
    }
}
