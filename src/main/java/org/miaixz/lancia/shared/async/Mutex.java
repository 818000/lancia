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

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FIFO asynchronous mutex aligned with Puppeteer's Mutex.ts.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Mutex {

    /**
     * Waiting acquirers in FIFO order.
     */
    private final Queue<Runnable> acquirers = new ArrayDeque<>();

    /**
     * Lock state.
     */
    private boolean locked;

    /**
     * Acquires the mutex.
     *
     * @return guard that releases the mutex when closed
     */
    public CompletableFuture<Guard> acquire() {
        return acquire(null);
    }

    /**
     * Acquires the mutex and runs a callback when the guard is released.
     *
     * @param onRelease release callback
     * @return guard that releases the mutex when closed
     */
    public CompletableFuture<Guard> acquire(Runnable onRelease) {
        LanciaOnce<Void> deferred;
        synchronized (this) {
            if (!locked) {
                locked = true;
                return CompletableFuture.completedFuture(new Guard(this, onRelease));
            }
            deferred = LanciaOnce.create();
            LanciaOnce<Void> current = deferred;
            acquirers.add(() -> current.resolve(null));
        }
        return deferred.valueOrThrow().thenApply(ignored -> new Guard(this, onRelease));
    }

    /**
     * Releases the mutex and wakes the next queued acquirer when present.
     */
    public void release() {
        Runnable resolve;
        synchronized (this) {
            resolve = acquirers.poll();
            if (resolve == null) {
                locked = false;
                return;
            }
        }
        resolve.run();
    }

    /**
     * Returns the locked.
     *
     * @return {@code true} when the condition matches
     */
    public synchronized boolean locked() {
        return locked;
    }

    /**
     * Returns the number of queued acquirers.
     *
     * @return queue size
     */
    public synchronized int queueSize() {
        return acquirers.size();
    }

    /**
     * Guard that releases a mutex when closed.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Guard implements AutoCloseable {

        /**
         * Owning mutex.
         */
        private final Mutex mutex;

        /**
         * Release callback.
         */
        private final Runnable onRelease;

        /**
         * Closed flag.
         */
        private final AtomicBoolean closed = new AtomicBoolean();

        /**
         * Creates a guard.
         *
         * @param mutex     owning mutex
         * @param onRelease release callback
         */
        private Guard(Mutex mutex, Runnable onRelease) {
            this.mutex = mutex;
            this.onRelease = onRelease;
        }

        /**
         * Releases the guard once.
         */
        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            if (onRelease != null) {
                onRelease.run();
            }
            mutex.release();
        }
    }

}
