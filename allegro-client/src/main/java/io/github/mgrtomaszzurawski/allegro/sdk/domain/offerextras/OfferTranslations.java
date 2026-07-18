/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslation;
import java.util.List;

/**
 * An offer's translations into other languages — reached via
 * {@code AllegroClient.offers().translations()}.
 *
 * <p>The SDK currently covers the <strong>title</strong> translation; the richly
 * structured description and safety-information translations are not yet
 * modelled. All operations use the {@code sale:offers:*} scopes and need a user
 * (seller) token.
 *
 * @since 0.2.0
 */
public interface OfferTranslations {

    /**
     * The translations set for an offer, one per language.
     *
     * @param offerId the offer identifier
     * @return the offer's translations; never {@code null}, possibly empty
     */
    List<OfferTranslation> ofOffer(String offerId);

    /**
     * Set (create or update) an offer's translation for one language.
     *
     * @param offerId the offer identifier
     * @param language the BCP-47 language tag to translate into
     * @param request the translation to set
     */
    void update(String offerId, String language, TranslationRequest request);

    /**
     * Delete an offer's manual translation for one language.
     *
     * @param offerId the offer identifier
     * @param language the BCP-47 language tag whose translation to remove
     */
    void delete(String offerId, String language);
}
