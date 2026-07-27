/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibilitySuggestionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductSearchRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityList;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import java.io.IOException;
import java.util.List;

/**
 * Bucket E live probe for compatibility-list suggestions. The endpoint is
 * read-only, so this applies the read-only carve-out (TESTING.md §2): it verifies
 * response SHAPE against the live wire. It walks a compatibility-supporting
 * category, finds a product in it, and asks
 * {@code catalog().compatibility().suggestionsFor(forProduct(...))} — confirming the
 * polymorphic {@code MANUAL}/{@code PRODUCT_BASED} body and its items map through
 * the SDK. Uses the stored seller token (and stores the rotated one back).
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=catalog-compatibility-suggestions -Pdemo.account=seller
 * </pre>
 */
final class CatalogCompatibilitySuggestionsDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "catalog-compatibility-suggestions";

    private static final String SEARCH_PHRASE = "filtr oleju";
    private static final int CATEGORY_SAMPLE = 3;
    private static final int PRODUCT_SAMPLE = 5;
    private static final int ITEM_SAMPLE = 5;
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";

    private CatalogCompatibilitySuggestionsDemo() {
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
            probeSuggestions(client);
            String rotated = client.refreshToken();
            if (rotated != null) {
                tokenStore.store(account, rotated);
            }
        }
    }

    private static void probeSuggestions(AllegroClient client) {
        List<CompatibleCategory> categories = client.catalog().compatibility().supportedCategories();
        System.out.println("supportedCategories(): " + categories.size());
        for (CompatibleCategory category : categories.stream().limit(CATEGORY_SAMPLE).toList()) {
            List<ProductSummary> products = client.catalog().products()
                    .search(ProductSearchRequest.builder()
                            .phrase(SEARCH_PHRASE).categoryId(category.categoryId()).build())
                    .limit(PRODUCT_SAMPLE)
                    .toList();
            System.out.println("category " + category.categoryId() + " '" + category.name()
                    + "' (input=" + category.inputType() + "): " + products.size() + " products");
            for (ProductSummary product : products) {
                if (suggestForProduct(client, product)) {
                    return;
                }
            }
        }
        System.out.println("(no product yielded a compatibility suggestion in the sampled categories)");
    }

    /** Returns true when a suggestion mapped, so the caller can stop probing. */
    private static boolean suggestForProduct(AllegroClient client, ProductSummary product) {
        try {
            CompatibilityList list = client.catalog().compatibility()
                    .suggestionsFor(CompatibilitySuggestionRequest.forProduct(product.id()));
            System.out.println("  suggestionsFor(product " + product.id() + " '" + product.name()
                    + "') -> type=" + list.type() + ", id=" + list.id()
                    + ", items=" + list.items().size());
            for (CompatibilityItem item : list.items().stream().limit(ITEM_SAMPLE).toList()) {
                System.out.println("    item type=" + item.type() + ", id=" + item.id()
                        + ", text='" + item.text() + "', additionalInfo=" + item.additionalInfo());
            }
            return true;
        } catch (AllegroException noSuggestion) {
            System.out.println("  suggestionsFor(product " + product.id() + ") -> "
                    + noSuggestion.getClass().getSimpleName());
            return false;
        }
    }
}
