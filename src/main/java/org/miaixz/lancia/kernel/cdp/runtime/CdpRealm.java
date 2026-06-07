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
package org.miaixz.lancia.kernel.cdp.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Harness;
import org.miaixz.lancia.Realm;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.page.PageExtension;
import org.miaixz.lancia.shared.runtime.WaitTask;

/**
 * page realm implementation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpRealm implements Realm, WaitTask.Runtime {

    /**
     * Default timeout.
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    /**
     * Default polling.
     */
    private static final Duration DEFAULT_POLLING = Duration.ofMillis(50);
    /**
     * Current environment.
     */
    private final Harness environment;
    /**
     * Current origin.
     */
    private String origin;
    /**
     * Current extension.
     */
    private PageExtension extension;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean();
    /**
     * Current task manager.
     */
    private final WaitTask.TaskManager taskManager = new WaitTask.TaskManager();

    /**
     * Creates a page realm.
     *
     * @param environment environment
     */
    public CdpRealm(Harness environment) {
        this.environment = Assert.notNull(environment, "environment");
    }

    /**
     * Returns the environment.
     *
     * @return environment value
     */
    public Harness environment() {
        return environment;
    }

    /**
     * Returns the task manager.
     *
     * @return task manager value
     */
    public WaitTask.TaskManager taskManager() {
        return taskManager;
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    public CDPSession client() {
        return (CDPSession) environment.client();
    }

    /**
     * Returns the origin.
     *
     * @return optional value
     */
    public Optional<String> origin() {
        return Optional.ofNullable(origin);
    }

    /**
     * Updates origin.
     *
     * @param origin origin value
     */
    public void setOrigin(String origin) {
        this.origin = StringKit.isBlank(origin) ? null : origin;
    }

    /**
     * Returns the extension.
     *
     * @return optional value
     */
    public Optional<PageExtension> extension() {
        return Optional.ofNullable(extension);
    }

    /**
     * Updates extension.
     *
     * @param extension extension value
     */
    public void setExtension(PageExtension extension) {
        this.extension = extension;
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return disposed.get();
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return evaluate value
     */
    public Object evaluate(String expression) {
        CdpPayload remoteObject = Awaitable
                .await(evaluateRaw(expression, true), "Realm script evaluation failed.", DEFAULT_TIMEOUT.toMillis());
        return CdpRemoteObjectValue.from(remoteObject);
    }

    /**
     * Returns the evaluate async.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public CompletableFuture<Object> evaluateAsync(String expression) {
        return evaluateRaw(expression, true).thenApply(CdpRemoteObjectValue::from);
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return evaluate handle value
     */
    public CdpJSHandle evaluateHandle(String expression) {
        return new CdpJSHandle(Awaitable.await(
                evaluateRaw(expression, false),
                "Realm script evaluation with handle failed.",
                DEFAULT_TIMEOUT.toMillis()), client());
    }

    /**
     * Returns the evaluate raw.
     *
     * @param expression    JavaScript expression
     * @param returnByValue whether the result should be returned by value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> evaluateRaw(String expression, boolean returnByValue) {
        ensureActive();
        return client().send(
                "Runtime.evaluate",
                Map.of(
                        "expression",
                        expression == null ? Normal.EMPTY : expression,
                        "returnByValue",
                        returnByValue,
                        "awaitPromise",
                        true))
                .thenApply(result -> result.get("result"));
    }

    /**
     * Waits for function.
     *
     * @param expression JavaScript expression
     * @return wait for function value
     */
    public CdpJSHandle waitForFunction(String expression) {
        return waitForFunction(expression, new WaitForFunctionOptions());
    }

    /**
     * Waits for function.
     *
     * @param expression JavaScript expression
     * @param options    operation options
     * @return wait for function value
     */
    public CdpJSHandle waitForFunction(String expression, WaitForFunctionOptions options) {
        WaitForFunctionOptions actualOptions = options == null ? new WaitForFunctionOptions() : options;
        if (actualOptions.polling().isNegative() || actualOptions.polling().isZero()) {
            throw new InternalException("Realm waitForFunction polling interval must be greater than 0.");
        }
        Duration timeout = actualOptions.timeout();
        long deadline = timeout.isZero() || timeout.isNegative() ? Long.MAX_VALUE
                : System.nanoTime() + timeout.toNanos();
        while (true) {
            CdpPayload remoteObject = Awaitable.await(
                    evaluateRaw(expression, true),
                    "Realm wait for function failed.",
                    DEFAULT_TIMEOUT.toMillis());
            if (Builder.isTruthy(CdpRemoteObjectValue.from(remoteObject))) {
                return evaluateHandle(expression);
            }
            if (System.nanoTime() >= deadline) {
                throw new TimeoutException("Realm waitForFunction timed out.");
            }
            sleep(actualOptions.polling());
        }
    }

    /**
     * Returns the adopt handle.
     *
     * @param handle handle value
     * @param <T>    handle type
     * @return adopt handle value
     */
    public <T extends CdpJSHandle> T adoptHandle(T handle) {
        ensureActive();
        return Assert.notNull(handle, "handle");
    }

    /**
     * Returns the transfer handle.
     *
     * @param handle handle value
     * @param <T>    handle type
     * @return transfer handle value
     */
    public <T extends CdpJSHandle> T transferHandle(T handle) {
        ensureActive();
        return Assert.notNull(handle, "handle");
    }

    /**
     * Returns the adopt backend node.
     *
     * @param backendNodeId backend node ID value
     * @return adopt backend node value
     */
    public CdpJSHandle adoptBackendNode(Integer backendNodeId) {
        ensureActive();
        if (backendNodeId == null) {
            return new CdpJSHandle(CdpPayload.NULL, client());
        }
        CdpPayload result = Awaitable.await(
                client().send("DOM.resolveNode", Map.of("backendNodeId", backendNodeId)),
                "Realm backend node adoption failed.",
                DEFAULT_TIMEOUT.toMillis());
        return new CdpJSHandle(result.get("object"), client());
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            taskManager.terminateAll(new InternalException("Realm has been disposed."));
        }
    }

    /**
     * Handles ensure active.
     */
    private void ensureActive() {
        if (disposed()) {
            throw new InternalException("Realm has been disposed.");
        }
        if (client() == null) {
            throw new InternalException("Realm is missing a CDP session.");
        }
    }

    /**
     * Handles sleep.
     *
     * @param polling polling value
     */
    private void sleep(Duration polling) {
        if (!ThreadKit.sleep(polling.toMillis())) {
            throw new InternalException("Realm wait was interrupted.");
        }
    }

    /**
     * Defines options for wait for function operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class WaitForFunctionOptions {

        /**
         * Creates wait-for-function options.
         */
        public WaitForFunctionOptions() {
            // No initialization required.
        }

        /**
         * Current polling.
         */
        private Duration polling = DEFAULT_POLLING;
        /**
         * Current timeout.
         */
        private Duration timeout = DEFAULT_TIMEOUT;
        /**
         * Current root.
         */
        private CdpElementHandle root;

        /**
         * Returns the polling.
         *
         * @return polling value
         */
        public Duration polling() {
            return polling;
        }

        /**
         * Updates polling.
         *
         * @param polling polling value
         */
        public void setPolling(Duration polling) {
            this.polling = polling == null ? DEFAULT_POLLING : polling;
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

        /**
         * Returns the root.
         *
         * @return optional value
         */
        public Optional<CdpElementHandle> root() {
            return Optional.ofNullable(root);
        }

        /**
         * Updates root.
         *
         * @param root root value
         */
        public void setRoot(CdpElementHandle root) {
            this.root = root;
        }
    }

}
