/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.CatalogCategories;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Category;
import java.util.List;

/**
 * Bucket E live probe (TESTING.md §2). The catalogue is read-only reference
 * data, so this scenario applies the read-only carve-out: instead of write→read
 * it verifies response SHAPE against the live wire — navigate the category tree
 * ({@code roots} → {@code childrenOf} → {@code get}) and confirm the mapped
 * fields actually arrive.
 *
 * <p>Category reads are public data, so an app-only client-credentials token
 * suffices; this scenario ignores the stored user token and the {@code account}
 * argument.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=catalog-categories
 * </pre>
 */
public final class CatalogDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "catalog-categories";

    private static final String ERR_NO_CATEGORIES =
            "sandbox returned no root categories - unexpected, catalogue should never be empty";

    private CatalogDemo() {
    }

    static void run(String clientId, String clientSecret, String account) {
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            CatalogCategories categories = client.catalog().categories();

            List<Category> roots = categories.roots();
            System.out.println("roots(): " + roots.size() + " top-level categories");
            if (roots.isEmpty()) {
                throw new IllegalStateException(ERR_NO_CATEGORIES);
            }

            Category firstRoot = roots.get(0);
            List<Category> children = categories.childrenOf(firstRoot.id());
            System.out.println("childrenOf(" + firstRoot.id() + "): " + children.size() + " children");

            // Probe a leaf where possible - that is where options/parameters matter.
            String probeId = children.isEmpty() ? firstRoot.id() : children.get(0).id();
            Category fetched = categories.get(probeId);
            System.out.println("get(" + fetched.id() + "): name='" + fetched.name()
                    + "', leaf=" + fetched.leaf()
                    + ", optionsPresent=" + (fetched.options() != null));
        }
    }
}
