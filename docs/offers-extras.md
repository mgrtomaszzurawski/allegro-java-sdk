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
`client.offers().translations()`. Covers the **title**, the standardized
**description**, and the per-product **safety-information** translations:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTranslations;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.DescriptionSection;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.DescriptionSectionItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.ProductSafetyInformationTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.StandardizedDescription;
import java.util.List;

OfferTranslations translations = client.offers().translations();

for (OfferTranslation translation : translations.ofOffer(offerId)) {
    System.out.println(translation.language() + ": " + translation.title()
            + " (" + translation.titleType() + ")");
}

// A partial update: set only the parts you want to translate. The others are
// left untouched — the SDK omits them from the request body rather than sending
// them as null (which the server could read as "clear this translation").
translations.update(offerId, "en-US", TranslationRequest.builder()
        .title("Wireless keyboard")
        .description(StandardizedDescription.of(DescriptionSection.of(
                DescriptionSectionItem.text("A comfortable, quiet wireless keyboard."),
                DescriptionSectionItem.image("https://images.allegrostatic.pl/kbd.jpg"))))
        .safetyInformation(List.of(
                ProductSafetyInformationTranslation.of(productId, "Keep away from water.")))
        .build());

translations.delete(offerId, "en-US");
```

Each `OfferTranslation` carries the `language`; the translated `title` and its
`titleType`; the translated `description` (a `StandardizedDescription` of
`DescriptionSection`s, each a list of text/image `DescriptionSectionItem`s) and
its `descriptionType`; and the per-product `safetyInformation`. Every type is
`AUTO`/`MANUAL`/`BASE`, or `UNKNOWN` for a kind Allegro added after this SDK
version. All operations use `sale:offers:*` and need a **user (seller) access
token**.

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

## Flexible offer bundles

A flexible bundle is made of slots, each offering the buyer a choice of offers,
sold together at a whole-bundle or per-slot discount. Reach them via
`client.offers().flexibleBundles()`. This SDK version covers reading and deleting
them (creating/updating the nested slot/offer/discount definition is a planned
follow-up):

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.FlexibleBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundle;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleSummary;

FlexibleBundles flexible = client.offers().flexibleBundles();

for (FlexibleBundleSummary summary : flexible.streamBundles().toList()) {
    System.out.println(summary.id() + " slots=" + summary.slotRepresentatives().size());
}

FlexibleBundle bundle = flexible.get(bundleId);
bundle.slots().forEach(slot ->
        System.out.println("slot " + slot.order() + ": " + slot.offers().size() + " offer(s)"));
flexible.delete(bundleId);
```

`streamBundles()` is a lazy **cursor** stream of summaries (identity + discount +
one representative offer id per slot); `get(bundleId)` returns the full bundle
with every slot's offers. The `FlexibleBundleDiscount` is discriminated by
`type()` — `WHOLE_BUNDLE_DISCOUNT` (one discount, `wholeBundle()`) or
`SLOT_DISCOUNT` (per-slot, `slotDiscounts()`), each with per-marketplace
percentages. All operations use `sale:offers:*` and need a **user (seller) access
token**.
