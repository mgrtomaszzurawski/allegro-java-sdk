/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductNameProposalRaw;
import org.jspecify.annotations.Nullable;

/**
 * One proposed field of a {@link ProductChangeProposal} — the product name — showing
 * the {@code current} value, the {@code proposal}, an optional {@code reason}, and how
 * Allegro {@link #resolution() resolved} it.
 *
 * @param current the value before the change, or {@code null}
 * @param proposal the proposed value, or {@code null}
 * @param reason the reviewer's reason, or {@code null}
 * @param resolution how the proposal was resolved
 * @since 0.2.0
 */
public record ProductNameProposal(
        @Nullable String current,
        @Nullable String proposal,
        @Nullable String reason,
        ProposalResolution resolution) {

    /** Map the generated Layer-1 proposal DTO onto the domain record. */
    public static ProductNameProposal from(ProductNameProposalRaw raw) {
        String wireResolution = raw.getResolutionType() == null
                ? null
                : raw.getResolutionType().getValue();
        return new ProductNameProposal(
                raw.getCurrent(), raw.getProposal(), raw.getReason(),
                ProposalResolution.from(wireResolution));
    }
}
