/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import java.util.List;

/**
 * Bucket E live probe (TESTING.md §2) for vehicle/part compatibility reference
 * data. The supported-categories list is read-only, so this applies the
 * read-only carve-out: it verifies response SHAPE against the live wire — fetch
 * the categories that support a compatibility list and confirm the mapped fields
 * (input type, validation rules) arrive.
 *
 * <p>Like the category reads, this is public reference data, so an app-only
 * client-credentials token suffices; this scenario ignores the stored user token
 * and the {@code account} argument.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=catalog-compatibility
 * </pre>
 */
final class CatalogCompatibilityDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "catalog-compatibility";

    private static final int SAMPLE_LIMIT = 5;
    private static final String ERR_NO_CATEGORIES =
            "sandbox returned no compatibility-supported categories - unexpected for this endpoint";

    private CatalogCompatibilityDemo() {
    }

    static void run(String clientId, String clientSecret, String account) {
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            List<CompatibleCategory> categories = client.catalog().compatibility().supportedCategories();
            System.out.println("supportedCategories(): " + categories.size() + " categories");
            if (categories.isEmpty()) {
                throw new IllegalStateException(ERR_NO_CATEGORIES);
            }
            for (CompatibleCategory category : categories.stream().limit(SAMPLE_LIMIT).toList()) {
                System.out.println("  " + category.categoryId() + " '" + category.name()
                        + "', items=" + category.itemsType()
                        + ", input=" + category.inputType()
                        + ", rules=" + category.validationRules());
            }
        }
    }
}
