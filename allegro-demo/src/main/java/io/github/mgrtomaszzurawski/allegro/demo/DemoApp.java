/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Live sandbox probe runner — the exploration and verification tool
 * (TESTING.md §2). Manual execution only, never part of {@code check}:
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=auth-bootstrap -Pdemo.account=seller
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=me             -Pdemo.account=seller
 * </pre>
 *
 * Credentials come from the environment ({@code ALLEGRO_SANDBOX_CLIENT_ID} /
 * {@code ALLEGRO_SANDBOX_CLIENT_SECRET}); refresh tokens live in the shared
 * flock-guarded store (ADR-008). Output is status-level only — never bodies
 * or tokens.
 */
public final class DemoApp {

    private static final String SCENARIO_AUTH_BOOTSTRAP = "auth-bootstrap";
    private static final String SCENARIO_ME = "me";
    private static final String SCENARIO_OFFER = "offer";
    private static final String SCENARIO_ORDERS_GET = "orders-get";
    private static final String SCENARIO_MARKETPLACES = "marketplaces";
    private static final String SCENARIO_CLASSIFIEDS = "classifieds";
    private static final String SCENARIO_PRICING = "pricing";
    private static final String SCENARIO_CAMPAIGNS = "campaigns";
    private static final String SCENARIO_POS_ROUNDTRIP = "pos-roundtrip";
    private static final String SCENARIO_CONTACTS = "contacts";
    private static final String CLIENT_ID_ENV = "ALLEGRO_SANDBOX_CLIENT_ID";
    private static final String CLIENT_SECRET_ENV = "ALLEGRO_SANDBOX_CLIENT_SECRET";
    private static final String ACCOUNT_PROPERTY = "demo.account";
    private static final String DEFAULT_ACCOUNT = "seller";
    private static final String ERR_NO_SCENARIO =
            "Usage: run -Pdemo.scenario=<%s> [-Pdemo.account=seller|buyer]";
    private static final String SCENARIO_NAME_SEPARATOR = "|";
    private static final String ERR_NO_CREDENTIALS =
            "Missing env vars %s / %s - source /workspace/shared/secrets/allegro-sandbox.env first";
    private static final String ERR_UNKNOWN_SCENARIO = "Unknown scenario: ";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";

    /**
     * Scenario registry — the demo's single dispatch point. Each domain bucket
     * adds ONE append-only {@code SCENARIOS.put(...)} line below the marker,
     * pointing at its own {@code <Feature>Demo} class, so buckets extend the
     * verification tool without colliding on a shared switch.
     */
    private static final Map<String, DemoScenario> SCENARIOS = new LinkedHashMap<>();

    static {
        SCENARIOS.put(SCENARIO_AUTH_BOOTSTRAP, DemoApp::authBootstrap);
        SCENARIOS.put(SCENARIO_ME, DemoApp::currentUser);
        SCENARIOS.put(SCENARIO_OFFER, OffersDemo::run);
        SCENARIOS.put(SCENARIO_PRICING, PricingDemo::run);
        // [append point: demo scenarios] One line per bucket, append-only:
        //   SCENARIOS.put("<scenario-name>", <Feature>Demo::run);
        SCENARIOS.put(SCENARIO_ORDERS_GET, OrdersDemo::run);
        SCENARIOS.put(SCENARIO_MARKETPLACES, MarketplacesDemo::run);
        SCENARIOS.put(CatalogDemo.SCENARIO, CatalogDemo::run);
        SCENARIOS.put(SCENARIO_CLASSIFIEDS, ClassifiedsDemo::run);
        SCENARIOS.put(SCENARIO_CAMPAIGNS, CampaignsDemo::run);
        SCENARIOS.put(SCENARIO_POS_ROUNDTRIP, PointsOfServiceDemo::run);
        SCENARIOS.put(FulfillmentDemo.SCENARIO, FulfillmentDemo::run);
        SCENARIOS.put(SCENARIO_CONTACTS, ContactsDemo::run);
    }

    private DemoApp() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println(ERR_NO_SCENARIO.formatted(
                    String.join(SCENARIO_NAME_SEPARATOR, SCENARIOS.keySet())));
            System.exit(2);
        }
        String clientId = System.getenv(CLIENT_ID_ENV);
        String clientSecret = System.getenv(CLIENT_SECRET_ENV);
        if (clientId == null || clientSecret == null) {
            System.out.println(ERR_NO_CREDENTIALS.formatted(CLIENT_ID_ENV, CLIENT_SECRET_ENV));
            System.exit(2);
        }
        String account = System.getProperty(ACCOUNT_PROPERTY, DEFAULT_ACCOUNT);
        DemoScenario scenario = SCENARIOS.get(args[0]);
        if (scenario == null) {
            System.out.println(ERR_UNKNOWN_SCENARIO + args[0]);
            System.exit(2);
            return;
        }
        scenario.run(clientId, clientSecret, account);
    }

    /**
     * One-time interactive authorization: starts the device flow, prints the
     * verification link for the operator, and persists the rotated refresh
     * token to the shared store so every later run is non-interactive.
     */
    private static void authBootstrap(String clientId, String clientSecret, String account)
            throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        DeviceCodeCredentials credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                authorization -> {
                    System.out.println();
                    System.out.println("==== ACTION REQUIRED (account: " + account + ") ====");
                    System.out.println("Open:  " + authorization.verificationUriComplete());
                    System.out.println("Code:  " + authorization.userCode());
                    System.out.println("Valid: " + authorization.expiresIn().toMinutes() + " min");
                    System.out.println("Confirm in the browser where the SANDBOX "
                            + account + " account is logged in.");
                    System.out.println();
                });
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                CurrentUser user = client.user().me();
                System.out.println("Authorized as: " + user.login() + " (id " + user.id() + ")");
            } finally {
                // The user's one-time authorization must survive any downstream
                // failure — persist the refresh token as soon as it exists.
                String issuedRefreshToken = client.refreshToken();
                if (issuedRefreshToken != null) {
                    tokenStore.store(account, issuedRefreshToken);
                    System.out.println("Refresh token stored for account '" + account + "'.");
                }
            }
        }
    }

    /** Non-interactive vertical proof: stored refresh token → /me round-trip. */
    private static void currentUser(String clientId, String clientSecret, String account)
            throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            System.exit(2);
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println("(stored token expired - rerun auth-bootstrap)"),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            CurrentUser user = client.user().me();
            // Rotation: the refresh we just did invalidated the stored token.
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
            // Status-level output only - no e-mail (PII).
            System.out.println("me(): login=" + user.login()
                    + ", id=" + user.id()
                    + ", features=" + user.features().size());
            System.out.println("SDK version: " + AllegroClient.sdkVersion());
        }
    }
}
