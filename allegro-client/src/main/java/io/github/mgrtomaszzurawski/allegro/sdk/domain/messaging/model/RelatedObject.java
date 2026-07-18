/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MessageOrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MessageRelatedObjectRaw;
import org.jspecify.annotations.Nullable;

/**
 * The offer and/or order a message relates to. Both parts are optional — a
 * message may reference an offer, an order, both, or neither.
 *
 * @param offerId id of the related offer, or {@code null}
 * @param orderId id of the related order (checkout form), or {@code null}
 *
 * @since 0.2.0
 */
public record RelatedObject(@Nullable String offerId, @Nullable String orderId) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static RelatedObject from(MessageRelatedObjectRaw raw) {
        MessageOfferRaw offer = raw.getOffer();
        MessageOrderRaw order = raw.getOrder();
        return new RelatedObject(
                offer == null ? null : offer.getId(),
                order == null ? null : order.getId());
    }
}
