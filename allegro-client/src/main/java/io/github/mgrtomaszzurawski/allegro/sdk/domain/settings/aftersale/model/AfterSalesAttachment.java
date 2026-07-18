/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesAttachmentRaw;
import org.jspecify.annotations.Nullable;

/**
 * A file attached to an after-sale condition (e.g. a warranty document), as it
 * is returned by the server.
 *
 * @param id attachment identifier
 * @param name file name, or {@code null} when the server omits it
 * @param url direct download link, or {@code null} when not yet available
 *
 * @since 0.2.0
 */
public record AfterSalesAttachment(String id, @Nullable String name, @Nullable String url) {

    /** Map the generated Layer-1 DTO, or {@code null} when the field is absent. */
    public static @Nullable AfterSalesAttachment from(@Nullable AfterSalesServicesAttachmentRaw raw) {
        if (raw == null) {
            return null;
        }
        return new AfterSalesAttachment(
                raw.getId().toString(),
                raw.getName(),
                raw.getUrl());
    }
}
