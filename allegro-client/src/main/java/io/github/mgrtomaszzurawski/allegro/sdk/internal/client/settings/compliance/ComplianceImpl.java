/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.compliance;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonsGET200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducersGET200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.Compliance;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsiblePersonRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsibleProducerRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePerson;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsibleProducer;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link Compliance} facade.
 *
 * @since 0.3.0
 */
public final class ComplianceImpl implements Compliance {

    private static final String OP_STREAM_PERSONS = "list responsible persons";
    private static final String OP_CREATE_PERSON = "create responsible person";
    private static final String OP_UPDATE_PERSON = "update responsible person";
    private static final String OP_STREAM_PRODUCERS = "list responsible producers";
    private static final String OP_GET_PRODUCER = "get responsible producer";
    private static final String OP_CREATE_PRODUCER = "create responsible producer";
    private static final String OP_UPDATE_PRODUCER = "update responsible producer";

    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_LIMIT = "limit";
    /** Spec cap on {@code limit} for both list endpoints (also their default). */
    private static final int PAGE_LIMIT = 1000;

    private static final String ERR_PERSON_ID_NULL = "responsiblePersonId must not be null";
    private static final String ERR_PRODUCER_ID_NULL = "responsibleProducerId must not be null";
    private static final String ERR_REQUEST_NULL = "request must not be null";

    private final HttpSupport http;

    public ComplianceImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<ResponsiblePerson> streamResponsiblePersons() {
        return PagedSpliterator.stream(this::fetchPersonPage);
    }

    private PagedSpliterator.Page<ResponsiblePerson> fetchPersonPage(int pageIndex) {
        int offset = pageIndex * PAGE_LIMIT;
        ResponsiblePersonsGET200ResponseRaw page = http.request(OP_STREAM_PERSONS)
                .get(ApiPaths.RESPONSIBLE_PERSONS)
                .query(pageQuery(offset))
                .fetch(ResponsiblePersonsGET200ResponseRaw.class);
        List<ResponsiblePersonResponseRaw> items =
                page.getResponsiblePersons() == null ? List.of() : page.getResponsiblePersons();
        List<ResponsiblePerson> persons = items.stream().map(ResponsiblePerson::from).toList();
        return new PagedSpliterator.Page<>(persons, hasNextPage(offset, persons.size(), page.getTotalCount()));
    }

    @Override
    public ResponsiblePerson createResponsiblePerson(ResponsiblePersonRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return ResponsiblePerson.from(http.request(OP_CREATE_PERSON)
                .post(ApiPaths.RESPONSIBLE_PERSONS)
                .jsonBody(ResponsiblePersonMapper.toCreateRaw(request))
                .fetch(ResponsiblePersonResponseRaw.class));
    }

    @Override
    public ResponsiblePerson updateResponsiblePerson(String responsiblePersonId, ResponsiblePersonRequest request) {
        Objects.requireNonNull(responsiblePersonId, ERR_PERSON_ID_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        UUID id = UUID.fromString(responsiblePersonId);
        return ResponsiblePerson.from(http.request(OP_UPDATE_PERSON)
                .put(ApiPaths.subPath(ApiPaths.RESPONSIBLE_PERSONS, responsiblePersonId))
                .jsonBody(ResponsiblePersonMapper.toUpdateRaw(id, request))
                .fetch(ResponsiblePersonResponseRaw.class));
    }

    @Override
    public Stream<ResponsibleProducer> streamResponsibleProducers() {
        return PagedSpliterator.stream(this::fetchProducerPage);
    }

    private PagedSpliterator.Page<ResponsibleProducer> fetchProducerPage(int pageIndex) {
        int offset = pageIndex * PAGE_LIMIT;
        ResponsibleProducersGET200ResponseRaw page = http.request(OP_STREAM_PRODUCERS)
                .get(ApiPaths.RESPONSIBLE_PRODUCERS)
                .query(pageQuery(offset))
                .fetch(ResponsibleProducersGET200ResponseRaw.class);
        List<ResponsibleProducerResponseRaw> items =
                page.getResponsibleProducers() == null ? List.of() : page.getResponsibleProducers();
        List<ResponsibleProducer> producers = items.stream().map(ResponsibleProducer::from).toList();
        return new PagedSpliterator.Page<>(producers, hasNextPage(offset, producers.size(), page.getTotalCount()));
    }

    @Override
    public ResponsibleProducer responsibleProducer(String responsibleProducerId) {
        Objects.requireNonNull(responsibleProducerId, ERR_PRODUCER_ID_NULL);
        return ResponsibleProducer.from(http.request(OP_GET_PRODUCER)
                .get(ApiPaths.subPath(ApiPaths.RESPONSIBLE_PRODUCERS, responsibleProducerId))
                .fetch(ResponsibleProducerResponseRaw.class));
    }

    @Override
    public ResponsibleProducer createResponsibleProducer(ResponsibleProducerRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return ResponsibleProducer.from(http.request(OP_CREATE_PRODUCER)
                .post(ApiPaths.RESPONSIBLE_PRODUCERS)
                .jsonBody(ResponsibleProducerMapper.toCreateRaw(request))
                .fetch(ResponsibleProducerResponseRaw.class));
    }

    @Override
    public ResponsibleProducer updateResponsibleProducer(String responsibleProducerId,
            ResponsibleProducerRequest request) {
        Objects.requireNonNull(responsibleProducerId, ERR_PRODUCER_ID_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        UUID id = UUID.fromString(responsibleProducerId);
        return ResponsibleProducer.from(http.request(OP_UPDATE_PRODUCER)
                .put(ApiPaths.subPath(ApiPaths.RESPONSIBLE_PRODUCERS, responsibleProducerId))
                .jsonBody(ResponsibleProducerMapper.toUpdateRaw(id, request))
                .fetch(ResponsibleProducerResponseRaw.class));
    }

    private static Query pageQuery(int offset) {
        return Query.create()
                .add(PARAM_OFFSET, offset)
                .add(PARAM_LIMIT, PAGE_LIMIT);
    }

    /**
     * Whether the lazy walk should request another page. When the server reports a
     * {@code totalCount} the walk stops once the current offset plus this page's
     * size reaches it; otherwise it falls back to the full-page heuristic (a page
     * shorter than the requested limit ends the walk).
     */
    private static boolean hasNextPage(int offset, int pageSize, @Nullable Integer totalCount) {
        if (totalCount != null) {
            return offset + pageSize < totalCount;
        }
        return pageSize == PAGE_LIMIT;
    }
}
