/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferRaw;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * An inline product definition attached to a {@linkplain ProductSetElement product-set element}
 * on a create request — used to define a brand-new product, or to override a catalogue product's
 * non-identifying data, in the same call that creates the offer (the "one-request product offer").
 *
 * <p>This is a WRITE-side shape: build one with {@link #builder()} and attach it via
 * {@link ProductSetElement#withInlineProduct(InlineProduct)}. Every field is optional — supply the
 * {@link #name()}, the {@link #categoryId() product category}, the {@link #parameters() catalogue
 * parameters} (reusing the same {@link OfferParameter} values as the offer-level parameters) and
 * the product {@link #images()} the definition needs. The product reference itself (id / GTIN/MPN)
 * stays on the {@link ProductSetElement}.
 *
 * @param name       the product name, or {@code null} to leave it to the catalogue
 * @param categoryId the product category id, or {@code null}
 * @param images     the product image urls (empty when none)
 * @param parameters the product's catalogue parameters to send (empty when none)
 * @since 0.6.0
 */
public record InlineProduct(
        @Nullable String name,
        @Nullable String categoryId,
        List<String> images,
        List<OfferParameter> parameters) {

    /** Canonical constructor: normalizes {@code images} and {@code parameters} to immutable copies. */
    public InlineProduct {
        images = images == null ? List.of() : List.copyOf(images);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    /** A new, empty builder. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Apply this inline definition onto the given generated product request object, next to the
     * id / idType the {@link ProductSetElement} already set. Package-private: the {@code *Raw}
     * type never appears on the public surface.
     */
    void applyTo(ProductOfferRaw raw) {
        if (name != null) {
            raw.name(name);
        }
        if (categoryId != null) {
            raw.category(new ProductCategoryRaw().id(categoryId));
        }
        if (!images.isEmpty()) {
            raw.images(images);
        }
        if (!parameters.isEmpty()) {
            raw.parameters(parameters.stream().map(OfferParameter::toRaw).toList());
        }
    }

    /** Fluent builder for an {@link InlineProduct}; every field is optional. */
    public static final class Builder {

        private @Nullable String name;
        private @Nullable String categoryId;
        private final List<String> images = new ArrayList<>();
        private final List<OfferParameter> parameters = new ArrayList<>();

        private Builder() {
        }

        /** Set the product name. */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Set the product category id. */
        public Builder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        /** Add one product image url. */
        public Builder image(String imageUrl) {
            this.images.add(Objects.requireNonNull(imageUrl, "imageUrl"));
            return this;
        }

        /** Replace the product images with the given urls. */
        public Builder images(List<String> imageUrls) {
            Objects.requireNonNull(imageUrls, "imageUrls");
            this.images.clear();
            imageUrls.forEach(this::image);
            return this;
        }

        /** Add one product parameter. */
        public Builder parameter(OfferParameter parameter) {
            this.parameters.add(Objects.requireNonNull(parameter, "parameter"));
            return this;
        }

        /** Replace the product parameters with the given values. */
        public Builder parameters(List<OfferParameter> productParameters) {
            Objects.requireNonNull(productParameters, "parameters");
            this.parameters.clear();
            productParameters.forEach(this::parameter);
            return this;
        }

        /** Build the immutable inline product definition. */
        public InlineProduct build() {
            return new InlineProduct(name, categoryId, images, parameters);
        }
    }
}
