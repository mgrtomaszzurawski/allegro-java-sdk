/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import io.github.mgrtomaszzurawski.allegro.client.model.AllegroMarketplacesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceItemRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.Marketplaces;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Marketplace;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;

/**
 * Endpoint wrapper behind the {@link Marketplaces} facade.
 *
 * @since 0.2.0
 */
public final class MarketplacesImpl implements Marketplaces {

    private static final String OP_LIST = "list marketplaces";

    private final HttpSupport http;

    public MarketplacesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<Marketplace> list() {
        AllegroMarketplacesRaw response =
                http.getAuthenticated(ApiPaths.MARKETPLACES, AllegroMarketplacesRaw.class, OP_LIST);
        List<MarketplaceItemRaw> items = response.getMarketplaces();
        if (items == null) {
            return List.of();
        }
        return items.stream().map(Marketplace::from).toList();
    }
}
