/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductSearchRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductSummary;
import java.io.IOException;
import java.util.List;

/**
 * Bucket E live probe for product search. The product database is read-only, so
 * this applies the read-only carve-out (TESTING.md §2): it verifies response
 * SHAPE against the live wire — search a common phrase, take a lazy sample, and
 * confirm the mapped summary fields arrive. {@code /sale/products} requires the
 * {@code sale:offers:read} scope, so this uses the stored seller token (and
 * stores the rotated one back, never rotating ad hoc).
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=catalog-products -Pdemo.account=seller
 * </pre>
 */
final class CatalogProductsDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "catalog-products";

    private static final String SEARCH_PHRASE = "iphone";
    private static final int SAMPLE_LIMIT = 3;
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";

    private CatalogProductsDemo() {
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
            // Lazy stream: only the pages needed for SAMPLE_LIMIT are fetched.
            List<ProductSummary> sample = client.catalog().products()
                    .search(ProductSearchRequest.byPhrase(SEARCH_PHRASE))
                    .limit(SAMPLE_LIMIT)
                    .toList();
            System.out.println("search('" + SEARCH_PHRASE + "') sample: " + sample.size()
                    + " (lazy take of many)");
            for (ProductSummary summary : sample) {
                System.out.println("  " + summary.id() + " '" + summary.name()
                        + "', category=" + summary.categoryId()
                        + ", status=" + summary.publicationStatus()
                        + ", images=" + summary.imageUrls().size());
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
