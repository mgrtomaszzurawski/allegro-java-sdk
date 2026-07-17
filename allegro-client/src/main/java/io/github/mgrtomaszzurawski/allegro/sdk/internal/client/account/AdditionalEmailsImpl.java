/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalEmailRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalEmailRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalEmailsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.AdditionalEmails;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.AdditionalEmail;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;

/**
 * Endpoint wrapper behind the {@link AdditionalEmails} facade.
 *
 * @since 0.2.0
 */
public final class AdditionalEmailsImpl implements AdditionalEmails {

    private static final String OP_LIST = "list additional emails";
    private static final String OP_GET = "get additional email";
    private static final String OP_ADD = "add additional email";
    private static final String OP_DELETE = "delete additional email";

    private final HttpSupport http;

    public AdditionalEmailsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<AdditionalEmail> list() {
        AdditionalEmailsResponseRaw response = http.getAuthenticated(
                ApiPaths.ADDITIONAL_EMAILS, AdditionalEmailsResponseRaw.class, OP_LIST);
        List<AdditionalEmailRaw> items = response.getAdditionalEmails();
        if (items == null) {
            return List.of();
        }
        return items.stream().map(AdditionalEmail::from).toList();
    }

    @Override
    public AdditionalEmail get(String emailId) {
        return AdditionalEmail.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.ADDITIONAL_EMAILS, emailId),
                AdditionalEmailRaw.class, OP_GET));
    }

    @Override
    public AdditionalEmail add(String emailAddress) {
        AdditionalEmailRequestRaw request = new AdditionalEmailRequestRaw().email(emailAddress);
        return AdditionalEmail.from(http.postJsonAuthenticated(
                ApiPaths.ADDITIONAL_EMAILS, request, AdditionalEmailRaw.class, OP_ADD));
    }

    @Override
    public void delete(String emailId) {
        http.deleteAuthenticated(
                ApiPaths.subPath(ApiPaths.ADDITIONAL_EMAILS, emailId), OP_DELETE);
    }
}
