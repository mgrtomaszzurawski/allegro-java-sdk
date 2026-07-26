/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductDtoAttributesInnerRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One attribute of a {@link CompatibleProduct} — a named property (identified by
 * {@link #id()}) and the one or more {@link #values()} it takes for that product,
 * e.g. a production year range or an engine code.
 *
 * @param id the attribute identifier, or {@code null} when the server omits it
 * @param values the attribute's values; empty when none are returned
 *
 * @since 0.2.0
 */
public record CompatibleProductAttribute(@Nullable String id, List<String> values) {

    /** Canonical constructor; defensively copies {@code values} immutable. */
    public CompatibleProductAttribute {
        values = values == null ? List.of() : List.copyOf(values);
    }

    /** Map one generated Layer-1 attribute to the public record. */
    static CompatibleProductAttribute from(CompatibleProductDtoAttributesInnerRaw raw) {
        return new CompatibleProductAttribute(raw.getId(), raw.getValues());
    }
}
