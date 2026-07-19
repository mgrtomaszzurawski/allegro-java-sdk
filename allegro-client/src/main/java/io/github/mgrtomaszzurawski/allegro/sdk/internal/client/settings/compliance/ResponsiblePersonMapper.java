/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.compliance;

import io.github.mgrtomaszzurawski.allegro.client.model.CreateResponsiblePersonRequestPersonalDataRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CreateResponsiblePersonRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonContactRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonResponsePersonalDataRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UpdateResponsiblePersonRequestRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsiblePersonRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Maps the public {@code ResponsiblePersonRequest} onto the generated Layer-1
 * DTOs for create ({@code POST}) and update ({@code PUT}). Package-private:
 * request mapping never leaks out of the endpoint-wrapper layer.
 */
final class ResponsiblePersonMapper {

    private ResponsiblePersonMapper() {
    }

    static CreateResponsiblePersonRequestRaw toCreateRaw(ResponsiblePersonRequest request) {
        CreateResponsiblePersonRequestRaw raw = new CreateResponsiblePersonRequestRaw().name(request.name());
        if (hasPersonalData(request)) {
            raw.personalData(new CreateResponsiblePersonRequestPersonalDataRaw()
                    .name(request.personName())
                    .address(toAddressRaw(request.address()))
                    .contact(toContactRaw(request.contact())));
        }
        return raw;
    }

    static UpdateResponsiblePersonRequestRaw toUpdateRaw(UUID id, ResponsiblePersonRequest request) {
        UpdateResponsiblePersonRequestRaw raw = new UpdateResponsiblePersonRequestRaw()
                .id(id)
                .name(request.name());
        if (hasPersonalData(request)) {
            raw.personalData(new ResponsiblePersonResponsePersonalDataRaw()
                    .name(request.personName())
                    .address(toAddressRaw(request.address()))
                    .contact(toContactRaw(request.contact())));
        }
        return raw;
    }

    private static boolean hasPersonalData(ResponsiblePersonRequest request) {
        return request.personName() != null || request.address() != null || request.contact() != null;
    }

    private static @Nullable ResponsiblePersonAddressRaw toAddressRaw(@Nullable ResponsiblePartyAddress address) {
        if (address == null) {
            return null;
        }
        ResponsiblePersonAddressRaw raw = new ResponsiblePersonAddressRaw()
                .street(address.street())
                .postalCode(address.postalCode())
                .city(address.city());
        if (address.countryCode() != null) {
            raw.countryCode(ResponsiblePersonAddressRaw.CountryCodeEnum.fromValue(address.countryCode()));
        }
        return raw;
    }

    private static @Nullable ResponsiblePersonContactRaw toContactRaw(@Nullable ResponsiblePartyContact contact) {
        if (contact == null) {
            return null;
        }
        return new ResponsiblePersonContactRaw()
                .email(contact.email())
                .phoneNumber(contact.phoneNumber())
                .formUrl(contact.formUrl());
    }
}
