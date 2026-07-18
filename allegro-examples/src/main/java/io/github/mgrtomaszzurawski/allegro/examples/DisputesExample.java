/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.Issue;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueChatEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueStatus;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/disputes.md} snippets — if the documented
 * disputes usage stops compiling, this module breaks the build.
 */
public final class DisputesExample {

    private DisputesExample() {
    }

    static List<Issue> openDisputes(AllegroCredentials credentials) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            // Streams are lazy — the filter's status/order criteria propagate to every page.
            return client.disputes().streamIssues(IssueFilter.builder()
                            .status(IssueStatus.DISPUTE_ONGOING)
                            .build())
                    .toList();
        }
    }

    static Issue readIssue(AllegroCredentials credentials, String issueId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            return client.disputes().get(issueId);
        }
    }

    static List<IssueChatEntry> readChat(AllegroCredentials credentials, String issueId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            return client.disputes().streamChat(issueId).toList();
        }
    }
}
