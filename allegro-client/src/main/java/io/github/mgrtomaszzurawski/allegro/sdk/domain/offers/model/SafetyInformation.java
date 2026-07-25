/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentsSafetyInformationAttachmentsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentsSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NoSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementSafetyInformationRequestSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementSafetyInformationResponseSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TextSafetyInformationRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A product's GPSR safety information on a {@link ProductSetElement}. The EU General Product
 * Safety Regulation lets a seller declare product safety either as free {@link #TEXT text}, as
 * a set of {@link #ATTACHMENTS attachment} documents, or as an explicit "{@link #NONE none}";
 * this value flattens that {@code oneOf} into a {@link #type() type} discriminator plus the
 * payload each form carries.
 *
 * <p>The same immutable value is used both ways. On READ it is projected from the response with
 * {@link #from(ProductSetElementSafetyInformationResponseSafetyInformationRaw) from} and can
 * carry any of the three forms. For a WRITE, build one with {@link #text(String)} or
 * {@link #attachments(List)} and attach it via {@code ProductSetElement.withSafetyInformation};
 * Allegro accepts only {@code TEXT} and {@code ATTACHMENTS} on a request (the "none" form is a
 * read-only server state), so {@link #toRaw()} rejects a {@code NONE}/untyped value.
 *
 * @param type          the safety-information form ({@code TEXT}/{@code ATTACHMENTS}/
 *                      {@code NONE}), or {@code null}
 * @param description   the free-text safety description (only for {@code TEXT}), or {@code null}
 * @param attachmentIds the ids of the safety-document attachments (only for {@code ATTACHMENTS},
 *                      empty otherwise)
 * @since 0.6.0
 */
public record SafetyInformation(
        @Nullable String type,
        @Nullable String description,
        List<String> attachmentIds) {

    /** The {@code TEXT} safety-information form (a free-text description). */
    public static final String TEXT = "TEXT";
    /** The {@code ATTACHMENTS} safety-information form (a set of document attachments). */
    public static final String ATTACHMENTS = "ATTACHMENTS";
    /** The explicit "no safety information" form (Allegro's {@code NO_SAFETY_INFORMATION}). */
    public static final String NONE = "NO_SAFETY_INFORMATION";

    private static final String ERR_EMPTY_ATTACHMENTS = "attachmentIds must not be empty";
    private static final String ERR_NOT_WRITABLE =
            "safety information can only be sent as TEXT or ATTACHMENTS on a request, not: ";

    /** Canonical constructor: normalizes {@code attachmentIds} to an immutable copy. */
    public SafetyInformation {
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }

    /** A free-text safety declaration (the {@code TEXT} form). */
    public static SafetyInformation text(String description) {
        return new SafetyInformation(TEXT, Objects.requireNonNull(description, "description"), List.of());
    }

    /**
     * A safety declaration backed by document attachments (the {@code ATTACHMENTS} form); the
     * ids reference previously uploaded safety documents. At least one id is required.
     */
    public static SafetyInformation attachments(List<String> attachmentIds) {
        List<String> copiedIds = List.copyOf(Objects.requireNonNull(attachmentIds, "attachmentIds"));
        if (copiedIds.isEmpty()) {
            throw new IllegalArgumentException(ERR_EMPTY_ATTACHMENTS);
        }
        return new SafetyInformation(ATTACHMENTS, null, copiedIds);
    }

    /** Project the generated {@code oneOf} safety-information onto the consumer value, or {@code null}. */
    public static @Nullable SafetyInformation from(
            @Nullable ProductSetElementSafetyInformationResponseSafetyInformationRaw raw) {
        if (raw == null) {
            return null;
        }
        Object instance = raw.getActualInstance();
        if (instance instanceof TextSafetyInformationRaw text) {
            return new SafetyInformation(
                    text.getType() == null ? null : text.getType().getValue(),
                    text.getDescription(),
                    List.of());
        }
        if (instance instanceof AttachmentsSafetyInformationRaw attachments) {
            List<AttachmentsSafetyInformationAttachmentsInnerRaw> items = attachments.getAttachments();
            return new SafetyInformation(
                    attachments.getType() == null ? null : attachments.getType().getValue(),
                    null,
                    items == null ? List.of()
                            : items.stream().map(AttachmentsSafetyInformationAttachmentsInnerRaw::getId)
                                    .filter(Objects::nonNull).toList());
        }
        if (instance instanceof NoSafetyInformationRaw none) {
            return new SafetyInformation(none.getType() == null ? null : none.getType().getValue(),
                    null, List.of());
        }
        return null;
    }

    /**
     * The generated request {@code oneOf}: the {@code TEXT} or {@code ATTACHMENTS} form. Allegro
     * accepts only these two on a create/edit request, so a {@code NONE}/untyped value (which can
     * only originate from a read) is rejected with {@link IllegalStateException}.
     */
    public ProductSetElementSafetyInformationRequestSafetyInformationRaw toRaw() {
        if (TEXT.equals(type)) {
            return new ProductSetElementSafetyInformationRequestSafetyInformationRaw(
                    new TextSafetyInformationRaw()
                            .type(TextSafetyInformationRaw.TypeEnum.TEXT)
                            .description(description));
        }
        if (ATTACHMENTS.equals(type)) {
            List<AttachmentsSafetyInformationAttachmentsInnerRaw> items = attachmentIds.stream()
                    .map(id -> new AttachmentsSafetyInformationAttachmentsInnerRaw().id(id)).toList();
            return new ProductSetElementSafetyInformationRequestSafetyInformationRaw(
                    new AttachmentsSafetyInformationRaw()
                            .type(AttachmentsSafetyInformationRaw.TypeEnum.ATTACHMENTS)
                            .attachments(items));
        }
        throw new IllegalStateException(ERR_NOT_WRITABLE + type);
    }
}
