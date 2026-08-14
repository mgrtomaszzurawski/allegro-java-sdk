/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAttachment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Bucket-K live WRITE probe (#181, TESTING.md §2) for the after-sale warranty-document
 * attachment upload: declare an attachment then PUT the file bytes through the SDK
 * ({@code uploadAttachment(fileName, byte[], contentType)}), and confirm the server returns a
 * hosted attachment with an id (and url once processed). Seller-only.
 *
 * <p>Run:
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=settings-aftersale-attachment -Pdemo.account=seller}.
 */
final class AfterSaleAttachmentDemo {

    static final String SCENARIO = "settings-aftersale-attachment";

    private static final String MSG_NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final String FILE_NAME = "sdk-live-verify.pdf";

    /** Minimal single-page PDF document used as the upload payload. */
    private static final String PDF_DOCUMENT =
            "%PDF-1.4\n"
            + "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
            + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Resources<<>>>>endobj\n"
            + "trailer<</Root 1 0 R>>\n"
            + "%%EOF\n";

    private AfterSaleAttachmentDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(MSG_NO_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                probe(client.settings().afterSale());
            } finally {
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void probe(AfterSaleConditions afterSaleConditions) {
        byte[] content = PDF_DOCUMENT.getBytes(StandardCharsets.US_ASCII);
        System.out.println("uploading " + content.length + "-byte " + CONTENT_TYPE + " attachment (" + FILE_NAME + ")");
        AfterSalesAttachment attachment = afterSaleConditions.uploadAttachment(FILE_NAME, content, CONTENT_TYPE);
        System.out.println("attachment id=" + attachment.id()
                + ", name=" + attachment.name()
                + ", url present=" + (attachment.url() != null));
        System.out.println("upload-ok=true");
    }
}
