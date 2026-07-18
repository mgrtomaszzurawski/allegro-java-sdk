/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRefundDispositionBuyerRaw;
import org.jspecify.annotations.Nullable;

/**
 * The buyer associated with a {@link RefundDisposition}.
 *
 * @param login the buyer's Allegro login
 *
 * @since 0.3.0
 */
public record RefundBuyer(@Nullable String login) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static RefundBuyer from(FulfillmentRefundDispositionBuyerRaw raw) {
        return new RefundBuyer(raw.getLogin());
    }
}
