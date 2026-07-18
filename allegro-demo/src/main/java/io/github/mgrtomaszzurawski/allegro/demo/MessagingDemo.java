/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.Messaging;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.AttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.MessageThread;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Bucket J write→read verification for the message-center facade (TESTING.md §2).
 *
 * <p>Two verifications, both driven THROUGH the SDK:
 * <ul>
 *   <li><strong>Attachment round-trip (self-seeded, seller-only):</strong>
 *       {@code declareAttachment} → {@code uploadAttachment} → {@code downloadAttachment},
 *       asserting the bytes survive verbatim. This needs no buyer and no order.</li>
 *   <li><strong>Threads read + {@code markRead} write→read:</strong> stream the seller's
 *       existing threads; if one exists, {@code markRead} it and read the thread back to
 *       confirm the flag flipped, then shape-verify its messages.</li>
 * </ul>
 *
 * <p>Opening a brand-new thread ({@code send}) needs a buyer-initiated context (an order
 * with a buyer recipient); that half is seeded by the buyer-side E2E layer (see
 * {@code PLAN-BUCKET-J.md} §7 and {@code TESTING.md} §3), so this probe does not cold-send.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=messaging -Pdemo.account=seller
 * </pre>
 */
public final class MessagingDemo {

    static final String SCENARIO = "messaging";

    // A tiny valid 1×1 transparent PNG — a well-formed binary the attachment
    // endpoint accepts, so the round-trip exercises the wire, not a codec quirk.
    private static final String PROBE_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    private static final String PROBE_FILENAME = "[J-demo] probe.png";
    private static final String PROBE_MIME = "image/png";
    private static final int THREAD_SAMPLE_LIMIT = 5;
    private static final long MESSAGE_SAMPLE_LIMIT = 5L;

    private static final String MSG_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String NO_THREADS =
            "threads read->read: none on this account - a buyer must seed a thread "
                    + "(PLAN-BUCKET-J.md §7); attachment round-trip still verified above";

    private MessagingDemo() {
    }

    /** Entry point registered in {@link DemoApp}'s scenario table. */
    public static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_EXPIRED), storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            Messaging messaging = client.messaging();
            verifyAttachmentRoundTrip(messaging);
            verifyThreads(messaging);
            // Rotation: the refresh we just did invalidated the stored token.
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
        }
    }

    private static void verifyAttachmentRoundTrip(Messaging messaging) {
        byte[] bytes = Base64.getDecoder().decode(PROBE_PNG_BASE64);
        AttachmentRef declared = messaging.declareAttachment(AttachmentDeclaration.builder()
                .filename(PROBE_FILENAME)
                .size(bytes.length)
                .build());
        AttachmentRef uploaded = messaging.uploadAttachment(declared.id(), bytes, PROBE_MIME);
        byte[] downloaded = messaging.downloadAttachment(uploaded.id());
        boolean roundTripMatches = Arrays.equals(bytes, downloaded);
        System.out.println("attachment write->read: id=" + uploaded.id()
                + ", bytes=" + bytes.length
                + ", roundTrip=" + roundTripMatches);
    }

    private static void verifyThreads(Messaging messaging) {
        List<MessageThread> threads = messaging.streamThreads().limit(THREAD_SAMPLE_LIMIT).toList();
        Optional<MessageThread> first = threads.stream().findFirst();
        if (first.isEmpty()) {
            System.out.println(NO_THREADS);
            return;
        }
        String threadId = first.get().id();
        messaging.markRead(threadId);
        MessageThread readBack = messaging.thread(threadId);
        long sampledMessages = messaging.streamMessages(threadId).limit(MESSAGE_SAMPLE_LIMIT).count();
        System.out.println("threads read->read: sampled=" + threads.size()
                + ", markRead id=" + threadId
                + ", readFlag=" + readBack.read()
                + ", sampledMessages=" + sampledMessages);
    }
}
