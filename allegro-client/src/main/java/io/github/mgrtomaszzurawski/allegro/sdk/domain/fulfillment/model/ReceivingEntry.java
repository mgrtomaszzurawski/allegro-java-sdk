/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReceivingEntryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReceivingStatusRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One product's line in an Advance Ship Notice's receiving state: how many units
 * were expected and how the units actually received were dispositioned.
 *
 * @param expected  the expected quantity, when reported
 * @param productId the One Fulfillment product identifier (a UUID), when reported
 * @param received  the received-quantity breakdown (never {@code null}; may be empty)
 *
 * @since 0.4.0
 */
public record ReceivingEntry(
        @Nullable Integer expected,
        @Nullable String productId,
        List<ReceivingStatus> received) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReceivingEntry from(ReceivingEntryRaw raw) {
        List<ReceivingStatusRaw> received = raw.getReceived();
        return new ReceivingEntry(
                raw.getExpected(),
                raw.getProduct() == null ? null : raw.getProduct().getId().toString(),
                received == null ? List.of() : received.stream().map(ReceivingStatus::from).toList());
    }
}
