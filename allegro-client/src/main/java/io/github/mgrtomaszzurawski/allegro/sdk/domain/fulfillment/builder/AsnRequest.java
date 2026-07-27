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
 * The body for creating or fully updating an Advance Ship Notice
 * ({@code advanceShipNotices().create(...)} / {@code update(...)}). At least one
 * product line is required; the packing and declared volume are optional. Each
 * line's quantity must be between {@code 1} and {@code 1000000}, enforced
 * fail-fast so an out-of-range value never reaches the server.
 *
 * <p>How the goods reach the warehouse is declared with {@link Builder#shipping} —
 * one of the writable {@link AsnShipping} methods (courier, own transport, or third
 * party). {@link AsnShipping.AlreadyInWarehouse} is read-only and is rejected.
 *
 * <pre>{@code
 * AsnRequest request = AsnRequest.builder()
 *         .addItem("2f1e...-product-uuid", 12)
 *         .shipping(new AsnShipping.OwnTransport("FZ12453", eta, "PL"))
 *         .build();
 * }</pre>
 *
 * @since 0.4.0
 */
public final class AsnRequest {

    private static final String ERR_ITEMS_EMPTY = "an ASN request needs at least one item";
    private static final String ERR_SHIPPING_READ_ONLY =
            "ALREADY_IN_WAREHOUSE shipping is read-only and cannot be declared on a request";

    private final List<AsnItem> items;
    private final @Nullable HandlingUnit handlingUnit;
    private final @Nullable BigDecimal declaredVolumeInCc;
    private final @Nullable AsnShipping shipping;

    private AsnRequest(Builder builder) {
        if (builder.items.isEmpty()) {
            throw new IllegalStateException(ERR_ITEMS_EMPTY);
        }
        this.items = List.copyOf(builder.items);
        this.handlingUnit = builder.handlingUnit;
        this.declaredVolumeInCc = builder.declaredVolumeInCc;
        this.shipping = builder.shipping;
    }

    /** The product lines (never empty). */
    public List<AsnItem> items() {
        return items;
    }

    /** How the goods are packed, or {@code null}. */
    public @Nullable HandlingUnit handlingUnit() {
        return handlingUnit;
    }

    /** The declared volume in cubic centimetres, or {@code null}. */
    public @Nullable BigDecimal declaredVolumeInCc() {
        return declaredVolumeInCc;
    }

    /** How the goods reach the warehouse, or {@code null}. */
    public @Nullable AsnShipping shipping() {
        return shipping;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder()
                .items(items)
                .handlingUnit(handlingUnit)
                .declaredVolumeInCc(declaredVolumeInCc)
                .shipping(shipping);
    }

    /** Fluent builder for {@link AsnRequest}. */
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

        /** Declare how the goods are packed. */
        public Builder handlingUnit(@Nullable HandlingUnit handlingUnit) {
            this.handlingUnit = handlingUnit;
            return this;
        }

        /** Declare the volume in cubic centimetres. */
        public Builder declaredVolumeInCc(@Nullable BigDecimal declaredVolumeInCc) {
            this.declaredVolumeInCc = declaredVolumeInCc;
            return this;
        }

        /**
         * Declare how the goods reach the warehouse. Rejects the read-only
         * {@link AsnShipping.AlreadyInWarehouse} method, which is never sent.
         */
        public Builder shipping(@Nullable AsnShipping shipping) {
            if (shipping instanceof AsnShipping.AlreadyInWarehouse) {
                throw new IllegalArgumentException(ERR_SHIPPING_READ_ONLY);
            }
            this.shipping = shipping;
            return this;
        }

        /** Build the request, failing fast if no item was added. */
        public AsnRequest build() {
            return new AsnRequest(this);
        }
    }
}
