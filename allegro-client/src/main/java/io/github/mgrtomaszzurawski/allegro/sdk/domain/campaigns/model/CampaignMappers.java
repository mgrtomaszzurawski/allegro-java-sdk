/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationRejectionReasonRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ErrorRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Shared mapping helpers for the campaigns model records: nullable-money assembly,
 * badge rejection-reason list mapping, and AlleDiscount command-error mapping.
 * Package-private — not part of the public surface.
 */
final class CampaignMappers {

    private CampaignMappers() {
    }

    /**
     * Assemble a {@link Money} from a wire {@code {amount, currency}} pair, or
     * {@code null} when either half is absent. Allegro sometimes returns a price
     * object whose {@code amount}/{@code currency} are missing (see
     * {@code KNOWN-SERVER-BEHAVIORS.md}); such an incomplete price maps to
     * {@code null} rather than failing the read.
     */
    static @Nullable Money nullableMoney(@Nullable String amount, @Nullable String currency) {
        return amount == null || currency == null ? null : Money.of(amount, currency);
    }

    /** Map the wire rejection-reason list (possibly absent) to public records. */
    static List<CampaignRefusalReason> rejectionReasons(
            @Nullable List<BadgeApplicationRejectionReasonRaw> reasons) {
        return reasons == null
                ? List.of()
                : reasons.stream().map(CampaignRefusalReason::from).toList();
    }

    /**
     * Map an AlleDiscount command's coded error list ({@code output.errors[]}, a
     * flat {@code Error[]} since the 2026-11 spec — the earlier spec nested it one
     * level deeper) to violations, tolerating an absent list and a code-only error
     * whose message the server omitted.
     */
    static List<ConditionViolation> commandErrorViolations(@Nullable List<ErrorRaw> errors) {
        return errors == null
                ? List.of()
                : errors.stream().map(ConditionViolation::from).toList();
    }
}
