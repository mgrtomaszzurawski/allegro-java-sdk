/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentsSafetyInformationAttachmentsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentsSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NoSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementSafetyInformationResponseSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TextSafetyInformationRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A product's GPSR safety information as read back on a {@link ProductSetElement}. The EU
 * General Product Safety Regulation lets a seller declare product safety either as free {@link
 * #TEXT text}, as a set of {@link #ATTACHMENTS attachment} documents, or as an explicit "{@link
 * #NONE none}"; this value flattens that {@code oneOf} into a {@link #type() type} discriminator
 * plus the payload each form carries.
 *
 * @param type          the safety-information form as reported by Allegro ({@code TEXT}/
 *                      {@code ATTACHMENTS}/{@code NONE}), or {@code null}
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

    /** Canonical constructor: normalizes {@code attachmentIds} to an immutable copy. */
    public SafetyInformation {
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
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
                            : items.stream().map(AttachmentsSafetyInformationAttachmentsInnerRaw::getId).toList());
        }
        if (instance instanceof NoSafetyInformationRaw none) {
            return new SafetyInformation(none.getType() == null ? null : none.getType().getValue(),
                    null, List.of());
        }
        return null;
    }
}
