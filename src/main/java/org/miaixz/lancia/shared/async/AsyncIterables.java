/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                           ~
 ~ Copyright (c) 2015-2026 miaixz.org and other contributors.                ~
 ~                                                                           ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");           ~
 ~ you may not use this file except in compliance with the License.          ~
 ~ You may obtain a copy of the License at                                   ~
 ~                                                                           ~
 ~      https://www.apache.org/licenses/LICENSE-2.0                          ~
 ~                                                                           ~
 ~ Unless required by applicable law or agreed to in writing, software       ~
 ~ distributed under the License is distributed on an "AS IS" BASIS,         ~
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  ~
 ~ See the License for the specific language governing permissions and       ~
 ~ limitations under the License.                                            ~
 ~                                                                           ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.shared.async;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.ListKit;

/**
 * Provides iterable operations that preserve asynchronous evaluation order.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class AsyncIterables {

    /**
     * Prevents instantiation.
     */
    private AsyncIterables() {
        // No initialization required.
    }

    /**
     * Lazily maps an iterable and waits for each mapped stage before yielding it.
     *
     * @param iterable source iterable
     * @param mapper   asynchronous mapper
     * @param <T>      source item type
     * @param <U>      mapped item type
     * @return lazy mapped iterable
     */
    public static <T, U> Iterable<U> map(
            Iterable<? extends T> iterable,
            Function<? super T, ? extends CompletionStage<? extends U>> mapper) {
        Assert.notNull(iterable, "iterable");
        Assert.notNull(mapper, "mapper");
        return () -> new Iterator<>() {

            /**
             * Source iterator.
             */
            private final Iterator<? extends T> iterator = iterable.iterator();

            /**
             * Returns whether next is available.
             *
             * @return {@code true} when the condition matches
             */
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            /**
             * Returns the next.
             *
             * @return next value
             */
            @Override
            public U next() {
                CompletionStage<? extends U> stage = Assert.notNull(mapper.apply(iterator.next()), "mapped stage");
                return await(stage);
            }
        };
    }

    /**
     * Lazily maps each source item to another iterable and yields all mapped items in order.
     *
     * @param iterable source iterable
     * @param mapper   iterable mapper
     * @param <T>      source item type
     * @param <U>      mapped item type
     * @return lazy flattened iterable
     */
    public static <T, U> Iterable<U> flatMap(
            Iterable<? extends T> iterable,
            Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Assert.notNull(iterable, "iterable");
        Assert.notNull(mapper, "mapper");
        return () -> new Iterator<>() {

            /**
             * Source iterator.
             */
            private final Iterator<? extends T> outer = iterable.iterator();

            /**
             * Current inner iterator.
             */
            private Iterator<? extends U> inner = Collections.emptyIterator();

            /**
             * Returns whether next is available.
             *
             * @return {@code true} when the condition matches
             */
            @Override
            public boolean hasNext() {
                while (!inner.hasNext() && outer.hasNext()) {
                    Iterable<? extends U> mapped = Assert.notNull(mapper.apply(outer.next()), "mapped iterable");
                    inner = mapped.iterator();
                }
                return inner.hasNext();
            }

            /**
             * Returns the next.
             *
             * @return next value
             */
            @Override
            public U next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                return inner.next();
            }
        };
    }

    /**
     * Collects every value from an iterable in iteration order.
     *
     * @param iterable source iterable
     * @param <T>      source item type
     * @return collected values
     */
    public static <T> List<T> collect(Iterable<? extends T> iterable) {
        Assert.notNull(iterable, "iterable");
        List<T> result = ListKit.of();
        for (T value : iterable) {
            result.add(value);
        }
        return result;
    }

    /**
     * Returns first from an iterable.
     *
     * @param iterable source iterable
     * @param <T>      source item type
     * @return first value, or empty when iterable has nos
     */
    public static <T> Optional<T> first(Iterable<? extends T> iterable) {
        Assert.notNull(iterable, "iterable");
        Iterator<? extends T> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        }
        return Optional.ofNullable(iterator.next());
    }

    /**
     * Converts completion stages into a lazy iterable that waits for each stage in order.
     *
     * @param stages completion stages
     * @param <T>    item type
     * @return lazy iterable
     */
    public static <T> Iterable<T> awaitEach(Iterable<? extends CompletionStage<? extends T>> stages) {
        Assert.notNull(stages, "stages");
        return () -> new Iterator<>() {

            /**
             * Source iterator.
             */
            private final Iterator<? extends CompletionStage<? extends T>> iterator = stages.iterator();

            /**
             * Returns whether next is available.
             *
             * @return {@code true} when the condition matches
             */
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            /**
             * Returns the next.
             *
             * @return next value
             */
            @Override
            public T next() {
                return await(Assert.notNull(iterator.next(), "stage"));
            }
        };
    }

    /**
     * Waits for a completion stage and converts checked failures to bus-all runtime exceptions.
     *
     * @param stage completion stage
     * @param <T>   result type
     * @return completed value
     */
    private static <T> T await(CompletionStage<? extends T> stage) {
        try {
            return stage.toCompletableFuture().get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InternalException("Interrupted while consuming an async iterable.", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new InternalException("Failed to consume an async iterable.", cause);
        }
    }

}
