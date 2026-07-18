/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmittedOfferDtoProcessRaw;

/**
 * The lifecycle state of an offer submitted to an AlleDiscount campaign.
 *
 * @since 0.2.0
 */
public enum AlleDiscountOfferStatus {

    /** The submission is being verified. */
    VERIFICATION,

    /** Accepted, awaiting activation. */
    ACCEPTED,

    /** Currently active in the campaign. */
    ACTIVE,

    /** The submission was declined. */
    DECLINED,

    /** The offer's participation has ended. */
    FINISHED;

    /** Map the generated Layer-1 status enum to the public enum. */
    static AlleDiscountOfferStatus from(AlleDiscountSubmittedOfferDtoProcessRaw.StatusEnum wireValue) {
        return valueOf(wireValue.name());
    }
}
