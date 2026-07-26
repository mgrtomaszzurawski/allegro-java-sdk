/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.SizeTables;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder.SizeTableRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTable;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableTemplate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model.TaxSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Bucket-K write→read verification (TESTING.md §2) for size tables: read the
 * template catalog, create (or reuse) a size table built from a template's own
 * headers and an example row, read it back and assert the round-trip. Size tables
 * have no DELETE, so the probe reuses a single table per name. Also reads the tax
 * settings for a category. Seller-only.
 *
 * <p>Run:
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=settings-size-tables -Pdemo.account=seller}.
 */
final class SizeTablesDemo {

    static final String SCENARIO = "settings-size-tables";

    private static final String DEMO_TABLE_NAME = "[K-demo] size table";
    /** A leaf category used only to read tax settings; adjust if the sandbox rejects it. */
    private static final String TAX_CATEGORY_ID = "316194";
    /** Placeholder cell value for a synthesized row (templates carry columns, not example rows). */
    private static final String PLACEHOLDER_CELL = "-";
    private static final int DIAGNOSTIC_TEMPLATES = 3;

    private static final String MSG_NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String MSG_NO_TEMPLATE = "no size-table templates available - cannot verify writes";
    private static final int EXIT_NO_TOKEN = 2;

    private SizeTablesDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(MSG_NO_TOKEN.formatted(account));
            System.exit(EXIT_NO_TOKEN);
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                SizeTables sizeTables = client.settings().sizeTables();
                boolean tableOk = verifySizeTable(sizeTables);
                readTaxSettings(client);
                System.out.println("round-trip-ok=" + tableOk);
            } catch (AllegroBadRequestException rejection) {
                printFieldErrors(rejection);
                throw rejection;
            } catch (AllegroException serverError) {
                System.out.println("server-error: status=" + serverError.statusCode()
                        + ", traceId=" + serverError.traceId()
                        + ", body=" + serverError.responseBody());
                throw serverError;
            } finally {
                persistRotatedToken(tokenStore, account, client);
            }
        }
    }

    private static boolean verifySizeTable(SizeTables sizeTables) {
        List<SizeTableTemplate> templates = sizeTables.templates();
        System.out.println("templates=" + templates.size());
        templates.stream().limit(DIAGNOSTIC_TEMPLATES).forEach(candidate ->
                System.out.println("  template '" + candidate.name() + "' headers=" + candidate.headers().size()
                        + " rows=" + candidate.rows().size()));
        Optional<SizeTableTemplate> template = templates.stream()
                .filter(candidate -> !candidate.headers().isEmpty())
                .findFirst();
        if (template.isEmpty()) {
            System.out.println(MSG_NO_TEMPLATE);
            return false;
        }
        SizeTableTemplate source = template.get();
        System.out.println("chosen template '" + source.name() + "' id=" + source.id()
                + " headers=" + source.headers());
        // Templates define columns but usually carry no example rows, so synthesize
        // one placeholder row sized to the template's headers.
        List<String> cells = source.rows().isEmpty()
                ? source.headers().stream().map(header -> PLACEHOLDER_CELL).toList()
                : source.rows().get(0).cells();
        SizeTableRequest request = SizeTableRequest.builder()
                .name(DEMO_TABLE_NAME)
                .templateId(source.id())
                .headers(source.headers())
                .row(cells)
                .build();

        Optional<SizeTable> existing = sizeTables.list().stream()
                .filter(table -> DEMO_TABLE_NAME.equals(table.name()))
                .findFirst();
        SizeTable written = writeTable(sizeTables, existing, request);
        System.out.println((existing.isPresent() ? "updated" : "created") + " table: id=" + written.id()
                + ", template=" + source.id() + ", headers=" + source.headers().size());

        SizeTable readBack = sizeTables.get(written.id());
        System.out.println("read-back: name='" + readBack.name() + "', headers=" + readBack.headers()
                + ", rows=" + readBack.rows().size());
        return DEMO_TABLE_NAME.equals(readBack.name()) && !readBack.headers().isEmpty();
    }

    /**
     * Create the table (or update it when it already exists). Allegro's sandbox
     * size-table create can answer {@code 504 ServiceTimeoutException} ("Query
     * response time exceeded. Try again.") while still creating the table
     * (KNOWN-SERVER-BEHAVIORS); recover by re-reading the created table by name so
     * the write→read round-trip still completes.
     */
    private static SizeTable writeTable(SizeTables sizeTables, Optional<SizeTable> existing,
            SizeTableRequest request) {
        if (existing.isPresent()) {
            return sizeTables.update(existing.get().id(), request);
        }
        try {
            return sizeTables.create(request);
        } catch (AllegroServerException timeout) {
            System.out.println("create timed out (status=" + timeout.statusCode()
                    + "); recovering the created table by name");
            return sizeTables.list().stream()
                    .filter(table -> DEMO_TABLE_NAME.equals(table.name()))
                    .findFirst()
                    .orElseThrow(() -> timeout);
        }
    }

    private static void readTaxSettings(AllegroClient client) {
        try {
            TaxSettings taxSettings = client.settings().taxSettings(TAX_CATEGORY_ID);
            System.out.println("tax category=" + TAX_CATEGORY_ID
                    + ": subjects=" + taxSettings.subjects().size()
                    + ", rates=" + taxSettings.rates().size()
                    + ", exemptions=" + taxSettings.exemptions().size());
        } catch (AllegroException taxError) {
            System.out.println("tax read for category " + TAX_CATEGORY_ID
                    + " failed (" + taxError.getClass().getSimpleName()
                    + ") - pick a valid leaf category id");
        }
    }

    private static void printFieldErrors(AllegroBadRequestException rejection) {
        for (AllegroFieldError fieldError : rejection.errors()) {
            System.out.println("field-error: code=" + fieldError.code()
                    + ", path=" + fieldError.path()
                    + ", message=" + fieldError.message());
        }
    }

    private static void persistRotatedToken(SharedTokenStore tokenStore, String account,
            AllegroClient client) throws IOException {
        String rotatedRefreshToken = client.refreshToken();
        if (rotatedRefreshToken != null) {
            tokenStore.store(account, rotatedRefreshToken);
        }
    }
}
