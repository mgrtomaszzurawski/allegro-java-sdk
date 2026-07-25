/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageToSellerSettingsRaw;
import org.jspecify.annotations.Nullable;

/**
 * How a buyer may attach a note to the seller when ordering an offer.
 *
 * @since 0.6.0
 */
public enum MessageToSellerMode {

    /** A buyer note is required to place the order. */
    REQUIRED,
    /** A buyer note is optional. */
    OPTIONAL,
    /** No buyer note field is shown. */
    HIDDEN,
    /** A mode this SDK release does not model yet. */
    UNKNOWN;

    private static final String ERR_NOT_SETTABLE =
            "mode is not a value a client can request: ";

    /** Map the generated mode, tolerating unknown future values; {@code null} maps to {@code null}. */
    public static @Nullable MessageToSellerMode from(MessageToSellerSettingsRaw.@Nullable ModeEnum raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case REQUIRED -> REQUIRED;
            case OPTIONAL -> OPTIONAL;
            case HIDDEN -> HIDDEN;
            default -> UNKNOWN;
        };
    }

    /**
     * Map to the generated mode a client may request ({@link #REQUIRED}/{@link #OPTIONAL}/
     * {@link #HIDDEN}); {@link #UNKNOWN} is not a real mode and is rejected.
     *
     * @throws IllegalArgumentException if the mode cannot be requested by a client
     */
    public MessageToSellerSettingsRaw.ModeEnum toRaw() {
        return switch (this) {
            case REQUIRED -> MessageToSellerSettingsRaw.ModeEnum.REQUIRED;
            case OPTIONAL -> MessageToSellerSettingsRaw.ModeEnum.OPTIONAL;
            case HIDDEN -> MessageToSellerSettingsRaw.ModeEnum.HIDDEN;
            case UNKNOWN -> throw new IllegalArgumentException(ERR_NOT_SETTABLE + this);
        };
    }
}
