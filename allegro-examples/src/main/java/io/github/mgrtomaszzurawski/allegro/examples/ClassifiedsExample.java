/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/offers-extras.md} classifieds snippet —
 * if the documented usage stops compiling, this module breaks the build.
 */
public final class ClassifiedsExample {

    private ClassifiedsExample() {
    }

    static int countPackages(String clientId, String clientSecret, String categoryId) {
        // The endpoint needs a user (seller) token — see docs/offers-extras.md.
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            List<ClassifiedPackage> packages = client.classifieds().availablePackages(categoryId);
            for (ClassifiedPackage classifiedPackage : packages) {
                System.out.println(classifiedPackage.name() + " (" + classifiedPackage.type() + ")");
            }
            return packages.size();
        }
    }
}
