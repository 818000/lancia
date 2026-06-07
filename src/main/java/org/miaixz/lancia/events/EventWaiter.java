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
package org.miaixz.lancia.events;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Emitter;
import org.miaixz.lancia.shared.async.LanciaOnce;

/**
 * Waits for one of several event-driven completion signals.
 *
 * @param <E> the generic type handled by this member
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class EventWaiter<E> {

    /**
     * Creates an EventWaiter instance.
     */
    public EventWaiter() {
        // No initialization required.
    }

    /**
     * Executor used for lifecycle timeout tasks.
     */
    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-event-waiter-", true));

    /**
     * Returns the subscribe.
     *
     * @param emitter  emitter value
     * @param event    event type
     * @param listener event listener
     * @return subscribe value
     */
    public Binding subscribe(Emitter<E> emitter, E event, Consumer<Object> listener) {
        Assert.notNull(emitter, "emitter");
        Assert.notNull(event, "event");
        Assert.notNull(listener, "listener");
        emitter.on(event, listener);
        return new EventBinding(() -> emitter.off(event, listener));
    }

    /**
     * Returns the wait for.
     *
     * @param emitter emitter value
     * @param event   event type
     * @param timeout timeout value
     * @return completion future
     */
    public CompletableFuture<Object> waitFor(Emitter<E> emitter, E event, Duration timeout) {
        return waitFor(emitter, event, value -> true, timeout);
    }

    /**
     * Returns the wait for.
     *
     * @param emitter   emitter value
     * @param event     event type
     * @param predicate predicate value
     * @param timeout   timeout value
     * @return completion future
     */
    public CompletableFuture<Object> waitFor(
            Emitter<E> emitter,
            E event,
            Predicate<Object> predicate,
            Duration timeout) {
        Assert.notNull(predicate, "predicate");
        Assert.notNull(timeout, "timeout");
        LanciaOnce<Object> once = new LanciaOnce<>();
        AtomicReference<Binding> binding = new AtomicReference<>();
        Consumer<Object> listener = payload -> {
            if (predicate.test(payload) && once.success(payload)) {
                Binding current = binding.get();
                if (current != null) {
                    current.unbind();
                }
            }
        };
        binding.set(subscribe(emitter, event, listener));
        TIMEOUT_EXECUTOR.schedule(() -> {
            if (once.failure(new TimeoutException("Timed out waiting for event: " + event))) {
                Binding current = binding.get();
                if (current != null) {
                    current.unbind();
                }
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        return once.future();
    }

    /**
     * Returns the first of.
     *
     * @param futures futures value
     * @return completion future
     */
    @SafeVarargs
    public final <T> CompletableFuture<T> firstOf(CompletableFuture<? extends T>... futures) {
        Assert.notNull(futures, "futures");
        CompletableFuture<T> result = new CompletableFuture<>();
        if (futures.length == 0) {
            result.completeExceptionally(
                    new IllegalArgumentException("Candidate asynchronous results must not be empty."));
            return result;
        }
        AtomicBoolean completed = new AtomicBoolean(false);
        for (CompletableFuture<? extends T> future : futures) {
            Assert.notNull(future, "future");
            future.whenComplete((value, throwable) -> {
                if (completed.compareAndSet(false, true)) {
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

}
