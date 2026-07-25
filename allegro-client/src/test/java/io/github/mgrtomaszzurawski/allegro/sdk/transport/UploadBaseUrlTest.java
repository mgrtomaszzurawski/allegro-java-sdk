/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** The {@link HttpRuntime#uploadBaseUrl()} derivation (API base {@code //api.} → {@code //upload.}). */
class UploadBaseUrlTest {

    private static final String PROD_API = "https://api.allegro.pl";
    private static final String PROD_UPLOAD = "https://upload.allegro.pl";
    private static final String SANDBOX_API = "https://api.allegro.pl.allegrosandbox.pl";
    private static final String SANDBOX_UPLOAD = "https://upload.allegro.pl.allegrosandbox.pl";
    private static final String WIREMOCK_BASE = "http://localhost:8080";

    private static HttpRuntime withBase(String base) {
        return new HttpRuntime() {
            @Override public String baseUrl() {
                return base;
            }

            @Override public RetryHandler retryHandler() {
                return null;
            }

            @Override public AllegroExecutionInterceptor executionInterceptor() {
                return AllegroExecutionInterceptor.noop();
            }

            @Override public ObjectMapper objectMapper() {
                return null;
            }

            @Override public Duration readTimeout() {
                return null;
            }

            @Override public String requireToken() {
                return null;
            }

            @Override public void reauthenticate() {
                // unused in this test
            }
        };
    }

    @Test
    void uploadBaseUrl_whenProduction_rewritesApiToUpload() {
        assertEquals(PROD_UPLOAD, withBase(PROD_API).uploadBaseUrl());
    }

    @Test
    void uploadBaseUrl_whenSandbox_rewritesApiToUpload() {
        assertEquals(SANDBOX_UPLOAD, withBase(SANDBOX_API).uploadBaseUrl());
    }

    @Test
    void uploadBaseUrl_whenNotAnApiHost_isUnchanged() {
        // a WireMock (or any non-api.) host has no //api. label, so it is used as-is
        assertEquals(WIREMOCK_BASE, withBase(WIREMOCK_BASE).uploadBaseUrl());
    }
}
