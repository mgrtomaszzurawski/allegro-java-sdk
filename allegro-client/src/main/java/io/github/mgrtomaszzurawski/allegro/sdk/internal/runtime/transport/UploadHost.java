/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

/**
 * Derives Allegro's binary-media upload host from the REST API base URL. Allegro
 * serves {@code POST /sale/images} (and returns attachment upload URLs) from
 * {@code upload.*} rather than {@code api.*}; the two hosts differ only in that
 * label, so the upload base is the API base with {@code //api.} rewritten to
 * {@code //upload.}. A base that is not an {@code //api.} host (e.g. a WireMock
 * test host) is returned unchanged, so the same request still reaches it.
 *
 * @since 0.4.0
 */
final class UploadHost {

    /** The API host label in a base URL (e.g. {@code https://api.allegro.pl}). */
    private static final String API_HOST_MARKER = "//api.";
    /** The upload host label that replaces it (e.g. {@code https://upload.allegro.pl}). */
    private static final String UPLOAD_HOST_MARKER = "//upload.";

    private UploadHost() {
    }

    /** The upload host base for {@code apiBaseUrl}; unchanged if it is not an {@code //api.} host. */
    static String from(String apiBaseUrl) {
        int marker = apiBaseUrl.indexOf(API_HOST_MARKER);
        if (marker < 0) {
            return apiBaseUrl;
        }
        return apiBaseUrl.substring(0, marker)
                + UPLOAD_HOST_MARKER
                + apiBaseUrl.substring(marker + API_HOST_MARKER.length());
    }
}
