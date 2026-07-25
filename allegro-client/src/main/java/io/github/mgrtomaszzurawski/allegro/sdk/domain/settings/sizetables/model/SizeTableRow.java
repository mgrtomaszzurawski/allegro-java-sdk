/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CellsRaw;
import java.util.List;

/**
 * One row of a size table — the ordered cell values under the table's
 * {@code headers} (e.g. {@code ["M", "38", "96-104"]}).
 *
 * @param cells the row's cell values, positionally aligned with the headers
 *
 * @since 0.3.0
 */
public record SizeTableRow(List<String> cells) {

    /** Canonical constructor — defensively copies the cells. */
    public SizeTableRow {
        cells = cells == null ? List.of() : List.copyOf(cells);
    }

    /** A row of the given cell values. */
    public static SizeTableRow of(List<String> cells) {
        return new SizeTableRow(cells);
    }

    /** Map the generated Layer-1 DTO. */
    public static SizeTableRow from(CellsRaw raw) {
        return new SizeTableRow(raw.getCells());
    }
}
