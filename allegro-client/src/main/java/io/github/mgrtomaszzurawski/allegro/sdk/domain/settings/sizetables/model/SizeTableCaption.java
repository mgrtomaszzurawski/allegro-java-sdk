/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CaptionRaw;

/**
 * A caption pinned to a template image — an {@code index} marking a point on the
 * image and the {@code value} describing what to measure there.
 *
 * @param index the caption index shown on the image
 * @param value the caption text
 *
 * @since 0.3.0
 */
public record SizeTableCaption(String index, String value) {

    /** Map the generated Layer-1 DTO. */
    public static SizeTableCaption from(CaptionRaw raw) {
        return new SizeTableCaption(raw.getIndex(), raw.getValue());
    }
}
