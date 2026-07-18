/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.campaigns;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.allegro.client.model.AllegroPricesAccountParticipationResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyExcludeOfferItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyExcludeOffersCommandPreviewRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyManageOffersCommandResultRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidySubmitOfferItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidySubmitOffersCommandPreviewRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.AllegroPrices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.AllegroPricesOfferQuery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ExcludeOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ParticipationUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesOfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesParticipation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SubsidyCommandReport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link AllegroPrices} facade (bucket H, Allegro
 * Prices). Participation reads/writes and the subsidy commands map through the
 * generated DTOs; the offer-status query maps from a {@link JsonNode} to dodge the
 * generated {@code oneOf} deserializer (see {@link AllegroPricesMapper}). The
 * submit and exclusion commands are polled to a terminal per-offer state via
 * {@link CommandPoller}.
 *
 * @since 0.2.0
 */
public final class AllegroPricesClient implements AllegroPrices {

    private static final int PAGE_SIZE = 100;

    private static final String OP_PARTICIPATION = "get allegro prices participation";
    private static final String OP_UPDATE_PARTICIPATION = "update allegro prices participation";
    private static final String OP_STREAM_OFFERS = "stream allegro prices offers status";
    private static final String OP_SUBMIT = "submit allegro prices offers";
    private static final String OP_SUBMIT_POLL = "poll allegro prices submit command";
    private static final String OP_EXCLUDE = "exclude allegro prices offers";
    private static final String OP_EXCLUDE_POLL = "poll allegro prices exclusion command";

    private static final String ERR_NULL_UPDATE = "update must not be null";
    private static final String ERR_NULL_QUERY = "query must not be null";
    private static final String ERR_NULL_REQUEST = "request must not be null";

    private final HttpSupport http;
    private final CommandPoller commandPoller;

    public AllegroPricesClient(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.commandPoller = new CommandPoller();
    }

    @Override
    public AllegroPricesParticipation participation() {
        return AllegroPricesParticipation.from(http.request(OP_PARTICIPATION)
                .get(ApiPaths.ALLEGRO_PRICES_PARTICIPATIONS)
                .fetch(AllegroPricesAccountParticipationResponseRaw.class));
    }

    @Override
    public AllegroPricesParticipation updateParticipation(ParticipationUpdate update) {
        if (update == null) {
            throw new IllegalArgumentException(ERR_NULL_UPDATE);
        }
        return AllegroPricesParticipation.from(http.request(OP_UPDATE_PARTICIPATION)
                .patch(ApiPaths.ALLEGRO_PRICES_PARTICIPATIONS)
                .jsonBody(AllegroPricesMapper.toRaw(update))
                .fetch(AllegroPricesAccountParticipationResponseRaw.class));
    }

    @Override
    public Stream<AllegroPricesOfferStatus> streamOffersStatus(AllegroPricesOfferQuery query) {
        if (query == null) {
            throw new IllegalArgumentException(ERR_NULL_QUERY);
        }
        return PagedSpliterator.stream(pageIndex -> fetchOffersPage(query, pageIndex));
    }

    private PagedSpliterator.Page<AllegroPricesOfferStatus> fetchOffersPage(
            AllegroPricesOfferQuery query, int pageIndex) {
        JsonNode response = http.request(OP_STREAM_OFFERS)
                .post(ApiPaths.ALLEGRO_PRICES_OFFERS_QUERIES)
                .jsonBody(AllegroPricesMapper.toRaw(query, pageIndex * PAGE_SIZE, PAGE_SIZE))
                .fetch(JsonNode.class);
        JsonNode offers = AllegroPricesMapper.offersArray(response);
        List<AllegroPricesOfferStatus> items = new ArrayList<>();
        if (offers != null) {
            offers.forEach(item -> items.add(AllegroPricesMapper.offerStatusFrom(item)));
        }
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public SubsidyCommandReport submitOffers(SubmitOffersRequest request) {
        return runSubmit(request, null);
    }

    @Override
    public SubsidyCommandReport submitOffers(SubmitOffersRequest request, Duration timeout) {
        return runSubmit(request, timeout);
    }

    private SubsidyCommandReport runSubmit(SubmitOffersRequest request, @Nullable Duration timeout) {
        if (request == null) {
            throw new IllegalArgumentException(ERR_NULL_REQUEST);
        }
        SubsidyManageOffersCommandResultRaw accepted = http.request(OP_SUBMIT)
                .post(ApiPaths.ALLEGRO_PRICES_SUBMIT_COMMANDS)
                .jsonBody(AllegroPricesMapper.toRaw(request))
                .fetch(SubsidyManageOffersCommandResultRaw.class);
        String commandId = accepted.getCommandId();
        SubsidySubmitOffersCommandPreviewRaw terminal = await(
                () -> fetchSubmitPreview(commandId), AllegroPricesClient::submitTerminal, OP_SUBMIT, timeout);
        return SubsidyCommandReport.from(terminal);
    }

    @Override
    public SubsidyCommandReport excludeOffers(ExcludeOffersRequest request) {
        return runExclude(request, null);
    }

    @Override
    public SubsidyCommandReport excludeOffers(ExcludeOffersRequest request, Duration timeout) {
        return runExclude(request, timeout);
    }

    private SubsidyCommandReport runExclude(ExcludeOffersRequest request, @Nullable Duration timeout) {
        if (request == null) {
            throw new IllegalArgumentException(ERR_NULL_REQUEST);
        }
        SubsidyManageOffersCommandResultRaw accepted = http.request(OP_EXCLUDE)
                .post(ApiPaths.ALLEGRO_PRICES_EXCLUSION_COMMANDS)
                .jsonBody(AllegroPricesMapper.toRaw(request))
                .fetch(SubsidyManageOffersCommandResultRaw.class);
        String commandId = accepted.getCommandId();
        SubsidyExcludeOffersCommandPreviewRaw terminal = await(
                () -> fetchExcludePreview(commandId), AllegroPricesClient::excludeTerminal, OP_EXCLUDE, timeout);
        return SubsidyCommandReport.from(terminal);
    }

    private SubsidySubmitOffersCommandPreviewRaw fetchSubmitPreview(String commandId) {
        return http.request(OP_SUBMIT_POLL)
                .get(ApiPaths.subPath(ApiPaths.ALLEGRO_PRICES_SUBMIT_COMMANDS, commandId))
                .fetch(SubsidySubmitOffersCommandPreviewRaw.class);
    }

    private SubsidyExcludeOffersCommandPreviewRaw fetchExcludePreview(String commandId) {
        return http.request(OP_EXCLUDE_POLL)
                .get(ApiPaths.subPath(ApiPaths.ALLEGRO_PRICES_EXCLUSION_COMMANDS, commandId))
                .fetch(SubsidyExcludeOffersCommandPreviewRaw.class);
    }

    private <S> S await(Supplier<S> fetchStatus, Predicate<S> isTerminal, String operationName,
            @Nullable Duration timeout) {
        return timeout == null
                ? commandPoller.await(fetchStatus, isTerminal, operationName)
                : commandPoller.await(fetchStatus, isTerminal, operationName, timeout);
    }

    private static boolean submitTerminal(SubsidySubmitOffersCommandPreviewRaw preview) {
        List<SubsidySubmitOfferItemRaw> offers = preview.getOffers();
        return offers != null && !offers.isEmpty()
                && offers.stream().allMatch(offer ->
                        offer.getStatus() != SubsidySubmitOfferItemRaw.StatusEnum.IN_PROGRESS);
    }

    private static boolean excludeTerminal(SubsidyExcludeOffersCommandPreviewRaw preview) {
        List<SubsidyExcludeOfferItemRaw> offers = preview.getOffers();
        return offers != null && !offers.isEmpty()
                && offers.stream().allMatch(offer ->
                        offer.getStatus() != SubsidyExcludeOfferItemRaw.StatusEnum.IN_PROGRESS);
    }
}
