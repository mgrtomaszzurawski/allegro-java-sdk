/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.Compliance;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsiblePersonRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsibleProducerRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePerson;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsibleProducer;

/**
 * Compile-only twin of the {@code docs/settings.md} product-compliance snippets:
 * create a responsible person and a responsible producer, then read them back.
 */
final class SettingsComplianceExample {

    private SettingsComplianceExample() {
    }

    static String createResponsiblePerson(AllegroClient client) {
        Compliance compliance = client.settings().compliance();

        ResponsiblePersonRequest request = ResponsiblePersonRequest.builder()
                .name("Person responsible for batteries")
                .personName("Responsible Person Sp. z o.o.")
                .address(new ResponsiblePartyAddress("PL", "Grunwaldzka 182", "60-166", "Poznań"))
                .contact(new ResponsiblePartyContact("compliance@example.com", null, null))
                .build();

        ResponsiblePerson created = compliance.createResponsiblePerson(request);
        long total = compliance.streamResponsiblePersons().count();
        return created.name() + " (" + total + " defined)";
    }

    static String createResponsibleProducer(AllegroClient client) {
        Compliance compliance = client.settings().compliance();

        ResponsibleProducerRequest request = ResponsibleProducerRequest.builder()
                .name("Company responsible for producing product")
                .tradeName("Responsible Producer Sp. z o.o.")
                .address(new ResponsiblePartyAddress("PL", "Grunwaldzka 182", "60-166", "Poznań"))
                .contact(new ResponsiblePartyContact("compliance@example.com", null, null))
                .build();

        ResponsibleProducer created = compliance.createResponsibleProducer(request);
        ResponsibleProducer readBack = compliance.responsibleProducer(created.id());
        return readBack.name() + " / " + readBack.tradeName();
    }
}
