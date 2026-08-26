/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerResponseRaw;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A producer or economic operator responsible for a product's safety (GPSR): its
 * id, name, and registered {@link ProducerData}.
 *
 * @param id the responsible-producer id, or {@code null}
 * @param name the responsible-producer name, or {@code null}
 * @param producerData the registered producer data, or {@code null}
 *
 * @since 0.4.0
 */
public record ResponsibleProducer(
        @Nullable String id,
        @Nullable String name,
        @Nullable ProducerData producerData) {

    /** Map the generated Layer-1 DTO. */
    public static ResponsibleProducer from(ResponsibleProducerResponseRaw raw) {
        UUID rawId = raw.getId();
        return new ResponsibleProducer(
                rawId == null ? null : rawId.toString(),
                raw.getName(),
                ProducerData.from(raw.getProducerData()));
    }
}
