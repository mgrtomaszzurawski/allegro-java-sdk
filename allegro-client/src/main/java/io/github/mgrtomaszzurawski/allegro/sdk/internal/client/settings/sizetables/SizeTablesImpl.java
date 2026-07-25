/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.sizetables;

import io.github.mgrtomaszzurawski.allegro.client.model.PublicTableDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PublicTablesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTableTemplateResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTableTemplatesResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.SizeTables;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder.SizeTableRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTable;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableTemplate;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;
import java.util.Objects;

/**
 * Endpoint wrapper behind the {@link SizeTables} facade.
 *
 * @since 0.3.0
 */
public final class SizeTablesImpl implements SizeTables {

    private static final String OP_LIST = "list size tables";
    private static final String OP_GET = "get size table";
    private static final String OP_TEMPLATES = "list size-table templates";
    private static final String OP_CREATE = "create size table";
    private static final String OP_UPDATE = "update size table";

    private static final String ERR_TABLE_ID_NULL = "tableId must not be null";
    private static final String ERR_REQUEST_NULL = "request must not be null";
    private static final String ERR_TEMPLATE_REQUIRED =
            "Creating a size table requires a templateId; pick one from templates()";

    private final HttpSupport http;

    public SizeTablesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<SizeTable> list() {
        PublicTablesDtoRaw raw = http.request(OP_LIST)
                .get(ApiPaths.SIZE_TABLES)
                .fetch(PublicTablesDtoRaw.class);
        List<PublicTableDtoRaw> tables = raw.getTables() == null ? List.of() : raw.getTables();
        return tables.stream().map(SizeTable::from).toList();
    }

    @Override
    public SizeTable get(String tableId) {
        Objects.requireNonNull(tableId, ERR_TABLE_ID_NULL);
        return SizeTable.from(http.request(OP_GET)
                .get(ApiPaths.subPath(ApiPaths.SIZE_TABLES, tableId))
                .fetch(PublicTableDtoRaw.class));
    }

    @Override
    public List<SizeTableTemplate> templates() {
        SizeTableTemplatesResponseRaw raw = http.request(OP_TEMPLATES)
                .get(ApiPaths.SIZE_TABLES_TEMPLATES)
                .fetch(SizeTableTemplatesResponseRaw.class);
        List<SizeTableTemplateResponseRaw> templates =
                raw.getTemplates() == null ? List.of() : raw.getTemplates();
        return templates.stream().map(SizeTableTemplate::from).toList();
    }

    @Override
    public SizeTable create(SizeTableRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        if (request.templateId() == null || request.templateId().isBlank()) {
            throw new IllegalArgumentException(ERR_TEMPLATE_REQUIRED);
        }
        return SizeTable.from(http.request(OP_CREATE)
                .post(ApiPaths.SIZE_TABLES)
                .jsonBody(SizeTableMapper.toPostRaw(request, request.templateId()))
                .fetch(PublicTableDtoRaw.class));
    }

    @Override
    public SizeTable update(String tableId, SizeTableRequest request) {
        Objects.requireNonNull(tableId, ERR_TABLE_ID_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return SizeTable.from(http.request(OP_UPDATE)
                .put(ApiPaths.subPath(ApiPaths.SIZE_TABLES, tableId))
                .jsonBody(SizeTableMapper.toPutRaw(request))
                .fetch(PublicTableDtoRaw.class));
    }
}
