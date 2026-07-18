/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.campaigns;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationBargainPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationCampaignRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationDeclaredStockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationPricesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationPurchaseConstraintsLimitPerUserRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationPurchaseConstraintsLimitRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationPurchaseConstraintsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeOperationProcessRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeOperationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgePatchPricesPricesBargainRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgePatchPricesPricesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgePatchPricesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgePatchProcessProcessRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgePatchProcessRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgesListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetBadgeCampaignsListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PatchBadge202ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Badges;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgePatch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.Badge;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplication;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link Badges} facade (bucket H, badge campaigns).
 *
 * <p>Reads (campaign discovery, application and badge streams) map straight
 * through {@code from(Raw)} factories. The write operations follow the two shapes
 * Allegro uses: {@code apply} submits an application and returns it immediately
 * (verification is asynchronous and e-mail-notified — never blocked on, per
 * ADR-005's carve-out for human-verified commands), while {@code update} submits a
 * short badge operation and is polled to a terminal state via {@link CommandPoller}.
 *
 * @since 0.2.0
 */
public final class BadgesClient implements Badges {

    private static final int PAGE_SIZE = 100;

    private static final String OP_AVAILABLE_CAMPAIGNS = "list badge campaigns";
    private static final String OP_APPLY = "apply badge";
    private static final String OP_STREAM_APPLICATIONS = "stream badge applications";
    private static final String OP_GET_APPLICATION = "get badge application";
    private static final String OP_STREAM_BADGES = "stream badges";
    private static final String OP_UPDATE = "update badge";
    private static final String OP_POLL_OPERATION = "poll badge operation";

    private static final String PARAM_MARKETPLACE_ID = "marketplace.id";
    private static final String PARAM_CAMPAIGN_ID = "campaign.id";
    private static final String PARAM_OFFER_ID = "offer.id";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_LIMIT = "limit";

    private static final String ERR_BLANK_MARKETPLACE = "marketplaceId must not be null or blank";
    private static final String ERR_BLANK_APPLICATION_ID = "applicationId must not be null or blank";
    private static final String ERR_BLANK_OFFER_ID = "offerId must not be null or blank";
    private static final String ERR_BLANK_CAMPAIGN_ID = "campaignId must not be null or blank";
    private static final String ERR_NULL_PATCH = "patch must not be null";

    private final HttpSupport http;
    private final CommandPoller commandPoller;

    public BadgesClient(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.commandPoller = new CommandPoller();
    }

    @Override
    public List<BadgeCampaign> availableCampaigns() {
        return fetchCampaigns(null);
    }

    @Override
    public List<BadgeCampaign> availableCampaigns(String marketplaceId) {
        if (marketplaceId == null || marketplaceId.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_MARKETPLACE);
        }
        return fetchCampaigns(marketplaceId);
    }

    private List<BadgeCampaign> fetchCampaigns(@Nullable String marketplaceId) {
        GetBadgeCampaignsListRaw raw = http.request(OP_AVAILABLE_CAMPAIGNS)
                .get(ApiPaths.BADGE_CAMPAIGNS)
                .query(Query.create().add(PARAM_MARKETPLACE_ID, marketplaceId))
                .fetch(GetBadgeCampaignsListRaw.class);
        return raw.getBadgeCampaigns().stream().map(BadgeCampaign::from).toList();
    }

    @Override
    public BadgeApplication apply(BadgeApplicationRequest request) {
        BadgeApplicationRaw raw = http.request(OP_APPLY)
                .post(ApiPaths.BADGES)
                .jsonBody(toRaw(request))
                .fetch(BadgeApplicationRaw.class);
        return BadgeApplication.from(raw);
    }

    @Override
    public Stream<BadgeApplication> streamApplications(BadgeApplicationFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchApplicationsPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<BadgeApplication> fetchApplicationsPage(
            BadgeApplicationFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(PARAM_CAMPAIGN_ID, filter.campaignId())
                .add(PARAM_OFFER_ID, filter.offerId())
                .add(PARAM_OFFSET, pageIndex * PAGE_SIZE)
                .add(PARAM_LIMIT, PAGE_SIZE);
        BadgeApplicationsRaw response = http.request(OP_STREAM_APPLICATIONS)
                .get(ApiPaths.BADGE_APPLICATIONS)
                .query(query)
                .fetch(BadgeApplicationsRaw.class);
        List<BadgeApplicationRaw> raws = response.getBadgeApplications();
        List<BadgeApplication> items = raws == null
                ? List.of()
                : raws.stream().map(BadgeApplication::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public BadgeApplication application(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_APPLICATION_ID);
        }
        BadgeApplicationRaw raw = http.request(OP_GET_APPLICATION)
                .get(ApiPaths.subPath(ApiPaths.BADGE_APPLICATIONS, applicationId))
                .fetch(BadgeApplicationRaw.class);
        return BadgeApplication.from(raw);
    }

    @Override
    public Stream<Badge> streamBadges(BadgeFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchBadgesPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<Badge> fetchBadgesPage(BadgeFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(PARAM_MARKETPLACE_ID, filter.marketplaceId())
                .add(PARAM_OFFER_ID, filter.offerId())
                .add(PARAM_OFFSET, pageIndex * PAGE_SIZE)
                .add(PARAM_LIMIT, PAGE_SIZE);
        BadgesListRaw response = http.request(OP_STREAM_BADGES)
                .get(ApiPaths.BADGES)
                .query(query)
                .fetch(BadgesListRaw.class);
        List<BadgeRaw> raws = response.getBadges();
        List<Badge> items = raws == null ? List.of() : raws.stream().map(Badge::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public BadgeOperation update(String offerId, String campaignId, BadgePatch patch) {
        return runUpdate(offerId, campaignId, patch, null);
    }

    @Override
    public BadgeOperation update(String offerId, String campaignId, BadgePatch patch, Duration timeout) {
        return runUpdate(offerId, campaignId, patch, timeout);
    }

    private BadgeOperation runUpdate(String offerId, String campaignId, BadgePatch patch,
            @Nullable Duration timeout) {
        validateUpdateArgs(offerId, campaignId, patch);
        String path = ApiPaths.subPath(ApiPaths.BADGES, ApiPaths.BADGE_OFFERS_SEGMENT, offerId,
                ApiPaths.BADGE_CAMPAIGNS_SEGMENT, campaignId);
        PatchBadge202ResponseRaw accepted = http.request(OP_UPDATE)
                .patch(path)
                .jsonBody(toRaw(patch))
                .fetch(PatchBadge202ResponseRaw.class);
        String operationId = accepted.getId();
        BadgeOperationRaw terminal = timeout == null
                ? commandPoller.await(() -> fetchOperation(operationId), BadgesClient::isTerminal, OP_UPDATE)
                : commandPoller.await(() -> fetchOperation(operationId), BadgesClient::isTerminal, OP_UPDATE,
                        timeout);
        return BadgeOperation.from(terminal);
    }

    private BadgeOperationRaw fetchOperation(String operationId) {
        return http.request(OP_POLL_OPERATION)
                .get(ApiPaths.subPath(ApiPaths.BADGE_OPERATIONS, operationId))
                .fetch(BadgeOperationRaw.class);
    }

    private static boolean isTerminal(BadgeOperationRaw operation) {
        return operation.getProcess().getStatus() != BadgeOperationProcessRaw.StatusEnum.REQUESTED;
    }

    private static void validateUpdateArgs(String offerId, String campaignId, BadgePatch patch) {
        if (offerId == null || offerId.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_OFFER_ID);
        }
        if (campaignId == null || campaignId.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_CAMPAIGN_ID);
        }
        if (patch == null) {
            throw new IllegalArgumentException(ERR_NULL_PATCH);
        }
    }

    private static BadgeApplicationRequestRaw toRaw(BadgeApplicationRequest request) {
        BadgeApplicationRequestRaw raw = new BadgeApplicationRequestRaw()
                .campaign(new BadgeApplicationCampaignRaw().id(request.campaignId()))
                .offer(new BadgeApplicationOfferRaw().id(request.offerId()));
        Money bargain = request.bargainPrice();
        if (bargain != null) {
            raw.prices(new BadgeApplicationPricesRaw().bargain(
                    new BadgeApplicationBargainPriceRaw().amount(bargain.amount()).currency(bargain.currency())));
        }
        Integer limitPerUser = request.purchaseLimitPerUser();
        if (limitPerUser != null) {
            raw.purchaseConstraints(new BadgeApplicationPurchaseConstraintsRaw().limit(
                    new BadgeApplicationPurchaseConstraintsLimitRaw().perUser(
                            new BadgeApplicationPurchaseConstraintsLimitPerUserRaw().maxItems(limitPerUser))));
        }
        BigDecimal declaredStock = request.declaredStockQuantity();
        if (declaredStock != null) {
            raw.declaredStock(new BadgeApplicationDeclaredStockRaw().quantity(declaredStock));
        }
        return raw;
    }

    /**
     * Build the badge-update body. The endpoint accepts one of two shapes; the SDK
     * serialises the chosen sub-type directly rather than the generated {@code oneOf}
     * wrapper (a write picks its own branch, so the wrapper's discriminating
     * deserialiser is not needed).
     */
    private static Object toRaw(BadgePatch patch) {
        if (patch.kind() == BadgePatch.Kind.FINISH) {
            return new BadgePatchProcessRaw().process(
                    new BadgePatchProcessProcessRaw().status(BadgePatchProcessProcessRaw.StatusEnum.FINISHED));
        }
        Money price = Objects.requireNonNull(patch.bargainPrice());
        return new BadgePatchPricesRaw().prices(
                new BadgePatchPricesPricesRaw().bargain(new BadgePatchPricesPricesBargainRaw().value(
                        new BadgeApplicationBargainPriceRaw().amount(price.amount()).currency(price.currency()))));
    }
}
