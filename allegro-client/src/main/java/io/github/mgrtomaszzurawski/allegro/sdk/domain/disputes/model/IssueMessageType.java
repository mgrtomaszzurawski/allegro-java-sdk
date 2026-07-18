/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

/**
 * The kind of message a seller posts into a post-purchase issue.
 *
 * <p>A request-side enum: the seller chooses one when adding a message. Most
 * messages are {@link #REGULAR}; the others drive a formal transition and are
 * context-restricted by Allegro — {@link #END_REQUEST} is only valid on a
 * dispute, while the {@code RETURN_*} types are only valid on a claim.
 *
 * <p>The constant names match the Allegro wire values verbatim, so the SDK
 * serializes them without a translation table.
 *
 * @since 0.2.0
 */
public enum IssueMessageType {

    /** An ordinary message in the discussion. */
    REGULAR,

    /** Request to end a dispute (disputes only). */
    END_REQUEST,

    /** Accept a return and provide the seller's own shipping label (claims only). */
    RETURN_REQUIRED_SELLER_LABEL,

    /** Accept a return the buyer arranges and pays for (claims only). */
    RETURN_REQUIRED_CUSTOM,

    /** Accept the claim without requiring the product to be returned (claims only). */
    RETURN_NOT_REQUIRED
}
