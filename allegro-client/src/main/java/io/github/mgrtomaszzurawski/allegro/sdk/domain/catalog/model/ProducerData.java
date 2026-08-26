/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerResponseProducerDataRaw;
import org.jspecify.annotations.Nullable;

/**
 * The registered data of a product-safety {@link ResponsibleProducer} (GPSR): the
 * trade name plus the {@link ProducerAddress address} and {@link ProducerContact
 * contact} the producer can be reached at.
 *
 * @param tradeName the producer's trade name, or {@code null}
 * @param address the registered address, or {@code null}
 * @param contact the contact details, or {@code null}
 *
 * @since 0.4.0
 */
public record ProducerData(
        @Nullable String tradeName,
        @Nullable ProducerAddress address,
        @Nullable ProducerContact contact) {

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable ProducerData from(@Nullable ResponsibleProducerResponseProducerDataRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ProducerData(
                raw.getTradeName(),
                ProducerAddress.from(raw.getAddress()),
                ProducerContact.from(raw.getContact()));
    }
}
