/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.AdditionalServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.AdditionalServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.AdditionalServicesGroupRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.GroupTranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.ServiceConfigurationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServiceCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServiceDefinition;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import java.io.IOException;
import java.util.Optional;

/**
 * Bucket-K live WRITE probe (#180, TESTING.md §2) for the additional-services group
 * write surface: create a group referencing a real definition from the seller's
 * catalog, read it back through the SDK, then upsert and delete a per-language
 * translation. Proves {@code createGroup}/{@code group}/{@code upsertTranslation}/
 * {@code deleteTranslation} against the live wire. Seller-only.
 *
 * <p>Run:
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=settings-additional-services-write -Pdemo.account=seller}.
 */
final class AdditionalServicesWriteDemo {

    static final String SCENARIO = "settings-additional-services-write";

    private static final String MSG_NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String MSG_NO_DEFINITION =
            "No additional-service definition available on the account - cannot build a group";

    private static final String GROUP_NAME = "SDK live-verify group";
    private static final String LANGUAGE = "pl-PL";
    private static final String TRANSLATED_DESCRIPTION = "Usluga dodatkowa (weryfikacja SDK)";
    private static final String FALLBACK_AMOUNT = "1.00";
    private static final String FALLBACK_CURRENCY = "PLN";

    private AdditionalServicesWriteDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(MSG_NO_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                probe(client.settings().additionalServices());
            } finally {
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void probe(AdditionalServices additionalServices) {
        Optional<AdditionalServiceDefinition> definition = additionalServices.categoryDefinitions().stream()
                .flatMap(category -> category.definitions().stream())
                .filter(candidate -> candidate.id() != null)
                .findFirst();
        if (definition.isEmpty()) {
            System.out.println(MSG_NO_DEFINITION);
            return;
        }
        String definitionId = definition.get().id();
        Money price = priceFor(definition.get());
        System.out.println("using definition id=" + definitionId + ", price=" + price.amount() + " " + price.currency());

        AdditionalServicesGroup created = additionalServices.createGroup(
                AdditionalServicesGroupRequest.builder()
                        .name(GROUP_NAME)
                        .language(LANGUAGE)
                        .addService(AdditionalServiceRequest.of(definitionId, GROUP_NAME,
                                ServiceConfigurationRequest.of(price)))
                        .build());
        System.out.println("created group id=" + created.id() + ", services=" + created.services().size());

        AdditionalServicesGroup readBack = additionalServices.group(created.id());
        System.out.println("read-back group id=" + readBack.id() + ", name=" + readBack.name()
                + ", services=" + readBack.services().size());

        additionalServices.upsertTranslation(created.id(), LANGUAGE,
                GroupTranslationRequest.builder()
                        .addTranslation(definitionId, TRANSLATED_DESCRIPTION)
                        .build());
        System.out.println("translation upserted for " + LANGUAGE);

        additionalServices.deleteTranslation(created.id(), LANGUAGE);
        System.out.println("translation deleted for " + LANGUAGE);

        System.out.println("write-read-ok=true (group " + created.id() + " left on the sandbox account)");
    }

    private static Money priceFor(AdditionalServiceDefinition definition) {
        Money maxPrice = definition.maxPrice();
        if (maxPrice != null) {
            return maxPrice;
        }
        return Money.of(FALLBACK_AMOUNT, FALLBACK_CURRENCY);
    }
}
