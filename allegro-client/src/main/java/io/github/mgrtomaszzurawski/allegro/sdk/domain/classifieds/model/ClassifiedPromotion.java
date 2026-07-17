/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPromotionRaw;
import java.time.Duration;

/**
 * An additional promotion included in a classifieds package.
 *
 * @param name promotion name (for example {@code emphasized})
 * @param duration how long the promotion lasts
 *
 * @since 0.2.0
 */
public record ClassifiedPromotion(String name, Duration duration) {

    /** Map the generated Layer-1 DTO to the public record. */
    static ClassifiedPromotion from(ClassifiedPromotionRaw raw) {
        return new ClassifiedPromotion(raw.getName(), ClassifiedDurations.parse(raw.getDuration()));
    }
}
