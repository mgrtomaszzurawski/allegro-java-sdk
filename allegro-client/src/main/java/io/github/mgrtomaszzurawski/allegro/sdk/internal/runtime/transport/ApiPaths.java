/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

/**
 * Single source of truth for Allegro REST paths. Domain clients build their
 * endpoint URIs exclusively from these constants — never from inline string
 * literals — so a path change is a one-line diff.
 *
 * @since 0.1.0
 */
public final class ApiPaths {

    // ---- account (bucket D) ----
    /** Basic information about the authenticated user ({@code /me}). */
    public static final String CURRENT_USER = "/me";

    // [append point: domain paths] Each domain bucket appends its own
    // "---- <feature> (bucket X) ----" section above this marker, one block
    // per bucket, in BACKLOG order.

    private ApiPaths() {
    }

    private static final char PATH_SEPARATOR = '/';

    /**
     * Join a base path with dynamic segments, guaranteeing exactly one
     * {@code /} separator between parts.
     */
    public static String subPath(String basePath, String... segments) {
        StringBuilder joined = new StringBuilder(basePath);
        for (String segment : segments) {
            if (joined.charAt(joined.length() - 1) != PATH_SEPARATOR) {
                joined.append(PATH_SEPARATOR);
            }
            joined.append(segment);
        }
        return joined.toString();
    }
}
