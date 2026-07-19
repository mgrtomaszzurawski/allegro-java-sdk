/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.compliance;

import io.github.mgrtomaszzurawski.allegro.client.model.CreateResponsibleProducerRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerContactRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerResponseProducerDataRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UpdateResponsibleProducerRequestRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsibleProducerRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Maps the public {@code ResponsibleProducerRequest} onto the generated Layer-1
 * DTOs for create ({@code POST}) and update ({@code PUT}). Package-private:
 * request mapping never leaks out of the endpoint-wrapper layer.
 */
final class ResponsibleProducerMapper {

    private ResponsibleProducerMapper() {
    }

    static CreateResponsibleProducerRequestRaw toCreateRaw(ResponsibleProducerRequest request) {
        CreateResponsibleProducerRequestRaw raw = new CreateResponsibleProducerRequestRaw().name(request.name());
        if (hasProducerData(request)) {
            raw.producerData(toProducerData(request));
        }
        return raw;
    }

    static UpdateResponsibleProducerRequestRaw toUpdateRaw(UUID id, ResponsibleProducerRequest request) {
        UpdateResponsibleProducerRequestRaw raw = new UpdateResponsibleProducerRequestRaw()
                .id(id)
                .name(request.name());
        if (hasProducerData(request)) {
            raw.producerData(toProducerData(request));
        }
        return raw;
    }

    private static ResponsibleProducerResponseProducerDataRaw toProducerData(ResponsibleProducerRequest request) {
        return new ResponsibleProducerResponseProducerDataRaw()
                .tradeName(request.tradeName())
                .address(toAddressRaw(request.address()))
                .contact(toContactRaw(request.contact()));
    }

    private static boolean hasProducerData(ResponsibleProducerRequest request) {
        return request.tradeName() != null || request.address() != null || request.contact() != null;
    }

    private static @Nullable ResponsibleProducerAddressRaw toAddressRaw(@Nullable ResponsiblePartyAddress address) {
        if (address == null) {
            return null;
        }
        return new ResponsibleProducerAddressRaw()
                .countryCode(address.countryCode())
                .street(address.street())
                .postalCode(address.postalCode())
                .city(address.city());
    }

    private static @Nullable ResponsibleProducerContactRaw toContactRaw(@Nullable ResponsiblePartyContact contact) {
        if (contact == null) {
            return null;
        }
        return new ResponsibleProducerContactRaw()
                .email(contact.email())
                .phoneNumber(contact.phoneNumber())
                .formUrl(contact.formUrl());
    }
}
