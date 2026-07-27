/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServiceCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.GroupTranslations;
import java.util.List;
import java.util.stream.Stream;

/**
 * Additional-services groups a seller offers on their listings — reached via
 * {@code AllegroClient.settings().additionalServices()}.
 *
 * <p>This slice exposes the read surface: the definition catalog available to the
 * seller, the seller's own groups (list + single read), and a group's
 * translations. Group create/update and translation writes ship separately.
 *
 * @since 0.3.0
 */
public interface AdditionalServices {

    /**
     * The additional-service definition catalog available to the seller, grouped
     * by category.
     *
     * @return the categories and their definitions
     */
    List<AdditionalServiceCategory> categoryDefinitions();

    /**
     * Lazily stream the seller's additional-services groups (offset/limit paging).
     *
     * @return a lazy {@link Stream} of groups
     */
    Stream<AdditionalServicesGroup> streamGroups();

    /**
     * Read a single additional-services group.
     *
     * @param groupId the group id
     * @return the group
     */
    AdditionalServicesGroup group(String groupId);

    /**
     * Read all translations defined for a group.
     *
     * @param groupId the group id
     * @return the group's per-language translations
     */
    GroupTranslations translations(String groupId);
}
