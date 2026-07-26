/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferImageUploadResponseRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * The result of uploading an offer image: the hosted image {@link #location() URL}
 * to reference in a {@code CreateOfferRequest}'s image list, and when that hosted
 * copy {@link #expiresAt() expires} if it is not attached to an offer.
 *
 * @param location  the hosted image URL to use in an offer, or {@code null} if absent
 * @param expiresAt when the hosted image expires if unused, or {@code null} if not returned
 * @since 0.4.0
 */
public record OfferImage(@Nullable String location, @Nullable OffsetDateTime expiresAt) {

    /** Project the generated upload response onto the consumer value. */
    public static OfferImage from(OfferImageUploadResponseRaw raw) {
        return new OfferImage(raw.getLocation(), raw.getExpiresAt());
    }
}
