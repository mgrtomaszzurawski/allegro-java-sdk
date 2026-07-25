/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentTypeRaw;
import org.jspecify.annotations.Nullable;

/**
 * The kind of document an offer attachment carries (a user manual, energy label,
 * competition rules, …). A value Allegro adds after this SDK release reads back as
 * {@link #UNKNOWN} rather than failing the response; {@code UNKNOWN} is a read-only
 * sentinel and cannot be sent when declaring an attachment.
 *
 * @since 0.4.0
 */
public enum AttachmentType {

    /** Product manual. */
    MANUAL,
    /** Special-offer rules. */
    SPECIAL_OFFER_RULES,
    /** Competition rules. */
    COMPETITION_RULES,
    /** Book excerpt. */
    BOOK_EXCERPT,
    /** User manual. */
    USER_MANUAL,
    /** Installation instructions. */
    INSTALLATION_INSTRUCTIONS,
    /** Game instructions. */
    GAME_INSTRUCTIONS,
    /** Energy label. */
    ENERGY_LABEL,
    /** Product information sheet. */
    PRODUCT_INFORMATION_SHEET,
    /** Tire label. */
    TIRE_LABEL,
    /** Safety-information manual. */
    SAFETY_INFORMATION_MANUAL,
    /** Software data processing. */
    SOFTWARE_DATA_PROCESSING,
    /** Hardware data processing. */
    HARDWARE_DATA_PROCESSING,
    /** Plant-protection-products authorization. */
    PLANT_PROTECTION_PRODUCTS_AUTHORIZATION,
    /** A type introduced after this SDK release (read-only; never sent). */
    UNKNOWN;

    private static final String ERR_UNKNOWN = "UNKNOWN is a read-only sentinel and cannot be sent";

    /** Project a generated attachment type onto the consumer enum ({@code null}/unrecognised → {@link #UNKNOWN}). */
    public static AttachmentType from(@Nullable AttachmentTypeRaw raw) {
        if (raw == null || raw == AttachmentTypeRaw.UNKNOWN_DEFAULT_OPEN_API) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw.name());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /** The generated wire enum for this type; rejects {@link #UNKNOWN} (not sendable). */
    public AttachmentTypeRaw toRaw() {
        if (this == UNKNOWN) {
            throw new IllegalStateException(ERR_UNKNOWN);
        }
        return AttachmentTypeRaw.valueOf(name());
    }
}
