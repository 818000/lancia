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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import lombok.Setter;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;

/**
 * One-shot asynchronous result with Puppeteer Deferred semantics.
 *
 * <p>
 * The class preserves the existing success/failure API while exposing Deferred-style resolve, reject, status, timeout,
 * value, and race helpers.
 * </p>
 *
 * @param <T> result type
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class LanciaOnce<T> {

    /**
     * Shared timeout scheduler.
     */
    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-once-timeout-", true));

    /**
     * Creates a one-shot result without timeout.
     */
    public LanciaOnce() {
        // No initialization required.
    }

    /**
     * Creates a one-shot result with optional timeout.
     *
     * @param options deferred options
     */
    public LanciaOnce(Options options) {
        Options actualOptions = options == null ? new Options() : options;
        scheduleTimeout(actualOptions.getMessage(), actualOptions.getTimeoutMillis());
    }

    /**
     * Creates a one-shot result with optional timeout.
     *
     * @param message timeout message
     * @param timeout timeout value
     * @param unit    timeout unit
     */
    public LanciaOnce(String message, long timeout, TimeUnit unit) {
        Assert.notNull(unit, "unit");
        scheduleTimeout(message, unit.toMillis(timeout));
    }

    /**
     * Underlying asynchronous result.
     */
    private final CompletableFuture<T> future = new CompletableFuture<>();

    /**
     * Finished flag.
     */
    private final AtomicBoolean finished = new AtomicBoolean();

    /**
     * Whether result resolved successfully.
     */
    private volatile boolean resolved;

    /**
     * Whether result was rejected.
     */
    private volatile boolean rejected;

    /**
     * Stored successful value or failure object.
     */
    private volatile Object value;

    /**
     * Timeout task.
     */
    private volatile ScheduledFuture<?> timeoutTask;

    /**
     * Creates a one-shot result without timeout.
     *
     * @param <R> result type
     * @return one-shot result
     */
    public static <R> LanciaOnce<R> create() {
        return new LanciaOnce<>();
    }

    /**
     * Creates a one-shot result with optional timeout.
     *
     * @param options deferred options
     * @param <R>     result type
     * @return one-shot result
     */
    public static <R> LanciaOnce<R> create(Options options) {
        return new LanciaOnce<>(options);
    }

    /**
     * Races completion stages or {@link LanciaOnce} values and clears pending timeouts after the first result.
     *
     * @param awaitables completion stages or one-shot results
     * @param <R>        result type
     * @return first completed result
     */
    public static <R> CompletableFuture<R> race(Collection<?> awaitables) {
        Assert.notNull(awaitables, "awaitables");
        CompletableFuture<R> result = new CompletableFuture<>();
        if (awaitables.isEmpty()) {
            result.completeExceptionally(new IllegalArgumentException("Awaitables must not be empty."));
            return result;
        }
        List<LanciaOnce<?>> deferredWithTimeout = new ArrayList<>();
        List<CompletionStage<? extends R>> stages = new ArrayList<>();
        for (Object awaitable : awaitables) {
            stages.add(completionStage(awaitable, deferredWithTimeout));
        }
        AtomicBoolean completed = new AtomicBoolean(false);
        for (CompletionStage<? extends R> stage : stages) {
            stage.whenComplete((value, throwable) -> {
                if (completed.compareAndSet(false, true)) {
                    clearTimeouts(deferredWithTimeout);
                    if (throwable == null) {
                        result.complete(value);
                    } else {
                        result.completeExceptionally(throwable);
                    }
                }
            });
        }
        return result;
    }

    /**
     * Resolves result successfully.
     *
     * @param value successful value
     * @return whether this call completed result
     */
    public boolean success(T value) {
        return resolve(value);
    }

    /**
     * Resolves result successfully.
     *
     * @param value successful value
     * @return whether this call completed result
     */
    public boolean resolve(T value) {
        if (!finished.compareAndSet(false, true)) {
            return false;
        }
        resolved = true;
        finish(value);
        return future.complete(value);
    }

    /**
     * Rejects result with a failure.
     *
     * @param throwable failure object
     * @return whether this call completed result
     */
    public boolean failure(Throwable throwable) {
        return reject(throwable);
    }

    /**
     * Rejects result with a failure.
     *
     * @param throwable failure object
     * @return whether this call completed result
     */
    public boolean reject(Throwable throwable) {
        Throwable actualThrowable = Assert.notNull(throwable, "throwable");
        if (!finished.compareAndSet(false, true)) {
            return false;
        }
        rejected = true;
        finish(actualThrowable);
        return future.completeExceptionally(actualThrowable);
    }

    /**
     * Waits for result within the specified timeout.
     *
     * @param timeout wait timeout
     * @param unit    timeout unit
     * @return asynchronous result
     * @throws InterruptedException                  when the current thread is interrupted
     * @throws ExecutionException                    when the asynchronous result fails
     * @throws java.util.concurrent.TimeoutException when waiting times out
     */
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
        return future.get(timeout, unit);
    }

    /**
     * Resolves d.
     *
     * @return {@code true} when resolved
     */
    public boolean resolved() {
        return resolved;
    }

    /**
     * Returns the finished.
     *
     * @return {@code true} when the condition matches
     */
    public boolean finished() {
        return finished.get();
    }

    /**
     * Returns successful or failure object.
     *
     * @return stored value, or {@code null} when unfinished or when resolved with {@code null}
     */
    public Object value() {
        return value;
    }

    /**
     * Returns result future and throws failures through future completion.
     *
     * @return completion future
     */
    public CompletableFuture<T> valueOrThrow() {
        return future;
    }

    /**
     * Returns the underlying asynchronous result.
     *
     * @return underlying asynchronous result
     */
    public CompletableFuture<T> future() {
        return future;
    }

    /**
     * Returns whether timeout is available.
     *
     * @return {@code true} when the condition matches
     */
    private boolean hasTimeout() {
        ScheduledFuture<?> current = timeoutTask;
        return current != null && !current.isDone();
    }

    /**
     * Schedules timeout rejection.
     *
     * @param message       timeout message
     * @param timeoutMillis timeout in milliseconds
     */
    private void scheduleTimeout(String message, long timeoutMillis) {
        if (timeoutMillis <= 0) {
            return;
        }
        String actualMessage = message == null ? "Deferred timeout exceeded." : message;
        timeoutTask = TIMEOUT_EXECUTOR.schedule(
                () -> reject(new org.miaixz.bus.core.lang.exception.TimeoutException(actualMessage)),
                timeoutMillis,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Handles finish.
     *
     * @param value to use
     */
    private void finish(Object value) {
        ScheduledFuture<?> current = timeoutTask;
        if (current != null) {
            current.cancel(false);
        }
        this.value = value;
    }

    /**
     * Clears all timeout-backed one-shot results after a race finishes.
     *
     * @param deferredWithTimeout one-shot results with active timeouts
     */
    private static void clearTimeouts(List<LanciaOnce<?>> deferredWithTimeout) {
        for (LanciaOnce<?> deferred : deferredWithTimeout) {
            deferred.reject(new InternalException("Timeout cleared"));
        }
    }

    /**
     * Converts a supported awaitable into a completion stage.
     *
     * @param awaitable           awaitable value
     * @param deferredWithTimeout timeout-backed one-shot results
     * @param <R>                 result type
     * @return completion stage
     */
    private static <R> CompletionStage<? extends R> completionStage(
            Object awaitable,
            List<LanciaOnce<?>> deferredWithTimeout) {
        Assert.notNull(awaitable, "awaitable");
        if (awaitable instanceof LanciaOnce<?> deferred) {
            if (deferred.hasTimeout()) {
                deferredWithTimeout.add(deferred);
            }
            return (CompletionStage<? extends R>) deferred.valueOrThrow();
        }
        if (awaitable instanceof CompletionStage<?> stage) {
            return (CompletionStage<? extends R>) stage;
        }
        throw new IllegalArgumentException("Awaitable must be a CompletionStage or LanciaOnce.");
    }

    /**
     * Deferred timeout options.
     */
    @Getter
    @Setter
    /**
     * Defines options for operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static class Options {

        /**
         * Creates deferred timeout options.
         */
        public Options() {
            // No initialization required.
        }

        /**
         * Timeout failure message.
         */
        private String message = "Deferred timeout exceeded.";

        /**
         * Timeout in milliseconds. Values less than or equal to zero disable timeout.
         */
        private long timeoutMillis;
    }

}
