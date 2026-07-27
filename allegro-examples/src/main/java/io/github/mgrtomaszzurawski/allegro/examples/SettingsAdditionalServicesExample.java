/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.AdditionalServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.GroupTranslations;

/**
 * Compile-only twin of the {@code docs/settings.md} additional-services snippets:
 * read the definition catalog and the seller's groups (with a single-group read
 * and its translations).
 */
final class SettingsAdditionalServicesExample {

    private SettingsAdditionalServicesExample() {
    }

    static long countDefinitions(AllegroClient client) {
        AdditionalServices additional = client.settings().additionalServices();
        return additional.categoryDefinitions().stream()
                .mapToLong(category -> category.definitions().size())
                .sum();
    }

    static String readGroup(AllegroClient client, String groupId) {
        AdditionalServices additional = client.settings().additionalServices();
        long groups = additional.streamGroups().count();
        AdditionalServicesGroup group = additional.group(groupId);
        GroupTranslations translations = additional.translations(groupId);
        return group.name() + " (" + group.services().size() + " services, "
                + translations.translations().size() + " translations, " + groups + " groups total)";
    }
}
