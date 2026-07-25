/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A reference to a seller-registered resource by exactly one of its {@linkplain #byId(String) id}
 * or its {@linkplain #byName(String) name}. Several create-request attachments — the seller's
 * contact, additional-services group, fundraising campaign and wholesale price list — accept
 * either form; Allegro resolves a name to the stored resource. On read those attachments come
 * back resolved to their id.
 *
 * <p>Use this shared value for the plain {@code {id|name}} attachments. The GPSR
 * {@link ResponsibleProducerRef}/{@link ResponsiblePersonRef} references keep their own types
 * because their wire form is a discriminated {@code oneOf} (a {@code type: ID|NAME} field), not a
 * plain object.
 *
 * @param id   the registered resource id, or {@code null} when referenced by name
 * @param name the resource name, or {@code null} when referenced by id
 * @since 0.6.0
 */
public record NamedReference(@Nullable String id, @Nullable String name) {

    private static final String ERR_ONE_OF = "a reference needs exactly one of id or name";

    /** Canonical constructor: exactly one of {@code id} / {@code name} must be set. */
    public NamedReference {
        if ((id == null) == (name == null)) {
            throw new IllegalArgumentException(ERR_ONE_OF);
        }
    }

    /** Reference the resource by its registered id. */
    public static NamedReference byId(String id) {
        return new NamedReference(Objects.requireNonNull(id, "id"), null);
    }

    /** Reference the resource by its name (Allegro resolves it to the stored resource). */
    public static NamedReference byName(String name) {
        return new NamedReference(null, Objects.requireNonNull(name, "name"));
    }
}
