/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.Tag;
import java.util.List;
import java.util.stream.Stream;

/**
 * The seller's private offer tags and their assignment to offers — reached via
 * {@code AllegroClient.offers().tags()}.
 *
 * <p>Tags are a seller-only organisation aid (not visible to buyers): the seller
 * defines a set of tags and assigns them to offers. The tag catalogue reads and
 * writes use the {@code sale:settings:*} scopes; the per-offer assignment reads
 * and writes use {@code sale:offers:*}. All require a user (seller) token.
 *
 * @since 0.2.0
 */
public interface OfferTags {

    /**
     * Stream the seller's tags, fetched page by page and lazily.
     *
     * @return a lazy stream of tags
     */
    Stream<Tag> streamTags();

    /**
     * Create a tag.
     *
     * @param request the tag name (and optional hidden flag)
     * @return the identifier of the created tag
     */
    String create(TagRequest request);

    /**
     * Modify an existing tag.
     *
     * @param tagId the tag identifier
     * @param request the new tag name (and optional hidden flag)
     */
    void rename(String tagId, TagRequest request);

    /**
     * Delete a tag (also removing it from every offer it was assigned to).
     *
     * @param tagId the tag identifier
     */
    void delete(String tagId);

    /**
     * The tags currently assigned to an offer.
     *
     * @param offerId the offer identifier
     * @return the assigned tags; never {@code null}, possibly empty
     */
    List<Tag> ofOffer(String offerId);

    /**
     * Assign tags to an offer, replacing any current assignment.
     *
     * @param offerId the offer identifier
     * @param tagIds the identifiers of the tags to assign
     */
    void assignToOffer(String offerId, List<String> tagIds);
}
