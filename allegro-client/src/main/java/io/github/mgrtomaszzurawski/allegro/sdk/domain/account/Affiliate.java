/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.ConversionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CpsConversion;
import java.util.stream.Stream;

/**
 * Affiliate CPS (Cost Per Sale) conversions — reached via
 * {@code AllegroClient.affiliate()}. Beta resource; needs the
 * {@code affiliate:read} scope.
 *
 * @since 0.2.0
 */
public interface Affiliate {

    /**
     * Lazily stream CPS conversions matching a filter, newest pages fetched on
     * demand as the stream is consumed.
     *
     * @param filter selection criteria ({@link ConversionFilter#all()} for all)
     * @return a lazy stream of conversions
     */
    Stream<CpsConversion> streamCpsConversions(ConversionFilter filter);
}
