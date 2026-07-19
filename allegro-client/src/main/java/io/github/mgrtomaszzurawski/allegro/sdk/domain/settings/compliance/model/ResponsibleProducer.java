/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerResponseProducerDataRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerResponseRaw;
import org.jspecify.annotations.Nullable;

/**
 * A product-compliance responsible producer (GPSR) defined in the seller's dictionary.
 *
 * <p>The nested wire {@code producerData} is flattened here: {@link #name} is the
 * internal dictionary label (visible only to the seller), {@link #tradeName} is
 * the producing company's name / trade name.
 *
 * @param id server-assigned identifier
 * @param name internal dictionary label
 * @param tradeName the producing company's name or trade name, or {@code null} when absent
 * @param address the producer's address, or {@code null} when absent
 * @param contact the producer's contact, or {@code null} when absent
 *
 * @since 0.3.0
 */
public record ResponsibleProducer(
        String id,
        @Nullable String name,
        @Nullable String tradeName,
        @Nullable ResponsiblePartyAddress address,
        @Nullable ResponsiblePartyContact contact) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ResponsibleProducer from(ResponsibleProducerResponseRaw raw) {
        ResponsibleProducerResponseProducerDataRaw data = raw.getProducerData();
        String tradeName = data == null ? null : data.getTradeName();
        ResponsiblePartyAddress address = data == null ? null : ResponsiblePartyAddress.from(data.getAddress());
        ResponsiblePartyContact contact = data == null ? null : ResponsiblePartyContact.from(data.getContact());
        return new ResponsibleProducer(raw.getId().toString(), raw.getName(), tradeName, address, contact);
    }
}
