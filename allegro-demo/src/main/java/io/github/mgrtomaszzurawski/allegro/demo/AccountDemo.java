/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.AdditionalEmail;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.SalesQuality;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.SmartClassification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.UserRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.UserRatingSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import java.io.IOException;
import java.util.List;

/**
 * Bucket D exploration/verification probe (TESTING.md §2). Runs against the
 * SANDBOX with the stored SELLER user token: read-shape checks on the reporting
 * endpoints (sales quality, Smart! classification, ratings) plus a full
 * write&rarr;read&rarr;delete cycle on additional e-mail addresses.
 *
 * <p>Status-level output only. The bidding/charity/affiliate probes are omitted
 * here: bidding is the buyer half and lives in its own {@link BiddingDemo}
 * ({@code -Pdemo.account=buyer}), and the beta charity/affiliate resources may be
 * unavailable on the sandbox seller account.
 */
public final class AccountDemo {

    private static final String DEMO_EMAIL_PREFIX = "d-demo+";
    private static final String DEMO_EMAIL_DOMAIN = "@example.com";
    private static final String STORED_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";

    private AccountDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println("No stored refresh token for '" + account
                    + "' - run the auth-bootstrap scenario first");
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(STORED_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            CurrentUser currentUser = client.user().me();
            System.out.println("me(): login=" + currentUser.login() + ", id=" + currentUser.id());
            CurrentUser.Company company = currentUser.company();
            System.out.println("me(): baseMarketplaceId=" + currentUser.baseMarketplaceId()
                    + ", company=" + (company == null
                            ? "none"
                            : "name/present=" + (company.name() != null)
                                    + ", taxId/present=" + (company.taxId() != null)));
            String rotated = client.refreshToken();
            if (rotated != null) {
                tokenStore.store(account, rotated);
            }

            reportProbes(client, currentUser.id());
            additionalEmailsCycle(client);
        }
    }

    private static void reportProbes(AllegroClient client, String userId) {
        SalesQuality quality = client.user().salesQuality();
        System.out.println("salesQuality(): days=" + quality.days().size());

        SmartClassification smart = client.user().smartClassification();
        System.out.println("smartClassification(): fulfilled=" + smart.fulfilled()
                + ", conditions=" + smart.conditions().size());

        UserRatingSummary summary = client.user().ratings().summaryOf(userId);
        System.out.println("ratings().summaryOf(self): recommended="
                + summary.recommended().total() + ", notRecommended="
                + summary.notRecommended().total());

        List<UserRating> firstRatings = client.user().ratings()
                .stream(RatingFilter.all())
                .limit(5).toList();
        System.out.println("ratings().stream(all).limit(5): fetched=" + firstRatings.size());
    }

    private static void additionalEmailsCycle(AllegroClient client) {
        // Unique local part so parallel runs never collide; created then deleted.
        String address = DEMO_EMAIL_PREFIX + System.nanoTime() + DEMO_EMAIL_DOMAIN;
        try {
            AdditionalEmail created = client.user().additionalEmails().add(address);
            System.out.println("additionalEmails().add: id=" + created.id());
            boolean present = client.user().additionalEmails().list().stream()
                    .anyMatch(email -> created.id().equals(email.id()));
            System.out.println("additionalEmails().list contains created: " + present);
            client.user().additionalEmails().delete(created.id());
            boolean gone = client.user().additionalEmails().list().stream()
                    .noneMatch(email -> created.id().equals(email.id()));
            System.out.println("additionalEmails().delete removed it: " + gone);
        } catch (AllegroException failure) {
            // Additional-email management may be restricted on the sandbox seller
            // account; report the wire outcome without failing the whole probe.
            System.out.println("additionalEmails() write cycle skipped: "
                    + failure.getClass().getSimpleName() + " (" + failure.statusCode() + ")");
        }
    }
}
