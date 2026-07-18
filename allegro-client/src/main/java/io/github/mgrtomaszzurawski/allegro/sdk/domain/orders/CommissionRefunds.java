/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ClaimFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RefundClaimRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.RefundClaim;
import java.util.stream.Stream;

/**
 * Commission-refund claims — reached via {@code orders().commissionRefunds()}:
 * ask Allegro to refund the sales commission on a line item, browse existing
 * claims, and cancel one.
 *
 * @since 0.6.0
 */
public interface CommissionRefunds {

    /**
     * Lazily stream the seller's commission-refund claims matching {@code filter}.
     *
     * @param filter the claim filter ({@link ClaimFilter#all()} for every claim)
     * @return a lazy stream of refund claims
     */
    Stream<RefundClaim> streamClaims(ClaimFilter filter);

    /**
     * Fetch a single commission-refund claim by id.
     *
     * @param claimId the claim identifier
     * @return the refund claim
     */
    RefundClaim get(String claimId);

    /**
     * File a new commission-refund claim.
     *
     * @param request the line item and quantity to claim
     * @return the id Allegro assigned the new claim
     */
    String claim(RefundClaimRequest request);

    /**
     * Cancel a commission-refund claim.
     *
     * @param claimId the claim identifier
     */
    void cancel(String claimId);
}
