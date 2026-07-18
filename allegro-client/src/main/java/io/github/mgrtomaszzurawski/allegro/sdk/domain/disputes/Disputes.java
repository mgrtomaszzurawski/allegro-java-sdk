/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.Issue;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueChatEntry;
import java.util.stream.Stream;

/**
 * Post-purchase issues — disputes and claims — reached via
 * {@code AllegroClient.disputes()}.
 *
 * <p>Wraps the {@code /sale/issues} resource (a <strong>beta</strong> API served with the
 * {@code application/vnd.allegro.beta.v1+json} media type); every operation needs the
 * {@code disputes} OAuth scope. Issues are opened by buyers, so this facade is read-oriented:
 * list the issues on the account, read one, and read its chat.
 *
 * <p>The seller-side write operations of this resource (add a message, change a claim's
 * status, attach a file) need a beta request-body media type that the shared transport does
 * not yet expose; they ship once that core capability lands (tracked in the shared backlog).
 *
 * @since 0.2.0
 */
public interface Disputes {

    /**
     * The post-purchase issues on the authenticated seller's account, most-recent first,
     * as a lazily paginated stream.
     *
     * @param filter status / order filter (use {@link IssueFilter#none()} for all)
     * @return a lazy stream over the matching issues; never {@code null}
     */
    Stream<Issue> streamIssues(IssueFilter filter);

    /**
     * A single dispute or claim by its identifier.
     *
     * @param issueId the issue identifier
     * @return the issue
     */
    Issue get(String issueId);

    /**
     * The chat of a post-purchase issue — its messages and state changes — oldest first,
     * as a lazily paginated stream.
     *
     * @param issueId the issue identifier
     * @return a lazy stream over the issue's chat entries; never {@code null}
     */
    Stream<IssueChatEntry> streamChat(String issueId);
}
