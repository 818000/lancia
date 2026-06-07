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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Harness;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.cdp.page.CdpFrame;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.kernel.cdp.worker.CdpWorker;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.page.PageExtension;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents isolated world.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpIsolatedWorld extends CdpRealm {

    /**
     * Shared constant for context.
     */
    public static final String CONTEXT = "context";

    /**
     * Shared constant for disposed.
     */
    public static final String DISPOSED = "disposed";

    /**
     * Shared constant for console api called.
     */
    public static final String CONSOLE_API_CALLED = "consoleapicalled";

    /**
     * Shared constant for binding called.
     */
    public static final String BINDING_CALLED = "bindingcalled";
    /**
     * Default context timeout.
     */
    private static final Duration DEFAULT_CONTEXT_TIMEOUT = Duration.ofSeconds(30);
    /**
     * Current environment.
     */
    private final Harness environment;
    /**
     * Current world ID.
     */
    private Object worldId;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Registered context waiters values.
     */
    private final List<CompletableFuture<CdpExecutionContext>> contextWaiters = new ArrayList<>();
    /**
     * Current context.
     */
    private CdpExecutionContext context;

    /**
     * Creates an CdpIsolatedWorld instance.
     *
     * @param environment environment
     * @param worldId     world id
     */
    public CdpIsolatedWorld(Harness environment, Object worldId) {
        super(environment);
        this.environment = Assert.notNull(environment, "environment");
        this.worldId = worldId == null ? CdpIsolatedWorlds.MAIN_WORLD : worldId;
    }

    /**
     * Returns the environment.
     *
     * @return environment value
     */
    @Override
    public Harness environment() {
        return environment;
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    @Override
    public CDPSession client() {
        return (CDPSession) environment.client();
    }

    /**
     * Returns the emitter.
     *
     * @return emitter value
     */
    public EventEmitter<String> emitter() {
        return emitter;
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        emitter.on(event, listener);
        return new EventBinding(() -> emitter.off(event, listener));
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String event, Consumer<Object> listener) {
        emitter.once(event, listener);
        return new EventBinding(() -> emitter.off(event, listener));
    }

    /**
     * Updates context.
     *
     * @param context browser context
     */
    public synchronized void setContext(CdpExecutionContext context) {
        if (this.context != null) {
            this.context.dispose();
        }
        CdpExecutionContext actualContext = Assert.notNull(context, "context");
        actualContext.once(CdpExecutionContext.DISPOSED, payload -> onContextDisposed());
        actualContext.on(CdpExecutionContext.CONSOLE_API_CALLED, payload -> emitter.emit(CONSOLE_API_CALLED, payload));
        actualContext.on(CdpExecutionContext.BINDING_CALLED, payload -> emitter.emit(BINDING_CALLED, payload));
        this.context = actualContext;
        emitter.emit(CONTEXT, actualContext);
        for (CompletableFuture<CdpExecutionContext> waiter : List.copyOf(contextWaiters)) {
            waiter.complete(actualContext);
        }
        contextWaiters.clear();
    }

    /**
     * Returns whether context is available.
     *
     * @return {@code true} when the condition matches
     */
    public synchronized boolean hasContext() {
        return context != null && !context.disposed();
    }

    /**
     * Returns the context.
     *
     * @return optional value
     */
    public synchronized Optional<CdpExecutionContext> context() {
        return hasContext() ? Optional.of(context) : Optional.empty();
    }

    /**
     * Waits for execution context.
     *
     * @return wait for execution context value
     */
    public CompletableFuture<CdpExecutionContext> waitForExecutionContext() {
        return waitForExecutionContext(DEFAULT_CONTEXT_TIMEOUT);
    }

    /**
     * Waits for execution context.
     *
     * @param timeout timeout value
     * @return wait for execution context value
     */
    public synchronized CompletableFuture<CdpExecutionContext> waitForExecutionContext(Duration timeout) {
        if (disposed()) {
            CompletableFuture<CdpExecutionContext> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new InternalException("Execution context was destroyed"));
            return rejected;
        }
        if (hasContext()) {
            return CompletableFuture.completedFuture(context);
        }
        CompletableFuture<CdpExecutionContext> future = new CompletableFuture<>();
        contextWaiters.add(future);
        Duration actualTimeout = timeout == null ? DEFAULT_CONTEXT_TIMEOUT : timeout;
        if (!actualTimeout.isZero() && !actualTimeout.isNegative()) {
            future.orTimeout(actualTimeout.toMillis(), TimeUnit.MILLISECONDS).exceptionally(error -> {
                synchronized (this) {
                    contextWaiters.remove(future);
                }
                return null;
            });
        }
        return future;
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return evaluate value
     */
    @Override
    public Object evaluate(String expression) {
        return executionContext().evaluate(expression);
    }

    /**
     * Returns the evaluate async.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    @Override
    public CompletableFuture<Object> evaluateAsync(String expression) {
        return waitForExecutionContext().thenApply(context -> context.evaluate(expression));
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return evaluate handle value
     */
    @Override
    public CdpJSHandle evaluateHandle(String expression) {
        return executionContext().evaluateHandle(expression);
    }

    /**
     * Returns the evaluate raw.
     *
     * @param expression    JavaScript expression
     * @param returnByValue whether the result should be returned by value
     * @return completion future
     */
    @Override
    public CompletableFuture<CdpPayload> evaluateRaw(String expression, boolean returnByValue) {
        return waitForExecutionContext().thenApply(context -> context.evaluateRemoteObject(expression, returnByValue));
    }

    /**
     * Returns the adopt backend node.
     *
     * @param backendNodeId backend node ID value
     * @return adopt backend node value
     */
    @Override
    public CdpJSHandle adoptBackendNode(Integer backendNodeId) {
        CdpExecutionContext actualContext = executionContext();
        CdpPayload result = Awaitable.await(
                client().send(
                        "DOM.resolveNode",
                        Map.of("backendNodeId", backendNodeId, "executionContextId", actualContext.id())),
                "CdpIsolatedWorld backend node adoption failed.",
                DEFAULT_CONTEXT_TIMEOUT.toMillis());
        return createCdpHandle(result.get("object"));
    }

    /**
     * Returns the adopt handle.
     *
     * @param handle handle value
     * @return adopt handle value
     */
    @Override
    public <T extends CdpJSHandle> T adoptHandle(T handle) {
        Assert.notNull(handle, "handle");
        if (handle.session().filter(session -> session == client()).isPresent()) {
            return handle;
        }
        if (StringKit.isBlank(handle.id())) {
            return handle;
        }
        CdpPayload nodeInfo = Awaitable.await(
                client().send("DOM.describeNode", Map.of("objectId", handle.id())),
                "CdpIsolatedWorld node description failed.",
                DEFAULT_CONTEXT_TIMEOUT.toMillis());
        return (T) adoptBackendNode(nodeInfo.get("node").get("backendNodeId").asInt());
    }

    /**
     * Returns the transfer handle.
     *
     * @param handle handle value
     * @return transfer handle value
     */
    @Override
    public <T extends CdpJSHandle> T transferHandle(T handle) {
        Assert.notNull(handle, "handle");
        if (handle.session().filter(session -> session == client()).isPresent() || StringKit.isBlank(handle.id())) {
            return handle;
        }
        T adopted = adoptHandle(handle);
        handle.dispose();
        return (T) adopted;
    }

    /**
     * Creates CDP handle.
     *
     * @param remoteObject remote object payload
     * @return created CDP handle
     */
    public CdpJSHandle createCdpHandle(CdpPayload remoteObject) {
        String subtype = PayloadReader.text(remoteObject.get("subtype"));
        if ("node".equals(subtype) || "element".equals(subtype)) {
            return new CdpElementHandle(remoteObject, client());
        }
        return new CdpJSHandle(this, remoteObject);
    }

    /**
     * Returns the world ID.
     *
     * @return world ID value
     */
    public Object worldId() {
        return worldId;
    }

    /**
     * Updates world ID.
     *
     * @param worldId world id
     */
    public void setWorldId(Object worldId) {
        this.worldId = worldId == null ? CdpIsolatedWorlds.MAIN_WORLD : worldId;
    }

    /**
     * Returns the extension.
     *
     * @return optional value
     */
    public Optional<PageExtension> extension() {
        if (environment instanceof CdpWorker) {
            throw new InternalException("Unable to get extension from Realm");
        }
        if (!(environment instanceof CdpFrame frame) || !(worldId instanceof String extensionId)
                || StringKit.isBlank(extensionId) || CdpIsolatedWorlds.isMainWorld(extensionId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(frame.page()).flattedMap(page -> Optional.ofNullable(page.browser()).toOptional())
                .map(browser -> (PageExtension) browser.extensions().get(extensionId));
    }

    /**
     * Releases resources held by this object.
     */
    @Override
    public synchronized void dispose() {
        if (disposed()) {
            return;
        }
        if (context != null) {
            context.dispose();
            context = null;
        }
        for (CompletableFuture<CdpExecutionContext> waiter : List.copyOf(contextWaiters)) {
            waiter.completeExceptionally(new InternalException("Execution context was destroyed"));
        }
        contextWaiters.clear();
        emitter.emit(DISPOSED, CdpPayload.NULL);
        emitter.removeAllListeners(CONTEXT);
        emitter.removeAllListeners(DISPOSED);
        emitter.removeAllListeners(CONSOLE_API_CALLED);
        emitter.removeAllListeners(BINDING_CALLED);
        super.dispose();
    }

    /**
     * Handles on context disposed.
     */
    private synchronized void onContextDisposed() {
        context = null;
    }

    /**
     * Returns the execution context.
     *
     * @return execution context value
     */
    private CdpExecutionContext executionContext() {
        if (disposed()) {
            throw new InternalException("Execution context is not available in detached frame or worker \""
                    + environmentUrl() + "\" (are you trying to evaluate?)");
        }
        return context().orElseGet(
                () -> Awaitable.await(
                        waitForExecutionContext(),
                        "Failed to wait for execution context.",
                        DEFAULT_CONTEXT_TIMEOUT.toMillis()));
    }

    /**
     * Returns the environment URL.
     *
     * @return environment URL value
     */
    private String environmentUrl() {
        if (environment instanceof CdpFrame frame) {
            return frame.url();
        }
        if (environment instanceof CdpWorker worker) {
            return worker.url();
        }
        return Normal.EMPTY;
    }

}
