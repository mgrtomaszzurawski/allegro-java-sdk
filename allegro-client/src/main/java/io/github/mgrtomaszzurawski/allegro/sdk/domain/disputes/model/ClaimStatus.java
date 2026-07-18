/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

/**
 * The formal status a seller sets on a claim — an acceptance variant or a
 * rejection reason. Not valid for disputes (only claims carry a formal status).
 *
 * <p>A request-side enum passed to {@code disputes.changeStatus(...)}. Choose an
 * {@code ACCEPTED_*} value to grant the claim (use {@link #ACCEPTED_PARTIAL_REFUND}
 * together with a partial-refund amount) or a {@code REJECTED_*} value to decline
 * it with a documented reason.
 *
 * <p>The constant names match the Allegro wire values verbatim.
 *
 * @since 0.2.0
 */
public enum ClaimStatus {

    /** Accept the claim and repair the product. */
    ACCEPTED_REPAIR,

    /** Accept the claim and refund the full amount. */
    ACCEPTED_REFUND,

    /** Accept the claim and exchange the product. */
    ACCEPTED_EXCHANGE,

    /** Accept the claim with a partial refund (supply the amount). */
    ACCEPTED_PARTIAL_REFUND,

    /** Reject: the buyer did not complete the additional requirements. */
    REJECTED_ADDITIONAL_REQUIREMENTS_NOT_COMPLETED,

    /** Reject: the product was not returned. */
    REJECTED_PRODUCT_NOT_RETURNED,

    /** Reject: the product was damaged by the buyer. */
    REJECTED_PRODUCT_DAMAGED_BY_USER,

    /** Reject: the product conforms to the contract. */
    REJECTED_PRODUCT_CONFORMS_TO_CONTRACT,

    /** Reject: the defect is minor. */
    REJECTED_MINOR_DEFECT,

    /** Reject for another documented reason. */
    REJECTED_OTHER,

    /** Reject: the claim was withdrawn by the buyer. */
    REJECTED_CLAIM_WITHDRAWN_BY_BUYER
}
