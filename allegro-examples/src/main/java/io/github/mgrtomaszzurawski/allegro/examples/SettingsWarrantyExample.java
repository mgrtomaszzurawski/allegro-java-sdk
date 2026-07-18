/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ImpliedWarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;

/**
 * Compile-only twin of the {@code docs/settings.md} after-sale snippets — if the
 * documented after-sale API stops compiling, this module breaks the build.
 */
public final class SettingsWarrantyExample {

    private SettingsWarrantyExample() {
    }

    static String createAndReadWarranty(AllegroClient client) {
        AfterSaleConditions afterSale = client.settings().afterSale();

        WarrantyRequest request = WarrantyRequest.builder()
                .name("2 year seller warranty")
                .type(WarrantyType.SELLER)
                .individual(WarrantyPeriod.of("P24M"))
                .corporate(WarrantyPeriod.lifetimeWarranty())
                .description("Covers manufacturing defects")
                .build();

        Warranty created = afterSale.createWarranty(request);
        Warranty readBack = afterSale.warranty(created.id());
        long total = afterSale.streamWarranties().count();
        return readBack.name() + " (" + total + " defined)";
    }

    static String createAndReadImpliedWarranty(AllegroClient client) {
        AfterSaleConditions afterSale = client.settings().afterSale();

        ImpliedWarrantyRequest request = ImpliedWarrantyRequest.builder()
                .name("2 year implied warranty")
                .individual(ImpliedWarrantyPeriod.of("P2Y"))
                .corporate(ImpliedWarrantyPeriod.of("P1Y"))
                .address(new AfterSalesAddress(
                        "Allegro sp. z o.o.", "Grunwaldzka 182", "60-166", "Poznań", "PL"))
                .description("Statutory warranty of conformity")
                .build();

        ImpliedWarranty created = afterSale.createImpliedWarranty(request);
        ImpliedWarranty readBack = afterSale.impliedWarranty(created.id());
        long total = afterSale.streamImpliedWarranties().count();
        return readBack.name() + " (" + total + " defined)";
    }
}
