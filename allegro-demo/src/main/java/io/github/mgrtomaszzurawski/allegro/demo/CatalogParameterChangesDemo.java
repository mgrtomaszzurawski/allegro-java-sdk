/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CategoryParameterChangeFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryParameterScheduledChange;
import java.util.List;

/**
 * Bucket E live probe (TESTING.md §2) for the planned category-parameter-change
 * feed. Read-only global reference data, so an app-only client-credentials token
 * suffices; this scenario ignores the stored user token and the {@code account}
 * argument. It applies the read-only carve-out: verify response SHAPE against the
 * live wire — take a lazy sample and confirm the mapped fields (type, schedule
 * dates, affected category/parameter) arrive. The sandbox may legitimately have no
 * planned changes, in which case an empty sample is reported (not an error).
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=catalog-parameter-changes
 * </pre>
 */
final class CatalogParameterChangesDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "catalog-parameter-changes";

    private static final int SAMPLE_LIMIT = 10;

    private CatalogParameterChangesDemo() {
    }

    static void run(String clientId, String clientSecret, String account) {
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            List<CategoryParameterScheduledChange> sample = client.catalog().categories()
                    .scheduledParameterChanges(CategoryParameterChangeFilter.all())
                    .limit(SAMPLE_LIMIT)
                    .toList();
            System.out.println("scheduledParameterChanges(all): " + sample.size()
                    + " (lazy take; empty is valid — no planned changes)");
            for (CategoryParameterScheduledChange change : sample) {
                System.out.println("  " + change.type() + "  for=" + change.scheduledFor()
                        + "  at=" + change.scheduledAt()
                        + "  category=" + change.categoryId() + "  parameter=" + change.parameterId());
            }
        }
    }
}
