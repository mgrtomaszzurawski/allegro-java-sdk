/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ValidationErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ValidationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ValidationWarningRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The result of Allegro's validation of an offer, reached from {@link Offer#validation()}. A
 * create/edit is accepted asynchronously; the response carries this block so the seller learns
 * WHY the offer is in its current state — blocking {@link #errors() errors} (which keep it from
 * activating) and non-blocking {@link #warnings() warnings} (e.g. GPSR safety-info verification
 * reminders) — each reusing the same {@link AllegroFieldError} shape as a rejected request.
 *
 * @param errors      blocking validation errors (empty when the offer validated cleanly)
 * @param warnings    non-blocking validation warnings (empty when there are none)
 * @param validatedAt when the offer was validated, or {@code null}
 * @since 0.6.0
 */
public record OfferValidation(
        List<AllegroFieldError> errors,
        List<AllegroFieldError> warnings,
        @Nullable OffsetDateTime validatedAt) {

    /** Canonical constructor: normalizes the error/warning lists to immutable copies. */
    public OfferValidation {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }

    /** Project the generated validation block onto the consumer value, or {@code null}. */
    public static @Nullable OfferValidation from(@Nullable ValidationRaw raw) {
        if (raw == null) {
            return null;
        }
        return new OfferValidation(errorsOf(raw.getErrors()), warningsOf(raw.getWarnings()),
                raw.getValidatedAt());
    }

    private static List<AllegroFieldError> errorsOf(@Nullable List<ValidationErrorRaw> raws) {
        return raws == null ? List.of() : raws.stream()
                .map(raw -> new AllegroFieldError(raw.getCode(), raw.getMessage(),
                        raw.getUserMessage(), raw.getPath(), raw.getDetails(), raw.getMetadata()))
                .toList();
    }

    private static List<AllegroFieldError> warningsOf(@Nullable List<ValidationWarningRaw> raws) {
        return raws == null ? List.of() : raws.stream()
                .map(raw -> new AllegroFieldError(raw.getCode(), raw.getMessage(),
                        raw.getUserMessage(), raw.getPath(), raw.getDetails(), raw.getMetadata()))
                .toList();
    }
}
