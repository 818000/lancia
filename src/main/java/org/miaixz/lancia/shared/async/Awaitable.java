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

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.ExceptionKit;
import org.miaixz.bus.core.xyz.ThreadKit;

/**
 * Provides shared awaitable result helpers.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Awaitable {

    /**
     * Hides the awaitable constructor.
     */
    private Awaitable() {
        // No initialization required.
    }

    /**
     * Waits for a future and wraps failures as InternalException.
     *
     * @param future  future
     * @param message failure message
     * @param <T>     result type
     * @return resolved value
     */
    public static <T> T await(CompletableFuture<T> future, String message) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InternalException(message, ex);
        } catch (ExecutionException ex) {
            throw new InternalException(message, ExceptionKit.unwrap(ex.getCause() == null ? ex : ex.getCause()));
        }
    }

    /**
     * Waits for a future with timeout and raises the project timeout exception on timeout.
     *
     * @param future        future
     * @param message       failure message
     * @param timeoutMillis timeout in milliseconds
     * @param <T>           result type
     * @return resolved value
     */
    public static <T> T await(CompletableFuture<T> future, String message, long timeoutMillis) {
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException ex) {
            throw new TimeoutException(message);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InternalException(message, ex);
        } catch (ExecutionException ex) {
            throw new InternalException(message, ExceptionKit.unwrap(ex.getCause() == null ? ex : ex.getCause()));
        }
    }

    /**
     * Creates a failed future.
     *
     * @param throwable throwable
     * @param <T>       result type
     * @return failed future
     */
    public static <T> CompletableFuture<T> failed(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    /**
     * Creates a failed future with an internal exception.
     *
     * @param message failure message
     * @param <T>     result type
     * @return failed future
     */
    public static <T> CompletableFuture<T> failed(String message) {
        return failed(new InternalException(message));
    }

    /**
     * Waits for all futures.
     *
     * @param futures futures
     * @return combined future
     */
    public static CompletableFuture<Void> all(Collection<? extends CompletableFuture<?>> futures) {
        if (futures == null || futures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.allOf(futures.stream().filter(Objects::nonNull).toArray(CompletableFuture[]::new));
    }

    /**
     * Waits for all futures.
     *
     * @param futures futures
     * @return combined future
     */
    public static CompletableFuture<Void> all(CompletableFuture<?>... futures) {
        return futures == null ? CompletableFuture.completedFuture(null) : all(Arrays.asList(futures));
    }

    /**
     * Sleeps for the requested duration and wraps interruption.
     *
     * @param delayMillis delay in milliseconds
     * @param message     failure message
     */
    public static void sleep(long delayMillis, String message) {
        if (delayMillis <= 0L) {
            return;
        }
        if (!ThreadKit.sleep(delayMillis)) {
            throw new InternalException(message);
        }
    }

}
