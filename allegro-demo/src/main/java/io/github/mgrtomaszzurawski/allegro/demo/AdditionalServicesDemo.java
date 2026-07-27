/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.AdditionalServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServiceCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.GroupTranslations;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Bucket-K read verification for additional services: read the definition catalog
 * and the seller's groups (with a single-group read + translations when one
 * exists). Read-only — this slice ships no writes. Seller-only.
 *
 * <p>Run:
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=settings-additional-services -Pdemo.account=seller}.
 */
final class AdditionalServicesDemo {

    static final String SCENARIO = "settings-additional-services";

    private static final String MSG_NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final int EXIT_NO_TOKEN = 2;

    private AdditionalServicesDemo() {
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
                probe(client.settings().additionalServices());
            } finally {
                persistRotatedToken(tokenStore, account, client);
            }
        }
    }

    private static void probe(AdditionalServices additionalServices) {
        List<AdditionalServiceCategory> categories = additionalServices.categoryDefinitions();
        long definitionCount = categories.stream().mapToLong(category -> category.definitions().size()).sum();
        System.out.println("definition categories=" + categories.size() + ", definitions=" + definitionCount);
        categories.stream().flatMap(category -> category.definitions().stream()).findFirst()
                .ifPresent(definition -> System.out.println("first definition id=" + definition.id()));

        List<AdditionalServicesGroup> groups = additionalServices.streamGroups().toList();
        System.out.println("groups=" + groups.size());
        Optional<AdditionalServicesGroup> first = groups.stream().findFirst();
        if (first.isPresent()) {
            String groupId = first.get().id();
            AdditionalServicesGroup readBack = additionalServices.group(groupId);
            GroupTranslations translations = additionalServices.translations(groupId);
            System.out.println("group id=" + readBack.id() + ", services=" + readBack.services().size()
                    + ", translations=" + translations.translations().size());
        }
        System.out.println("read-ok=true");
    }

    private static void persistRotatedToken(SharedTokenStore tokenStore, String account,
            AllegroClient client) throws IOException {
        String rotatedRefreshToken = client.refreshToken();
        if (rotatedRefreshToken != null) {
            tokenStore.store(account, rotatedRefreshToken);
        }
    }
}
