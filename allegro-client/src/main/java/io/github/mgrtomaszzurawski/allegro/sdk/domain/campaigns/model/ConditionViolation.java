/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountEligibleOfferDtoAlleDiscountCampaignConditionsViolationsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmittedOfferDtoProcessErrorsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ErrorRaw;
import org.jspecify.annotations.Nullable;

/**
 * A coded reason an offer does not meet an AlleDiscount campaign's conditions, why
 * its submission was rejected, or why a submit/withdraw command failed.
 *
 * @param code    stable reason code
 * @param message human-readable explanation, or {@code null} if the server sent none
 *
 * @since 0.2.0
 */
public record ConditionViolation(String code, @Nullable String message) {

    /** Map an eligible-offer condition violation to the public record. */
    static ConditionViolation from(
            AlleDiscountEligibleOfferDtoAlleDiscountCampaignConditionsViolationsInnerRaw raw) {
        return new ConditionViolation(raw.getCode(), raw.getMessage());
    }

    /** Map a submitted-offer process error to the public record. */
    static ConditionViolation from(AlleDiscountSubmittedOfferDtoProcessErrorsInnerRaw raw) {
        return new ConditionViolation(raw.getCode(), raw.getMessage());
    }

    /** Map a command-level error to the public record. */
    static ConditionViolation from(ErrorRaw raw) {
        return new ConditionViolation(raw.getCode(), raw.getMessage());
    }
}
