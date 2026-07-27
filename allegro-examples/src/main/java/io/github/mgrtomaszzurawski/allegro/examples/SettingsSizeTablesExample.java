/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.SizeTables;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder.SizeTableRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTable;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableTemplate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model.TaxSettings;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/settings.md} size-tables and tax snippets:
 * create a size table from a template, read it back, and read a category's tax
 * settings.
 */
final class SettingsSizeTablesExample {

    private SettingsSizeTablesExample() {
    }

    static String createFromFirstTemplate(AllegroClient client) {
        SizeTables sizeTables = client.settings().sizeTables();
        SizeTableTemplate template = sizeTables.templates().get(0);
        SizeTableRequest request = SizeTableRequest.builder()
                .name("My shoes size table")
                .templateId(template.id())
                .headers(template.headers())
                .row(template.rows().get(0))
                .build();
        SizeTable created = sizeTables.create(request);
        SizeTable readBack = sizeTables.get(created.id());
        return readBack.name() + " (" + readBack.rows().size() + " rows)";
    }

    static long countTables(AllegroClient client) {
        List<SizeTable> tables = client.settings().sizeTables().list();
        return tables.size();
    }

    static long countVatRates(AllegroClient client, String categoryId) {
        TaxSettings settings = client.settings().taxSettings(categoryId);
        return settings.rates().stream().mapToLong(rate -> rate.values().size()).sum();
    }
}
