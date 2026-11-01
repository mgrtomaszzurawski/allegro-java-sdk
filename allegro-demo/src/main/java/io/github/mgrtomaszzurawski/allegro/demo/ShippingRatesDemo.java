/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.ShippingRates;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.ShippingRateSetRequestBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSet;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Bucket C live probe (TESTING.md §2) for {@code shipping().rates()}. Verifies the
 * read mapping (list → get, including the nested rate rows) and — on the first
 * seller-editable set with a modelled type — the write→read contract with a
 * marker rename: PUT the set under a probe-suffixed name, assert the update
 * response and a fresh GET both carry the new name with the type, dispatch
 * country and rows unchanged, then PUT the base name back (the restore runs even
 * if an assertion fails, so the sandbox set never stays renamed). The API has no
 * delete for rate sets, so the probe never creates one (which would linger).
 *
 * <p>A legacy set whose name breaks the current server rule (letters, numbers,
 * hyphens, dots, commas and spaces — see {@code KNOWN-SERVER-BEHAVIORS.md}) is
 * restored under the rule-conforming form of its name, because the server
 * refuses to re-save the old one. Falls back to read-only if the seller has no
 * editable set with a modelled type and a name that can be normalised.
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
    private static final String ERR_ROUND_TRIP = "rate set did not round-trip through update: ";
    private static final String NO_EDITABLE_SET = "(no seller-editable set - skipping write check)";
    private static final String UNTYPED_SET =
            "(editable set has no modelled type - cannot re-send it; skipping write check)";
    private static final String UNNAMEABLE_SET =
            "(editable set name has no allowed characters - cannot re-send it; skipping write check)";
    private static final String LEGACY_NAME =
            "(set name violates the current server rule - the probe restores it as '%s')";
    private static final String STAGE_UPDATE = "update response";
    private static final String STAGE_RE_READ = "re-read";
    private static final String STAGE_RESTORE = "restore";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DISPATCH_COUNTRY = "dispatchCountry";
    private static final String FIELD_ROW_COUNT = "row count";
    private static final String FIELD_FIRST_ITEM_RATE = "first row firstItemRate";
    private static final String PROBE_SUFFIX = " probe";
    private static final String DISALLOWED_NAME_CHARS =
            "[^" + ShippingRateSetRequestBuilder.NAME_ALLOWED_CHARACTERS + "]";
    private static final String REPEATED_SPACES = " {2,}";
    private static final String SINGLE_SPACE = " ";
    private static final String EMPTY = "";

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
                    + ", maxQuantity=" + rateRow.maxQuantityPerPackage()
                    + ", maxWeight=" + rateRow.maxPackageWeight()
                    + ", shippingTime=" + rateRow.shippingTime());
        }

        if (editable == null) {
            System.out.println(NO_EDITABLE_SET);
            return;
        }
        if (rateSet.type() == RateSetType.UNKNOWN) {
            System.out.println(UNTYPED_SET);
            return;
        }
        writeReadRestore(rates, rateSet);
    }

    /** Marker rename → update response + re-read assert → restore the base name (always). */
    private static void writeReadRestore(ShippingRates rates, ShippingRateSet rateSet) {
        String baseName = legalName(rateSet.name());
        if (baseName.isEmpty()) {
            System.out.println(UNNAMEABLE_SET);
            return;
        }
        if (!baseName.equals(rateSet.name())) {
            System.out.println(LEGACY_NAME.formatted(baseName));
        }
        String markerName = truncate(baseName,
                ShippingRateSetRequestBuilder.NAME_MAX_LENGTH - PROBE_SUFFIX.length()) + PROBE_SUFFIX;

        ShippingRateSet updated = update(rates, rateSet, markerName);
        System.out.println("rates().update() ok: name='" + updated.name() + "', type=" + updated.type()
                + ", dispatch=" + updated.dispatchCountry() + ", rows=" + updated.rates().size());
        ShippingRateSet restored;
        try {
            assertSameSet(rateSet, updated, markerName, STAGE_UPDATE);
            ShippingRateSet reRead = rates.get(rateSet.id());
            System.out.println("rates().get() after update ok: name='" + reRead.name()
                    + "', rows=" + reRead.rates().size());
            assertSameSet(rateSet, reRead, markerName, STAGE_RE_READ);
        } finally {
            // the marker landed on the live set — put the base name back whatever happened above
            restored = update(rates, rateSet, baseName);
            System.out.println("rates().update() restored: name='" + restored.name() + "'");
        }
        assertSameSet(rateSet, restored, baseName, STAGE_RESTORE);
    }

    private static ShippingRateSet update(ShippingRates rates, ShippingRateSet rateSet, String name) {
        try {
            return rates.update(rateSet.id(), requestFrom(rateSet, name));
        } catch (AllegroBadRequestException rejected) {
            // field errors are status-level diagnostics (code + path), never a body or token
            rejected.errors().forEach(error -> System.out.println("rates().update() rejected: code="
                    + error.code() + ", path=" + error.path() + ", message=" + error.message()));
            throw rejected;
        }
    }

    private static void assertSameSet(ShippingRateSet expected, ShippingRateSet actual,
            String expectedName, String stage) {
        String mismatch = firstMismatch(expected, actual, expectedName);
        if (mismatch != null) {
            throw new IllegalStateException(ERR_ROUND_TRIP + stage + " (" + mismatch + ")");
        }
    }

    /** The first field that differs, or {@code null} when the set round-tripped. */
    private static String firstMismatch(ShippingRateSet expected, ShippingRateSet actual,
            String expectedName) {
        if (!expectedName.equals(actual.name())) {
            return FIELD_NAME;
        }
        if (expected.type() != actual.type()) {
            return FIELD_TYPE;
        }
        if (!Objects.equals(expected.dispatchCountry(), actual.dispatchCountry())) {
            return FIELD_DISPATCH_COUNTRY;
        }
        if (expected.rates().size() != actual.rates().size()) {
            return FIELD_ROW_COUNT;
        }
        if (!expected.rates().isEmpty() && !Objects.equals(
                expected.rates().get(0).firstItemRate(), actual.rates().get(0).firstItemRate())) {
            return FIELD_FIRST_ITEM_RATE;
        }
        return null;
    }

    /** The rule-conforming form of a name: drop disallowed characters, squeeze spaces, cap length. */
    private static String legalName(String name) {
        return truncate(name.replaceAll(DISALLOWED_NAME_CHARS, EMPTY)
                .replaceAll(REPEATED_SPACES, SINGLE_SPACE).trim(),
                ShippingRateSetRequestBuilder.NAME_MAX_LENGTH);
    }

    private static String truncate(String name, int maxLength) {
        return name.length() <= maxLength ? name : name.substring(0, maxLength).trim();
    }

    private static ShippingRateSetSummary firstEditable(List<ShippingRateSetSummary> summaries) {
        return summaries.stream()
                .filter(summary -> summary.features() == null || !summary.features().managedByAllegro())
                .findFirst()
                .orElse(null);
    }

    private static ShippingRateSetRequest requestFrom(ShippingRateSet rateSet, String name) {
        return ShippingRateSetRequest.builder()
                .name(name)
                .type(rateSet.type())
                .dispatchCountry(rateSet.dispatchCountry())
                .rates(rateSet.rates())
                .build();
    }
}
