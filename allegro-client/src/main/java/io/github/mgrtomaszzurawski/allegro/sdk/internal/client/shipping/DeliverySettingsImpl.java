/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.shipping;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.DeliverySettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsView;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Endpoint wrapper behind the {@link DeliverySettings} facade — a stateless view
 * over the shared runtime.
 *
 * @since 0.3.0
 */
public final class DeliverySettingsImpl implements DeliverySettings {

    private static final String OP_GET = "get delivery settings";
    private static final String OP_UPDATE = "update delivery settings";

    private final HttpSupport http;

    public DeliverySettingsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public DeliverySettingsView get() {
        DeliverySettingsResponseRaw raw = http.getAuthenticated(
                ApiPaths.DELIVERY_SETTINGS, DeliverySettingsResponseRaw.class, OP_GET);
        return DeliverySettingsView.from(raw);
    }

    @Override
    public DeliverySettingsView update(DeliverySettingsRequest request) {
        DeliverySettingsResponseRaw raw = http.putJsonAuthenticated(
                ApiPaths.DELIVERY_SETTINGS, request.toRaw(),
                DeliverySettingsResponseRaw.class, OP_UPDATE);
        return DeliverySettingsView.from(raw);
    }
}
