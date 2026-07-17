/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PagedSpliteratorTest {

    private static final List<String> PAGE_ONE = List.of("a1", "a2");
    private static final List<String> PAGE_TWO = List.of("b1", "b2");
    private static final String CURSOR_NEXT = "cursor-2";
    private static final int GUARD_TRIP_LIMIT = 200_000;

    @Test
    void stream_whenConsumerTakesFirstPageOnly_secondPageIsNeverFetched() {
        // given
        AtomicInteger fetchCount = new AtomicInteger();
        Stream<String> lazyStream = PagedSpliterator.stream(pageIndex -> {
            fetchCount.incrementAndGet();
            return pageIndex == 0
                    ? new PagedSpliterator.Page<>(PAGE_ONE, true)
                    : new PagedSpliterator.Page<>(PAGE_TWO, false);
        });

        // when — consume exactly the first page's worth of items
        List<String> taken = lazyStream.limit(PAGE_ONE.size()).toList();

        // then — LAZINESS: page two was never requested
        assertEquals(PAGE_ONE, taken);
        assertEquals(1, fetchCount.get());
    }

    @Test
    void stream_whenConsumedFully_walksAllPagesInOrder() {
        // given
        Stream<String> lazyStream = PagedSpliterator.stream(pageIndex ->
                pageIndex == 0
                        ? new PagedSpliterator.Page<>(PAGE_ONE, true)
                        : new PagedSpliterator.Page<>(PAGE_TWO, false));

        // when / then
        assertEquals(List.of("a1", "a2", "b1", "b2"), lazyStream.toList());
    }

    @Test
    void stream_whenServerKeepsReturningEmptyPagesWithHasMore_abortsWithGuard() {
        // given — a misbehaving pager: always empty, always hasMore
        Stream<String> runawayStream = PagedSpliterator.<String>stream(ignored ->
                new PagedSpliterator.Page<>(List.of(), true)).limit(GUARD_TRIP_LIMIT);

        // then — the defensive cap turns a hung walk into a loud failure
        assertThrows(IllegalStateException.class, runawayStream::toList);
    }

    @Test
    void cursorStream_whenCursorAdvances_passesPreviousCursorToNextFetch() {
        // given
        Stream<String> cursorStream = PagedSpliterator.cursorStream(cursor -> {
            if (cursor == null) {
                return new PagedSpliterator.CursorPage<>(PAGE_ONE, CURSOR_NEXT);
            }
            assertEquals(CURSOR_NEXT, cursor);
            return new PagedSpliterator.CursorPage<>(PAGE_TWO, null);
        });

        // when / then — terminates on null cursor with all items in order
        assertEquals(List.of("a1", "a2", "b1", "b2"), cursorStream.toList());
    }
}
