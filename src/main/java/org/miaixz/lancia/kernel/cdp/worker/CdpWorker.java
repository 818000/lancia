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
package org.miaixz.lancia.kernel.cdp.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Worker;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.runtime.CdpExecutionContext;
import org.miaixz.lancia.kernel.cdp.runtime.CdpIsolatedWorld;
import org.miaixz.lancia.kernel.cdp.runtime.CdpIsolatedWorlds;
import org.miaixz.lancia.kernel.cdp.runtime.CdpJSHandle;
import org.miaixz.lancia.kernel.cdp.runtime.CdpRealm;
import org.miaixz.lancia.kernel.cdp.runtime.CdpRuntimeValues;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.nimble.browser.TargetType;
import org.miaixz.lancia.shared.TimeoutSettings;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.page.ConsoleMessage;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * CDP worker implementation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpWorker extends Worker {

    /**
     * Shared constant for console.
     */
    public static final String CONSOLE = "console";

    /**
     * Shared constant for exception.
     */
    public static final String EXCEPTION = "exception";

    /**
     * Shared constant for error.
     */
    public static final String ERROR = "error";
    /**
     * Default timeout millis.
     */
    private static final long DEFAULT_TIMEOUT_MILLIS = 30_000L;
    /**
     * Current URL.
     */
    private final String url;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current target type.
     */
    private final TargetType targetType;
    /**
     * Current world.
     */
    private final CdpIsolatedWorld world = new CdpIsolatedWorld(this, CdpIsolatedWorlds.MAIN_WORLD);
    /**
     * Registered timeout settings values.
     */
    private final TimeoutSettings timeoutSettings = new TimeoutSettings();
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current internal emitter.
     */
    private final EventEmitter<String> internalEmitter = new EventEmitter<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Current close callback.
     */
    private Runnable closeCallback = () -> {
    };
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean();

    /**
     * Creates a CDP worker.
     *
     * @param url target URL
     */
    public CdpWorker(String url) {
        this(url, null);
    }

    /**
     * Creates a CDP worker.
     *
     * @param url     target URL
     * @param session protocol session
     */
    public CdpWorker(String url, CDPSession session) {
        this(url, session, Normal.EMPTY, TargetType.OTHER);
    }

    /**
     * Creates a CDP worker.
     *
     * @param url        target URL
     * @param session    protocol session
     * @param id         identifier
     * @param targetType target type
     */
    public CdpWorker(String url, CDPSession session, String id, String targetType) {
        this(url, session, id, targetType(targetType));
    }

    /**
     * Creates a CDP worker.
     *
     * @param url        target URL
     * @param session    protocol session
     * @param id         identifier
     * @param targetType target type
     */
    public CdpWorker(String url, CDPSession session, String id, TargetType targetType) {
        this.url = url == null ? Normal.EMPTY : url;
        this.id = id == null ? Normal.EMPTY : id;
        this.targetType = targetType == null ? TargetType.OTHER : targetType;
        setSession(session);
        Logger.debug(
                false,
                "Worker",
                "CDP worker initialized: workerId={}, type={}, url={}",
                this.id,
                this.targetType,
                this.url.replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Updates session.
     *
     * @param session protocol session
     */
    public final void setSession(CDPSession session) {
        clearBindings();
        this.session = session;
        Logger.debug(
                true,
                "Worker",
                "CDP worker session update requested: workerId={}, hasSession={}",
                id,
                session != null);
        if (session != null && !disposed()) {
            binding = binding.combine(session.once("Runtime.executionContextCreated", this::onExecutionContextCreated));
            binding = binding.combine(world.on(CdpIsolatedWorld.CONSOLE_API_CALLED, this::onConsoleApiCalled));
            binding = binding.combine(session.on("Runtime.exceptionThrown", this::onExceptionThrown));
            binding = binding.combine(session.once(CDPSession.Events.DISCONNECTED, payload -> dispose()));
            session.send("Runtime.enable").exceptionally(error -> CdpPayload.NULL);
            Logger.debug(false, "Worker", "CDP worker runtime enabled: workerId={}", id);
        }
    }

    /**
     * Updates close callback.
     *
     * @param closeCallback close callback value
     */
    public void setCloseCallback(Runnable closeCallback) {
        this.closeCallback = closeCallback == null ? () -> {
        } : closeCallback;
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        emitter.on(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        Logger.debug(true, "Worker", "CDP worker listener added: workerId={}, event={}", id, event);
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
        emitter.once(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        Logger.debug(true, "Worker", "CDP worker one-time listener added: workerId={}, event={}", id, event);
        return new EventBinding(() -> emitter.off(event, listener));
    }

    /**
     * Removes an event listener.
     *
     * @param event    event name
     * @param listener event listener
     */
    public Binding off(String event, Consumer<Object> listener) {
        emitter.off(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        Logger.debug(true, "Worker", "CDP worker listener removed: workerId={}, event={}", id, event);
        return new EventBinding();
    }

    /**
     * Removes an event listener.
     *
     * @param event event name
     * @return off value
     */
    public Binding off(String event) {
        emitter.off(Assert.notBlank(event, "event"));
        Logger.debug(true, "Worker", "CDP worker event listeners removed: workerId={}, event={}", id, event);
        return new EventBinding();
    }

    /**
     * Emits an event to registered listeners.
     *
     * @param event   event name
     * @param payload protocol payload
     * @return {@code true} when at least one listener received the event
     */
    public boolean emit(String event, Object payload) {
        Logger.debug(false, "Worker", "CDP worker event emitted: workerId={}, event={}", id, event);
        return emitter.emit(Assert.notBlank(event, "event"), payload);
    }

    /**
     * Returns the listener count.
     *
     * @param event event type
     * @return listener count value
     */
    public int listenerCount(String event) {
        return emitter.listenerCount(Assert.notBlank(event, "event"));
    }

    /**
     * Removes all listeners.
     *
     * @param event event name
     * @return remove all listeners value
     */
    public Binding removeAllListeners(String event) {
        emitter.removeAllListeners(Assert.notBlank(event, "event"));
        Logger.debug(true, "Worker", "CDP worker event listeners cleared: workerId={}, event={}", id, event);
        return new EventBinding();
    }

    /**
     * Removes all listeners.
     *
     * @return remove all listeners value
     */
    public Binding removeAllListeners() {
        emitter.removeAllListeners();
        Logger.debug(true, "Worker", "CDP worker all event listeners cleared: workerId={}", id);
        return new EventBinding();
    }

    /**
     * Returns the internal emitter.
     *
     * @return internal emitter value
     */
    public EventEmitter<String> internalEmitter() {
        return internalEmitter;
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return evaluate value
     */
    public Object evaluate(String expression) {
        Logger.debug(
                true,
                "Worker",
                "CDP worker evaluate requested: workerId={}, expressionChars={}",
                id,
                expression == null ? Normal._0 : expression.length());
        return world.evaluate(expression);
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return evaluate handle value
     */
    public CdpJSHandle evaluateHandle(String expression) {
        Logger.debug(
                true,
                "Worker",
                "CDP worker evaluateHandle requested: workerId={}, expressionChars={}",
                id,
                expression == null ? Normal._0 : expression.length());
        return world.evaluateHandle(expression);
    }

    /**
     * Returns the evaluate remote object.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public CompletableFuture<CdpPayload> evaluateRemoteObject(String expression) {
        Logger.debug(
                true,
                "Worker",
                "CDP worker remote evaluation requested: workerId={}, expressionChars={}",
                id,
                expression == null ? Normal._0 : expression.length());
        return world.evaluateRaw(expression, true);
    }

    /**
     * Returns the main realm.
     *
     * @return main realm value
     */
    @Override
    public CdpRealm mainRealm() {
        return world;
    }

    /**
     * Returns the timeout settings.
     *
     * @return values
     */
    public TimeoutSettings timeoutSettings() {
        return timeoutSettings;
    }

    /**
     * Returns whether olated world is enabled.
     *
     * @return isolated world value
     */
    public CdpIsolatedWorld isolatedWorld() {
        return world;
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return url;
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
    }

    /**
     * Returns the target type.
     *
     * @return target type value
     */
    public TargetType targetType() {
        return targetType;
    }

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    public Optional<CDPSession> session() {
        return Optional.ofNullable(session);
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    @Override
    public CDPSession client() {
        return session;
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
     * Releases resources held by this object.
     */
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            Logger.debug(true, "Worker", "CDP worker dispose requested: workerId={}", id);
            clearBindings();
            world.dispose();
            Logger.debug(false, "Worker", "CDP worker disposed: workerId={}", id);
        }
    }

    /**
     * Closes this object and releases its resources.
     */
    public void close() {
        Logger.debug(true, "Worker", "CDP worker close requested: workerId={}", id);
        Awaitable.await(
                closeAsync().thenApply(result -> CdpPayload.NULL),
                "Failed to close Worker.",
                DEFAULT_TIMEOUT_MILLIS);
        Logger.debug(false, "Worker", "CDP worker close completed: workerId={}", id);
    }

    /**
     * Returns the close async.
     *
     * @return completion future
     */
    public CompletableFuture<Void> closeAsync() {
        if (disposed()) {
            Logger.debug(false, "Worker", "CDP worker close skipped because disposed: workerId={}", id);
            return CompletableFuture.completedFuture(null);
        }
        CDPSession actualSession = assertSession();
        Logger.debug(true, "Worker", "CDP worker close command requested: workerId={}, type={}", id, targetType);
        return closeCommand(actualSession).thenRun(this::finishClose);
    }

    /**
     * Handles clear bindings.
     */
    private void clearBindings() {
        binding.unbind();
        binding = new EventBinding();
    }

    /**
     * Handles on execution context created.
     *
     * @param params protocol parameters
     */
    private void onExecutionContextCreated(CdpPayload params) {
        CDPSession actualSession = session;
        if (actualSession == null || disposed()) {
            return;
        }
        CdpPayload context = params.get("context");
        int contextId = context.get("id").isNull() ? Normal._0 : context.get("id").asInt();
        String name = PayloadReader.text(context.get("name"));
        world.setContext(new CdpExecutionContext(actualSession, contextId, name));
        Logger.debug(false, "Worker", "CDP worker execution context created: workerId={}, contextId={}", id, contextId);
    }

    /**
     * Handles on console api called.
     *
     * @param payload protocol payload
     */
    private void onConsoleApiCalled(Object payload) {
        CdpPayload params = payload instanceof CdpPayload event ? event : CdpPayload.NULL;
        List<CdpJSHandle> values = params.get("args").elements().stream().map(world::createCdpHandle).toList();
        boolean noInternalListeners = internalEmitter.listenerCount(CONSOLE) == Normal._0;
        boolean noWorkerListeners = emitter.listenerCount(CONSOLE) == Normal._0;
        if (noInternalListeners && noWorkerListeners) {
            values.forEach(CdpJSHandle::dispose);
            return;
        }
        CdpPayload stackTrace = params.get("stackTrace");
        ConsoleMessage message = new ConsoleMessage(PayloadReader.text(params.get("type")), consoleText(values), values,
                ConsoleMessage.locationsFromStackTrace(stackTrace), null, stackTrace, id);
        internalEmitter.emit(CONSOLE, message);
        if (!noWorkerListeners) {
            emitter.emit(CONSOLE, message);
        }
        Logger.debug(false, "Worker", "CDP worker console event emitted: workerId={}, argCount={}", id, values.size());
    }

    /**
     * Handles on exception thrown.
     *
     * @param params protocol parameters
     */
    private void onExceptionThrown(CdpPayload params) {
        emitter.emit(EXCEPTION, params);
        emitter.emit(ERROR, params);
        Logger.warn(false, "Worker", "CDP worker exception emitted: workerId={}", id);
    }

    /**
     * Asserts the session condition.
     *
     * @return assert session value
     */
    private CDPSession assertSession() {
        if (session == null || session.detached()) {
            Logger.warn(false, "Worker", "CDP worker session unavailable: workerId={}", id);
            throw new InternalException("Worker CDP session is unavailable.");
        }
        return session;
    }

    /**
     * Returns the close command.
     *
     * @param actualSession actual session value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> closeCommand(CDPSession actualSession) {
        return switch (targetType) {
            case SERVICE_WORKER -> actualSession.connection().send("Target.closeTarget", Map.of("targetId", id))
                    .thenCompose(
                            ignored -> actualSession.connection()
                                    .send("Target.detachFromTarget", Map.of("sessionId", actualSession.id())));
            case SHARED_WORKER -> actualSession.connection().send("Target.closeTarget", Map.of("targetId", id));
            default -> actualSession.send(
                    "Runtime.evaluate",
                    Map.of("expression", "self.close()", "awaitPromise", false, "returnByValue", true));
        };
    }

    /**
     * Handles finish close.
     */
    private void finishClose() {
        dispose();
        closeCallback.run();
        Logger.debug(false, "Worker", "CDP worker close finished: workerId={}", id);
    }

    /**
     * Returns the console text.
     *
     * @param values values value
     * @return console text value
     */
    private String consoleText(List<CdpJSHandle> values) {
        List<String> tokens = new ArrayList<>();
        for (CdpJSHandle value : values) {
            tokens.add(consoleToken(value));
        }
        return String.join(Symbol.SPACE, tokens);
    }

    /**
     * Returns the console token.
     *
     * @param handle handle value
     * @return console token value
     */
    private String consoleToken(CdpJSHandle handle) {
        Object value = CdpRuntimeValues.valueFromJSHandle(handle);
        return value == null ? Normal.EMPTY : String.valueOf(value);
    }

    /**
     * Returns the target type.
     *
     * @param type type to use
     * @return target type value
     */
    private static TargetType targetType(String type) {
        return switch (type == null ? Normal.EMPTY : type) {
            case "page" -> TargetType.PAGE;
            case "background_page" -> TargetType.BACKGROUND_PAGE;
            case "service_worker" -> TargetType.SERVICE_WORKER;
            case "shared_worker" -> TargetType.SHARED_WORKER;
            case "browser" -> TargetType.BROWSER;
            case "webview" -> TargetType.WEBVIEW;
            case "tab" -> TargetType.TAB;
            default -> TargetType.OTHER;
        };
    }

}
