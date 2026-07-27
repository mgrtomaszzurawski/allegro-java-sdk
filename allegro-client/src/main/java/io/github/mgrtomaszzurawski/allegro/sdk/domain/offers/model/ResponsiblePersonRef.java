/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementResponsiblePersonRequestResponsiblePersonRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetAllOfResponsiblePersonRaw;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The GPSR responsible person (EU compliance economic operator) of a
 * {@linkplain ProductSetElement product-set element}. Reference a person you registered
 * (via the responsible-persons settings) either {@linkplain #byId(String) by its id} or
 * {@linkplain #byName(String) by its name}; exactly one is set. The same immutable value is
 * used both ways — build one to attach a person to an offer, or read one back from an
 * {@link Offer}. On read Allegro returns the person only by {@code id} (it resolves a name to
 * the stored operator), so a read value always carries the id.
 *
 * @param id   the registered person id, or {@code null} when referenced by name
 * @param name the person name, or {@code null} when referenced by id
 * @since 0.4.0
 */
public record ResponsiblePersonRef(@Nullable String id, @Nullable String name) {

    private static final String ERR_ONE_OF = "a responsible person needs exactly one of id or name";

    /** Canonical constructor: exactly one of {@code id} / {@code name} must be set. */
    public ResponsiblePersonRef {
        if ((id == null) == (name == null)) {
            throw new IllegalArgumentException(ERR_ONE_OF);
        }
    }

    /** Reference a registered person by its id. */
    public static ResponsiblePersonRef byId(String id) {
        return new ResponsiblePersonRef(Objects.requireNonNull(id, "id"), null);
    }

    /** Reference a person by its name (Allegro matches it to a registered operator). */
    public static ResponsiblePersonRef byName(String name) {
        return new ResponsiblePersonRef(null, Objects.requireNonNull(name, "name"));
    }

    /** Project a generated response person (id-only) onto the consumer value. */
    public static ResponsiblePersonRef from(
            SaleProductOfferResponseV1AllOfProductSetAllOfResponsiblePersonRaw raw) {
        return new ResponsiblePersonRef(raw.getId(), null);
    }

    /**
     * The generated request person: the flat {@code {id}} or {@code {name}} form. Exactly one
     * field is populated, matching the reference form this value was built with.
     */
    public ProductSetElementResponsiblePersonRequestResponsiblePersonRaw toRaw() {
        ProductSetElementResponsiblePersonRequestResponsiblePersonRaw raw =
                new ProductSetElementResponsiblePersonRequestResponsiblePersonRaw();
        if (id != null) {
            return raw.id(id);
        }
        return raw.name(name);
    }
}
