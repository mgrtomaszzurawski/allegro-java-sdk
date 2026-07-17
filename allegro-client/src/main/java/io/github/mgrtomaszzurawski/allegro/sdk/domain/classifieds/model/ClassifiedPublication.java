/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPublicationRaw;
import java.time.Duration;

/**
 * Publication terms of a classifieds package.
 *
 * @param duration how long the advertisement stays published under this package
 *
 * @since 0.2.0
 */
public record ClassifiedPublication(Duration duration) {

    /** Map the generated Layer-1 DTO to the public record. */
    static ClassifiedPublication from(ClassifiedPublicationRaw raw) {
        return new ClassifiedPublication(Duration.parse(raw.getDuration()));
    }
}
