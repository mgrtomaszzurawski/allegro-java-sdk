/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CellsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.HeaderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PublicTableDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A seller's size table: named column {@code headers} and the {@code rows} of
 * cell values beneath them, optionally derived from a template.
 *
 * @param id the table id (assigned by Allegro; {@code null} before creation)
 * @param name the table name (seller-provided)
 * @param templateId the id of the template this table is based on, or {@code null}
 * @param headers the ordered column names
 * @param rows the table rows
 *
 * @since 0.3.0
 */
public record SizeTable(
        @Nullable String id,
        String name,
        @Nullable String templateId,
        List<String> headers,
        List<SizeTableRow> rows) {

    /** Canonical constructor — defensively copies the collections. */
    public SizeTable {
        headers = headers == null ? List.of() : List.copyOf(headers);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    /** Map the generated Layer-1 DTO. */
    public static SizeTable from(PublicTableDtoRaw raw) {
        List<HeaderRaw> rawHeaders = raw.getHeaders() == null ? List.of() : raw.getHeaders();
        List<CellsRaw> rawValues = raw.getValues() == null ? List.of() : raw.getValues();
        List<String> headers = rawHeaders.stream().map(HeaderRaw::getName).toList();
        List<SizeTableRow> rows = rawValues.stream().map(SizeTableRow::from).toList();
        String templateId = raw.getTemplate() == null ? null : raw.getTemplate().getId();
        return new SizeTable(raw.getId(), raw.getName(), templateId, headers, rows);
    }
}
