/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueReasonRaw;
import org.jspecify.annotations.Nullable;

/**
 * Why a buyer opened a post-purchase issue — the standardized reason the buyer
 * selected. A seller uses it to route and answer the issue.
 *
 * @since 0.2.0
 */
public enum IssueReasonType {

    /** The product was never received. */
    NO_PRODUCT_RECEIVED,
    /** The parcel arrived without the product inside. */
    NO_PRODUCT_IN_PARCEL,
    /** No proof of purchase, manual, or warranty was supplied. */
    NO_PROOF_OF_PURCHASE_MANUAL_OR_WARRANTY,
    /** An element of the product is missing. */
    MISSING_PRODUCT_ELEMENT,
    /** Both the product and the parcel were damaged in transit. */
    PRODUCT_AND_PARCEL_DAMAGED_IN_TRANSIT,
    /** The product is damaged although the parcel was intact. */
    PRODUCT_DAMAGED_PARCEL_INTACT,
    /** A defect was found during use. */
    DEFECT_FOUND_DURING_USE,
    /** The product is not as described. */
    NOT_AS_DESCRIBED,
    /** The seller does not want to accept a return. */
    SELLER_DOES_NOT_WANT_TO_ACCEPT_RETURN,
    /** There is a problem with sending the product back. */
    PROBLEM_WITH_SENDING_PRODUCT_BACK,
    /** No refund was issued after the product was returned. */
    NO_REFUND_AFTER_RETURNING_PRODUCT,
    /** No refund was issued after the order was cancelled. */
    NO_REFUND_AFTER_CANCELING_ORDER,
    /** The goods did not arrive after payment. */
    DID_NOT_RECEIVE_GOODS_AFTER_PAYMENT,
    /** The received items do not match the description. */
    RECEIVED_ITEMS_NOT_MATCHING_DESCRIPTION,
    /** The received order was incomplete. */
    RECEIVED_INCOMPLETE_ORDER,
    /** The item is damaged. */
    ITEM_IS_DAMAGED,
    /** There is a problem with the withdrawal or cancellation of the purchase. */
    PROBLEM_WITH_WITHDRAWAL_CANCELLATION_OF_PURCHASE,
    /** There is a problem with the goods return or cancellation of the purchase. */
    PROBLEM_WITH_GOODS_RETURN_CANCELLATION_OF_PURCHASE,
    /** A reason not covered by the above. */
    OTHER,
    /** A reason this SDK release does not model yet. */
    UNKNOWN;

    /**
     * Map the generated reason type, tolerating unknown future values. The domain
     * constant names mirror the wire values one-to-one, so a name lookup suffices;
     * an unmodelled value (including the generated {@code UNKNOWN_DEFAULT_OPEN_API}
     * sentinel) degrades to {@link #UNKNOWN}.
     */
    public static IssueReasonType from(PostPurchaseIssueReasonRaw.@Nullable TypeEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw.name());
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
