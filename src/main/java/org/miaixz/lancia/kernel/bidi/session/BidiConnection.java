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
package org.miaixz.lancia.kernel.bidi.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.Dispatcher;
import org.miaixz.lancia.kernel.bidi.protocol.message.BidiCommandMessage;
import org.miaixz.lancia.kernel.bidi.protocol.message.BidiEventMessage;
import org.miaixz.lancia.kernel.bidi.protocol.message.BidiResponseMessage;
import org.miaixz.lancia.kernel.bidi.transport.BidiTransport;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.protocol.TextWriter;

/**
 * Coordinates WebDriver BiDi command transport and event routing.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiConnection implements AutoCloseable {

    /**
     * Maximum retained protocol errors.
     */
    private static final int MAX_PENDING_PROTOCOL_ERRORS = 100;
    /**
     * Shared callback timeout scheduler.
     */
    private static final ScheduledExecutorService CALLBACK_TIMER = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-bidi-callback-", true));

    /**
     * Current URL.
     */
    private final String url;
    /**
     * Current transport.
     */
    private final BidiTransport transport;
    /**
     * Thread-safe ids state.
     */
    private final AtomicInteger ids = new AtomicInteger();
    /**
     * Mapped callbacks values.
     */
    private final Map<Integer, CompletableFuture<CdpPayload>> callbacks = new ConcurrentHashMap<>();
    /**
     * Mapped callback timeout tasks.
     */
    private final Map<Integer, ScheduledFuture<?>> callbackTimeouts = new ConcurrentHashMap<>();
    /**
     * Current events.
     */
    private final EventEmitter<String> events = new EventEmitter<>();
    /**
     * Registered pending protocol errors values.
     */
    private final List<Throwable> pendingProtocolErrors = new ArrayList<>();
    /**
     * Current writer.
     */
    private final TextWriter writer = new TextWriter();
    /**
     * Default protocol timeout millis.
     */
    private final long defaultTimeoutMillis;
    /**
     * Thread-safe closed state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a bidi connection.
     *
     * @param url       target URL
     * @param transport transport
     */
    public BidiConnection(String url, BidiTransport transport) {
        this(url, transport, Builder.DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Creates a bidi connection.
     *
     * @param url                  target URL
     * @param transport            transport
     * @param defaultTimeoutMillis default timeout millis
     */
    public BidiConnection(String url, BidiTransport transport, long defaultTimeoutMillis) {
        this.url = url == null ? Normal.EMPTY : url;
        this.transport = Assert.notNull(transport, "transport");
        this.defaultTimeoutMillis = defaultTimeoutMillis <= 0 ? Builder.DEFAULT_TIMEOUT_MILLIS : defaultTimeoutMillis;
        Logger.debug(
                false,
                "Protocol",
                "BiDi connection initialized: url={}, transport={}, timeout={}",
                this.url.replaceAll("[?#].*$", "?<redacted>"),
                this.transport.getClass().getSimpleName(),
                this.defaultTimeoutMillis);
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        Assert.notNull(method, "method");
        if (closed.get()) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new IllegalStateException("BiDi connection has been closed."));
            Logger.warn(false, "Protocol", "BiDi command rejected on closed connection: method={}", method);
            return rejected;
        }
        int id = ids.incrementAndGet();
        CompletableFuture<CdpPayload> future = new CompletableFuture<>();
        callbacks.put(id, future);
        callbackTimeouts.put(
                id,
                CALLBACK_TIMER
                        .schedule(() -> timeoutCallback(id, method), defaultTimeoutMillis, TimeUnit.MILLISECONDS));
        Logger.debug(
                true,
                "Protocol",
                "BiDi command send requested: id={}, method={}, pending={}",
                id,
                method,
                callbacks.size());
        try {
            transport.send(writer.writeValue(new BidiCommandMessage(id, method, params).toMap()));
        } catch (RuntimeException ex) {
            callbacks.remove(id);
            cancelCallbackTimeout(id);
            future.completeExceptionally(ex);
        }
        return future.whenComplete((value, throwable) -> {
            callbacks.remove(id);
            cancelCallbackTimeout(id);
            if (throwable == null) {
                Logger.debug(false, "Protocol", "BiDi command completed: id={}, method={}", id, method);
            } else {
                Logger.warn(false, "Protocol", throwable, "BiDi command failed: id={}, method={}", id, method);
            }
        });
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method) {
        return send(method, Map.of());
    }

    /**
     * Creates session.
     *
     * @param capabilities capabilities
     * @return created session
     */
    public CompletableFuture<BidiProtocolSession> createSession(Map<String, Object> capabilities) {
        Logger.debug(
                true,
                "Protocol",
                "BiDi protocol session create requested: capabilities={}",
                capabilities == null ? Normal._0 : capabilities.size());
        return send("session.new", Map.of("capabilities", capabilities == null ? Map.of() : capabilities))
                .thenApply(result -> {
                    String sessionId = PayloadReader.text(result.get("sessionId"));
                    Logger.debug(false, "Protocol", "BiDi protocol session created: session={}", sessionId);
                    return new BidiProtocolSession(this, sessionId, result.get("capabilities"));
                });
    }

    /**
     * Registers an event listener.
     *
     * @param method   protocol method
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String method, Consumer<CdpPayload> listener) {
        return EventHooks.onPayload(events, method, listener, CdpPayload.class);
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param method   protocol method
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String method, Consumer<CdpPayload> listener) {
        return EventHooks.oncePayload(events, method, listener, CdpPayload.class);
    }

    /**
     * Returns the listener count.
     *
     * @param method protocol method
     * @return listener count value
     */
    public int listenerCount(String method) {
        return events.listenerCount(Assert.notNull(method, "method"));
    }

    /**
     * Handles on message.
     *
     * @param message message text
     */
    public void onMessage(String message) {
        Logger.debug(
                false,
                "Protocol",
                "BiDi message received: chars={}",
                message == null ? Normal._0 : message.length());
        CdpPayload payload = CdpPayload.parse(message);
        String type = PayloadReader.text(payload.get("type"));
        if ("success".equals(type) || "error".equals(type)) {
            handleResponse(BidiResponseMessage.from(payload));
            return;
        }
        if ("event".equals(type)) {
            BidiEventMessage event = BidiEventMessage.from(payload);
            if (event.method().startsWith("goog:cdp.")) {
                Logger.debug(false, "Protocol", "BiDi CDP event routed: method={}", event.method());
                routeCdpEvent(event);
                return;
            }
            Logger.debug(
                    false,
                    "Protocol",
                    "BiDi event emitted: method={}, listeners={}",
                    event.method(),
                    listenerCount(event.method()));
            events.emit(event.method(), event.params());
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
     * Returns whether this object is closed.
     *
     * @return whether this object is closed
     */
    public boolean closed() {
        return isClosed();
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
     * Releases resources held by this object.
     */
    public void dispose() {
        close();
    }

    /**
     * Removes the registered binding.
     */
    public void unbind() {
        if (closed.compareAndSet(false, true)) {
            Logger.debug(true, "Protocol", "BiDi connection unbind requested: pending={}", callbacks.size());
            rejectCallbacks(new IllegalStateException("BiDi connection has been unbound."));
            Logger.debug(false, "Protocol", "BiDi connection unbound.");
        }
    }

    /**
     * Handles transport errors.
     *
     * @param throwable error to propagate
     */
    public void onError(Throwable throwable) {
        Logger.warn(false, "Protocol", throwable, "BiDi connection error.");
        recordProtocolError(throwable);
        rejectCallbacks(throwable);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Logger.debug(true, "Protocol", "BiDi connection close requested: pending={}", callbacks.size());
            rejectCallbacks(new IllegalStateException("BiDi connection has been closed."));
            transport.close();
            Logger.debug(false, "Protocol", "BiDi connection closed.");
        }
    }

    /**
     * Returns the pending protocol errors.
     *
     * @return values
     */
    public List<Throwable> getPendingProtocolErrors() {
        synchronized (pendingProtocolErrors) {
            return List.copyOf(pendingProtocolErrors);
        }
    }

    /**
     * Handles route CDP event.
     *
     * @param event event type
     */
    private void routeCdpEvent(BidiEventMessage event) {
        CdpPayload params = event.params();
        String sessionId = PayloadReader.text(params.get("session"));
        String cdpEvent = PayloadReader.text(params.get("event"));
        CdpPayload cdpParams = params.get("params");
        Dispatcher.emit(sessionId, cdpEvent, cdpParams);
    }

    /**
     * Handles handle response.
     *
     * @param response response object
     */
    private void handleResponse(BidiResponseMessage response) {
        CompletableFuture<CdpPayload> callback = callbacks.remove(response.id());
        cancelCallbackTimeout(response.id());
        if (callback == null) {
            return;
        }
        if (response.success()) {
            callback.complete(response.result());
            Logger.debug(false, "Protocol", "BiDi response completed: id={}", response.id());
        } else {
            IllegalStateException error = new IllegalStateException(response.error() + ": " + response.message());
            recordProtocolError(error);
            Logger.warn(
                    false,
                    "Protocol",
                    error,
                    "BiDi response failed: id={}, error={}",
                    response.id(),
                    response.error());
            callback.completeExceptionally(error);
        }
    }

    /**
     * Handles a callback timeout.
     *
     * @param id     callback id
     * @param method protocol method
     */
    private void timeoutCallback(int id, String method) {
        CompletableFuture<CdpPayload> callback = callbacks.remove(id);
        callbackTimeouts.remove(id);
        if (callback == null) {
            return;
        }
        IllegalStateException error = new IllegalStateException(
                "BiDi command timed out after " + defaultTimeoutMillis + " ms: " + method);
        recordProtocolError(error);
        callback.completeExceptionally(error);
    }

    /**
     * Cancels a callback timeout task.
     *
     * @param id callback id
     */
    private void cancelCallbackTimeout(int id) {
        ScheduledFuture<?> timeout = callbackTimeouts.remove(id);
        if (timeout != null) {
            timeout.cancel(false);
        }
    }

    /**
     * Completes all pending callbacks exceptionally and cancels timeout tasks.
     *
     * @param throwable error to propagate
     */
    private void rejectCallbacks(Throwable throwable) {
        Throwable actualThrowable = throwable == null ? new IllegalStateException("BiDi connection failed.")
                : throwable;
        for (ScheduledFuture<?> timeout : callbackTimeouts.values()) {
            timeout.cancel(false);
        }
        callbackTimeouts.clear();
        for (CompletableFuture<CdpPayload> callback : callbacks.values()) {
            callback.completeExceptionally(actualThrowable);
        }
        callbacks.clear();
    }

    /**
     * Records a bounded protocol error diagnostic.
     *
     * @param throwable error to retain
     */
    private void recordProtocolError(Throwable throwable) {
        if (throwable == null) {
            return;
        }
        synchronized (pendingProtocolErrors) {
            while (pendingProtocolErrors.size() >= MAX_PENDING_PROTOCOL_ERRORS) {
                pendingProtocolErrors.remove(0);
            }
            pendingProtocolErrors.add(throwable);
        }
    }

}
