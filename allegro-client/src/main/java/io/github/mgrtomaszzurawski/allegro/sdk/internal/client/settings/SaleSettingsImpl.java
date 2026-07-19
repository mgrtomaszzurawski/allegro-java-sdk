/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.SaleSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.Compliance;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.aftersale.AfterSaleConditionsImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.compliance.ComplianceImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;

/**
 * Root implementation of the {@link SaleSettings} facade; wires the nested
 * sub-facades per bucket K.
 *
 * @since 0.2.0
 */
public final class SaleSettingsImpl implements SaleSettings {

    private final AfterSaleConditions afterSaleConditions;
    private final Compliance compliance;

    public SaleSettingsImpl(HttpRuntime runtime) {
        this.afterSaleConditions = new AfterSaleConditionsImpl(runtime);
        this.compliance = new ComplianceImpl(runtime);
    }

    @Override
    public AfterSaleConditions afterSale() {
        return afterSaleConditions;
    }

    @Override
    public Compliance compliance() {
        return compliance;
    }
}
