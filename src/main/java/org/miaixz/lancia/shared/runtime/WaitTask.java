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
package org.miaixz.lancia.shared.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.bus.core.xyz.ExceptionKit;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.Handle;

/**
 * Waits for page-side predicates using the configured polling strategy.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class WaitTask {

    /**
     * Shared constant for runner.
     */
    private static final ExecutorService RUNNER = new ThreadPoolExecutor(0,
            Math.max(4, java.lang.Runtime.getRuntime().availableProcessors()), 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1024), new NamedThreadFactory("lancia-wait-task-", true),
            new ThreadPoolExecutor.AbortPolicy());
    /**
     * Shared constant for timer.
     */
    private static final ScheduledExecutorService TIMER = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-wait-task-timer-", true));
    /**
     * Current runtime.
     */
    private final Runtime runtime;
    /**
     * Current options.
     */
    private final WaitTaskOptions options;
    /**
     * Current expression.
     */
    private final String expression;

    /**
     * Current result.
     */
    private final CompletableFuture<Handle> result = new CompletableFuture<>();
    /**
     * Thread-safe terminated state.
     */
    private final AtomicBoolean terminated = new AtomicBoolean();
    /**
     * Thread-safe rerun ID state.
     */
    private final AtomicInteger rerunId = new AtomicInteger();
    /**
     * Current timeout task.
     */
    private volatile ScheduledFuture<?> timeoutTask;

    /**
     * Creates a wait task.
     *
     * @param runtime    runtime
     * @param options    operation options
     * @param expression JavaScript expression
     */
    public WaitTask(Runtime runtime, WaitTaskOptions options, String expression) {
        this.runtime = Assert.notNull(runtime, "runtime");
        this.options = options == null ? new WaitTaskOptions() : options;
        this.expression = expression == null ? "undefined" : expression;
        this.runtime.taskManager().add(this);
        Logger.debug(
                true,
                "Page",
                "WaitTask created: expressionChars={}, timeout={}, polling={}",
                this.expression.length(),
                this.options.timeout(),
                this.options.polling());
        scheduleTimeout();
        rerun();
    }

    /**
     * Returns the result.
     *
     * @return completion future
     */
    public CompletableFuture<Handle> result() {
        return result;
    }

    /**
     * Returns the rerun.
     *
     * @return completion future
     */
    public CompletableFuture<Void> rerun() {
        int id = rerunId.incrementAndGet();
        Logger.debug(true, "Page", "WaitTask rerun requested: id={}", id);
        try {
            return CompletableFuture.runAsync(() -> run(id), RUNNER);
        } catch (RejectedExecutionException ex) {
            InternalException error = new InternalException("WaitTask queue is full.", ex);
            terminate(error);
            CompletableFuture<Void> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(error);
            return rejected;
        }
    }

    /**
     * Returns the terminate.
     *
     * @return completion future
     */
    public CompletableFuture<Void> terminate() {
        return terminate(null);
    }

    /**
     * Returns the terminate.
     *
     * @param error error to propagate
     * @return completion future
     */
    public CompletableFuture<Void> terminate(Throwable error) {
        if (terminated.compareAndSet(false, true)) {
            cancelTimeout();
            runtime.taskManager().delete(this);
            if (error != null && !result.isDone()) {
                result.completeExceptionally(error);
                Logger.warn(false, "Page", error, "WaitTask terminated with error.");
            } else {
                Logger.debug(false, "Page", "WaitTask terminated.");
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the terminated.
     *
     * @return {@code true} when the condition matches
     */
    public boolean terminated() {
        return terminated.get();
    }

    /**
     * Returns the bad error.
     *
     * @param error error to propagate
     * @return bad error
     */
    public Optional<Throwable> getBadError(Throwable error) {
        if (error == null) {
            return Optional.of(new InternalException("WaitTask failed with an error"));
        }
        String message = error.getMessage() == null ? Normal.EMPTY : error.getMessage();
        if (message.contains("Execution context is not available in detached frame")) {
            return Optional.of(new InternalException("Waiting failed: Frame detached", error));
        }
        if (message.contains("Execution context was destroyed")
                || message.contains("Cannot find context with specified id")
                || message.contains("DiscardedBrowsingContextError")) {
            return Optional.empty();
        }
        return Optional.of(error);
    }

    /**
     * Handles run.
     *
     * @param id identifier
     */
    private void run(int id) {
        while (!terminated.get() && rerunId.get() == id && !result.isDone()) {
            Handle handle = null;
            try {
                handle = runtime.evaluateHandle(expression);
                if (Builder.isTruthy(handle.jsonValue())) {
                    terminate();
                    if (result.complete(handle)) {
                        handle = null;
                        Logger.debug(false, "Page", "WaitTask completed: id={}", id);
                    }
                    return;
                }
            } catch (Throwable error) {
                Optional<Throwable> badError = getBadError(
                        ExceptionKit.unwrap(error.getCause() == null ? error : error.getCause()));
                if (badError.isPresent()) {
                    Logger.warn(false, "Page", badError.orElseThrow(), "WaitTask failed: id={}", id);
                    terminate(new InternalException("Waiting failed", badError.orElseThrow()));
                    return;
                }
            } finally {
                if (handle != null) {
                    handle.dispose();
                }
            }
            sleep(options.pollingInterval());
        }
    }

    /**
     * Handles schedule timeout.
     */
    private void scheduleTimeout() {
        Duration timeout = options.timeout();
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return;
        }
        Logger.debug(false, "Page", "WaitTask timeout scheduled: timeout={}", timeout);
        timeoutTask = TIMER.schedule(() -> {
            Logger.warn(false, "Page", "WaitTask timeout exceeded: timeoutMillis={}", timeout.toMillis());
            terminate(new TimeoutException("Waiting failed: " + timeout.toMillis() + "ms exceeded"));
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Cancels the timeout task.
     */
    private void cancelTimeout() {
        ScheduledFuture<?> timeout = timeoutTask;
        if (timeout != null) {
            timeout.cancel(false);
            timeoutTask = null;
        }
    }

    /**
     * Handles sleep.
     *
     * @param polling polling value
     */
    private void sleep(Duration polling) {
        if (!ThreadKit.sleep(Math.max(1L, polling.toMillis()))) {
            terminate(new InternalException("WaitTask interrupted"));
        }
    }

    /**
     * Defines options for wait task operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class WaitTaskOptions {

        /**
         * Default timeout.
         */
        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
        /**
         * Current polling.
         */
        private Polling polling = Polling.MUTATION;
        /**
         * Current interval.
         */
        private Duration interval = Duration.ofMillis(50);
        /**
         * Current root.
         */
        private Element root;
        /**
         * Current timeout.
         */
        private Duration timeout = DEFAULT_TIMEOUT;

        /**
         * Returns the polling.
         *
         * @return polling value
         */
        public Polling polling() {
            return polling;
        }

        /**
         * Updates polling.
         *
         * @param polling polling value
         */
        public void setPolling(Polling polling) {
            this.polling = polling == null ? Polling.MUTATION : polling;
        }

        /**
         * Updates polling.
         *
         * @param interval interval value
         */
        public void setPolling(Duration interval) {
            this.polling = Polling.INTERVAL;
            this.interval = interval == null ? Duration.ofMillis(50) : interval;
        }

        /**
         * Returns the polling interval.
         *
         * @return polling interval value
         */
        public Duration pollingInterval() {
            return switch (polling) {
                case RAF -> Duration.ofMillis(16);
                case MUTATION -> Duration.ofMillis(50);
                case INTERVAL -> interval == null ? Duration.ofMillis(50) : interval;
            };
        }

        /**
         * Returns the root.
         *
         * @return optional value
         */
        public Optional<Element> root() {
            return Optional.ofNullable(root);
        }

        /**
         * Updates root.
         *
         * @param root root value
         */
        public void setRoot(Element root) {
            this.root = root;
        }

        /**
         * Returns the timeout.
         *
         * @return timeout value
         */
        public Duration timeout() {
            return timeout;
        }

        /**
         * Updates timeout.
         *
         * @param timeout timeout value
         */
        public void setTimeout(Duration timeout) {
            this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        }
    }

    /**
     * Enumerates Pollings.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public enum Polling {

        /**
         * Represents the raf enum member.
         */
        RAF,

        /**
         * Represents the mutation enum member.
         */
        MUTATION,

        /**
         * Manages task state and protocol events.
         */
        INTERVAL
    }

    /**
     * Provides the runtime operations required by wait tasks.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface Runtime {

        /**
         * Evaluates JavaScript and returns a local value.
         *
         * @param expression JavaScript expression
         * @return evaluated value
         */
        Object evaluate(String expression);

        /**
         * Evaluates JavaScript and returns a remote handle.
         *
         * @param expression JavaScript expression
         * @return remote handle
         */
        Handle evaluateHandle(String expression);

        /**
         * Returns the owning task manager.
         *
         * @return task manager
         */
        TaskManager taskManager();
    }

    /**
     * Manages task state and protocol events.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class TaskManager {

        /**
         * Registered tasks values.
         */
        private final Set<WaitTask> tasks = ConcurrentHashMap.newKeySet();

        /**
         * Handles add.
         *
         * @param task task value
         */
        public void add(WaitTask task) {
            if (task != null) {
                tasks.add(task);
            }
        }

        /**
         * Handles delete.
         *
         * @param task task value
         */
        public void delete(WaitTask task) {
            if (task != null) {
                tasks.remove(task);
            }
        }

        /**
         * Returns the size.
         *
         * @return size value
         */
        public int size() {
            return tasks.size();
        }

        /**
         * Handles terminate all.
         *
         * @param error error to propagate
         */
        public void terminateAll(Throwable error) {
            List<WaitTask> snapshot = new ArrayList<>(tasks);
            for (WaitTask task : snapshot) {
                task.terminate(error);
            }
            tasks.clear();
        }

        /**
         * Returns the rerun all.
         *
         * @return completion future
         */
        public CompletableFuture<Void> rerunAll() {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (WaitTask task : tasks) {
                futures.add(task.rerun());
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }
    }

}
