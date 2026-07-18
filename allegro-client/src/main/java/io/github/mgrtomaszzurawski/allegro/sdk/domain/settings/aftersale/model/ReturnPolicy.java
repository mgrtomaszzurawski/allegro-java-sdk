/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyReturnCostRaw;
import org.jspecify.annotations.Nullable;

/**
 * A seller return-policy definition in full, as returned by
 * {@code afterSale().returnPolicy(id)}, {@code createReturnPolicy(...)},
 * {@code updateReturnPolicy(...)}, and the {@code streamReturnPolicies()} listing
 * (which, unlike the warranty listings, returns full policies rather than
 * summaries).
 *
 * @param id return-policy definition identifier
 * @param fulfillment whether the policy is for One Fulfillment offers (set at
 *     creation, immutable thereafter)
 * @param sellerId identifier of the owning seller (spec-required, always present)
 * @param name return-policy name (spec-required, always present)
 * @param availability whether returns are full, restricted or disabled
 * @param withdrawalPeriod ISO-8601 withdrawal period (whole days), or {@code null}
 *     when the range is {@code DISABLED}
 * @param returnCost who covers the return delivery cost, or {@code null}
 * @param address the return address, or {@code null} when the range is {@code DISABLED}
 * @param contact seller contact details, or {@code null} when none
 * @param options boolean return-handling options, or {@code null} when the range
 *     is {@code DISABLED}
 *
 * @since 0.3.0
 */
public record ReturnPolicy(
        String id,
        boolean fulfillment,
        String sellerId,
        String name,
        ReturnPolicyAvailability availability,
        @Nullable String withdrawalPeriod,
        @Nullable ReturnCostCoveredBy returnCost,
        @Nullable AfterSalesAddress address,
        @Nullable ReturnPolicyContact contact,
        @Nullable ReturnPolicyOptions options) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static ReturnPolicy from(ReturnPolicyResponseV1Raw raw) {
        // seller and name are spec-required, so they are trusted non-null.
        ReturnPolicyReturnCostRaw returnCost = raw.getReturnCost();
        return new ReturnPolicy(
                raw.getId().toString(),
                Boolean.TRUE.equals(raw.getIsFulfillment()),
                raw.getSeller().getId(),
                raw.getName(),
                ReturnPolicyAvailability.from(raw.getAvailability()),
                raw.getWithdrawalPeriod(),
                returnCost == null || returnCost.getCoveredBy() == null
                        ? null : ReturnCostCoveredBy.from(returnCost.getCoveredBy()),
                AfterSalesAddress.from(raw.getAddress()),
                ReturnPolicyContact.from(raw.getContact()),
                ReturnPolicyOptions.from(raw.getOptions()));
    }
}
