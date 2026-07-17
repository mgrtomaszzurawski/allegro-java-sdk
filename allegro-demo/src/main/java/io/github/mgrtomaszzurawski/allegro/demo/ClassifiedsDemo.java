/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import java.util.List;

/**
 * Live sandbox probe for bucket F — classifieds (advertisement) packages.
 *
 * <p>Classifieds package configurations are public reference data, so this
 * scenario authenticates with the app-only client-credentials grant (no stored
 * user token needed). It is a read-shape verification (TESTING.md §2): it reads
 * the packages of a classifieds-enabled category through the SDK and prints
 * their shape, proving every mapped field arrives parseable on the live wire.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=classifieds -Pdemo.categoryId=&lt;id&gt;
 * </pre>
 *
 * where {@code <id>} is a category that supports advertisements. Output is
 * status-level only — never bodies or tokens.
 */
public final class ClassifiedsDemo {

    private static final String CATEGORY_ID_PROPERTY = "demo.categoryId";
    private static final String ERR_NO_CATEGORY =
            "Provide -Pdemo.categoryId=<classifieds-enabled category id>";

    private ClassifiedsDemo() {
    }

    static void run(String clientId, String clientSecret, String account) {
        String categoryId = System.getProperty(CATEGORY_ID_PROPERTY);
        if (categoryId == null || categoryId.isBlank()) {
            System.out.println(ERR_NO_CATEGORY);
            return;
        }
        ClientCredentials credentials = new ClientCredentials(clientId, clientSecret);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            List<ClassifiedPackage> packages = client.classifieds().availablePackages(categoryId);
            System.out.println("classifieds.availablePackages(" + categoryId + "): "
                    + packages.size() + " package(s)");
            for (ClassifiedPackage pkg : packages) {
                System.out.println("  - " + pkg.name() + " [" + pkg.type()
                        + "] extensions=" + pkg.extensions().size()
                        + " promotions=" + pkg.promotions().size());
            }
        }
    }
}
