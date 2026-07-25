/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CellsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.HeaderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTableTemplateResponseRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A ready-made size-table template offered by Allegro: predefined column
 * {@code headers}, example {@code rows}, and an optional annotated {@code image}.
 * A seller creates their own {@code SizeTable} from a template by referencing its
 * {@link #id()}.
 *
 * @param id the template id
 * @param name the template name
 * @param image the annotated template image, or {@code null}
 * @param headers the predefined column names
 * @param rows the example rows
 *
 * @since 0.3.0
 */
public record SizeTableTemplate(
        String id,
        String name,
        @Nullable SizeTableTemplateImage image,
        List<String> headers,
        List<SizeTableRow> rows) {

    /** Canonical constructor — defensively copies the collections. */
    public SizeTableTemplate {
        headers = headers == null ? List.of() : List.copyOf(headers);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    /** Map the generated Layer-1 DTO. */
    public static SizeTableTemplate from(SizeTableTemplateResponseRaw raw) {
        List<HeaderRaw> rawHeaders = raw.getHeaders() == null ? List.of() : raw.getHeaders();
        List<CellsRaw> rawValues = raw.getValues() == null ? List.of() : raw.getValues();
        List<String> headers = rawHeaders.stream().map(HeaderRaw::getName).toList();
        List<SizeTableRow> rows = rawValues.stream().map(SizeTableRow::from).toList();
        SizeTableTemplateImage image =
                raw.getImage() == null ? null : SizeTableTemplateImage.from(raw.getImage());
        return new SizeTableTemplate(raw.getId(), raw.getName(), image, headers, rows);
    }
}
