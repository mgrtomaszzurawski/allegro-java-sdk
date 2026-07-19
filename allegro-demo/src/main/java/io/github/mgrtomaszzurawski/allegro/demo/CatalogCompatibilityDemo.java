/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import java.io.IOException;
import java.util.List;

/**
 * Bucket E live probe for vehicle/part compatibility reference data. The
 * supported-categories list is read-only, so this applies the read-only
 * carve-out (TESTING.md §2): it verifies response SHAPE against the live wire —
 * fetch the categories that support a compatibility list and confirm the mapped
 * fields (input type, validation rules) arrive.
 * {@code /sale/compatibility-list/supported-categories} requires the
 * {@code sale:offers:read} scope, so this uses the stored seller token (and
 * stores the rotated one back, never rotating ad hoc).
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=catalog-compatibility -Pdemo.account=seller
 * </pre>
 */
final class CatalogCompatibilityDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "catalog-compatibility";

    private static final int SAMPLE_LIMIT = 5;
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";

    private CatalogCompatibilityDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println("(stored token expired - rerun auth-bootstrap)"),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            List<CompatibleCategory> categories = client.catalog().compatibility().supportedCategories();
            System.out.println("supportedCategories(): " + categories.size() + " categories");
            for (CompatibleCategory category : categories.stream().limit(SAMPLE_LIMIT).toList()) {
                System.out.println("  " + category.categoryId() + " '" + category.name()
                        + "', items=" + category.itemsType()
                        + ", input=" + category.inputType()
                        + ", rules=" + category.validationRules());
            }
            rotateToken(tokenStore, account, client);
        }
    }

    private static void rotateToken(SharedTokenStore tokenStore, String account,
            AllegroClient client) throws IOException {
        String rotated = client.refreshToken();
        if (rotated != null) {
            tokenStore.store(account, rotated);
        }
    }
}
