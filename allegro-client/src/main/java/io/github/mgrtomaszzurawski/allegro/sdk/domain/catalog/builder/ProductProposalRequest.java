/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.ImageUrlRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductProposalsRequestRaw;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A new-product proposal ({@code catalog().products().propose(...)}): the seller's
 * suggestion of a catalogue product Allegro does not yet carry. A proposal needs a
 * {@code name} and the {@code categoryId} it belongs to; images, parameters and a
 * listing language are optional. Allegro moderates the proposal — the response
 * carries its {@code PROPOSED}/{@code LISTED} status.
 *
 * <p>The standardized {@code description} is not modelled yet (field-depth follow-up).
 *
 * <pre>{@code
 * ProductProposal created = catalog.products().propose(
 *         ProductProposalRequest.builder()
 *                 .name("ACME Widget 3000")
 *                 .categoryId("257")
 *                 .addImageUrl("https://img.example/widget.jpg")
 *                 .addParameter(ProductProposalParameter.ofValueIds("11323", "1"))
 *                 .build());
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ProductProposalRequest {

    private static final String ERR_NAME = "name must not be blank";
    private static final String ERR_CATEGORY_ID = "categoryId must not be blank";

    private final String name;
    private final String categoryId;
    private final List<String> imageUrls;
    private final List<ProductProposalParameter> parameters;
    private final @Nullable String language;

    private ProductProposalRequest(Builder builder) {
        this.name = builder.name;
        this.categoryId = builder.categoryId;
        this.imageUrls = List.copyOf(builder.imageUrls);
        this.parameters = List.copyOf(builder.parameters);
        this.language = builder.language;
    }

    /** The proposed product name. */
    public String name() {
        return name;
    }

    /** The category the product belongs to. */
    public String categoryId() {
        return categoryId;
    }

    /** The proposed product image URLs (possibly empty). */
    public List<String> imageUrls() {
        return imageUrls;
    }

    /** The proposed product parameters (possibly empty). */
    public List<ProductProposalParameter> parameters() {
        return parameters;
    }

    /** The listing language, or {@code null} for the account default. */
    public @Nullable String language() {
        return language;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Project onto the generated Layer-1 request DTO. */
    public ProductProposalsRequestRaw toRaw() {
        ProductProposalsRequestRaw raw = new ProductProposalsRequestRaw();
        raw.setName(name);
        ProductCategoryRaw category = new ProductCategoryRaw();
        category.setId(categoryId);
        raw.setCategory(category);
        if (!imageUrls.isEmpty()) {
            raw.setImages(imageUrls.stream().map(ProductProposalRequest::imageRaw).toList());
        }
        if (!parameters.isEmpty()) {
            raw.setParameters(parameters.stream().map(ProductProposalParameter::toRaw).toList());
        }
        if (language != null) {
            raw.setLanguage(language);
        }
        return raw;
    }

    private static ImageUrlRaw imageRaw(String url) {
        ImageUrlRaw raw = new ImageUrlRaw();
        raw.setUrl(url);
        return raw;
    }

    /** Fluent fail-fast builder for {@link ProductProposalRequest}. */
    public static final class Builder {

        private @Nullable String name;
        private @Nullable String categoryId;
        private final List<String> imageUrls = new ArrayList<>();
        private final List<ProductProposalParameter> parameters = new ArrayList<>();
        private @Nullable String language;

        /** The proposed product name (required). */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** The category the product belongs to (required). */
        public Builder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        /** Add one product image URL. */
        public Builder addImageUrl(String imageUrl) {
            this.imageUrls.add(imageUrl);
            return this;
        }

        /** Add one product parameter. */
        public Builder addParameter(ProductProposalParameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        /** Localize the proposal to this language (e.g. {@code pl-PL}). */
        public Builder language(@Nullable String language) {
            this.language = language;
            return this;
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if {@code name} or {@code categoryId} is blank
         */
        public ProductProposalRequest build() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException(ERR_NAME);
            }
            if (categoryId == null || categoryId.isBlank()) {
                throw new IllegalStateException(ERR_CATEGORY_ID);
            }
            return new ProductProposalRequest(this);
        }
    }
}
