/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentFileRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAttachmentRequestRaw;
import java.util.Objects;

/**
 * A declaration of an offer attachment to create: the document {@link #type() kind}
 * and the {@link #fileName() file name}. Pass it to {@code media().createAttachment(...)},
 * then upload the file bytes with {@code media().uploadAttachment(id, bytes)}.
 *
 * @param type     the document kind (required; not {@link AttachmentType#UNKNOWN})
 * @param fileName the file name to declare (required)
 * @since 0.4.0
 */
public record AttachmentDeclaration(AttachmentType type, String fileName) {

    /** Canonical constructor: type and file name are required. */
    public AttachmentDeclaration {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fileName, "fileName");
    }

    /** An attachment declaration of the given kind and file name. */
    public static AttachmentDeclaration of(AttachmentType type, String fileName) {
        return new AttachmentDeclaration(type, fileName);
    }

    /** The generated request body for this declaration. */
    public OfferAttachmentRequestRaw toRaw() {
        return new OfferAttachmentRequestRaw()
                .type(type.toRaw())
                ._file(new AttachmentFileRequestRaw().name(fileName));
    }
}
