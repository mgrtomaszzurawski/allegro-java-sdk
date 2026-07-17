# ADR-006: Lazy Stream pagination; no listAll()

**Date:** 2026-07-16
**Status:** Accepted

## Context

Allegro paginates with offset/limit (limit ≤ 1000, offset+limit ≤ 10M, count/totalCount) and
occasionally with opaque cursors. Materializing full result sets silently truncates at the
offset cap and bloats heap.

## Decision

`stream…()` methods return lazy `Stream<T>` via the internal `PagedSpliterator` (offset and
cursor variants): one page in heap, next page fetched on consumer demand, defensive cap on
consecutive empty non-terminal pages. Single-page methods exist only where the cursor/offset
is a consumer-visible concept. `listAll()`-style methods are banned.

## Consequences

- Constant memory over arbitrarily large result sets; consumers control materialization.
- Streams are sequential by design (I/O-bound ordered fetching; trySplit → null).
