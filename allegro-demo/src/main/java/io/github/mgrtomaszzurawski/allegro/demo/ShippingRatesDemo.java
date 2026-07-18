/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.ShippingRates;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSet;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetSummary;
import java.io.IOException;
import java.util.List;

/**
 * Bucket C live probe (TESTING.md §2) for {@code shipping().rates()}. Verifies the
 * read mapping (list → get, including the nested rate rows) and — on the first
 * seller-editable set — the write contract with an <em>idempotent</em> update:
 * PUT the set back unchanged and assert it round-trips. The API has no delete for
 * rate sets, so the probe never creates one (which would linger); it re-sends an
 * existing set instead. Falls back to read-only if the seller has no editable set.
 *
 * <p>Needs the seller user token ({@code sale:settings:*}). Status-level output
 * only — never bodies or tokens.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=shipping-rates -Pdemo.account=seller
 * </pre>
 */
public final class ShippingRatesDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "shipping-rates";

    private static final String NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String ERR_ROUND_TRIP = "rate set name did not round-trip through update";

    private ShippingRatesDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(NO_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                probe(client.shipping().rates());
            } finally {
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void probe(ShippingRates rates) {
        List<ShippingRateSetSummary> summaries = rates.list();
        System.out.println("rates().list(): " + summaries.size() + " sets");
        if (summaries.isEmpty()) {
            System.out.println("(no rate sets on this account - cannot confirm field shape)");
            return;
        }

        ShippingRateSetSummary editable = firstEditable(summaries);
        ShippingRateSetSummary target = editable == null ? summaries.get(0) : editable;
        ShippingRateSet rateSet = rates.get(target.id());
        System.out.println("rates().get(" + rateSet.id() + "): name='" + rateSet.name()
                + "', type=" + rateSet.type() + ", dispatch=" + rateSet.dispatchCountry()
                + ", rows=" + rateSet.rates().size());
        if (!rateSet.rates().isEmpty()) {
            ShippingRate rateRow = rateSet.rates().get(0);
            System.out.println("first row: method=" + rateRow.deliveryMethodId()
                    + ", firstItemRate=" + rateRow.firstItemRate()
                    + ", nextItemRate=" + rateRow.nextItemRate()
                    + ", maxQuantity=" + rateRow.maxQuantityPerPackage()
                    + ", maxWeight=" + rateRow.maxPackageWeight()
                    + ", shippingTime=" + rateRow.shippingTime());
        }

        if (editable == null) {
            System.out.println("(no seller-editable set - skipping idempotent write check)");
            return;
        }
        ShippingRateSet updated = rates.update(rateSet.id(), requestFrom(rateSet));
        System.out.println("rates().update() ok: name='" + updated.name() + "'");
        if (!rateSet.name().equals(updated.name())) {
            throw new IllegalStateException(ERR_ROUND_TRIP);
        }
    }

    private static ShippingRateSetSummary firstEditable(List<ShippingRateSetSummary> summaries) {
        return summaries.stream()
                .filter(summary -> summary.features() == null || !summary.features().managedByAllegro())
                .findFirst()
                .orElse(null);
    }

    private static ShippingRateSetRequest requestFrom(ShippingRateSet rateSet) {
        RateSetType type = rateSet.type() == RateSetType.UNKNOWN ? null : rateSet.type();
        return ShippingRateSetRequest.builder()
                .name(rateSet.name())
                .type(type)
                .dispatchCountry(rateSet.dispatchCountry())
                .rates(rateSet.rates())
                .build();
    }
}
