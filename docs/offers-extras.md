# Offers extras & classifieds (bucket F)

Consumer usage guide for the offer add-ons — classifieds (advertisement)
packages and statistics, offer tags, translations, rating, and bundles.

This bucket is landing incrementally. Sections appear as each area ships; the
authoritative method layout is [`API-SURFACE.md`](../API-SURFACE.md) §F.

## Offer tags

Tags are a seller-only organisation aid (buyers never see them). Reach them via
`client.offers().tags()`. Manage the tag catalogue and assign tags to offers:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTags;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.Tag;
import java.util.List;

OfferTags tags = client.offers().tags();

String tagId = tags.create(TagRequest.builder().name("Priority").build());
tags.streamTags().forEach(tag -> System.out.println(tag.name() + " hidden=" + tag.hidden()));

tags.assignToOffer(offerId, List.of(tagId));
List<Tag> assigned = tags.ofOffer(offerId);

tags.rename(tagId, TagRequest.builder().name("Top priority").hidden(true).build());
tags.delete(tagId);
```

`streamTags()` is lazy (paged on demand). `create` returns the new tag's id; the
name is required (fail-fast) and `hidden` is optional. The tag catalogue
operations use the `sale:settings:*` scopes and the per-offer assignment uses
`sale:offers:*` — both need a **user (seller) access token**.

## Classifieds

Classifieds are Allegro's advertisement listings (for example automotive or
real-estate categories). Reach them via `client.classifieds()`.

### Available packages in a category

The packages a seller can attach to an advertisement depend on the category the
advertisement is listed in. Read them with `availablePackages(categoryId)`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;

try (AllegroClient client = AllegroClient.create(credentials, environment)) {
    for (ClassifiedPackage classifiedPackage : client.classifieds().availablePackages(categoryId)) {
        System.out.println(classifiedPackage.name() + " (" + classifiedPackage.type() + ")");
        classifiedPackage.promotions().forEach(promo ->
                System.out.println("  promotion " + promo.name() + " for " + promo.duration()));
    }
}
```

Each `ClassifiedPackage` exposes its `id`, `name`, `type`
(`BASE` or `EXTRA`), the bundled `extensions` and `promotions`, and the
`publication` terms (each duration is a `java.time.Duration`). The endpoint
requires a **user (seller) access token** with the `sale:offers:read` scope
(the spec declares it `bearer-token-for-user`), so authenticate with an
authorization-code or device grant — an app-only client-credentials token is
not sufficient.

Read one package configuration directly by id with `getPackage(packageId)`.

### Assigning packages to an offer

An advertisement offer carries exactly one **base** package and any number of
**extra** packages. Read the current assignment with `packagesOfOffer(offerId)`
and replace it with `assignPackages(offerId, assignment)`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifieds;

ClassifiedAssignment assignment = ClassifiedAssignment.builder()
        .basePackage(basePackageId)
        .addExtraPackage(extraPackageId, true)   // true republishes the advert
        .build();
client.classifieds().assignPackages(offerId, assignment);

OfferClassifieds assigned = client.classifieds().packagesOfOffer(offerId);
System.out.println("base package: " + assigned.basePackageId());
```

The builder validates that a base package is set (fail-fast); extra packages are
optional and each may set a `republish` flag. `assignPackages` writes with the
`sale:offers:write` scope, so it needs a **user (seller) access token** just like
the reads.

### Advertisement statistics

Read daily statistics for selected advertisements with `offerStats(offerIds,
filter)` (up to 50 offer ids), or aggregated across all your advertisements with
`sellerStats(filter)`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder.ClassifiedStatsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.SellerClassifiedStats;
import java.time.OffsetDateTime;

ClassifiedStatsFilter lastMonth = ClassifiedStatsFilter.builder()
        .eventsFrom(OffsetDateTime.now().minusMonths(1))
        .eventsTo(OffsetDateTime.now())
        .build();

SellerClassifiedStats stats = client.classifieds().sellerStats(lastMonth);
stats.totals().forEach(stat ->
        System.out.println(stat.eventType() + ": " + stat.count()));
stats.perDay().forEach(day ->
        System.out.println(day.date() + " -> " + day.events().size() + " event type(s)"));
```

Each `ClassifiedEventStat` pairs a `ClassifiedEventType` (for example
`SHOWED_PHONE_NUMBER`, `ASKED_QUESTION`, `ADDED_TO_FAVOURITES`) with its `count`.
The date range is optional — `ClassifiedStatsFilter.all()` leaves it to the
server default — but Allegro requires the two bounds to be less than three months
apart. Both reads use the `sale:offers:read` scope and therefore a **user
(seller) access token**.

## Offer translations

Read and set an offer's translations into other languages via
`client.offers().translations()`. The SDK currently covers the **title**
translation (description and safety-information translations are not yet
modelled):

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTranslations;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslation;

OfferTranslations translations = client.offers().translations();

for (OfferTranslation translation : translations.ofOffer(offerId)) {
    System.out.println(translation.language() + ": " + translation.title()
            + " (" + translation.titleType() + ")");
}

translations.update(offerId, "en-US", TranslationRequest.builder().title("Wireless keyboard").build());
translations.delete(offerId, "en-US");
```

Each `OfferTranslation` carries the `language`, the translated `title`, and the
`titleType` (`AUTO`/`MANUAL`/`BASE`). All operations use `sale:offers:*` and need
a **user (seller) access token**.

## Offer rating

Read an offer's aggregated buyer rating with `client.offers().rating(offerId)`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferRating;

OfferRating rating = client.offers().rating(offerId);
System.out.println("average " + rating.averageScore() + " over " + rating.totalResponses());
rating.scoreDistribution().forEach(bucket ->
        System.out.println("  score " + bucket.name() + ": " + bucket.count()));
```

`averageScore` is a decimal string (or `null` when the offer has no ratings yet).
`scoreDistribution` and `sizeFeedback` break the responses down by score and by
size feedback. Read-only, `sale:offers:read` (user token).

## Fixed offer bundles

A fixed bundle groups offers a buyer can buy together at a per-marketplace
discount. Reach them via `client.offers().bundles()`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferBundle;
import java.util.List;

OfferBundles bundles = client.offers().bundles();

bundles.streamBundles().forEach(bundle ->
        System.out.println(bundle.id() + ": " + bundle.offers().size() + " offer(s)"));

OfferBundle bundle = bundles.get(bundleId);
OfferBundle updated = bundles.updateDiscount(bundleId,
        List.of(new BundleDiscount("allegro-pl", Money.of("15.00", "PLN"))));
bundles.delete(bundleId);
```

`streamBundles()` is a lazy **cursor** stream. Each `OfferBundle` carries its
bundled `offers` (with `requiredQuantity`/`entryPoint`), the per-marketplace
`discounts` (as `Money`) and `publications`, and who created it. All operations
use `sale:offers:*` and need a **user (seller) access token**.
