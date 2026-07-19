/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ImageFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferAttachment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferImage;

/**
 * Offer media: uploading the images and document attachments an offer references.
 * Obtained from {@link Offers#media()}.
 *
 * <p>An image is uploaded in one step and yields a hosted URL to put in a
 * {@code CreateOfferRequest}'s image list. An attachment is two steps — declare it
 * (kind + file name), then upload the file bytes to the returned id.
 *
 * @since 0.4.0
 */
public interface OfferMedia {

    /** Upload image {@code bytes} of the given format; returns the hosted image URL. */
    OfferImage uploadImage(byte[] bytes, ImageFormat format);

    /** Upload an image by URL (Allegro fetches it); returns the hosted image URL. */
    OfferImage uploadImage(String imageUrl);

    /** Declare an offer attachment (kind + file name); returns it with an id to upload to. */
    OfferAttachment createAttachment(AttachmentDeclaration declaration);

    /** Upload the file {@code bytes} for a declared attachment; returns it with the hosted URL. */
    OfferAttachment uploadAttachment(String attachmentId, byte[] bytes);

    /** Read a declared offer attachment by id. */
    OfferAttachment getAttachment(String attachmentId);
}
