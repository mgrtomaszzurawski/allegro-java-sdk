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
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Located;

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
    private static final String ERR_NOT_DECLARED =
            "uploadAttachment needs the attachment returned by createAttachment (it carries the upload URL)";

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
        // The declaration's response carries the one-time upload URL in its Location header;
        // capture it so uploadAttachment PUTs to exactly that address (Allegro's docs forbid
        // composing it — the URL is single-use and its format may change).
        Located<OfferAttachmentRaw> located = http.request(OP_CREATE_ATTACHMENT)
                .post(ApiPaths.OFFER_ATTACHMENTS)
                .jsonBody(declaration.toRaw())
                .fetchLocation(OfferAttachmentRaw.class);
        return OfferAttachment.from(located.value(), located.location());
    }

    @Override
    public OfferAttachment uploadAttachment(OfferAttachment declared, byte[] bytes, String contentType) {
        String uploadUrl = declared.uploadUrl();
        if (uploadUrl == null) {
            throw new IllegalArgumentException(ERR_NOT_DECLARED);
        }
        return OfferAttachment.from(http.request(OP_UPLOAD_ATTACHMENT)
                .putAbsolute(uploadUrl)
                .binaryBody(bytes, contentType)
                .fetch(OfferAttachmentRaw.class));
    }

    @Override
    public OfferAttachment getAttachment(String attachmentId) {
        return OfferAttachment.from(http.getAuthenticated(
                ApiPaths.offerAttachment(attachmentId), OfferAttachmentRaw.class, OP_GET_ATTACHMENT));
    }
}
