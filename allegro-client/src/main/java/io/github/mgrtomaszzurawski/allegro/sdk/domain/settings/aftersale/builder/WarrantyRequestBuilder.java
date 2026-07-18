/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link WarrantyRequest}. Replicates the server constraints
 * the SDK can check cheaply (required {@code name}/{@code type}/{@code individual}/
 * {@code corporate}, name and description length caps) and fails fast at
 * {@link #build()}. Both buyer-class warranty periods are mandatory: the endpoint
 * rejects a missing {@code individual} or {@code corporate} with a
 * {@code 422 UNPROCESSABLE_ENTITY} even though the spec marks neither
 * {@code required} (verified on the sandbox — see {@code KNOWN-SERVER-BEHAVIORS.md}).
 *
 * @since 0.2.0
 */
public final class WarrantyRequestBuilder {

    /** Server cap on the warranty name length. */
    private static final int MAX_NAME_LENGTH = 200;
    /** Server cap on the warranty description length. */
    private static final int MAX_DESCRIPTION_LENGTH = 10_240;

    private static final String ERR_NAME_REQUIRED = "Warranty name is required";
    private static final String ERR_NAME_TOO_LONG =
            "Warranty name exceeds the " + MAX_NAME_LENGTH + "-character limit";
    private static final String ERR_TYPE_REQUIRED = "Warranty type is required";
    private static final String ERR_INDIVIDUAL_REQUIRED =
            "Warranty individual period is required "
                    + "(the endpoint rejects its absence with 422 path=individual)";
    private static final String ERR_CORPORATE_REQUIRED =
            "Warranty corporate period is required "
                    + "(the endpoint rejects its absence with 422 path=corporate)";
    private static final String ERR_DESCRIPTION_TOO_LONG =
            "Warranty description exceeds the " + MAX_DESCRIPTION_LENGTH + "-character limit";
    private static final String ERR_ATTACHMENT_ID_INVALID =
            "Attachment id must be a valid UUID returned by the attachment upload";

    private @Nullable String name;
    private @Nullable WarrantyType type;
    private @Nullable WarrantyPeriod individual;
    private @Nullable WarrantyPeriod corporate;
    private @Nullable String attachmentId;
    private @Nullable String attachmentName;
    private @Nullable String description;

    WarrantyRequestBuilder() {
    }

    /** Set the warranty name (required, at most 200 characters). */
    public WarrantyRequestBuilder name(@Nullable String warrantyName) {
        this.name = warrantyName;
        return this;
    }

    /** Set the warrantor (required). */
    public WarrantyRequestBuilder type(@Nullable WarrantyType warrantyType) {
        this.type = warrantyType;
        return this;
    }

    /** Set the warranty duration for individual buyers (required). */
    public WarrantyRequestBuilder individual(@Nullable WarrantyPeriod period) {
        this.individual = period;
        return this;
    }

    /** Set the warranty duration for corporate buyers (required). */
    public WarrantyRequestBuilder corporate(@Nullable WarrantyPeriod period) {
        this.corporate = period;
        return this;
    }

    /**
     * Attach a previously uploaded warranty document.
     *
     * @param uploadedAttachmentId id returned by the attachment upload
     * @param fileName the document file name
     */
    public WarrantyRequestBuilder attachment(@Nullable String uploadedAttachmentId,
            @Nullable String fileName) {
        this.attachmentId = uploadedAttachmentId;
        this.attachmentName = fileName;
        return this;
    }

    /** Set the free-text description (at most 10240 characters). */
    public WarrantyRequestBuilder description(@Nullable String warrantyDescription) {
        this.description = warrantyDescription;
        return this;
    }

    /**
     * Validate and build the immutable request.
     *
     * @throws IllegalStateException if a required field is missing or a length
     *     constraint is violated
     */
    public WarrantyRequest build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException(ERR_NAME_TOO_LONG);
        }
        if (type == null) {
            throw new IllegalStateException(ERR_TYPE_REQUIRED);
        }
        if (individual == null) {
            throw new IllegalStateException(ERR_INDIVIDUAL_REQUIRED);
        }
        if (corporate == null) {
            throw new IllegalStateException(ERR_CORPORATE_REQUIRED);
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_TOO_LONG);
        }
        validateAttachmentId();
        return new WarrantyRequest(name, type, individual, corporate, attachmentId,
                attachmentName, description);
    }

    private void validateAttachmentId() {
        if (attachmentId == null) {
            return;
        }
        try {
            UUID.fromString(attachmentId);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(ERR_ATTACHMENT_ID_INVALID, e);
        }
    }
}
