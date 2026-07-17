/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedExtensionRaw;
import org.jspecify.annotations.Nullable;

/**
 * An add-on export/extension bundled with a classifieds package (for example an
 * external partner listing).
 *
 * @param name extension identifier, or {@code null} when not provided
 * @param description human-readable extension description, or {@code null}
 *
 * @since 0.2.0
 */
public record ClassifiedExtension(@Nullable String name, @Nullable String description) {

    /** Map the generated Layer-1 DTO to the public record. */
    static ClassifiedExtension from(ClassifiedExtensionRaw raw) {
        return new ClassifiedExtension(raw.getName(), raw.getDescription());
    }
}
