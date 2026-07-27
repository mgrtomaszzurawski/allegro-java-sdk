/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.ImageUrlRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductChangeProposalRequestRaw;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A proposal to change an existing catalogue product
 * ({@code catalog().products().proposeChange(productId, ...)}). It carries the full
 * corrected product picture — the {@code name} (required) and the {@code categoryId},
 * images and parameters as they should read — plus an optional free-text {@code note}
 * explaining the change and a flag to be e-mailed once Allegro verifies it. Allegro
 * moderates the change; read its state back with
 * {@code catalog().products().changeProposal(changeProposalId)}.
 *
 * @since 0.2.0
 */
public final class ProductChangeProposalRequest {

    private static final String ERR_NAME = "name must not be blank";

    private final String name;
    private final @Nullable String note;
    private final @Nullable String categoryId;
    private final List<String> imageUrls;
    private final List<ProductProposalParameter> parameters;
    private final @Nullable Boolean notifyViaEmailAfterVerification;
    private final @Nullable String language;

    private ProductChangeProposalRequest(Builder builder) {
        this.name = builder.name;
        this.note = builder.note;
        this.categoryId = builder.categoryId;
        this.imageUrls = List.copyOf(builder.imageUrls);
        this.parameters = List.copyOf(builder.parameters);
        this.notifyViaEmailAfterVerification = builder.notifyViaEmailAfterVerification;
        this.language = builder.language;
    }

    /** The corrected product name. */
    public String name() {
        return name;
    }

    /** The free-text note explaining the change, or {@code null}. */
    public @Nullable String note() {
        return note;
    }

    /** The corrected category id, or {@code null} to leave it. */
    public @Nullable String categoryId() {
        return categoryId;
    }

    /** The corrected image URLs (possibly empty). */
    public List<String> imageUrls() {
        return imageUrls;
    }

    /** The corrected parameters (possibly empty). */
    public List<ProductProposalParameter> parameters() {
        return parameters;
    }

    /** Whether to e-mail the seller after verification, or {@code null} for the default. */
    public @Nullable Boolean notifyViaEmailAfterVerification() {
        return notifyViaEmailAfterVerification;
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
    public ProductChangeProposalRequestRaw toRaw() {
        ProductChangeProposalRequestRaw raw = new ProductChangeProposalRequestRaw();
        raw.setName(name);
        if (note != null) {
            raw.setNote(note);
        }
        if (categoryId != null) {
            ProductCategoryRaw category = new ProductCategoryRaw();
            category.setId(categoryId);
            raw.setCategory(category);
        }
        if (!imageUrls.isEmpty()) {
            raw.setImages(imageUrls.stream().map(ProductChangeProposalRequest::imageRaw).toList());
        }
        if (!parameters.isEmpty()) {
            raw.setParameters(parameters.stream().map(ProductProposalParameter::toRaw).toList());
        }
        if (notifyViaEmailAfterVerification != null) {
            raw.setNotifyViaEmailAfterVerification(notifyViaEmailAfterVerification);
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

    /** Fluent fail-fast builder for {@link ProductChangeProposalRequest}. */
    public static final class Builder {

        private @Nullable String name;
        private @Nullable String note;
        private @Nullable String categoryId;
        private final List<String> imageUrls = new ArrayList<>();
        private final List<ProductProposalParameter> parameters = new ArrayList<>();
        private @Nullable Boolean notifyViaEmailAfterVerification;
        private @Nullable String language;

        /** The corrected product name (required). */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** A free-text note explaining the change. */
        public Builder note(@Nullable String note) {
            this.note = note;
            return this;
        }

        /** The corrected category id. */
        public Builder categoryId(@Nullable String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        /** Add one corrected image URL. */
        public Builder addImageUrl(String imageUrl) {
            this.imageUrls.add(imageUrl);
            return this;
        }

        /** Add one corrected parameter. */
        public Builder addParameter(ProductProposalParameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        /** Ask Allegro to e-mail the seller once the change is verified. */
        public Builder notifyViaEmailAfterVerification(@Nullable Boolean notify) {
            this.notifyViaEmailAfterVerification = notify;
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
         * @throws IllegalStateException if {@code name} is blank
         */
        public ProductChangeProposalRequest build() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException(ERR_NAME);
            }
            return new ProductChangeProposalRequest(this);
        }
    }
}
