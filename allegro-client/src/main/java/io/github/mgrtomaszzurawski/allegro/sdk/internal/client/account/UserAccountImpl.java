/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import io.github.mgrtomaszzurawski.allegro.client.model.MeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.AdditionalEmails;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.UserAccount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Endpoint wrapper behind the {@link UserAccount} facade.
 *
 * @since 0.1.0
 */
public final class UserAccountImpl implements UserAccount {

    private static final String OP_ME = "get current user";

    private final HttpSupport http;
    private final AdditionalEmails additionalEmails;

    public UserAccountImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.additionalEmails = new AdditionalEmailsImpl(runtime);
    }

    @Override
    public CurrentUser me() {
        return CurrentUser.from(
                http.getAuthenticated(ApiPaths.CURRENT_USER, MeResponseRaw.class, OP_ME));
    }

    @Override
    public AdditionalEmails additionalEmails() {
        return additionalEmails;
    }
}
