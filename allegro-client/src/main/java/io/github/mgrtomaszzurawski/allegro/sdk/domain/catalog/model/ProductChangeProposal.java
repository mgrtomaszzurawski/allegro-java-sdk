/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductChangeProposalDtoRaw;
import org.jspecify.annotations.Nullable;

/**
 * The state of a proposed change to a catalogue product — returned by
 * {@code catalog().products().proposeChange(...)} and read back by
 * {@code catalog().products().changeProposal(changeProposalId)}. It carries the
 * proposal id, an optional note, the notify flag, the language, and the proposed
 * {@link #name()} with its resolution.
 *
 * <p>The proposed category, images and parameters are not read back yet (field-depth
 * follow-up); {@link #name()} is the modelled proposed field.
 *
 * @param id the change-proposal id
 * @param name the proposed name change with its resolution, or {@code null}
 * @param note the free-text note attached to the proposal, or {@code null}
 * @param notifyViaEmailAfterVerification whether the seller is e-mailed after verification, or {@code null}
 * @param language the listing language, or {@code null}
 * @since 0.2.0
 */
public record ProductChangeProposal(
        @Nullable String id,
        @Nullable ProductNameProposal name,
        @Nullable String note,
        @Nullable Boolean notifyViaEmailAfterVerification,
        @Nullable String language) {

    /** Map the generated Layer-1 response DTO onto the domain record. */
    public static ProductChangeProposal from(ProductChangeProposalDtoRaw raw) {
        ProductNameProposal name = raw.getName() == null
                ? null
                : ProductNameProposal.from(raw.getName());
        return new ProductChangeProposal(
                raw.getId(), name, raw.getNote(),
                raw.getNotifyViaEmailAfterVerification(), raw.getLanguage());
    }
}
