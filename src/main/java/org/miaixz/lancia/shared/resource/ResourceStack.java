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
package org.miaixz.lancia.shared.resource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.ExceptionKit;

/**
 * LIFO disposable resource stack aligned with Puppeteer's disposable.ts.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ResourceStack implements AutoCloseable {

    /**
     * Resources in acquisition order.
     */
    private Deque<AutoCloseable> stack = new ArrayDeque<>();

    /**
     * Disposed flag.
     */
    private boolean disposed;

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public synchronized boolean disposed() {
        return disposed;
    }

    /**
     * Adds a disposable resource and returns it.
     *
     * @param value disposable resource, or {@code null}
     * @param <T>   resource type
     * @return provided resource
     */
    public synchronized <T extends AutoCloseable> T use(T value) {
        ensureActive();
        if (value != null) {
            stack.addLast(value);
        }
        return value;
    }

    /**
     * Adopts a resource with a disposal callback.
     *
     * @param value     resource value
     * @param onDispose disposal callback
     * @param <T>       resource type
     * @return provided resource
     */
    public synchronized <T> T adopt(T value, Consumer<? super T> onDispose) {
        ensureActive();
        Assert.notNull(onDispose, "onDispose");
        stack.addLast(() -> onDispose.accept(value));
        return value;
    }

    /**
     * Defers a disposal callback.
     *
     * @param onDispose disposal callback
     */
    public synchronized void defer(ThrowingRunnable onDispose) {
        ensureActive();
        stack.addLast(Assert.notNull(onDispose, "onDispose")::run);
    }

    /**
     * Moves all resources into a new stack and marks this stack as disposed.
     *
     * @return moved resource stack
     */
    public synchronized ResourceStack move() {
        ensureActive();
        ResourceStack moved = new ResourceStack();
        moved.stack = stack;
        stack = new ArrayDeque<>();
        disposed = true;
        return moved;
    }

    /**
     * Disposes all resources in LIFO order.
     */
    public void dispose() {
        List<AutoCloseable> resources;
        synchronized (this) {
            if (disposed) {
                return;
            }
            disposed = true;
            resources = new ArrayList<>(stack);
            stack.clear();
        }
        List<Throwable> errors = new ArrayList<>();
        for (int index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Throwable ex) {
                errors.add(ex);
            }
        }
        if (!errors.isEmpty()) {
            throw disposalException(errors);
        }
    }

    /**
     * Disposes all resources in LIFO order.
     */
    @Override
    public void close() {
        dispose();
    }

    /**
     * Ensures the stack can still accept new resources.
     */
    private void ensureActive() {
        if (disposed) {
            throw new InternalException("A disposed stack can not use anything new");
        }
    }

    /**
     * Builds a bus-all runtime exception for one or more disposal failures.
     *
     * @param errors disposal failures
     * @return runtime exception
     */
    private static RuntimeException disposalException(List<Throwable> errors) {
        if (errors.size() == 1) {
            Throwable error = errors.get(0);
            if (error instanceof RuntimeException runtimeException) {
                return runtimeException;
            }
            return new InternalException("Resource disposal failed.", error);
        }
        InternalException exception = new InternalException("Multiple resources failed during disposal.",
                errors.get(0));
        for (int index = 1; index < errors.size(); index++) {
            exception.addSuppressed(errors.get(index));
        }
        return exception;
    }

    /**
     * Defines the ThrowingRunnable interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface ThrowingRunnable {

        /**
         * Runs the callback.
         *
         * @throws Exception when the callback fails
         */
        void run() throws Exception;
    }

    /**
     * Defines the AsyncResource interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface AsyncResource {

        /**
         * Disposes this resource asynchronously.
         *
         * @return asynchronous completion
         */
        CompletionStage<Void> disposeAsync();
    }

    /**
     * LIFO asynchronous disposable resource stack.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static class Async implements AutoCloseable {

        /**
         * Resources in acquisition order.
         */
        private Deque<AsyncResource> stack = new ArrayDeque<>();

        /**
         * Disposed flag.
         */
        private boolean disposed;

        /**
         * Returns the disposed.
         *
         * @return {@code true} when the condition matches
         */
        public synchronized boolean disposed() {
            return disposed;
        }

        /**
         * Adds a synchronous disposable resource and returns it.
         *
         * @param value disposable resource, or {@code null}
         * @param <T>   resource type
         * @return provided resource
         */
        public synchronized <T extends AutoCloseable> T use(T value) {
            ensureActive();
            if (value != null) {
                stack.addLast(() -> {
                    try {
                        value.close();
                        return CompletableFuture.completedFuture(null);
                    } catch (Throwable ex) {
                        return CompletableFuture.failedFuture(ex);
                    }
                });
            }
            return value;
        }

        /**
         * Adds an asynchronous disposable resource and returns it.
         *
         * @param value async disposable resource, or {@code null}
         * @param <T>   resource type
         * @return provided resource
         */
        public synchronized <T extends AsyncResource> T useAsync(T value) {
            ensureActive();
            if (value != null) {
                stack.addLast(value);
            }
            return value;
        }

        /**
         * Adopts a resource with an asynchronous disposal callback.
         *
         * @param value     resource value
         * @param onDispose asynchronous disposal callback
         * @param <T>       resource type
         * @return provided resource
         */
        public synchronized <T> T adopt(T value, Function<? super T, ? extends CompletionStage<Void>> onDispose) {
            ensureActive();
            Assert.notNull(onDispose, "onDispose");
            stack.addLast(() -> onDispose.apply(value));
            return value;
        }

        /**
         * Defers an asynchronous disposal callback.
         *
         * @param onDispose asynchronous disposal callback
         */
        public synchronized void defer(Supplier<? extends CompletionStage<Void>> onDispose) {
            ensureActive();
            Assert.notNull(onDispose, "onDispose");
            stack.addLast(onDispose::get);
        }

        /**
         * Moves all resources into a new asynchronous stack and marks this stack as disposed.
         *
         * @return moved asynchronous resource stack
         */
        public synchronized Async move() {
            ensureActive();
            Async moved = new Async();
            moved.stack = stack;
            stack = new ArrayDeque<>();
            disposed = true;
            return moved;
        }

        /**
         * Disposes all resources asynchronously in LIFO order.
         *
         * @return asynchronous completion
         */
        public CompletableFuture<Void> disposeAsync() {
            List<AsyncResource> resources;
            synchronized (this) {
                if (disposed) {
                    return CompletableFuture.completedFuture(null);
                }
                disposed = true;
                resources = new ArrayList<>(stack);
                stack.clear();
            }
            List<Throwable> errors = new ArrayList<>();
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (int index = resources.size() - 1; index >= 0; index--) {
                AsyncResource resource = resources.get(index);
                chain = chain.handle((ignored, throwable) -> {
                    if (throwable != null) {
                        errors.add(
                                ExceptionKit.unwrap(
                                        (throwable instanceof CompletionException
                                                || throwable instanceof ExecutionException)
                                                && throwable.getCause() != null ? throwable.getCause() : throwable));
                    }
                    return null;
                }).thenCompose(ignored -> disposeOne(resource, errors));
            }
            return chain.handle((ignored, throwable) -> {
                if (throwable != null) {
                    errors.add(
                            ExceptionKit.unwrap(
                                    (throwable instanceof CompletionException
                                            || throwable instanceof ExecutionException) && throwable.getCause() != null
                                                    ? throwable.getCause()
                                                    : throwable));
                }
                if (!errors.isEmpty()) {
                    throw disposalException(errors);
                }
                return null;
            });
        }

        /**
         * Disposes all resources and waits for completion.
         */
        @Override
        public void close() {
            try {
                disposeAsync().get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new InternalException("Interrupted while disposing async resources.", ex);
            } catch (ExecutionException ex) {
                throw new InternalException("Failed to dispose async resources.",
                        ExceptionKit.unwrap(ex.getCause() == null ? ex : ex.getCause()));
            }
        }

        /**
         * Ensures the stack can still accept new resources.
         */
        private void ensureActive() {
            if (disposed) {
                throw new InternalException("A disposed stack can not use anything new");
            }
        }

        /**
         * Disposes one async resource and converts synchronous callback failures to failed futures.
         *
         * @param resource async resource
         * @param errors   collected disposal failures
         * @return disposal completion
         */
        private static CompletableFuture<Void> disposeOne(AsyncResource resource, List<Throwable> errors) {
            try {
                CompletionStage<Void> stage = resource.disposeAsync();
                return stage == null ? CompletableFuture.completedFuture(null) : stage.toCompletableFuture();
            } catch (Throwable ex) {
                errors.add(ex);
                return CompletableFuture.completedFuture(null);
            }
        }
    }

}
