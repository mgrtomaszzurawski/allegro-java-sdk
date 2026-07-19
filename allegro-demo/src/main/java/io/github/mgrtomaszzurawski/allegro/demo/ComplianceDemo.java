/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.Compliance;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsiblePersonRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsibleProducerRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePerson;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsibleProducer;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import java.io.IOException;
import java.util.Optional;

/**
 * Bucket-K write→read verification (TESTING.md §2) for product compliance: create
 * (or reuse) a responsible person and a responsible producer through the SDK, then
 * read them back and assert the round-trip. These dictionaries have no DELETE, so
 * the probe reuses a single definition per name. Seller-only.
 *
 * <p>Run:
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=settings-compliance -Pdemo.account=seller}.
 */
final class ComplianceDemo {

    static final String SCENARIO = "settings-compliance";

    private static final String DEMO_PREFIX = "[K-demo] ";
    private static final String PERSON_NAME = DEMO_PREFIX + "responsible person";
    private static final String PERSON_LEGAL_NAME = "Responsible Person Sp. z o.o.";
    private static final String PRODUCER_NAME = DEMO_PREFIX + "responsible producer";
    private static final String PRODUCER_TRADE_NAME = "Responsible Producer Sp. z o.o.";
    private static final String CONTACT_EMAIL = "compliance@example.com";
    private static final ResponsiblePartyAddress DEMO_ADDRESS =
            new ResponsiblePartyAddress("PL", "Grunwaldzka 182", "60-166", "Poznań");
    private static final ResponsiblePartyContact DEMO_CONTACT =
            new ResponsiblePartyContact(CONTACT_EMAIL, null, null);

    private static final String MSG_NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final int EXIT_NO_TOKEN = 2;

    private ComplianceDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(MSG_NO_TOKEN.formatted(account));
            System.exit(EXIT_NO_TOKEN);
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                Compliance compliance = client.settings().compliance();
                boolean personOk = verifyPerson(compliance);
                boolean producerOk = verifyProducer(compliance);
                System.out.println("round-trip-ok=" + (personOk && producerOk)
                        + " (person=" + personOk + ", producer=" + producerOk + ")");
            } catch (AllegroBadRequestException rejection) {
                printFieldErrors(rejection);
                throw rejection;
            } finally {
                persistRotatedToken(tokenStore, account, client);
            }
        }
    }

    private static boolean verifyPerson(Compliance compliance) {
        ResponsiblePersonRequest request = ResponsiblePersonRequest.builder()
                .name(PERSON_NAME)
                .personName(PERSON_LEGAL_NAME)
                .address(DEMO_ADDRESS)
                .contact(DEMO_CONTACT)
                .build();
        Optional<ResponsiblePerson> existing = compliance.streamResponsiblePersons()
                .filter(person -> PERSON_NAME.equals(person.name()))
                .findFirst();
        ResponsiblePerson written = existing.isPresent()
                ? compliance.updateResponsiblePerson(existing.get().id(), request)
                : compliance.createResponsiblePerson(request);
        System.out.println((existing.isPresent() ? "updated" : "created") + " person: id=" + written.id());
        ResponsiblePerson readBack = compliance.streamResponsiblePersons()
                .filter(person -> person.id().equals(written.id()))
                .findFirst()
                .orElseThrow();
        return PERSON_NAME.equals(readBack.name()) && PERSON_LEGAL_NAME.equals(readBack.personName());
    }

    private static boolean verifyProducer(Compliance compliance) {
        ResponsibleProducerRequest request = ResponsibleProducerRequest.builder()
                .name(PRODUCER_NAME)
                .tradeName(PRODUCER_TRADE_NAME)
                .address(DEMO_ADDRESS)
                .contact(DEMO_CONTACT)
                .build();
        Optional<ResponsibleProducer> existing = compliance.streamResponsibleProducers()
                .filter(producer -> PRODUCER_NAME.equals(producer.name()))
                .findFirst();
        ResponsibleProducer written = existing.isPresent()
                ? compliance.updateResponsibleProducer(existing.get().id(), request)
                : compliance.createResponsibleProducer(request);
        System.out.println((existing.isPresent() ? "updated" : "created") + " producer: id=" + written.id());
        ResponsibleProducer readBack = compliance.responsibleProducer(written.id());
        return PRODUCER_NAME.equals(readBack.name()) && PRODUCER_TRADE_NAME.equals(readBack.tradeName());
    }

    private static void printFieldErrors(AllegroBadRequestException rejection) {
        for (AllegroFieldError fieldError : rejection.errors()) {
            System.out.println("field-error: code=" + fieldError.code()
                    + ", path=" + fieldError.path()
                    + ", message=" + fieldError.message());
        }
    }

    private static void persistRotatedToken(SharedTokenStore tokenStore, String account,
            AllegroClient client) throws IOException {
        String rotatedRefreshToken = client.refreshToken();
        if (rotatedRefreshToken != null) {
            tokenStore.store(account, rotatedRefreshToken);
        }
    }
}
