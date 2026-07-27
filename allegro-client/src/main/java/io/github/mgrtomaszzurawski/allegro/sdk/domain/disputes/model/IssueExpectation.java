/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueExpectationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueRefundRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * A resolution the buyer expects for a post-purchase issue: what they want done
 * ({@link #name()}) and, when the expectation is a (partial) refund, how much
 * they expect back ({@link #refund()}).
 *
 * @param name what the buyer expects; never {@code null} (unmapped wire values map
 *     to {@link IssueExpectationName#UNKNOWN})
 * @param refund the expected refund amount, or {@code null} when not a refund
 *     expectation or the server did not quote one
 *
 * @since 0.2.0
 */
public record IssueExpectation(IssueExpectationName name, @Nullable Money refund) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static IssueExpectation from(PostPurchaseIssueExpectationRaw raw) {
        return new IssueExpectation(IssueExpectationName.from(raw.getName()), refundOf(raw.getRefund()));
    }

    private static @Nullable Money refundOf(@Nullable PostPurchaseIssueRefundRaw refund) {
        if (refund == null || refund.getAmount() == null || refund.getCurrency() == null) {
            return null;
        }
        return Money.of(refund.getAmount(), refund.getCurrency());
    }
}
