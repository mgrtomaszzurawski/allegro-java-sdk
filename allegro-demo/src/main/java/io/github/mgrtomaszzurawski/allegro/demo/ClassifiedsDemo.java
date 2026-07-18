/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifieds;
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
 *       -Pdemo.account=seller -Pdemo.categoryId=&lt;id&gt; [-Pdemo.offerId=&lt;offerId&gt;]
 * </pre>
 *
 * where {@code <id>} is a category that supports advertisements. When an
 * advertisement {@code -Pdemo.offerId} is also supplied, the scenario runs the
 * full write→read cycle (TESTING.md §2): it assigns the category's first base
 * package to the offer with {@code assignPackages} and reads it back with
 * {@code packagesOfOffer}, asserting the base package id round-trips. Output is
 * status-level only — never bodies or tokens.
 */
public final class ClassifiedsDemo {

    private static final String CATEGORY_ID_PROPERTY = "demo.categoryId";
    private static final String OFFER_ID_PROPERTY = "demo.offerId";
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
            verifySinglePackageRead(client, packages);
            verifyAssignmentCycle(client, packages);
        }
    }

    /** Read one package back by id (getPackage), confirming the single-read shape. */
    private static void verifySinglePackageRead(AllegroClient client, List<ClassifiedPackage> packages) {
        if (packages.isEmpty()) {
            return;
        }
        String packageId = packages.get(0).id();
        ClassifiedPackage single = client.classifieds().getPackage(packageId);
        System.out.println("classifieds.getPackage(" + packageId + "): " + single.name()
                + " [" + single.type() + "]");
    }

    /**
     * Write→read cycle: assign the category's first base package to the offer and
     * read the assignment back, asserting the base package id round-trips. Skipped
     * unless an advertisement {@code -Pdemo.offerId} is supplied.
     */
    private static void verifyAssignmentCycle(AllegroClient client, List<ClassifiedPackage> packages) {
        String offerId = System.getProperty(OFFER_ID_PROPERTY);
        if (offerId == null || offerId.isBlank()) {
            System.out.println("(no -Pdemo.offerId - skipping the assign write→read cycle)");
            return;
        }
        String basePackageId = firstBasePackageId(packages);
        if (basePackageId == null) {
            System.out.println("(no BASE package in the category - cannot assign)");
            return;
        }
        client.classifieds().assignPackages(offerId,
                ClassifiedAssignment.builder().basePackage(basePackageId).build());
        OfferClassifieds assigned = client.classifieds().packagesOfOffer(offerId);
        System.out.println("classifieds.assignPackages/packagesOfOffer(" + offerId + "): base="
                + assigned.basePackageId() + " roundTrips=" + basePackageId.equals(assigned.basePackageId())
                + " extras=" + assigned.extraPackages().size());
    }

    private static String firstBasePackageId(List<ClassifiedPackage> packages) {
        return packages.stream()
                .filter(classifiedPackage -> classifiedPackage.type() == ClassifiedPackageType.BASE)
                .map(ClassifiedPackage::id)
                .findFirst()
                .orElse(null);
    }
}
