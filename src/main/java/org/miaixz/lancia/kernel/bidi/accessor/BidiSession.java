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
package org.miaixz.lancia.kernel.bidi.accessor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.bidi.session.BidiProtocolSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * WebDriver BiDi remote session.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiSession implements AutoCloseable {

    /**
     * Shared constant for ended.
     */
    public static final String ENDED = "ended";
    /**
     * Current connection.
     */
    private final BidiConnection connection;
    /**
     * Current info.
     */
    private final CdpPayload info;
    /**
     * Current raw session.
     */
    private final BidiProtocolSession rawSession;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Mapped bridges values.
     */
    private final Map<String, Binding> bridges = new ConcurrentHashMap<>();
    /**
     * Registered seen fragments values.
     */
    private final Set<CdpPayload> seenFragments = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Current browser.
     */
    private BidiBrowser browser;
    /**
     * Current reason.
     */
    private volatile String reason;

    /**
     * Returns the from.
     *
     * @param connection   protocol connection
     * @param capabilities capabilities value
     * @return completion future
     */
    public static CompletableFuture<BidiSession> from(BidiConnection connection, Map<String, Object> capabilities) {
        BidiConnection actualConnection = Assert.notNull(connection, "connection");
        return actualConnection
                .send("session.new", Map.of("capabilities", capabilities == null ? Map.of() : capabilities))
                .thenCompose(result -> {
                    BidiSession session = new BidiSession(actualConnection, result);
                    return session.initialize().thenApply(value -> session);
                });
    }

    /**
     * Returns the wrap.
     *
     * @param rawSession raw session value
     * @return wrap value
     */
    public static BidiSession wrap(BidiProtocolSession rawSession) {
        BidiProtocolSession actualSession = Assert.notNull(rawSession, "rawSession");
        return wrap(BidiConnection.from(actualSession.connection()), actualSession);
    }

    /**
     * Returns the wrap.
     *
     * @param connection protocol connection
     * @param rawSession raw session value
     * @return wrap value
     */
    public static BidiSession wrap(BidiConnection connection, BidiProtocolSession rawSession) {
        return new BidiSession(connection, rawSession);
    }

    /**
     * Creates a BiDi session.
     *
     * @param connection protocol connection
     * @param info       info
     */
    private BidiSession(BidiConnection connection, CdpPayload info) {
        this.connection = Assert.notNull(connection, "connection");
        this.info = info == null ? CdpPayload.NULL : info;
        this.rawSession = new BidiProtocolSession(connection.unwrap(), id(), capabilities());
    }

    /**
     * Creates a BiDi session.
     *
     * @param connection protocol connection
     * @param rawSession raw session
     */
    private BidiSession(BidiConnection connection, BidiProtocolSession rawSession) {
        this.connection = Assert.notNull(connection, "connection");
        this.rawSession = Assert.notNull(rawSession, "rawSession");
        Object capabilities = rawSession.capabilities().isNull() ? Map.of() : rawSession.capabilities().raw();
        Map<String, Object> sessionInfo = new LinkedHashMap<>();
        sessionInfo.put("sessionId", rawSession.id());
        sessionInfo.put("capabilities", capabilities == null ? Map.of() : capabilities);
        this.info = CdpPayload.of(sessionInfo);
        this.browser = new BidiBrowser(rawSession);
    }

    /**
     * Returns the initialize.
     *
     * @return completion future
     */
    private CompletableFuture<Void> initialize() {
        return BidiBrowser.from(rawSession).thenAccept(value -> {
            browser = value;
            browser.once(BidiBrowser.CLOSED, reason -> dispose(String.valueOf(reason)));
        });
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        if (ended()) {
            return Awaitable.failed(reason());
        }
        return connection.send(Assert.notBlank(method, "method"), params == null ? Map.of() : params);
    }

    /**
     * Returns the subscribe.
     *
     * @param events events value
     * @return completion future
     */
    public CompletableFuture<Void> subscribe(List<String> events) {
        return subscribe(events, null);
    }

    /**
     * Returns the subscribe.
     *
     * @param events   events value
     * @param contexts contexts value
     * @return completion future
     */
    public CompletableFuture<Void> subscribe(List<String> events, List<String> contexts) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("events", events == null ? List.of() : events);
        if (contexts != null && !contexts.isEmpty()) {
            params.put("contexts", contexts);
        }
        return send("session.subscribe", params).thenApply(result -> null);
    }

    /**
     * Adds intercepts.
     *
     * @param events   events
     * @param contexts contexts
     * @return add intercepts value
     */
    public CompletableFuture<Void> addIntercepts(List<String> events, List<String> contexts) {
        return subscribe(events, contexts);
    }

    /**
     * Returns the end.
     *
     * @return completion future
     */
    public CompletableFuture<Void> end() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        send("session.end", Map.of()).whenComplete((value, throwable) -> {
            dispose("Session already ended.");
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }
            result.complete(null);
        });
        return result;
    }

    /**
     * Registers an event listener.
     *
     * @param method   protocol method
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String method, Consumer<Object> listener) {
        String actualMethod = Assert.notBlank(method, "method");
        ensureBridge(actualMethod);
        return EventHooks.onNamed(emitter, actualMethod, listener);
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param method   protocol method
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String method, Consumer<Object> listener) {
        String actualMethod = Assert.notBlank(method, "method");
        ensureBridge(actualMethod);
        return EventHooks.onceNamed(emitter, actualMethod, listener);
    }

    /**
     * Returns the listener count.
     *
     * @param method protocol method
     * @return listener count value
     */
    public int listenerCount(String method) {
        return emitter.listenerCount(Assert.notBlank(method, "method"));
    }

    /**
     * Releases resources held by this object.
     *
     * @param reason reason
     */
    public void dispose(String reason) {
        if (this.reason != null) {
            return;
        }
        this.reason = reason == null ? "Session already destroyed, probably because the connection broke." : reason;
        for (Binding binding : bridges.values()) {
            binding.unbind();
        }
        bridges.clear();
        emitter.emit(ENDED, this.reason);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        end().join();
    }

    /**
     * Returns the capabilities.
     *
     * @return capabilities value
     */
    public CdpPayload capabilities() {
        return info.get("capabilities");
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return PayloadReader.text(info.get("sessionId"));
    }

    /**
     * Returns the browser.
     *
     * @return browser value
     */
    public BidiBrowser browser() {
        return browser;
    }

    /**
     * Returns the protocol connection.
     *
     * @return protocol connection
     */
    public BidiConnection connection() {
        return connection;
    }

    /**
     * Returns the raw session.
     *
     * @return raw session value
     */
    public BidiProtocolSession rawSession() {
        return rawSession;
    }

    /**
     * Returns the ended.
     *
     * @return {@code true} when the condition matches
     */
    public boolean ended() {
        return reason != null;
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return ended();
    }

    /**
     * Returns the reason.
     *
     * @return reason value
     */
    public String reason() {
        return reason == null ? Normal.EMPTY : reason;
    }

    /**
     * Handles ensure bridge.
     *
     * @param method protocol method
     */
    private void ensureBridge(String method) {
        if (ENDED.equals(method)) {
            return;
        }
        bridges.computeIfAbsent(method, key -> connection.on(key, payload -> {
            if ("browsingContext.fragmentNavigated".equals(key)) {
                onFragmentNavigated(payload);
                return;
            }
            emitter.emit(key, payload);
        }));
    }

    /**
     * Handles on fragment navigated.
     *
     * @param payload protocol payload
     */
    private void onFragmentNavigated(CdpPayload payload) {
        if (seenFragments.contains(payload)) {
            return;
        }
        seenFragments.add(payload);
        emitter.emit("browsingContext.navigationStarted", payload);
        emitter.emit("browsingContext.fragmentNavigated", payload);
    }

}
