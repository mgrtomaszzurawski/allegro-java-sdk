/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import java.io.IOException;
import java.util.List;

/**
 * Live sandbox probe for bucket F — classifieds (advertisement) packages.
 *
 * <p>{@code GET /sale/classifieds-packages} is declared {@code
 * bearer-token-for-user} in the spec, so this scenario authenticates with the
 * stored seller <em>user</em> token (device-flow refresh token from the shared
 * store, ADR-008) — the same non-interactive path as the {@code me} scenario.
 * It is a read-shape verification (TESTING.md §2): it reads the packages of a
 * classifieds-enabled category through the SDK and prints their shape, proving
 * every mapped field arrives parseable on the live wire.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=classifieds \
 *       -Pdemo.account=seller -Pdemo.categoryId=&lt;id&gt;
 * </pre>
 *
 * where {@code <id>} is a category that supports advertisements. Output is
 * status-level only — never bodies or tokens.
 */
public final class ClassifiedsDemo {

    private static final String CATEGORY_ID_PROPERTY = "demo.categoryId";
    private static final String ERR_NO_CATEGORY =
            "Provide -Pdemo.categoryId=<classifieds-enabled category id>";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED =
            "(stored token expired - rerun auth-bootstrap)";

    private ClassifiedsDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        String categoryId = System.getProperty(CATEGORY_ID_PROPERTY);
        if (categoryId == null || categoryId.isBlank()) {
            System.out.println(ERR_NO_CATEGORY);
            return;
        }
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            List<ClassifiedPackage> packages = client.classifieds().availablePackages(categoryId);
            // Allegro rotates the refresh token on every use — persist the new
            // one so the next run (or a sibling agent) keeps a valid session.
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
            System.out.println("classifieds.availablePackages(" + categoryId + "): "
                    + packages.size() + " package(s)");
            for (ClassifiedPackage classifiedPackage : packages) {
                System.out.println("  - " + classifiedPackage.name() + " [" + classifiedPackage.type()
                        + "] extensions=" + classifiedPackage.extensions().size()
                        + " promotions=" + classifiedPackage.promotions().size());
            }
        }
    }
}
