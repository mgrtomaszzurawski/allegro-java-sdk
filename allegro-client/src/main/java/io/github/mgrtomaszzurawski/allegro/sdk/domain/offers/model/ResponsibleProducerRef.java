/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementResponsibleProducerIdRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementResponsibleProducerNameRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementResponsibleProducerRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetAllOfResponsibleProducerRaw;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The GPSR responsible producer of a {@linkplain ProductSetElement product-set element}.
 * Reference an economic operator you registered (via the responsible-producers settings)
 * either {@linkplain #byId(String) by its id} or {@linkplain #byName(String) by its name};
 * exactly one is set. The same immutable value is used both ways — build one to attach a
 * producer to an offer, or read one back from an {@link Offer}. On read Allegro returns the
 * producer only by {@code id} (it resolves a name to the stored operator), so a read value
 * always carries the id.
 *
 * @param id   the registered producer id, or {@code null} when referenced by name
 * @param name the producer name, or {@code null} when referenced by id
 * @since 0.4.0
 */
public record ResponsibleProducerRef(@Nullable String id, @Nullable String name) {

    private static final String ERR_ONE_OF = "a responsible producer needs exactly one of id or name";
    /** Discriminator value for the id form (Jackson {@code type} property). */
    private static final String TYPE_ID = "ID";
    /** Discriminator value for the name form. */
    private static final String TYPE_NAME = "NAME";

    /** Canonical constructor: exactly one of {@code id} / {@code name} must be set. */
    public ResponsibleProducerRef {
        if ((id == null) == (name == null)) {
            throw new IllegalArgumentException(ERR_ONE_OF);
        }
    }

    /** Reference a registered producer by its id. */
    public static ResponsibleProducerRef byId(String id) {
        return new ResponsibleProducerRef(Objects.requireNonNull(id, "id"), null);
    }

    /** Reference a producer by its name (Allegro matches it to a registered operator). */
    public static ResponsibleProducerRef byName(String name) {
        return new ResponsibleProducerRef(null, Objects.requireNonNull(name, "name"));
    }

    /** Project a generated response producer (id-only) onto the consumer value. */
    public static ResponsibleProducerRef from(
            SaleProductOfferResponseV1AllOfProductSetAllOfResponsibleProducerRaw raw) {
        return new ResponsibleProducerRef(raw.getId(), null);
    }

    /**
     * The generated request producer: the id form ({@code type: ID}) or the name form
     * ({@code type: NAME}). The concrete subtype carries the discriminator Jackson emits
     * for the {@code oneOf}.
     */
    public ProductSetElementResponsibleProducerRequestRaw toRaw() {
        if (id != null) {
            return new ProductSetElementResponsibleProducerIdRequestRaw().type(TYPE_ID).id(id);
        }
        return new ProductSetElementResponsibleProducerNameRequestRaw().type(TYPE_NAME).name(name);
    }
}
