# Catalogue — `client.catalog()`

The catalogue is Allegro's read-only reference data: the category tree, per-category
parameters, the product database, and vehicle/part compatibility lists. It is what an offer is
classified and described against. All of it is public data, so an app-only **client-credentials**
token is enough — no user login required.

> **Status:** the `catalog().categories()` and `catalog().products()` sub-facades ship, along
> with the first `catalog().compatibility()` read; the remaining compatibility reads follow in the
> same bucket. See [`API-SURFACE.md`](../API-SURFACE.md) §E for the full planned surface.

## Categories

```java
try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
    CatalogCategories categories = client.catalog().categories();

    // Walk the tree from the top.
    for (Category root : categories.roots()) {
        System.out.println(root.id() + "  " + root.name());
    }

    // One level down.
    List<Category> children = categories.childrenOf("165929");

    // A single category, with what it permits.
    Category category = categories.get("165929");
    if (category.leaf()) {
        // Only leaf categories can hold offers.
        boolean canCreateProduct = category.options() != null
                && category.options().productCreationEnabled();
    }
}
```

### What you get back

`Category` is an immutable record:

| Component | Meaning |
|---|---|
| `id()` | opaque category id (treat as a string; may be integer- or UUID-shaped) |
| `name()` | display name, localized per request (marketplace default for now) |
| `leaf()` | `true` when the category has no children — only leaves hold offers |
| `parentId()` | the parent category id, or `null` for a root |
| `options()` | `CategoryOptions` (product creation, advertisements, …), or `null` |

### Errors

Every call maps non-2xx responses to the SDK's remediation-grouped exceptions: a bad category
id surfaces as `AllegroBadRequestException` (with the typed `errors()` list), a missing one as
`AllegroNotFoundException`, throttling as `AllegroRateLimitException` (honouring `Retry-After`),
and 5xx/transport failures as `AllegroServerException`. GET calls are retried on 429/5xx per the
configured `RetryPolicy`.

## Category parameters

Every category defines the parameters its offers and products may (or must) set — each with a
value type, its constraints, and, for dictionary parameters, the allowed values.

```java
for (CategoryParameter parameter : categories.parameters("165929")) {
    System.out.println(parameter.name() + " (" + parameter.type() + ")"
            + (parameter.required() ? " [required]" : ""));
    if (parameter.type() == CategoryParameterType.DICTIONARY) {
        for (DictionaryValue value : parameter.dictionary()) {
            System.out.println("  " + value.id() + " = " + value.value());
        }
    }
}
```

`CategoryParameter.type()` selects which part of the record carries the constraint:

| Type | Where the constraint lives |
|---|---|
| `DICTIONARY` | `dictionary()` — the selectable `DictionaryValue`s; `restrictions().multipleChoices()` |
| `FLOAT` | `restrictions()` — `minValue` / `maxValue` / `range` / `precision` |
| `INTEGER` | `restrictions()` — `minValue` / `maxValue` / `range` |
| `STRING` | `restrictions()` — `minLength` / `maxLength` / `allowedNumberOfValues` |
| `OTHER` | a type this SDK version does not model — `restrictions()` is `null`, `dictionary()` empty |

Use `parameter.id()` (and, for dictionaries, a `value.id()`) to set the value when you build an
offer or product.

## Category suggestions

Given a product or offer title, Allegro suggests the categories it best fits — the same hint the
sell form makes. Matches come best-first; walk `parent()` for the breadcrumb up to the root.

```java
for (CategorySuggestion match : categories.suggest("wiertarka udarowa Bosch")) {
    StringBuilder breadcrumb = new StringBuilder(match.name());
    for (CategorySuggestion node = match.parent(); node != null; node = node.parent()) {
        breadcrumb.insert(0, node.name() + " > ");
    }
    System.out.println(match.id() + "  " + breadcrumb);
}
```

## Category changes

The category tree evolves — categories are created, deleted, moved or renamed. `streamChanges`
is a lazy feed of those events; the stream follows Allegro's `from` cursor for you (each event's id
is the cursor), so a bounded consumer only fetches the pages it needs.

```java
categories.streamChanges(CategoryEventFilter.all())
        .limit(100)
        .forEach(event -> System.out.println(event.occurredAt() + "  " + event.type()
                + "  " + (event.category() == null ? "?" : event.category().id())));
```

Each `CategoryEvent` carries its `id()` (also the cursor to resume from), the change `type()`
(`CATEGORY_CREATED` / `CATEGORY_DELETED` / `CATEGORY_MOVED` / `CATEGORY_RENAMED`, or `UNKNOWN` for a
kind this release does not model), when it `occurredAt()`, and the affected `category()` as it stood
at the event. A `CATEGORY_DELETED` event also exposes `redirectCategoryId()` — where its offers were
redirected. Restrict the feed to certain kinds with `CategoryEventFilter.ofTypes(...)`, or resume
after a checkpoint with `CategoryEventFilter.since(lastSeenEventId)`.

Allegro also announces **planned** parameter changes ahead of time, so sellers can prepare —
`scheduledParameterChanges` is that (offset-paginated) feed:

```java
categories.scheduledParameterChanges(CategoryParameterChangeFilter.all())
        .forEach(change -> System.out.println(change.scheduledFor() + "  " + change.type()
                + "  category=" + change.categoryId() + " parameter=" + change.parameterId()));
```

Each `CategoryParameterScheduledChange` states the change `type()` (today only
`REQUIREMENT_CHANGE` — a parameter's requirement changing; `UNKNOWN` for a kind this release does not
model), when it was announced (`scheduledAt()`) and when it takes effect (`scheduledFor()`), and the
affected `categoryId()` / `parameterId()`. Narrow the feed by effective- or announcement-date range
and by kind with `CategoryParameterChangeFilter.builder()`.

## Products

Products are the shared descriptions offers are built from. Search the database lazily — the
returned stream follows Allegro's opaque `page.id` cursor for you, so a bounded consumer only
fetches the pages it needs.

```java
CatalogProducts products = client.catalog().products();

products.search(ProductSearchRequest.builder().phrase("iphone 15").categoryId("257").build())
        .limit(50)
        .forEach(summary -> System.out.println(summary.id() + "  " + summary.name()));
```

A search needs a phrase — `build()` fails fast otherwise; a category is an optional filter that
Allegro only honours alongside a phrase. Each `ProductSummary` carries the product id, name,
category id, publication status (`LISTED` / `PROPOSED`) and image URLs.

Read the full product — its parameter values and flags — with `get`:

```java
Product product = products.get(summary.id());
for (ProductParameterValue parameter : product.parameters()) {
    System.out.println(parameter.name() + " = " + String.join(", ", parameter.values()));
}
```

Before building a product for a category, learn the schema that category expects with
`parametersIn` — the product-side counterpart of `categories().parameters(...)`:

```java
for (ProductParameter parameter : products.parametersIn("257")) {
    System.out.println(parameter.id() + "  " + parameter.name()
            + "  (" + parameter.type() + (parameter.required() ? ", required)" : ")"));
}
```

Each `ProductParameter` carries the value `type` (`DICTIONARY` / `FLOAT` / `INTEGER` /
`STRING`, or `OTHER` for a type this release does not yet model), whether it is `required`,
its `unit`, and — by type — either the numeric/text `restrictions` or the selectable
`dictionary` values. It shares those value types with `CategoryParameter` but omits the two
components that apply only to an offer's parameters (`requiredForProduct`, display options).

## Compatibility

Some categories — car parts and accessories — let an offer carry a **compatibility list**: the
set of vehicles or products the item fits. Before building such a list, learn which categories
support one, and how each expects its items:

```java
Compatibility compatibility = client.catalog().compatibility();

for (CompatibleCategory category : compatibility.supportedCategories()) {
    System.out.println(category.categoryId() + "  " + category.name()
            + "  input=" + category.inputType());
    if (category.validationRules() != null) {
        System.out.println("    up to " + category.validationRules().maxRows() + " entries"
                + (category.validationRules().maxCharactersPerLine() != null
                        ? ", " + category.validationRules().maxCharactersPerLine() + " chars/row"
                        : ""));
    }
}
```

`inputType` tells you how the list's items are supplied: `ID` (chosen from Allegro's
compatible-products database) or `TEXT` (free text); an input type this release does not model
yet reads as `UNKNOWN`. `validationRules` caps the list size (`maxRows`) for either input type;
`maxCharactersPerLine` bounds a free-text row and is `null` for an `ID` category, whose items are
picked rather than typed.

### The compatible-products database

For an `ID`-type category, the items come from Allegro's compatible-products database. Browse it by
`type` (a value a category advertises as its `itemsType`, e.g. `CAR`), narrowed by a product group,
a TecDoc vehicle number, or a free-text phrase. Both reads are lazy offset-paginated streams:

```java
// The coarse dimension first — e.g. vehicle makes.
compatibility.productGroups(CompatibleProductGroupsFilter.ofType("CAR"))
        .limit(20)
        .forEach(group -> System.out.println(group.id() + "  " + group.text()));

// Then the products within a group.
compatibility.products(CompatibleProductsFilter.builder().type("CAR").groupId(groupId).build())
        .limit(50)
        .forEach(product -> System.out.println(product.id() + "  " + product.text()));
```

A `type` is required — `build()` fails fast without it. Each `CompatibleProduct` carries its `id()`
(reuse it as a compatibility-list `ID` item), a `text()` label, the `groupId()` it belongs to, and
`attributes()` that disambiguate it (`CompatibleProductAttribute` — an id such as `ENGINE_CODE` or
`BRAND` and its values). A phrase search returns all matches on a single page (Allegro ignores
offset/limit when a phrase is present), so the stream does not page further.

## Verifying against the sandbox

The read-only demo scenario navigates the live category tree and confirms the mapped fields
arrive (the read-only counterpart of the write→read rule in [`TESTING.md`](../TESTING.md)):

```bash
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-categories
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-products             -Pdemo.account=seller
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-compatibility        -Pdemo.account=seller
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-compatible-products
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-category-events
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-parameter-changes
```
