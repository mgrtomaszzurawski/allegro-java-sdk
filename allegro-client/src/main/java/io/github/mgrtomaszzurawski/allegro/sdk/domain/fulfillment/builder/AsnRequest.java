/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnItem;
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
 * <p>Declaring the notice's polymorphic {@code shipping} details is a deferred
 * follow-up and is not offered here yet.
 *
 * <pre>{@code
 * AsnRequest request = AsnRequest.builder()
 *         .addItem("2f1e...-product-uuid", 12)
 *         .build();
 * }</pre>
 *
 * @since 0.4.0
 */
public final class AsnRequest {

    private static final String ERR_ITEMS_EMPTY = "an ASN request needs at least one item";

    private final List<AsnItem> items;
    private final @Nullable HandlingUnit handlingUnit;
    private final @Nullable BigDecimal declaredVolumeInCc;

    private AsnRequest(Builder builder) {
        if (builder.items.isEmpty()) {
            throw new IllegalStateException(ERR_ITEMS_EMPTY);
        }
        this.items = List.copyOf(builder.items);
        this.handlingUnit = builder.handlingUnit;
        this.declaredVolumeInCc = builder.declaredVolumeInCc;
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

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder()
                .items(items)
                .handlingUnit(handlingUnit)
                .declaredVolumeInCc(declaredVolumeInCc);
    }

    /** Fluent builder for {@link AsnRequest}. */
    public static final class Builder {

        private final List<AsnItem> items = new ArrayList<>();
        private @Nullable HandlingUnit handlingUnit;
        private @Nullable BigDecimal declaredVolumeInCc;

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

        /** Build the request, failing fast if no item was added. */
        public AsnRequest build() {
            return new AsnRequest(this);
        }
    }
}
