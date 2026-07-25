/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CellsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.HeaderRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Shared Layer-1 grid mapping for {@link SizeTable} and {@link SizeTableTemplate},
 * which carry the same {@code headers} / {@code values} shape over different raw
 * response types. Package-private mapping helper.
 */
final class SizeGridMapping {

    private SizeGridMapping() {
    }

    /** The header column names, or an empty list when the wire array is absent. */
    static List<String> headerNames(@Nullable List<HeaderRaw> rawHeaders) {
        if (rawHeaders == null) {
            return List.of();
        }
        return rawHeaders.stream().map(HeaderRaw::getName).toList();
    }

    /** The grid rows, or an empty list when the wire array is absent. */
    static List<SizeTableRow> rows(@Nullable List<CellsRaw> rawValues) {
        if (rawValues == null) {
            return List.of();
        }
        return rawValues.stream().map(SizeTableRow::from).toList();
    }
}
