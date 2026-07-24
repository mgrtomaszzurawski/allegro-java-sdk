/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageToSellerSettingsRaw;
import org.jspecify.annotations.Nullable;

/**
 * The offer's "message to seller" settings, reached from {@link Offer#messageToSellerSettings()} —
 * whether and how a buyer may attach a note to the seller when ordering.
 *
 * @param mode  how a buyer note is handled ({@code REQUIRED}/{@code OPTIONAL}/{@code HIDDEN}
 *              as reported by Allegro), or {@code null}
 * @param hint  the hint shown to the buyer, or {@code null}
 * @since 0.6.0
 */
public record MessageToSellerSettings(
        @Nullable String mode,
        @Nullable String hint) {

    /** Project a generated settings block onto the consumer value, or {@code null}. */
    public static @Nullable MessageToSellerSettings from(@Nullable MessageToSellerSettingsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new MessageToSellerSettings(
                raw.getMode() == null ? null : raw.getMode().getValue(),
                raw.getHint());
    }
}
