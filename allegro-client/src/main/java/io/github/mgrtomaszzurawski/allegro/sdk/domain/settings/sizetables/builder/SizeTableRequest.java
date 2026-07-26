/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableRow;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Immutable create/update request for a seller {@code SizeTable}. Build via
 * {@link #builder()}; the builder validates the required fields fail-fast.
 *
 * <p>{@code templateId} is required when creating a table (the table is derived
 * from a template) and ignored when updating one; that create-only rule is
 * enforced at the call site, not here.
 *
 * @param name the table name (required)
 * @param templateId the id of the template to base the table on (create only)
 * @param headers the ordered column names (required, non-empty)
 * @param rows the table rows (required, non-empty)
 *
 * @since 0.3.0
 */
public record SizeTableRequest(
        String name,
        @Nullable String templateId,
        List<String> headers,
        List<SizeTableRow> rows) {

    /** Canonical constructor — defensively copies the collections. */
    public SizeTableRequest {
        headers = headers == null ? List.of() : List.copyOf(headers);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    /** Start a new request builder. */
    public static SizeTableRequestBuilder builder() {
        return new SizeTableRequestBuilder();
    }

    /** A builder pre-filled from this request. */
    public SizeTableRequestBuilder toBuilder() {
        return new SizeTableRequestBuilder()
                .name(name)
                .templateId(templateId)
                .headers(headers)
                .rows(rows);
    }
}
