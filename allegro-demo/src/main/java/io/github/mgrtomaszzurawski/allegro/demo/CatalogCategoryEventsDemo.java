/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CategoryEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryEvent;
import java.util.List;

/**
 * Bucket E live probe (TESTING.md §2) for the category-change feed. The feed is
 * read-only global reference data, so — like the other catalogue reads — an app-only
 * client-credentials token suffices; this scenario ignores the stored user token and
 * the {@code account} argument. It applies the read-only carve-out: verify response
 * SHAPE against the live wire — take a lazy sample of the change feed and confirm the
 * mapped fields (type, occurredAt, affected category) arrive.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=catalog-category-events
 * </pre>
 */
final class CatalogCategoryEventsDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "catalog-category-events";

    private static final int SAMPLE_LIMIT = 10;

    private CatalogCategoryEventsDemo() {
    }

    static void run(String clientId, String clientSecret, String account) {
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            // Lazy stream: only the pages needed for SAMPLE_LIMIT are fetched.
            List<CategoryEvent> sample = client.catalog().categories()
                    .streamChanges(CategoryEventFilter.all())
                    .limit(SAMPLE_LIMIT)
                    .toList();
            System.out.println("streamChanges(all): " + sample.size() + " (lazy take of the feed)");
            for (CategoryEvent event : sample) {
                String categoryId = event.category() == null ? "<none>" : event.category().id();
                String categoryName = event.category() == null ? "" : event.category().name();
                System.out.println("  " + event.occurredAt() + "  " + event.type()
                        + "  id=" + event.id() + "  category=" + categoryId + " '" + categoryName
                        + "'" + (event.redirectCategoryId() == null
                                ? "" : "  -> " + event.redirectCategoryId()));
            }
        }
    }
}
