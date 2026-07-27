/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.AdditionalServicesGroupRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.GroupTranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServiceCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.GroupTranslations;
import java.util.List;
import java.util.stream.Stream;

/**
 * Additional-services groups a seller offers on their listings — reached via
 * {@code AllegroClient.settings().additionalServices()}.
 *
 * <p>Reads: the definition catalog available to the seller, the seller's own groups
 * (list + single read), and a group's translations. Writes: group
 * {@link #createGroup(AdditionalServicesGroupRequest) create}/{@link
 * #updateGroup(String, AdditionalServicesGroupRequest) update} and per-language
 * translation {@link #upsertTranslation(String, String, GroupTranslationRequest)
 * upsert}/{@link #deleteTranslation(String, String) delete}.
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

    /**
     * Create an additional-services group.
     *
     * @param request the group to create (name and at least one service required)
     * @return the created group
     */
    AdditionalServicesGroup createGroup(AdditionalServicesGroupRequest request);

    /**
     * Replace an additional-services group (full PUT).
     *
     * @param groupId the group to update
     * @param request the new group content
     * @return the updated group
     */
    AdditionalServicesGroup updateGroup(String groupId, AdditionalServicesGroupRequest request);

    /**
     * Upsert a group's translation in one language (per-service descriptions).
     *
     * @param groupId the group id
     * @param language the language to write (e.g. {@code en-US})
     * @param request the per-service translated descriptions
     */
    void upsertTranslation(String groupId, String language, GroupTranslationRequest request);

    /**
     * Delete a group's translation in one language.
     *
     * @param groupId the group id
     * @param language the language to remove
     */
    void deleteTranslation(String groupId, String language);
}
