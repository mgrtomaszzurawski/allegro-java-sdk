/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferTranslationsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTranslations;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;
import java.util.Objects;

/**
 * Endpoint wrapper behind the {@link OfferTranslations} facade.
 *
 * @since 0.2.0
 */
public final class OfferTranslationsImpl implements OfferTranslations {

    private static final String OP_OF_OFFER = "get offer translations";
    private static final String OP_UPDATE = "update offer translation";
    private static final String OP_DELETE = "delete offer translation";

    private static final String ERR_OFFER_ID_NULL = "offerId must not be null";
    private static final String ERR_LANGUAGE_NULL = "language must not be null";
    private static final String ERR_REQUEST_NULL = "request must not be null";

    private final HttpSupport http;

    public OfferTranslationsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<OfferTranslation> ofOffer(String offerId) {
        Objects.requireNonNull(offerId, ERR_OFFER_ID_NULL);
        OfferTranslationsRaw raw = http.request(OP_OF_OFFER)
                .get(ApiPaths.offerTranslations(offerId))
                .fetch(OfferTranslationsRaw.class);
        return OfferTranslation.listFrom(raw);
    }

    @Override
    public void update(String offerId, String language, TranslationRequest request) {
        Objects.requireNonNull(offerId, ERR_OFFER_ID_NULL);
        Objects.requireNonNull(language, ERR_LANGUAGE_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        http.request(OP_UPDATE)
                .patch(ApiPaths.offerTranslation(offerId, language))
                .jsonBody(OfferExtrasMapper.toRaw(request))
                .send();
    }

    @Override
    public void delete(String offerId, String language) {
        Objects.requireNonNull(offerId, ERR_OFFER_ID_NULL);
        Objects.requireNonNull(language, ERR_LANGUAGE_NULL);
        http.request(OP_DELETE)
                .delete(ApiPaths.offerTranslation(offerId, language))
                .send();
    }
}
