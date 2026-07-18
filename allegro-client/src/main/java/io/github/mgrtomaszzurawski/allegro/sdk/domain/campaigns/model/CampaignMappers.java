/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationRejectionReasonRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ErrorsHolderRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Shared mapping helpers for the badge model records: nullable-money assembly and
 * rejection-reason list mapping. Package-private — not part of the public surface.
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
     * Flatten the doubly-nested command error structure ({@code errors[].errors[]})
     * to a flat list of messages, tolerating absent levels.
     */
    static List<String> commandErrorMessages(@Nullable List<ErrorsHolderRaw> holders) {
        if (holders == null) {
            return List.of();
        }
        List<String> messages = new ArrayList<>();
        for (ErrorsHolderRaw holder : holders) {
            if (holder.getErrors() != null) {
                for (ErrorRaw error : holder.getErrors()) {
                    messages.add(error.getMessage());
                }
            }
        }
        return List.copyOf(messages);
    }
}
