/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonResponsePersonalDataRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonResponseRaw;
import org.jspecify.annotations.Nullable;

/**
 * A product-compliance responsible person (GPSR) defined in the seller's dictionary.
 *
 * <p>The nested wire {@code personalData} is flattened here: {@link #name} is the
 * internal dictionary label (visible only to the seller), {@link #personName} is
 * the responsible person's own name.
 *
 * @param id server-assigned identifier
 * @param name internal dictionary label
 * @param personName the responsible person's name, or {@code null} when absent
 * @param address the responsible person's address, or {@code null} when absent
 * @param contact the responsible person's contact, or {@code null} when absent
 *
 * @since 0.3.0
 */
public record ResponsiblePerson(
        String id,
        @Nullable String name,
        @Nullable String personName,
        @Nullable ResponsiblePartyAddress address,
        @Nullable ResponsiblePartyContact contact) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ResponsiblePerson from(ResponsiblePersonResponseRaw raw) {
        ResponsiblePersonResponsePersonalDataRaw data = raw.getPersonalData();
        String personName = data == null ? null : data.getName();
        ResponsiblePartyAddress address = data == null ? null : ResponsiblePartyAddress.from(data.getAddress());
        ResponsiblePartyContact contact = data == null ? null : ResponsiblePartyContact.from(data.getContact());
        return new ResponsiblePerson(raw.getId().toString(), raw.getName(), personName, address, contact);
    }
}
