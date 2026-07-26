/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageToSellerSettingsRaw;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The offer's "message to seller" settings: whether and how a buyer may attach a note to the
 * seller when ordering.
 *
 * <p>The same immutable value is used both ways: build one to set the settings on
 * {@code CreateOfferRequest}, or read one back from {@link Offer#messageToSellerSettings()}.
 *
 * @param mode  how a buyer note is handled, or {@code null} if unset
 * @param hint  the hint shown to the buyer, or {@code null}
 * @since 0.6.0
 */
public record MessageToSellerSettings(
        @Nullable MessageToSellerMode mode,
        @Nullable String hint) {

    /** Settings with just a mode (required). */
    public static MessageToSellerSettings of(MessageToSellerMode mode) {
        return new MessageToSellerSettings(Objects.requireNonNull(mode, "mode"), null);
    }

    /** Settings with a mode (required) and an optional buyer hint. */
    public static MessageToSellerSettings of(MessageToSellerMode mode, @Nullable String hint) {
        return new MessageToSellerSettings(Objects.requireNonNull(mode, "mode"), hint);
    }

    /** Project a generated settings block onto the consumer value, or {@code null}. */
    public static @Nullable MessageToSellerSettings from(@Nullable MessageToSellerSettingsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new MessageToSellerSettings(MessageToSellerMode.from(raw.getMode()), raw.getHint());
    }

    /** The generated settings block for this value (only set fields are written). */
    public MessageToSellerSettingsRaw toRaw() {
        MessageToSellerSettingsRaw raw = new MessageToSellerSettingsRaw();
        if (mode != null) {
            raw.mode(mode.toRaw());
        }
        if (hint != null) {
            raw.hint(hint);
        }
        return raw;
    }
}
