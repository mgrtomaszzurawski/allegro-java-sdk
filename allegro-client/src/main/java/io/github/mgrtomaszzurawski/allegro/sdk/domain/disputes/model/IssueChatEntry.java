/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueAttachmentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueChatMessageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueMessageAuthorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueMessageRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One entry in a post-purchase issue chat — a message or a state change.
 *
 * @param id the entry identifier, or {@code null}
 * @param text the entry text, or {@code null}
 * @param createdAt when the entry was created, or {@code null}
 * @param author who wrote the entry, or {@code null}
 * @param attachments files attached to the entry; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record IssueChatEntry(
        @Nullable String id,
        @Nullable String text,
        @Nullable OffsetDateTime createdAt,
        @Nullable ChatAuthor author,
        List<IssueAttachment> attachments) {

    public IssueChatEntry {
        attachments = List.copyOf(attachments);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static IssueChatEntry from(PostPurchaseIssueChatMessageRaw raw) {
        PostPurchaseIssueMessageAuthorRaw author = raw.getAuthor();
        return new IssueChatEntry(
                raw.getId(),
                raw.getText(),
                raw.getCreatedAt(),
                author == null ? null : ChatAuthor.from(author),
                attachmentsOf(raw.getAttachments()));
    }

    /**
     * Map the message a write returns (the newly added message, {@code
     * PostPurchaseIssueMessage}) to the same chat-entry record. The created
     * message carries the identical fields as a chat entry, so it is surfaced as
     * one.
     */
    public static IssueChatEntry from(PostPurchaseIssueMessageRaw raw) {
        PostPurchaseIssueMessageAuthorRaw author = raw.getAuthor();
        return new IssueChatEntry(
                raw.getId(),
                raw.getText(),
                raw.getCreatedAt(),
                author == null ? null : ChatAuthor.from(author),
                attachmentsOf(raw.getAttachments()));
    }

    private static List<IssueAttachment> attachmentsOf(
            @Nullable List<PostPurchaseIssueAttachmentRaw> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream().map(IssueAttachment::from).toList();
    }
}
