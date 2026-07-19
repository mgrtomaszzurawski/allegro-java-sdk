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
 * The body for amending an already-submitted Advance Ship Notice
 * ({@code advanceShipNotices().updateSubmitted(...)}). Every field is optional —
 * only the parts you set are sent, so an empty update touches nothing. Any
 * product line supplied is quantity-checked to the spec's {@code 1}..{@code 1000000}
 * bounds fail-fast.
 *
 * <p>Amending the notice's polymorphic {@code shipping} details is deferred behind
 * the same Layer-1 read-DTO defect that stops the courier / own-transport /
 * third-party methods from deserializing (see {@link AsnRequest}).
 *
 * @since 0.4.0
 */
public final class SubmittedAsnUpdate {

    private final List<AsnItem> items;
    private final @Nullable HandlingUnit handlingUnit;
    private final @Nullable BigDecimal declaredVolumeInCc;

    private SubmittedAsnUpdate(Builder builder) {
        this.items = List.copyOf(builder.items);
        this.handlingUnit = builder.handlingUnit;
        this.declaredVolumeInCc = builder.declaredVolumeInCc;
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

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this update. */
    public Builder toBuilder() {
        return new Builder()
                .items(items)
                .handlingUnit(handlingUnit)
                .declaredVolumeInCc(declaredVolumeInCc);
    }

    /** Fluent builder for {@link SubmittedAsnUpdate}. */
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

        /** Build the update. */
        public SubmittedAsnUpdate build() {
            return new SubmittedAsnUpdate(this);
        }
    }
}
