# ADR-010: Total-count accessor via a limit=1 probe; opt-in where the server reports totalCount

**Date:** 2026-07-26
**Status:** Accepted

## Context

ADR-006 makes list results lazy `Stream<T>` and bans `listAll()`. A consequence is that a
consumer has no cheap way to learn *how many* items match — `stream.count()` drains the whole
result set (O(n) requests, and offset-based lists cap at the 10M offset ceiling). "How many
offers / stock products do I have" is a common need (e.g. a dashboard total) that should not
require paging everything.

Allegro pagination is **not uniform**, which rules out a blanket "every list returns a total":

- Offset/limit lists usually carry `count` (this page) + `totalCount` (all): e.g. the offers
  listing, loyalty promotions.
- But many resources carry **no** total and terminate on a short page: cursor lists
  (offer-events, disputes), and some offset lists (billing entries, affiliate conversions,
  fulfillment refund-dispositions).

So a total can only be reported honestly for the subset of resources whose response actually
carries `totalCount`.

## Decision

This ADR **extends** ADR-006 (which is immutable and still holds — lazy streams remain the
default and nothing here materializes a result set).

- Where a listing's response carries a server `totalCount`, the facade MAY expose a
  `long count<Feature>(filter)` accessor (verb-first, matching the `stream<Feature>` list
  methods — e.g. `countOffers` beside `streamOffers`). It issues **one** request for the smallest legal
  page (`offset = 0`, `limit = 1` — Allegro's `limit` range is 1..1000) purely to read
  `totalCount`. This is **O(1) in requests**, independent of how large the result set is.
- Where the server reports **no** total (cursor resources, or offset resources without
  `totalCount`), **no** count accessor is offered. There is no `OptionalLong.empty()`
  placeholder and no fabricated total — availability is honest by omission.
- The `stream…()` methods are **unchanged** everywhere, so the list surface stays uniform;
  the count accessor is a separate, additive method that appears only where it can be truthful.
- Return type is `long` (the wire `totalCount` is an `Integer`; it is widened and null-guarded
  to `0`).

`offers().countOffers(OfferFilter)` is the reference implementation (bucket A). Sibling buckets
add `count<Feature>(filter)` only on resources whose response carries `totalCount`.

## Consequences

- Cheap totals without paging; for a count, `countOffers(filter)` replaces
  `streamOffers(filter).count()` (which would page every match).
- The only redundant work is one extra request when a consumer wants **both** the total and the
  items — bounded O(1), not O(n), and avoidable: the same `totalCount` already rides page 1 of a
  stream, so a consumer that is paging anyway need not call the count accessor.
- Count availability is per-resource; consumers discover it by the method's presence, not by an
  always-empty Optional.
