/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
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
 * <p>Starter slice of bucket F (offers-extras): only
 * {@link #availablePackages(String)} ships first, as the end-to-end proof of the
 * classifieds surface; the remaining classifieds operations and the
 * {@code offers()}-attached extras (tags, translations, rating, bundles) land
 * per the task-division plan.
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
}
