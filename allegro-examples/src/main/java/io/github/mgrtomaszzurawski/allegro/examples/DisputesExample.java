/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.ClaimStatusChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueAttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.ClaimStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.Issue;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueAttachmentRef;
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

    static IssueChatEntry postReply(AllegroCredentials credentials, String issueId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            IssueMessageRequest reply = IssueMessageRequest.builder()
                    .text("Thank you - a replacement ships today.")
                    .build();
            return client.disputes().addMessage(issueId, reply);
        }
    }

    static void acceptClaimWithPartialRefund(AllegroCredentials credentials, String issueId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            ClaimStatusChange decision = ClaimStatusChange.builder()
                    .status(ClaimStatus.ACCEPTED_PARTIAL_REFUND)
                    .message("We agree to a partial refund.")
                    .partialRefund(Money.of("12.50", "PLN"))
                    .build();
            client.disputes().changeStatus(issueId, decision);
        }
    }

    static void attachPhotoToMessage(AllegroCredentials credentials, String issueId, byte[] bytes) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            IssueAttachmentDeclaration declaration = IssueAttachmentDeclaration.builder()
                    .filename("evidence.jpg")
                    .size(bytes.length)
                    .build();
            IssueAttachmentRef attachment = client.disputes()
                    .uploadAttachment(declaration, bytes, "image/jpeg");
            client.disputes().addMessage(issueId, IssueMessageRequest.builder()
                    .text("Please see the attached photo.")
                    .attachment(attachment.id())
                    .build());
        }
    }
}
