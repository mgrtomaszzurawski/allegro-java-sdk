/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductSafetyDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductSafetyDtoSafetyInformationRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Product-safety information required under the EU General Product Safety
 * Regulation (GPSR): the {@link ResponsibleProducer responsible producers} /
 * economic operators, and a free-text safety notice.
 *
 * @param responsibleProducers the responsible producers, in order; never
 *     {@code null}, possibly empty
 * @param safetyInformation the free-text safety notice, or {@code null} (the
 *     underlying value is a text-or-attachments choice; this exposes the textual
 *     description of the text variant)
 *
 * @since 0.4.0
 */
public record ProductSafety(
        List<ResponsibleProducer> responsibleProducers,
        @Nullable String safetyInformation) {

    /** Canonical constructor — defensively copies the responsible producers. */
    public ProductSafety {
        responsibleProducers = responsibleProducers == null ? List.of() : List.copyOf(responsibleProducers);
    }

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable ProductSafety from(@Nullable SaleProductSafetyDtoRaw raw) {
        if (raw == null) {
            return null;
        }
        List<ResponsibleProducer> producers = raw.getResponsibleProducers() == null ? List.of()
                : raw.getResponsibleProducers().stream().map(ResponsibleProducer::from).toList();
        return new ProductSafety(producers, safetyInformationText(raw.getSafetyInformation()));
    }

    private static @Nullable String safetyInformationText(
            @Nullable SaleProductSafetyDtoSafetyInformationRaw raw) {
        if (raw == null) {
            return null;
        }
        if (raw.getTextSafetyInformationRaw() != null) {
            return raw.getTextSafetyInformationRaw().getDescription();
        }
        return null;
    }
}
