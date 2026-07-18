/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.ClaimStatusChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueAttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.Issue;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueAttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueChatEntry;
import java.util.stream.Stream;

/**
 * Post-purchase issues — disputes and claims — reached via
 * {@code AllegroClient.disputes()}.
 *
 * <p>Wraps the {@code /sale/issues} resource (a <strong>beta</strong> API served with the
 * {@code application/vnd.allegro.beta.v1+json} media type); every operation needs the
 * {@code disputes} OAuth scope. Issues are opened by buyers: read the issues on the account,
 * read one, read its chat, then respond as the seller — post a message, change a claim's
 * status, or attach a file to a message.
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

    /**
     * Add a seller message to a post-purchase issue. The message may carry text, referenced
     * attachments, or both; an {@link IssueMessageRequest} type other than {@code REGULAR}
     * drives a formal transition (ending a dispute, or a return decision on a claim).
     *
     * @param issueId the issue identifier
     * @param request the message to add
     * @return the created chat entry
     */
    IssueChatEntry addMessage(String issueId, IssueMessageRequest request);

    /**
     * Change the formal status of a claim — accept it (optionally with a partial refund) or
     * reject it with a documented reason. Valid only for claims, never for disputes.
     *
     * @param issueId the claim identifier
     * @param change the status change to apply
     */
    void changeStatus(String issueId, ClaimStatusChange change);

    /**
     * Upload a file to attach to an issue message. The SDK declares the attachment and streams
     * the bytes to the one-time upload location Allegro returns, then yields a reference whose
     * {@link IssueAttachmentRef#id() id} can be attached to a message via
     * {@link IssueMessageRequest.Builder#attachment(String)}.
     *
     * @param declaration the file name and exact byte size
     * @param content the file bytes (their length must equal the declared size)
     * @param contentType the file's media type (e.g. {@code image/jpeg})
     * @return a reference to the uploaded attachment
     */
    IssueAttachmentRef uploadAttachment(IssueAttachmentDeclaration declaration, byte[] content,
            String contentType);

    /**
     * Download the raw bytes of an issue attachment.
     *
     * @param attachmentId the attachment identifier
     * @return the attachment's bytes
     */
    byte[] downloadAttachment(String attachmentId);
}
