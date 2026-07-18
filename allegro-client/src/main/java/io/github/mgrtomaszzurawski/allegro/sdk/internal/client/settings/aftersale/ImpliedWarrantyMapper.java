/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.aftersale;

import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyPeriodRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyRequestRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ImpliedWarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantyPeriod;
import org.jspecify.annotations.Nullable;

/**
 * Maps the public {@link ImpliedWarrantyRequest} onto the generated Layer-1
 * {@code ImpliedWarrantyRequestRaw} DTO. Package-private: request mapping never
 * leaks out of the endpoint-wrapper layer.
 */
final class ImpliedWarrantyMapper {

    private ImpliedWarrantyMapper() {
    }

    static ImpliedWarrantyRequestRaw toRaw(ImpliedWarrantyRequest request) {
        return new ImpliedWarrantyRequestRaw()
                .name(request.name())
                .individual(toRawPeriod(request.individual()))
                .corporate(toRawPeriod(request.corporate()))
                .address(toRawAddress(request.address()))
                .description(request.description());
    }

    private static @Nullable ImpliedWarrantyPeriodRaw toRawPeriod(@Nullable ImpliedWarrantyPeriod period) {
        if (period == null) {
            return null;
        }
        return new ImpliedWarrantyPeriodRaw().period(period.period());
    }

    private static @Nullable AfterSalesServicesAddressRaw toRawAddress(@Nullable AfterSalesAddress address) {
        if (address == null) {
            return null;
        }
        return new AfterSalesServicesAddressRaw()
                .name(address.name())
                .street(address.street())
                .postCode(address.postCode())
                .city(address.city())
                .countryCode(address.countryCode());
    }
}
