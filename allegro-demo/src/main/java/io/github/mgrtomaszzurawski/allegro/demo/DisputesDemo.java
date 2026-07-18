/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.Disputes;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueAttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.Issue;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueAttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueChatEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Bucket J live verification for the disputes facade (TESTING.md §2). Two probes:
 *
 * <ol>
 *   <li><b>Attachment declare+upload</b> — account-level, needs no seeded issue, so it always
 *   runs: it exercises the new beta <em>request-body</em> content type on the declaration and
 *   the cross-host PUT to the one-time {@code Location} URL (core C2 + C6) on live.</li>
 *   <li><b>Issue read + message write→read</b> — post-purchase issues are opened by buyers
 *   (web-only); when one exists this reads it and posts a benign {@code REGULAR} message, then
 *   reads the chat back to confirm it landed. When none exist it reports the gap (a real
 *   dispute must be seeded by a buyer through {@code allegro-e2e}; see PLAN-BUCKET-J.md §7).</li>
 * </ol>
 *
 * <p>The destructive {@code changeStatus} write (which irreversibly accepts/rejects a live
 * claim) is intentionally NOT auto-run here — it shares the same beta-POST transport as
 * {@code addMessage} (verified above) and its body shape is covered by unit tests.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=disputes -Pdemo.account=seller
 * </pre>
 */
public final class DisputesDemo {

    static final String SCENARIO = "disputes";

    private static final int ISSUE_SAMPLE_LIMIT = 5;
    private static final long CHAT_SAMPLE_LIMIT = 20L;

    // A 1x1 PNG — a real image the attachment endpoint accepts (mirrors MessagingDemo).
    private static final String PROBE_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    private static final String PROBE_FILENAME = "[J-demo] probe.png";
    private static final String PROBE_MIME = "image/png";
    private static final String PROBE_MESSAGE_TEXT = "[J-demo] SDK write-verification message.";

    private static final String MSG_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String NO_ISSUES =
            "disputes read: none on this account - a buyer must open a dispute "
                    + "(web-only, seeded via allegro-e2e; see PLAN-BUCKET-J.md §7); "
                    + "message/status writes need a seeded issue";

    private DisputesDemo() {
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
            try {
                verifyAttachmentUpload(client.disputes());
                verifyIssues(client.disputes());
            } finally {
                // Persist the rotated refresh token before any probe failure can propagate —
                // Allegro rotates on the first call, so the stored token is already dead
                // (pre-mortem B1). Mirrors authBootstrap.
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void verifyAttachmentUpload(Disputes disputes) {
        // Declare (beta JSON POST) then PUT the bytes to the one-time Location URL (C6).
        byte[] bytes = Base64.getDecoder().decode(PROBE_PNG_BASE64);
        IssueAttachmentRef uploaded = disputes.uploadAttachment(
                IssueAttachmentDeclaration.builder().filename(PROBE_FILENAME).size(bytes.length).build(),
                bytes, PROBE_MIME);
        System.out.println("disputes attachment declare+upload: uploadedId=" + uploaded.id()
                + ", bytes=" + bytes.length);
    }

    private static void verifyIssues(Disputes disputes) {
        List<Issue> issues = disputes.streamIssues(IssueFilter.none())
                .limit(ISSUE_SAMPLE_LIMIT).toList();
        Optional<Issue> first = issues.stream().findFirst();
        if (first.isEmpty()) {
            System.out.println(NO_ISSUES);
            return;
        }
        String issueId = first.get().id();
        Issue full = disputes.get(issueId);
        long chatEntries = disputes.streamChat(issueId).limit(CHAT_SAMPLE_LIMIT).count();
        System.out.println("disputes read: sampled=" + issues.size()
                + ", id=" + full.id()
                + ", type=" + full.type()
                + ", status=" + (full.state() == null ? "n/a" : full.state().status())
                + ", chatEntries=" + chatEntries);
        verifyMessageWriteReadThrough(disputes, issueId);
    }

    private static void verifyMessageWriteReadThrough(Disputes disputes, String issueId) {
        // Post a benign REGULAR message, then read the chat back and confirm it landed.
        try {
            IssueChatEntry added = disputes.addMessage(issueId,
                    IssueMessageRequest.builder().text(PROBE_MESSAGE_TEXT).build());
            boolean readBack = disputes.streamChat(issueId).limit(CHAT_SAMPLE_LIMIT)
                    .anyMatch(entry -> PROBE_MESSAGE_TEXT.equals(entry.text()));
            System.out.println("disputes message write->read: addedId=" + added.id()
                    + ", readBack=" + readBack);
        } catch (AllegroException failure) {
            // e.g. 409 when the issue's state forbids new messages, or 403 on a closed issue.
            System.out.println("disputes message write skipped: " + failure.getMessage());
        }
    }
}
