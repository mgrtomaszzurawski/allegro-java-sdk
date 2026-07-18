/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.campaigns;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetSubmitCommandResponseOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetSubmitCommandResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetWithdrawCommandResponseOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetWithdrawCommandResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListCampaignsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListEligibleResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListSubmittedResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmitCommandRequestInputCampaignRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmitCommandRequestInputOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmitCommandRequestInputProposedPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmitCommandRequestInputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmitCommandRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmitCommandResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountWithdrawCommandRequestInputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountWithdrawCommandRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountWithdrawCommandResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.AlleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.EligibleOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmittedOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountEligibleOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountSubmitResult;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountSubmittedOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountWithdrawResult;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link AlleDiscount} facade (bucket H, AlleDiscount).
 * Campaign discovery and the eligible/submitted offer streams are plain reads; the
 * submit and withdraw commands are polled to a terminal state via
 * {@link CommandPoller}.
 *
 * @since 0.2.0
 */
public final class AlleDiscountClient implements AlleDiscount {

    private static final int PAGE_SIZE = 100;

    private static final String OP_CAMPAIGNS = "list alle-discount campaigns";
    private static final String OP_STREAM_ELIGIBLE = "stream alle-discount eligible offers";
    private static final String OP_STREAM_SUBMITTED = "stream alle-discount submitted offers";
    private static final String OP_SUBMIT = "submit alle-discount offer";
    private static final String OP_SUBMIT_POLL = "poll alle-discount submit command";
    private static final String OP_WITHDRAW = "withdraw alle-discount offer";
    private static final String OP_WITHDRAW_POLL = "poll alle-discount withdraw command";

    private static final String PARAM_OFFER_ID = "offerId";
    private static final String PARAM_PARTICIPATION_ID = "participationId";
    private static final String PARAM_MEETS_CONDITIONS = "meetsConditions";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_LIMIT = "limit";

    private static final String ERR_NULL_FILTER = "filter must not be null";
    private static final String ERR_NULL_REQUEST = "request must not be null";
    private static final String ERR_BLANK_PARTICIPATION = "participationId must not be null or blank";

    private final HttpSupport http;
    private final CommandPoller commandPoller;

    public AlleDiscountClient(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.commandPoller = new CommandPoller();
    }

    @Override
    public List<AlleDiscountCampaign> campaigns() {
        AlleDiscountListCampaignsResponseRaw raw = http.request(OP_CAMPAIGNS)
                .get(ApiPaths.ALLE_DISCOUNT_CAMPAIGNS)
                .fetch(AlleDiscountListCampaignsResponseRaw.class);
        return raw.getAlleDiscountCampaigns() == null
                ? List.of()
                : raw.getAlleDiscountCampaigns().stream().map(AlleDiscountCampaign::from).toList();
    }

    @Override
    public Stream<AlleDiscountEligibleOffer> streamEligibleOffers(EligibleOffersFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException(ERR_NULL_FILTER);
        }
        return PagedSpliterator.stream(pageIndex -> fetchEligiblePage(filter, pageIndex));
    }

    private PagedSpliterator.Page<AlleDiscountEligibleOffer> fetchEligiblePage(
            EligibleOffersFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(PARAM_OFFER_ID, filter.offerId())
                .add(PARAM_MEETS_CONDITIONS, filter.meetsConditions())
                .add(PARAM_OFFSET, pageIndex * PAGE_SIZE)
                .add(PARAM_LIMIT, PAGE_SIZE);
        AlleDiscountListEligibleResponseRaw response = http.request(OP_STREAM_ELIGIBLE)
                .get(ApiPaths.subPath(ApiPaths.ALLE_DISCOUNT, filter.campaignId(),
                        ApiPaths.ALLE_DISCOUNT_ELIGIBLE_OFFERS_SEGMENT))
                .query(query)
                .fetch(AlleDiscountListEligibleResponseRaw.class);
        List<AlleDiscountEligibleOffer> items = response.getEligibleOffers() == null
                ? List.of()
                : response.getEligibleOffers().stream().map(AlleDiscountEligibleOffer::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public Stream<AlleDiscountSubmittedOffer> streamSubmittedOffers(SubmittedOffersFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException(ERR_NULL_FILTER);
        }
        return PagedSpliterator.stream(pageIndex -> fetchSubmittedPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<AlleDiscountSubmittedOffer> fetchSubmittedPage(
            SubmittedOffersFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(PARAM_OFFER_ID, filter.offerId())
                .add(PARAM_PARTICIPATION_ID, filter.participationId())
                .add(PARAM_OFFSET, pageIndex * PAGE_SIZE)
                .add(PARAM_LIMIT, PAGE_SIZE);
        AlleDiscountListSubmittedResponseRaw response = http.request(OP_STREAM_SUBMITTED)
                .get(ApiPaths.subPath(ApiPaths.ALLE_DISCOUNT, filter.campaignId(),
                        ApiPaths.ALLE_DISCOUNT_SUBMITTED_OFFERS_SEGMENT))
                .query(query)
                .fetch(AlleDiscountListSubmittedResponseRaw.class);
        List<AlleDiscountSubmittedOffer> items = response.getSubmittedOffers() == null
                ? List.of()
                : response.getSubmittedOffers().stream().map(AlleDiscountSubmittedOffer::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public AlleDiscountSubmitResult submitOffer(SubmitOfferRequest request) {
        return runSubmit(request, null);
    }

    @Override
    public AlleDiscountSubmitResult submitOffer(SubmitOfferRequest request, Duration timeout) {
        return runSubmit(request, timeout);
    }

    private AlleDiscountSubmitResult runSubmit(SubmitOfferRequest request, @Nullable Duration timeout) {
        if (request == null) {
            throw new IllegalArgumentException(ERR_NULL_REQUEST);
        }
        AlleDiscountSubmitCommandResponseRaw accepted = http.request(OP_SUBMIT)
                .post(ApiPaths.ALLE_DISCOUNT_SUBMIT_COMMANDS)
                .jsonBody(submitBody(request))
                .fetch(AlleDiscountSubmitCommandResponseRaw.class);
        String commandId = accepted.getId();
        AlleDiscountGetSubmitCommandResponseRaw terminal = await(
                () -> fetchSubmitCommand(commandId), AlleDiscountClient::submitTerminal, OP_SUBMIT, timeout);
        return AlleDiscountSubmitResult.from(terminal);
    }

    @Override
    public AlleDiscountWithdrawResult withdrawOffer(String participationId) {
        return runWithdraw(participationId, null);
    }

    @Override
    public AlleDiscountWithdrawResult withdrawOffer(String participationId, Duration timeout) {
        return runWithdraw(participationId, timeout);
    }

    private AlleDiscountWithdrawResult runWithdraw(String participationId, @Nullable Duration timeout) {
        if (participationId == null || participationId.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_PARTICIPATION);
        }
        AlleDiscountWithdrawCommandResponseRaw accepted = http.request(OP_WITHDRAW)
                .post(ApiPaths.ALLE_DISCOUNT_WITHDRAW_COMMANDS)
                .jsonBody(new AlleDiscountWithdrawCommandRequestRaw().input(
                        new AlleDiscountWithdrawCommandRequestInputRaw().participationId(participationId)))
                .fetch(AlleDiscountWithdrawCommandResponseRaw.class);
        String commandId = accepted.getId();
        AlleDiscountGetWithdrawCommandResponseRaw terminal = await(
                () -> fetchWithdrawCommand(commandId), AlleDiscountClient::withdrawTerminal, OP_WITHDRAW, timeout);
        return AlleDiscountWithdrawResult.from(terminal);
    }

    private AlleDiscountGetSubmitCommandResponseRaw fetchSubmitCommand(String commandId) {
        return http.request(OP_SUBMIT_POLL)
                .get(ApiPaths.subPath(ApiPaths.ALLE_DISCOUNT_SUBMIT_COMMANDS, commandId))
                .fetch(AlleDiscountGetSubmitCommandResponseRaw.class);
    }

    private AlleDiscountGetWithdrawCommandResponseRaw fetchWithdrawCommand(String commandId) {
        return http.request(OP_WITHDRAW_POLL)
                .get(ApiPaths.subPath(ApiPaths.ALLE_DISCOUNT_WITHDRAW_COMMANDS, commandId))
                .fetch(AlleDiscountGetWithdrawCommandResponseRaw.class);
    }

    private <S> S await(Supplier<S> fetchStatus, Predicate<S> isTerminal, String operationName,
            @Nullable Duration timeout) {
        return timeout == null
                ? commandPoller.await(fetchStatus, isTerminal, operationName)
                : commandPoller.await(fetchStatus, isTerminal, operationName, timeout);
    }

    private static AlleDiscountSubmitCommandRequestRaw submitBody(SubmitOfferRequest request) {
        Money proposedPrice = request.proposedPrice();
        return new AlleDiscountSubmitCommandRequestRaw().input(new AlleDiscountSubmitCommandRequestInputRaw()
                .offer(new AlleDiscountSubmitCommandRequestInputOfferRaw().id(request.offerId()))
                .campaign(new AlleDiscountSubmitCommandRequestInputCampaignRaw().id(request.campaignId()))
                .proposedPrice(new AlleDiscountSubmitCommandRequestInputProposedPriceRaw()
                        .amount(proposedPrice.amount()).currency(proposedPrice.currency())));
    }

    private static boolean submitTerminal(AlleDiscountGetSubmitCommandResponseRaw raw) {
        AlleDiscountGetSubmitCommandResponseOutputRaw.StatusEnum status = raw.getOutput().getStatus();
        return status == AlleDiscountGetSubmitCommandResponseOutputRaw.StatusEnum.SUCCESSFUL
                || status == AlleDiscountGetSubmitCommandResponseOutputRaw.StatusEnum.FAILED;
    }

    private static boolean withdrawTerminal(AlleDiscountGetWithdrawCommandResponseRaw raw) {
        AlleDiscountGetWithdrawCommandResponseOutputRaw.StatusEnum status = raw.getOutput().getStatus();
        return status == AlleDiscountGetWithdrawCommandResponseOutputRaw.StatusEnum.SUCCESSFUL
                || status == AlleDiscountGetWithdrawCommandResponseOutputRaw.StatusEnum.FAILED;
    }
}
