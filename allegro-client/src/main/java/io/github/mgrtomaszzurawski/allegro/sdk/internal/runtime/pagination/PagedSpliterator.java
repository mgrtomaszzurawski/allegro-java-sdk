/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

/**
 * Lazy spliterator that walks Allegro list/search pagination and exposes the
 * underlying records as a {@link Stream}. Only one page is held in memory;
 * the next page is fetched when the consumer advances past the buffer.
 *
 * <p>Two pagination idioms, both supported:
 * <ul>
 *   <li><strong>Offset-based</strong> — the dominant Allegro idiom:
 *       {@code offset}/{@code limit} query parameters with {@code count} /
 *       {@code totalCount} in the response ({@code hasMore = offset + count <
 *       totalCount}, computed by the caller). Use {@link #stream(IntFunction)};
 *       the function receives the zero-based <em>page index</em>.</li>
 *   <li><strong>Cursor-based</strong> — a few resources page with an opaque
 *       marker (e.g. checkout-forms event streams). Use
 *       {@link #cursorStream(Function)}; the function receives {@code null}
 *       first, then each {@link CursorPage#nextCursor()}.</li>
 * </ul>
 *
 * <p>Equivalent in spirit to AWS SDK v2 paginators. No upper bound is imposed
 * — memory pressure is bounded by what the caller materialises.
 *
 * @since 0.1.0
 */
public final class PagedSpliterator {

    /**
     * Defensive bound on consecutive empty-but-not-terminal pages. A
     * misbehaving server returning {@code Page(items=[], hasMore=true)} (or
     * the cursor analogue) repeatedly would otherwise drive an infinite walk;
     * we abort with a typed exception so callers see the bug instead of a
     * hung thread. Tunable via the JVM system property
     * {@code allegro.sdk.pagination.maxConsecutiveEmptyPages}.
     */
    static final int MAX_CONSECUTIVE_EMPTY_PAGES =
            Integer.getInteger("allegro.sdk.pagination.maxConsecutiveEmptyPages", 100_000);

    private static final String ERR_TOO_MANY_EMPTY_PAGES =
            "Aborting paginated walk: server returned more than "
                    + MAX_CONSECUTIVE_EMPTY_PAGES
                    + " consecutive empty pages with hasMore/nextCursor still set. "
                    + "This indicates a server bug or contract drift.";
    private static final String ERR_FETCH_PAGE_NULL = "fetchPage must not be null";
    private static final String ERR_ACTION_NULL = "action must not be null";
    private static final String ERR_ITEMS_NULL = "items must not be null";

    private PagedSpliterator() {
    }

    /**
     * Wrap an offset-based pager as a sequential {@link Stream}.
     *
     * @param fetchPage given the current zero-based page index, return the
     *     matching {@link Page}
     */
    public static <T> Stream<T> stream(IntFunction<Page<T>> fetchPage) {
        Objects.requireNonNull(fetchPage, ERR_FETCH_PAGE_NULL);
        return StreamSupport.stream(new OffsetSpliterator<>(fetchPage), false);
    }

    /**
     * Wrap a cursor-based pager as a sequential {@link Stream}. The first
     * invocation receives {@code null}; subsequent invocations receive the
     * previous {@link CursorPage#nextCursor()}. Iteration stops when the
     * cursor goes {@code null} or empty.
     */
    public static <T> Stream<T> cursorStream(Function<@Nullable String, CursorPage<T>> fetchPage) {
        Objects.requireNonNull(fetchPage, ERR_FETCH_PAGE_NULL);
        return StreamSupport.stream(new CursorSpliterator<>(fetchPage), false);
    }

    /**
     * One page from an offset-based pager.
     *
     * @param items records on this page (non-null, possibly empty)
     * @param hasMore {@code true} if at least one more page exists
     */
    public record Page<T>(List<T> items, boolean hasMore) {
        public Page {
            Objects.requireNonNull(items, ERR_ITEMS_NULL);
        }
    }

    /**
     * One page from a cursor-based pager.
     *
     * @param items records on this page (non-null, possibly empty)
     * @param nextCursor cursor to pass on the next call, or {@code null} when
     *     the result set is exhausted
     */
    public record CursorPage<T>(List<T> items, @Nullable String nextCursor) {
        public CursorPage {
            Objects.requireNonNull(items, ERR_ITEMS_NULL);
        }
    }

    private abstract static class BaseSpliterator<T> implements Spliterator<T> {

        private static final int CHARACTERISTICS = NONNULL | ORDERED;

        protected final Deque<T> buffer = new ArrayDeque<>();
        protected boolean exhausted;

        @Override
        public final boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action, ERR_ACTION_NULL);
            if (buffer.isEmpty() && !fetchUntilNonEmptyOrEnd()) {
                return false;
            }
            action.accept(buffer.poll());
            return true;
        }

        protected abstract boolean fetchUntilNonEmptyOrEnd();

        /*
         * Sequential by design — page fetching is I/O-bound and ordered;
         * returning null from trySplit means single-threaded traversal
         * (the Spliterator contract, not an unfinished method).
         */
        @Override
        @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
        public final @Nullable Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public final long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public final int characteristics() {
            return CHARACTERISTICS;
        }
    }

    private static final class OffsetSpliterator<T> extends BaseSpliterator<T> {

        private final IntFunction<Page<T>> fetchPage;
        private int nextPageIndex;

        OffsetSpliterator(IntFunction<Page<T>> fetchPage) {
            this.fetchPage = fetchPage;
        }

        @Override
        protected boolean fetchUntilNonEmptyOrEnd() {
            int consecutiveEmpty = 0;
            while (!exhausted) {
                Page<T> page = fetchPage.apply(nextPageIndex);
                nextPageIndex++;
                buffer.addAll(page.items());
                if (!page.hasMore()) {
                    exhausted = true;
                }
                if (!buffer.isEmpty()) {
                    return true;
                }
                if (page.hasMore()) {
                    consecutiveEmpty++;
                    if (consecutiveEmpty > MAX_CONSECUTIVE_EMPTY_PAGES) {
                        throw new IllegalStateException(ERR_TOO_MANY_EMPTY_PAGES);
                    }
                }
            }
            return false;
        }
    }

    private static final class CursorSpliterator<T> extends BaseSpliterator<T> {

        private final Function<@Nullable String, CursorPage<T>> fetchPage;
        private @Nullable String nextCursor;

        CursorSpliterator(Function<@Nullable String, CursorPage<T>> fetchPage) {
            this.fetchPage = fetchPage;
        }

        @Override
        protected boolean fetchUntilNonEmptyOrEnd() {
            int consecutiveEmpty = 0;
            while (!exhausted) {
                CursorPage<T> page = fetchPage.apply(nextCursor);
                buffer.addAll(page.items());
                String next = page.nextCursor();
                if (next == null || next.isEmpty()) {
                    exhausted = true;
                } else {
                    nextCursor = next;
                }
                if (!buffer.isEmpty()) {
                    return true;
                }
                if (!exhausted) {
                    consecutiveEmpty++;
                    if (consecutiveEmpty > MAX_CONSECUTIVE_EMPTY_PAGES) {
                        throw new IllegalStateException(ERR_TOO_MANY_EMPTY_PAGES);
                    }
                }
            }
            return false;
        }
    }
}
