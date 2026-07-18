/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder.ClassifiedStatsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifiedStats;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifieds;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.SellerClassifiedStats;
import java.util.List;

/**
 * Classifieds (advertisement) packages and statistics — reached via
 * {@code AllegroClient.classifieds()}.
 *
 * <p>Classifieds are Allegro's advertisement listings (for example automotive or
 * real-estate). This facade covers the package configurations a seller can
 * attach to a classified offer, the packages assigned to an offer, and the
 * advertisement statistics.
 *
 * <p>Bucket F (offers-extras): the classifieds package, assignment, and
 * statistics surface ships here; the {@code offers()}-attached extras (tags,
 * translations, rating, bundles) land per the task-division plan.
 *
 * @since 0.2.0
 */
public interface Classifieds {

    /**
     * The classifieds packages configured for a category — the packages a seller
     * may assign to an advertisement listed in that category.
     *
     * @param categoryId identifier of the category to read package
     *     configurations for
     * @return the packages available in the category; never {@code null},
     *     possibly empty
     */
    List<ClassifiedPackage> availablePackages(String categoryId);

    /**
     * The configuration of a single classifieds package.
     *
     * @param packageId identifier of the package to read
     * @return the package configuration
     */
    ClassifiedPackage getPackage(String packageId);

    /**
     * The classifieds packages currently assigned to an offer.
     *
     * @param offerId identifier of the offer to read the assignment for
     * @return the base and extra packages assigned to the offer
     */
    OfferClassifieds packagesOfOffer(String offerId);

    /**
     * Assign classifieds packages to an offer, replacing any current assignment.
     *
     * @param offerId identifier of the offer to assign packages to
     * @param assignment the base and extra packages to assign
     */
    void assignPackages(String offerId, ClassifiedAssignment assignment);

    /**
     * Daily advertisement statistics for one or more of the seller's offers.
     *
     * @param offerIds the offers to read statistics for (1 to 50 ids)
     * @param filter the optional date-time range
     * @return one statistics entry per offer that has data; never {@code null},
     *     possibly empty
     */
    List<OfferClassifiedStats> offerStats(List<String> offerIds, ClassifiedStatsFilter filter);

    /**
     * Daily advertisement statistics aggregated across all of the seller's
     * advertisements.
     *
     * @param filter the optional date-time range
     * @return the aggregated statistics
     */
    SellerClassifiedStats sellerStats(ClassifiedStatsFilter filter);
}
