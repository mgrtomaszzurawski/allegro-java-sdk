/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.sizetables;

import io.github.mgrtomaszzurawski.allegro.client.model.CellsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.HeaderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTablePostRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTablePutRequestRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder.SizeTableRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableRow;
import java.util.List;

/**
 * Maps the public {@code SizeTableRequest} onto the generated Layer-1 DTOs for
 * create ({@code POST}, template-bearing) and update ({@code PUT}, template-less).
 * Package-private: request mapping never leaks out of the endpoint-wrapper layer.
 */
final class SizeTableMapper {

    private SizeTableMapper() {
    }

    static SizeTablePostRequestRaw toPostRaw(SizeTableRequest request, String templateId) {
        return new SizeTablePostRequestRaw()
                .name(request.name())
                .template(new JustIdRaw().id(templateId))
                .headers(toHeaderRaws(request))
                .values(toCellRaws(request));
    }

    static SizeTablePutRequestRaw toPutRaw(SizeTableRequest request) {
        return new SizeTablePutRequestRaw()
                .name(request.name())
                .headers(toHeaderRaws(request))
                .values(toCellRaws(request));
    }

    private static List<HeaderRaw> toHeaderRaws(SizeTableRequest request) {
        return request.headers().stream().map(name -> new HeaderRaw().name(name)).toList();
    }

    private static List<CellsRaw> toCellRaws(SizeTableRequest request) {
        return request.rows().stream().map(SizeTableMapper::toCellsRaw).toList();
    }

    private static CellsRaw toCellsRaw(SizeTableRow tableRow) {
        return new CellsRaw().cells(tableRow.cells());
    }
}
