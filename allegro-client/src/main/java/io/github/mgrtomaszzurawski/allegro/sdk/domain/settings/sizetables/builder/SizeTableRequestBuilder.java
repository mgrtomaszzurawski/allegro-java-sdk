/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableRow;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link SizeTableRequest}. Enforces the required {@code name}
 * and the structural minimum of one column and one row fail-fast at
 * {@link #build()}; the finer field rules (cell/header alignment, template
 * compatibility) are owned by the server.
 *
 * @since 0.3.0
 */
public final class SizeTableRequestBuilder {

    private static final String ERR_NAME_REQUIRED = "Size table name is required";
    private static final String ERR_HEADERS_REQUIRED = "Size table requires at least one header";
    private static final String ERR_ROWS_REQUIRED = "Size table requires at least one row";

    private @Nullable String name;
    private @Nullable String templateId;
    private final List<String> headers = new ArrayList<>();
    private final List<SizeTableRow> rows = new ArrayList<>();

    SizeTableRequestBuilder() {
    }

    /** Set the table name (required). */
    public SizeTableRequestBuilder name(@Nullable String tableName) {
        this.name = tableName;
        return this;
    }

    /** Set the template id to base the table on (required when creating). */
    public SizeTableRequestBuilder templateId(@Nullable String sourceTemplateId) {
        this.templateId = sourceTemplateId;
        return this;
    }

    /** Append one column header. */
    public SizeTableRequestBuilder header(String columnName) {
        this.headers.add(columnName);
        return this;
    }

    /** Replace all column headers with the given ordered names. */
    public SizeTableRequestBuilder headers(List<String> columnNames) {
        this.headers.clear();
        if (columnNames != null) {
            this.headers.addAll(columnNames);
        }
        return this;
    }

    /** Append one row. */
    public SizeTableRequestBuilder row(SizeTableRow tableRow) {
        this.rows.add(tableRow);
        return this;
    }

    /** Append one row from its ordered cell values. */
    public SizeTableRequestBuilder row(List<String> cells) {
        this.rows.add(SizeTableRow.of(cells));
        return this;
    }

    /** Replace all rows. */
    public SizeTableRequestBuilder rows(List<SizeTableRow> tableRows) {
        this.rows.clear();
        if (tableRows != null) {
            this.rows.addAll(tableRows);
        }
        return this;
    }

    /**
     * Validate and build the immutable request.
     *
     * @throws IllegalStateException if {@code name} is missing or there is no
     *         header or no row
     */
    public SizeTableRequest build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        if (headers.isEmpty()) {
            throw new IllegalStateException(ERR_HEADERS_REQUIRED);
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException(ERR_ROWS_REQUIRED);
        }
        return new SizeTableRequest(name, templateId, headers, rows);
    }
}
