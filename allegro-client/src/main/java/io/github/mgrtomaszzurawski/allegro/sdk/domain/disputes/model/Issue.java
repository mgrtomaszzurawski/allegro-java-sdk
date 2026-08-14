/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueAttachmentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueCheckoutFormRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueExpectationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueProductRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueStateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueUserRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A post-purchase issue — a dispute or a claim raised by a buyer against an order.
 *
 * <p>The Allegro issues surface is a <strong>beta</strong> API and its schema marks no
 * field as required; the SDK treats {@code id}/{@code type}/{@code right} as always present
 * (they identify the issue and arrive on every response) and everything else as nullable.
 *
 * @param id server-assigned issue identifier
 * @param type dispute or claim
 * @param right the consumer right the issue is raised under
 * @param referenceNumber human-facing reference number, or {@code null}
 * @param subject the issue subject, or {@code null}
 * @param description the issue description, or {@code null}
 * @param reason the standardized reason the buyer opened the issue, or {@code null}
 * @param expectations what the buyer expects to resolve the issue; never {@code null},
 *     possibly empty
 * @param openedDate when the issue was opened, or {@code null}
 * @param decisionDueDate when a decision is due, or {@code null}
 * @param buyer the buyer who opened the issue, or {@code null}
 * @param checkoutFormId id of the related order (checkout form), or {@code null}
 * @param checkoutFormCreatedAt when the related order was created, or {@code null}
 * @param offerId id of the offer the issue concerns, or {@code null}
 * @param offerQuantity quantity of the offer the issue concerns, or {@code null}
 * @param productId id of the product the issue concerns, or {@code null}
 * @param attachments files the buyer attached to the issue; never {@code null},
 *     possibly empty
 * @param chat a summary of the issue's chat (count + initial/last message), or
 *     {@code null} when absent
 * @param state the current state, or {@code null}
 *
 * @since 0.2.0
 */
public record Issue(
        String id,
        IssueType type,
        IssueRight right,
        @Nullable String referenceNumber,
        @Nullable String subject,
        @Nullable String description,
        @Nullable IssueReason reason,
        List<IssueExpectation> expectations,
        @Nullable OffsetDateTime openedDate,
        @Nullable OffsetDateTime decisionDueDate,
        @Nullable IssueParticipant buyer,
        @Nullable String checkoutFormId,
        @Nullable OffsetDateTime checkoutFormCreatedAt,
        @Nullable String offerId,
        @Nullable Integer offerQuantity,
        @Nullable String productId,
        List<IssueAttachment> attachments,
        @Nullable IssueChatSummary chat,
        @Nullable IssueState state) {

    /** Canonical constructor normalizes null collections to empty and defensively copies. */
    public Issue {
        expectations = expectations == null ? List.of() : List.copyOf(expectations);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Issue from(PostPurchaseIssueRaw raw) {
        PostPurchaseIssueUserRaw buyer = raw.getBuyer();
        PostPurchaseIssueCheckoutFormRaw checkoutForm = raw.getCheckoutForm();
        PostPurchaseIssueOfferRaw offer = raw.getOffer();
        PostPurchaseIssueProductRaw product = raw.getProduct();
        PostPurchaseIssueStateRaw currentState = raw.getCurrentState();
        return new Issue(
                raw.getId(),
                IssueType.from(raw.getType()),
                IssueRight.from(raw.getRight()),
                raw.getReferenceNumber(),
                raw.getSubject(),
                raw.getDescription(),
                raw.getReason() == null ? null : IssueReason.from(raw.getReason()),
                expectationsOf(raw.getExpectations()),
                raw.getOpenedDate(),
                raw.getDecisionDueDate(),
                buyer == null ? null : IssueParticipant.from(buyer),
                checkoutForm == null ? null : checkoutForm.getId(),
                checkoutForm == null ? null : checkoutForm.getCreatedAt(),
                offer == null ? null : offer.getId(),
                offer == null ? null : offer.getQuantity(),
                productId(product),
                attachmentsOf(raw.getAttachments()),
                IssueChatSummary.from(raw.getChat()),
                currentState == null ? null : IssueState.from(currentState));
    }

    private static @Nullable String productId(@Nullable PostPurchaseIssueProductRaw product) {
        if (product == null || product.getId() == null) {
            return null;
        }
        return product.getId().toString();
    }

    private static List<IssueExpectation> expectationsOf(
            @Nullable List<PostPurchaseIssueExpectationRaw> expectations) {
        if (expectations == null) {
            return List.of();
        }
        return expectations.stream().map(IssueExpectation::from).toList();
    }

    private static List<IssueAttachment> attachmentsOf(
            @Nullable List<PostPurchaseIssueAttachmentRaw> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream().map(IssueAttachment::from).toList();
    }
}
