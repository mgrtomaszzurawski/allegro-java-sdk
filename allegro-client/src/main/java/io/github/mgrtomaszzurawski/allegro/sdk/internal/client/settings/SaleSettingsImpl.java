/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.SaleSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.aftersale.AfterSaleConditionsImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;

/**
 * Root implementation of the {@link SaleSettings} facade; wires the nested
 * sub-facades (after-sale conditions for the starter slice; more per bucket K).
 *
 * @since 0.2.0
 */
public final class SaleSettingsImpl implements SaleSettings {

    private final AfterSaleConditions afterSaleConditions;

    public SaleSettingsImpl(HttpRuntime runtime) {
        this.afterSaleConditions = new AfterSaleConditionsImpl(runtime);
    }

    @Override
    public AfterSaleConditions afterSale() {
        return afterSaleConditions;
    }
}
