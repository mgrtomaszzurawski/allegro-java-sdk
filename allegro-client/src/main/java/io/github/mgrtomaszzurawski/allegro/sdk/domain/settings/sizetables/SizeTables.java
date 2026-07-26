/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder.SizeTableRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTable;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableTemplate;
import java.util.List;

/**
 * The seller's size tables and the templates they are built from — reached via
 * {@code AllegroClient.settings().sizeTables()}.
 *
 * <p>A size table is a small grid (named column headers plus rows of cell values)
 * that a seller attaches to clothing and footwear offers. Tables are created from
 * an Allegro {@link SizeTableTemplate}.
 *
 * @since 0.3.0
 */
public interface SizeTables {

    /**
     * List every size table on the seller's account.
     *
     * @return the seller's size tables (empty if none)
     */
    List<SizeTable> list();

    /**
     * Read a single size table.
     *
     * @param tableId the table id
     * @return the size table
     */
    SizeTable get(String tableId);

    /**
     * List the size-table templates Allegro provides.
     *
     * @return the available templates
     */
    List<SizeTableTemplate> templates();

    /**
     * Create a size table from a template.
     *
     * @param request the table to create; its {@code templateId} is required
     * @return the created table, including its assigned id
     */
    SizeTable create(SizeTableRequest request);

    /**
     * Update a size table in place.
     *
     * @param tableId the id of the table to update
     * @param request the new table contents
     * @return the updated table
     */
    SizeTable update(String tableId, SizeTableRequest request);
}
