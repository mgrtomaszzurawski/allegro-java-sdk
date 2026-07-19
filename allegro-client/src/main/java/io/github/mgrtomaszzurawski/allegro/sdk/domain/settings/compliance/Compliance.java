/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsiblePersonRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsibleProducerRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePerson;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsibleProducer;
import java.util.stream.Stream;

/**
 * Product-compliance (GPSR) responsible parties — reached via
 * {@code AllegroClient.settings().compliance()}.
 *
 * <p>Two dictionaries the seller maintains and attaches to offers: responsible
 * <em>persons</em> and responsible <em>producers</em>. Each supports list, create
 * and update; producers additionally support a single-resource read.
 *
 * @since 0.3.0
 */
public interface Compliance {

    /**
     * Lazily stream the seller's responsible persons (offset/limit paging).
     *
     * @return a lazy {@link Stream} of responsible persons
     */
    Stream<ResponsiblePerson> streamResponsiblePersons();

    /**
     * Create a responsible person.
     *
     * @param request the definition to create
     * @return the created responsible person (with its server id)
     */
    ResponsiblePerson createResponsiblePerson(ResponsiblePersonRequest request);

    /**
     * Update a responsible person.
     *
     * @param responsiblePersonId the id to update
     * @param request the new definition
     * @return the updated responsible person
     */
    ResponsiblePerson updateResponsiblePerson(String responsiblePersonId, ResponsiblePersonRequest request);

    /**
     * Lazily stream the seller's responsible producers (offset/limit paging).
     *
     * @return a lazy {@link Stream} of responsible producers
     */
    Stream<ResponsibleProducer> streamResponsibleProducers();

    /**
     * Read a single responsible producer.
     *
     * @param responsibleProducerId the id to read
     * @return the responsible producer
     */
    ResponsibleProducer responsibleProducer(String responsibleProducerId);

    /**
     * Create a responsible producer.
     *
     * @param request the definition to create
     * @return the created responsible producer (with its server id)
     */
    ResponsibleProducer createResponsibleProducer(ResponsibleProducerRequest request);

    /**
     * Update a responsible producer.
     *
     * @param responsibleProducerId the id to update
     * @param request the new definition
     * @return the updated responsible producer
     */
    ResponsibleProducer updateResponsibleProducer(String responsibleProducerId, ResponsibleProducerRequest request);
}
