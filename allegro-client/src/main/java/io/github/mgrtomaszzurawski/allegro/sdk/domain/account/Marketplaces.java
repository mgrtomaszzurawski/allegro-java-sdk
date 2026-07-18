/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Marketplace;
import java.util.List;

/**
 * Details of the Allegro marketplaces — reached via {@code AllegroClient.marketplaces()}.
 *
 * <p>Marketplace metadata is public platform information: unlike the rest of
 * bucket D, {@link #list()} needs no user-context token and works with an
 * app-only client-credentials grant.
 *
 * @since 0.2.0
 */
public interface Marketplaces {

    /**
     * All marketplaces available on the platform, with the languages,
     * currencies and shipping countries each one supports.
     *
     * @return the marketplaces; never {@code null}, possibly empty
     */
    List<Marketplace> list();
}
