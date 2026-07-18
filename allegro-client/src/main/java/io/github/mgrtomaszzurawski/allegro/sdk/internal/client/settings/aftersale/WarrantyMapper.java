/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.aftersale;

import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyAttachmentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyPeriodRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyTypeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Maps the public {@link WarrantyRequest} onto the generated Layer-1
 * {@code WarrantyRequestRaw} DTO. Package-private: request mapping never leaks
 * out of the endpoint-wrapper layer.
 */
final class WarrantyMapper {

    private WarrantyMapper() {
    }

    static WarrantyRequestRaw toRaw(WarrantyRequest request) {
        WarrantyRequestRaw raw = new WarrantyRequestRaw()
                .name(request.name())
                .type(toRawType(request.type()))
                .individual(toRawPeriod(request.individual()))
                .corporate(toRawPeriod(request.corporate()))
                .description(request.description());
        String attachmentId = request.attachmentId();
        if (attachmentId != null) {
            raw.attachment(new WarrantyAttachmentRaw()
                    .id(UUID.fromString(attachmentId))
                    .name(request.attachmentName()));
        }
        return raw;
    }

    private static WarrantyTypeRaw toRawType(WarrantyType type) {
        // Domain constant names equal the wire values for this enum.
        return WarrantyTypeRaw.fromValue(type.name());
    }

    private static @Nullable WarrantyPeriodRaw toRawPeriod(@Nullable WarrantyPeriod period) {
        if (period == null) {
            return null;
        }
        return new WarrantyPeriodRaw().period(period.period()).lifetime(period.lifetime());
    }
}
