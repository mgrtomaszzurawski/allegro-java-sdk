/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRemovalPreferenceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentWithdrawalAddressRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.RemovalPreferenceBuilder;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A seller's active preference for how One Fulfillment handles goods that must
 * be removed from the warehouse — returned to the seller
 * ({@link RemovalOperation#WITHDRAWAL}, with a {@link #withdrawalAddress()}) or
 * disposed of ({@link RemovalOperation#DISPOSAL}).
 *
 * <p>The same record is read from {@code fulfillment().removalPreference()} and
 * submitted to {@code fulfillment().setRemovalPreference(...)}; build one with
 * {@link #builder()}.
 *
 * @param operation what to do with removable goods
 * @param withdrawalAddress return address, present only for {@code WITHDRAWAL}
 *
 * @since 0.2.0
 */
public record RemovalPreference(
        RemovalOperation operation,
        @Nullable WithdrawalAddress withdrawalAddress) {

    private static final String ERR_OPERATION = "operation must not be null";

    public RemovalPreference {
        Objects.requireNonNull(operation, ERR_OPERATION);
    }

    /** A new builder for a removal preference. */
    public static RemovalPreferenceBuilder builder() {
        return new RemovalPreferenceBuilder();
    }

    /** A builder pre-populated with this preference's values. */
    public RemovalPreferenceBuilder toBuilder() {
        RemovalPreferenceBuilder builder = new RemovalPreferenceBuilder().operation(operation);
        if (withdrawalAddress != null) {
            builder.withdrawalAddress(withdrawalAddress);
        }
        return builder;
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static RemovalPreference from(FulfillmentRemovalPreferenceRaw raw) {
        FulfillmentWithdrawalAddressRaw address = raw.getAddress();
        return new RemovalPreference(
                RemovalOperation.fromWire(raw.getOperation().getValue()),
                address == null ? null : WithdrawalAddress.from(address));
    }
}
