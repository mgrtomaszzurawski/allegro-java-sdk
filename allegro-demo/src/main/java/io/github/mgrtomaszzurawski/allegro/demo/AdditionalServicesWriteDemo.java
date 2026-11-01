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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.ServiceConstraintRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServiceDefinition;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Bucket-K live WRITE probe (#180, TESTING.md §2) for the additional-services group
 * write surface: create a group referencing a real definition from the seller's
 * catalog (or reuse the probe's group from an earlier run — the API has no group
 * delete, so creating one per run would pile them up), read it back through the
 * SDK, rename it with {@code updateGroup} and read the rename back, restore the
 * name, then upsert and delete a per-language translation. Proves
 * {@code createGroup}/{@code group}/{@code updateGroup}/{@code upsertTranslation}/
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
    private static final String ERR_RENAME_ROUND_TRIP = "group name did not round-trip through updateGroup: ";

    private static final String GROUP_NAME = "SDK live-verify group";
    private static final String RENAMED_GROUP_NAME = GROUP_NAME + " renamed";
    private static final String LANGUAGE = "pl-PL";
    // A group is translated into languages OTHER than its own base language.
    private static final String TRANSLATION_LANGUAGE = "en-US";
    private static final String TRANSLATED_DESCRIPTION = "Additional service (SDK verification)";
    private static final String PREFERRED_DEFINITION_ID = "GIFT_WRAP";
    private static final String CONSTRAINT_COUNTRY = "PL";
    private static final String PRICE_AMOUNT = "5.00";
    private static final String PRICE_CURRENCY = "PLN";

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
        List<AdditionalServiceDefinition> definitions = additionalServices.categoryDefinitions().stream()
                .flatMap(category -> category.definitions().stream())
                .filter(candidate -> candidate.id() != null)
                .toList();
        Optional<AdditionalServiceDefinition> definition = definitions.stream()
                .filter(candidate -> PREFERRED_DEFINITION_ID.equals(candidate.id()))
                .findFirst()
                .or(() -> definitions.stream().findFirst());
        if (definition.isEmpty()) {
            System.out.println(MSG_NO_DEFINITION);
            return;
        }
        String definitionId = definition.get().id();
        Money price = Money.of(PRICE_AMOUNT, PRICE_CURRENCY);
        System.out.println("using definition id=" + definitionId
                + ", price=" + price.amount() + " " + price.currency() + " (before-shipping/" + CONSTRAINT_COUNTRY + ")");

        Optional<AdditionalServicesGroup> existing = additionalServices.streamGroups()
                .filter(group -> GROUP_NAME.equals(group.name()))
                .findFirst();
        AdditionalServicesGroup group;
        if (existing.isPresent()) {
            group = existing.get();
            System.out.println("reusing probe group id=" + group.id() + " from an earlier run");
        } else {
            group = additionalServices.createGroup(groupRequest(GROUP_NAME, definitionId, price));
            System.out.println("created group id=" + group.id() + ", services=" + group.services().size());
        }
        String groupId = group.id();

        AdditionalServicesGroup readBack = additionalServices.group(groupId);
        System.out.println("read-back group id=" + readBack.id() + ", name=" + readBack.name()
                + ", services=" + readBack.services().size());

        // updateGroup (PUT, full replace) write→read: rename, read the rename back, restore
        AdditionalServicesGroup renamed = additionalServices.updateGroup(groupId,
                groupRequest(RENAMED_GROUP_NAME, definitionId, price));
        System.out.println("updateGroup ok: name=" + renamed.name() + ", services=" + renamed.services().size());
        AdditionalServicesGroup renamedReadBack = additionalServices.group(groupId);
        try {
            if (!RENAMED_GROUP_NAME.equals(renamed.name()) || !RENAMED_GROUP_NAME.equals(renamedReadBack.name())) {
                throw new IllegalStateException(ERR_RENAME_ROUND_TRIP + renamedReadBack.name());
            }
        } finally {
            AdditionalServicesGroup restored = additionalServices.updateGroup(groupId,
                    groupRequest(GROUP_NAME, definitionId, price));
            System.out.println("updateGroup restored: name=" + restored.name());
        }

        additionalServices.upsertTranslation(groupId, TRANSLATION_LANGUAGE,
                GroupTranslationRequest.builder()
                        .addTranslation(definitionId, TRANSLATED_DESCRIPTION)
                        .build());
        System.out.println("translation upserted for " + TRANSLATION_LANGUAGE);

        additionalServices.deleteTranslation(groupId, TRANSLATION_LANGUAGE);
        System.out.println("translation deleted for " + TRANSLATION_LANGUAGE);

        System.out.println("write-read-ok=true (group " + groupId + " kept on the sandbox account for reuse)");
    }

    private static AdditionalServicesGroupRequest groupRequest(String name, String definitionId, Money price) {
        return AdditionalServicesGroupRequest.builder()
                .name(name)
                .language(LANGUAGE)
                .addService(AdditionalServiceRequest.of(definitionId, GROUP_NAME,
                        ServiceConfigurationRequest.of(price,
                                ServiceConstraintRequest.beforeShipping(CONSTRAINT_COUNTRY))))
                .build();
    }
}
