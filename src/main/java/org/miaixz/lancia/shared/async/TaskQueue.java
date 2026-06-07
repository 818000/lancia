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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;

/**
 * Serializes asynchronous tasks onto a single queue.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class TaskQueue implements AutoCloseable {

    /**
     * Current executor.
     */
    private final ExecutorService executor;
    /**
     * Thread-safe closed state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);
    /**
     * Current monitor.
     */
    private final Object monitor = new Object();
    /**
     * Current chain.
     */
    private CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

    /**
     * Creates a task queue.
     */
    public TaskQueue() {
        this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("lancia-task-queue-", true));
    }

    /**
     * Returns the enqueue.
     *
     * @param task task value
     * @param <T>  task result type
     * @return completion future
     */
    public <T> CompletableFuture<T> enqueue(Callable<T> task) {
        return postTask(task);
    }

    /**
     * Returns the enqueue.
     *
     * @param task task value
     * @return completion future
     */
    public CompletableFuture<Void> enqueue(Runnable task) {
        return postTask(task);
    }

    /**
     * Returns the post task.
     *
     * @param task task value
     * @param <T>  task result type
     * @return completion future
     */
    public <T> CompletableFuture<T> postTask(Callable<T> task) {
        Assert.notNull(task, "task");
        return postTaskAsync(() -> {
            try {
                return CompletableFuture.completedFuture(task.call());
            } catch (Throwable throwable) {
                return Awaitable.failed(throwable);
            }
        });
    }

    /**
     * Returns the post task.
     *
     * @param task task value
     * @return completion future
     */
    public CompletableFuture<Void> postTask(Runnable task) {
        Assert.notNull(task, "task");
        return postTask(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Returns the post task async.
     *
     * @param task task value
     * @param <T>  task result type
     * @return completion future
     */
    public <T> CompletableFuture<T> postTaskAsync(Supplier<? extends CompletionStage<T>> task) {
        Assert.notNull(task, "task");
        synchronized (monitor) {
            if (closed.get()) {
                return Awaitable.failed(new InternalException("Task queue has been closed."));
            }
            CompletableFuture<T> result = chain.thenComposeAsync(ignored -> {
                try {
                    CompletionStage<T> stage = task.get();
                    if (stage == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return stage;
                } catch (Throwable throwable) {
                    return Awaitable.failed(throwable);
                }
            }, executor).toCompletableFuture();
            chain = result.handle((value, throwable) -> null);
            return result;
        }
    }

    /**
     * Returns whether this object is closed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        synchronized (monitor) {
            if (closed.compareAndSet(false, true)) {
                executor.shutdown();
            }
        }
    }

}
