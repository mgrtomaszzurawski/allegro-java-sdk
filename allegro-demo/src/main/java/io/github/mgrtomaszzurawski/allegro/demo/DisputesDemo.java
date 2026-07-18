/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.Disputes;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.Issue;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Bucket J read-shape verification for the disputes facade (TESTING.md §2). Post-purchase
 * issues are opened by buyers (web-only), so this seller-side probe is read-only: stream the
 * account's issues and, if any exist, read one and its chat, asserting the records map off a
 * live beta response. When none exist it reports the gap — a real dispute must be seeded by a
 * buyer through the {@code allegro-e2e} browser (see PLAN-BUCKET-J.md §7).
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=disputes -Pdemo.account=seller
 * </pre>
 */
public final class DisputesDemo {

    static final String SCENARIO = "disputes";

    private static final int ISSUE_SAMPLE_LIMIT = 5;
    private static final long CHAT_SAMPLE_LIMIT = 10L;

    private static final String MSG_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String NO_ISSUES =
            "disputes read: none on this account - a buyer must open a dispute "
                    + "(web-only, seeded via allegro-e2e; see PLAN-BUCKET-J.md §7)";

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
    }
}
