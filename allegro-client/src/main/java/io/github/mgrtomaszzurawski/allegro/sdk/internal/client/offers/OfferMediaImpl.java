/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferAttachmentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferImageLinkUploadRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferImageUploadResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.OfferMedia;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ImageFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferAttachment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferImage;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Endpoint wrapper behind the {@link OfferMedia} facade. The image upload and the
 * attachment binary {@code PUT} target the Allegro upload host ({@code onUploadHost});
 * the attachment declaration and read use the API base.
 *
 * @since 0.4.0
 */
public final class OfferMediaImpl implements OfferMedia {

    private static final String OP_UPLOAD_IMAGE = "upload offer image";
    private static final String OP_UPLOAD_IMAGE_URL = "upload offer image by URL";
    private static final String OP_CREATE_ATTACHMENT = "create offer attachment";
    private static final String OP_UPLOAD_ATTACHMENT = "upload offer attachment";
    private static final String OP_GET_ATTACHMENT = "get offer attachment";
    /** Offer attachments are documents; the upload endpoint takes them as {@code application/pdf}. */
    private static final String ATTACHMENT_CONTENT_TYPE = "application/pdf";

    private final HttpSupport http;

    public OfferMediaImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public OfferImage uploadImage(byte[] bytes, ImageFormat format) {
        return OfferImage.from(http.request(OP_UPLOAD_IMAGE)
                .post(ApiPaths.SALE_IMAGES)
                .onUploadHost()
                .binaryBody(bytes, format.mediaType())
                .fetch(OfferImageUploadResponseRaw.class));
    }

    @Override
    public OfferImage uploadImage(String imageUrl) {
        return OfferImage.from(http.request(OP_UPLOAD_IMAGE_URL)
                .post(ApiPaths.SALE_IMAGES)
                .onUploadHost()
                .jsonBody(new OfferImageLinkUploadRequestRaw().url(imageUrl))
                .fetch(OfferImageUploadResponseRaw.class));
    }

    @Override
    public OfferAttachment createAttachment(AttachmentDeclaration declaration) {
        return OfferAttachment.from(http.request(OP_CREATE_ATTACHMENT)
                .post(ApiPaths.OFFER_ATTACHMENTS)
                .jsonBody(declaration.toRaw())
                .fetch(OfferAttachmentRaw.class));
    }

    @Override
    public OfferAttachment uploadAttachment(String attachmentId, byte[] bytes) {
        return OfferAttachment.from(http.request(OP_UPLOAD_ATTACHMENT)
                .put(ApiPaths.offerAttachment(attachmentId))
                .onUploadHost()
                .binaryBody(bytes, ATTACHMENT_CONTENT_TYPE)
                .fetch(OfferAttachmentRaw.class));
    }

    @Override
    public OfferAttachment getAttachment(String attachmentId) {
        return OfferAttachment.from(http.getAuthenticated(
                ApiPaths.offerAttachment(attachmentId), OfferAttachmentRaw.class, OP_GET_ATTACHMENT));
    }
}
