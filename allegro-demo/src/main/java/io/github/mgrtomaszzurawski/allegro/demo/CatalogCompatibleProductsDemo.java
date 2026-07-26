/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibleProductGroupsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibleProductsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleProductGroup;
import java.util.List;

/**
 * Bucket E live probe (TESTING.md §2) for the compatible-products database. These
 * reads are global reference data, so — like the supported-categories read — an
 * app-only client-credentials token suffices (verified: they return 200, not
 * 401/403); this scenario ignores the stored user token and the {@code account}
 * argument. It applies the read-only carve-out: verify response SHAPE against the
 * live wire — list groups for a type, then products in a group, and confirm the
 * mapped fields (id, text, group id, attributes) arrive.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=catalog-compatible-products
 * </pre>
 */
final class CatalogCompatibleProductsDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "catalog-compatible-products";

    private static final String PROBE_TYPE = "CAR";
    private static final int GROUP_SAMPLE = 3;
    private static final int PRODUCT_SAMPLE = 3;
    private static final int ATTR_SAMPLE = 4;
    private static final String ERR_NO_GROUPS =
            "sandbox returned no compatible-product groups for type " + PROBE_TYPE;

    private CatalogCompatibleProductsDemo() {
    }

    static void run(String clientId, String clientSecret, String account) {
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            List<CompatibleProductGroup> groups = client.catalog().compatibility()
                    .productGroups(CompatibleProductGroupsFilter.ofType(PROBE_TYPE))
                    .limit(GROUP_SAMPLE)
                    .toList();
            System.out.println("productGroups(type=" + PROBE_TYPE + ") sample: " + groups.size());
            if (groups.isEmpty()) {
                throw new IllegalStateException(ERR_NO_GROUPS);
            }
            for (CompatibleProductGroup group : groups) {
                System.out.println("  group " + group.id() + " '" + group.text() + "'");
            }
            probeProducts(client, groups.get(0).id());
        }
    }

    private static void probeProducts(AllegroClient client, String groupId) {
        List<CompatibleProduct> products = client.catalog().compatibility()
                .products(CompatibleProductsFilter.builder().type(PROBE_TYPE).groupId(groupId).build())
                .limit(PRODUCT_SAMPLE)
                .toList();
        System.out.println("products(type=" + PROBE_TYPE + ", group.id=" + groupId + ") sample: "
                + products.size());
        for (CompatibleProduct product : products) {
            System.out.println("  " + product.id() + " group=" + product.groupId()
                    + " attrs=" + product.attributes().size() + " '" + product.text() + "'");
            product.attributes().stream().limit(ATTR_SAMPLE).forEach(attribute ->
                    System.out.println("      " + attribute.id() + "=" + attribute.values()));
        }
    }
}
