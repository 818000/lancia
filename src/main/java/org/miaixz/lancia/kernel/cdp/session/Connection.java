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
package org.miaixz.lancia.kernel.cdp.session;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.Transport;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.cdp.protocol.CdpCommandWriter;
import org.miaixz.lancia.kernel.cdp.protocol.CdpEnvelope;
import org.miaixz.lancia.kernel.cdp.protocol.CdpEnvelopeReader;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;

/**
 * Coordinates Chrome DevTools Protocol command transport and session routing.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Connection implements Session {

    /**
     * Shared constant for command timer.
     */
    private static final java.util.concurrent.ScheduledExecutorService COMMAND_TIMER = java.util.concurrent.Executors
            .newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "lancia-connection-command-timer");
                thread.setDaemon(true);
                return thread;
            });
    /**
     * Current transport.
     */
    private final Transport transport;
    /**
     * Current URL.
     */
    private final String url;
    /**
     * Current envelope reader.
     */
    private final CdpEnvelopeReader envelopeReader = new CdpEnvelopeReader();
    /**
     * Current command writer.
     */
    private final CdpCommandWriter commandWriter = new CdpCommandWriter();
    /**
     * Current callbacks.
     */
    private final CallbackRegistry callbacks;
    /**
     * Current delay millis.
     */
    private final long delayMillis;
    /**
     * Current default timeout millis.
     */
    private final long defaultTimeoutMillis;
    /**
     * Mapped sessions values.
     */
    private final Map<String, CDPSession> sessions = new ConcurrentHashMap<>();
    /**
     * Registered manually attached values.
     */
    private final Set<String> manuallyAttached = ConcurrentHashMap.newKeySet();
    /**
     * Current events.
     */
    private final EventEmitter<String> events = new EventEmitter<>();
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    /**
     * Whether reject emulate network conditions calls is enabled.
     */
    private volatile boolean rejectEmulateNetworkConditionsCalls;

    /**
     * Creates a connection.
     */
    public Connection() {
        this(new NoopTransport(), Normal.EMPTY);
    }

    /**
     * Creates a connection.
     *
     * @param transport transport
     */
    public Connection(Transport transport) {
        this(transport, Normal.EMPTY);
    }

    /**
     * Creates a connection.
     *
     * @param transport transport
     * @param url       target URL
     */
    public Connection(Transport transport, String url) {
        this(transport, url, 0L, Builder.DEFAULT_TIMEOUT_MILLIS, null);
    }

    /**
     * Creates a connection.
     *
     * @param transport            transport
     * @param url                  target URL
     * @param delayMillis          delay in milliseconds
     * @param defaultTimeoutMillis default timeout millis
     * @param idGenerator          id generator
     */
    public Connection(Transport transport, String url, long delayMillis, long defaultTimeoutMillis,
            IntSupplier idGenerator) {
        this.transport = transport;
        this.url = url == null ? Normal.EMPTY : url;
        this.delayMillis = Math.max(0L, delayMillis);
        this.defaultTimeoutMillis = Math.max(0L, defaultTimeoutMillis);
        this.callbacks = new CallbackRegistry(idGenerator);
        this.transport.setConnection(this);
        this.transport.setMessageHandler(this::onMessage);
        this.transport.setCloseHandler(() -> onClosed(0, Normal.EMPTY));
        Logger.debug(
                false,
                "Protocol",
                "CDP connection initialized: url={}, transport={}",
                this.url.isBlank() ? Normal.EMPTY
                        : this.url.contains("?") ? this.url.substring(Normal._0, this.url.indexOf('?')) : this.url,
                transport.getClass().getSimpleName());
    }

    /**
     * Returns whether configured transport is available.
     *
     * @return {@code true} when the condition matches
     */
    public boolean hasConfiguredTransport() {
        return !(transport instanceof NoopTransport);
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
     * Returns whether this object is closed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isClosed() {
        return disposed.get();
    }

    /**
     * Returns the pending protocol errors.
     *
     * @return values
     */
    public List<String> getPendingProtocolErrors() {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>(callbacks.getPendingProtocolErrors());
        for (CDPSession session : sessions.values()) {
            errors.addAll(session.getPendingProtocolErrors());
        }
        return List.copyOf(errors);
    }

    /**
     * Creates this value from session.
     *
     * @param session protocol session
     * @return from session value
     */
    public static Connection fromSession(CDPSession session) {
        return Assert.notNull(session, "session").connection();
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
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        return rawSend(Normal.EMPTY, method, params, callbacks);
    }

    /**
     * Sends a protocol command.
     *
     * @param method  protocol method
     * @param params  protocol parameters
     * @param options operation options
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(
            String method,
            Map<String, Object> params,
            CDPSession.CommandOptions options) {
        CompletableFuture<CdpPayload> future = send(method, params);
        CDPSession.CommandOptions actualOptions = options == null ? new CDPSession.CommandOptions() : options;
        if (actualOptions.getTimeoutMillis() <= 0) {
            return future;
        }
        CompletableFuture<CdpPayload> timed = new CompletableFuture<>();
        java.util.concurrent.ScheduledFuture<?> timeout = COMMAND_TIMER.schedule(
                () -> timed.completeExceptionally(
                        new org.miaixz.bus.core.lang.exception.TimeoutException("CDP command timed out: " + method)),
                actualOptions.getTimeoutMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS);
        future.whenComplete((value, error) -> {
            timeout.cancel(false);
            if (error != null) {
                timed.completeExceptionally(error);
            } else {
                timed.complete(value);
            }
        });
        return timed;
    }

    /**
     * Sends a raw protocol command.
     *
     * @param sessionId session id
     * @param method    protocol method
     * @param params    protocol parameters
     * @return raw send value
     */
    public CompletableFuture<CdpPayload> rawSend(String sessionId, String method, Map<String, Object> params) {
        return rawSend(sessionId, method, params, callbacks);
    }

    /**
     * Returns the raw send.
     *
     * @param sessionId session ID value
     * @param method    protocol method
     * @param params    protocol parameters
     * @param registry  registry value
     * @return completion future
     */
    CompletableFuture<CdpPayload> rawSend(
            String sessionId,
            String method,
            Map<String, Object> params,
            CallbackRegistry registry) {
        if (disposed.get()) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new IllegalStateException("CDP connection has been closed."));
            return rejected;
        }
        if (rejectEmulateNetworkConditionsCalls && "Network.emulateNetworkConditions".equals(method)) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(
                    new InternalException("Cannot reset network conditions: rule-based emulation is enabled."));
            return rejected;
        }
        int id = callbacks.nextId();
        CompletableFuture<CdpPayload> future = registry.create(id, method, defaultTimeoutMillis);
        String text = commandWriter.command(id, method, sessionId, params);
        Logger.trace(
                true,
                "Protocol",
                "CDP command send requested: id={}, method={}, session={}, params={}",
                id,
                method,
                sessionId == null ? Normal.EMPTY : sessionId,
                params == null ? Normal._0 : params.size());
        try {
            delay();
            transport.send(text);
        } catch (RuntimeException ex) {
            registry.rejectRaw(id, ex);
            Logger.error(
                    false,
                    "Protocol",
                    ex,
                    "CDP command send failed: id={}, method={}, session={}",
                    id,
                    method,
                    sessionId == null ? Normal.EMPTY : sessionId);
            throw ex;
        }
        future.whenComplete((payload, error) -> {
            if (error == null) {
                Logger.trace(
                        false,
                        "Protocol",
                        "CDP command completed: id={}, method={}, session={}",
                        id,
                        method,
                        sessionId == null ? Normal.EMPTY : sessionId);
            } else {
                Logger.warn(
                        false,
                        "Protocol",
                        error,
                        "CDP command failed: id={}, method={}, session={}",
                        id,
                        method,
                        sessionId == null ? Normal.EMPTY : sessionId);
            }
        });
        return future;
    }

    /**
     * Handles on message.
     *
     * @param message message text
     */
    public void onMessage(String message) {
        delay();
        CdpEnvelope envelope = envelopeReader.read(message);
        Logger.trace(
                false,
                "Protocol",
                "CDP message received: id={}, method={}, session={}, chars={}",
                envelope.hasId() ? envelope.getId() : Normal._0,
                envelope.getMethod(),
                envelope.getSessionId() == null ? Normal.EMPTY : envelope.getSessionId(),
                message == null ? Normal._0 : message.length());
        if (envelope.hasId()) {
            routeCommandResult(envelope);
            return;
        }
        if ("Target.attachedToTarget".equals(envelope.getMethod())) {
            CDPSession session = createSession(
                    TargetInfo.fromAttachedToTarget(envelope.getParams()),
                    envelope.getSessionId() == null ? null : sessions.get(envelope.getSessionId()));
            emit(CDPSession.Events.SESSION_ATTACHED, session);
            if (session.parentSession().isPresent()) {
                session.parentSession().orElseThrow().sessionAttached(session);
            }
        }
        if ("Target.detachedFromTarget".equals(envelope.getMethod())) {
            CdpPayload sessionPayload = envelope.getParams().get("sessionId");
            if (!sessionPayload.isNull()) {
                removeSession(sessionPayload.asText());
            }
        }
        if (envelope.getSessionId() != null) {
            CDPSession session = sessions.get(envelope.getSessionId());
            if (session != null) {
                session.onMessage(envelope);
            }
            return;
        }
        if (envelope.isEvent()) {
            emit(envelope.getMethod(), envelope.getParams());
        }
    }

    /**
     * Creates session.
     *
     * @param targetInfo target info
     * @return created session
     */
    public CDPSession createSession(TargetInfo targetInfo) {
        return createSession(targetInfo, null);
    }

    /**
     * Creates session.
     *
     * @param targetInfo    target info
     * @param parentSession parent session
     * @return created session
     */
    public CDPSession createSession(TargetInfo targetInfo, CDPSession parentSession) {
        CDPSession session = new CDPSession(this, targetInfo.getSessionId(), targetInfo, parentSession);
        sessions.put(targetInfo.getSessionId(), session);
        Logger.debug(
                false,
                "Protocol",
                "CDP session created: session={}, target={}, type={}, parent={}",
                targetInfo.getSessionId() == null ? Normal.EMPTY : targetInfo.getSessionId(),
                targetInfo.getTargetId(),
                targetInfo.getType(),
                parentSession == null ? Normal.EMPTY : parentSession.id());
        return session;
    }

    /**
     * Creates session async.
     *
     * @param targetInfo target info
     * @return created session async
     */
    public CompletableFuture<CDPSession> createSessionAsync(TargetInfo targetInfo) {
        TargetInfo actualTargetInfo = Assert.notNull(targetInfo, "targetInfo");
        manuallyAttached.add(actualTargetInfo.getTargetId());
        Logger.debug(
                true,
                "Protocol",
                "CDP session attach requested: target={}, type={}",
                actualTargetInfo.getTargetId(),
                actualTargetInfo.getType());
        CompletableFuture<CDPSession> created = send(
                "Target.attachToTarget",
                Map.of("targetId", actualTargetInfo.getTargetId(), "flatten", true)).thenApply(payload -> {
                    String sessionId = payload.get("sessionId").asText();
                    CDPSession existing = sessions.get(sessionId);
                    if (existing != null) {
                        return existing;
                    }
                    return createSession(
                            new TargetInfo(actualTargetInfo.getTargetId(), actualTargetInfo.getType(),
                                    actualTargetInfo.getUrl(), sessionId));
                });
        created.whenComplete((session, error) -> manuallyAttached.remove(actualTargetInfo.getTargetId()));
        created.whenComplete((session, error) -> {
            if (error == null) {
                Logger.debug(
                        false,
                        "Protocol",
                        "CDP session attach completed: target={}, session={}",
                        actualTargetInfo.getTargetId(),
                        session == null ? Normal.EMPTY : session.id());
            } else {
                Logger.warn(
                        false,
                        "Protocol",
                        error,
                        "CDP session attach failed: target={}",
                        actualTargetInfo.getTargetId());
            }
        });
        return created;
    }

    /**
     * Returns the session.
     *
     * @param sessionId session id
     * @return session
     */
    public Optional<CDPSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * Removes and closes a session.
     *
     * @param sessionId session id
     */
    void removeSession(String sessionId) {
        CDPSession detachedSession = sessions.remove(sessionId);
        if (detachedSession != null) {
            detachedSession.parentSession().ifPresent(parent -> parent.sessionDetached(detachedSession));
            detachedSession.onClosed();
            emit(CDPSession.Events.SESSION_DETACHED, detachedSession);
        }
    }

    /**
     * Returns the protocol session.
     *
     * @param sessionId session id
     * @return protocol session
     */
    public CDPSession session(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Returns whether the target is currently auto-attached.
     *
     * @param targetId target id
     * @return {@code true} when the target is auto-attached
     */
    public boolean isAutoAttached(String targetId) {
        return !manuallyAttached.contains(targetId);
    }

    /**
     * Updates reject emulate network conditions calls.
     *
     * @param rejectEmulateNetworkConditionsCalls reject emulate network conditions calls value
     */
    public void setRejectEmulateNetworkConditionsCalls(boolean rejectEmulateNetworkConditionsCalls) {
        this.rejectEmulateNetworkConditionsCalls = rejectEmulateNetworkConditionsCalls;
    }

    /**
     * Returns whether reject emulate network conditions calls is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isRejectEmulateNetworkConditionsCalls() {
        return rejectEmulateNetworkConditionsCalls;
    }

    /**
     * Returns the close browser.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> closeBrowser() {
        return send("Browser.close");
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, java.util.function.Consumer<Object> listener) {
        events.on(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        return new EventBinding(() -> events.off(event, listener));
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String event, java.util.function.Consumer<Object> listener) {
        events.once(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        return new EventBinding(() -> events.off(event, listener));
    }

    /**
     * Removes an event listener.
     *
     * @param event    event name
     * @param listener event listener
     */
    public void off(String event, java.util.function.Consumer<Object> listener) {
        events.off(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
    }

    /**
     * Emits an event to registered listeners.
     *
     * @param event   event name
     * @param payload protocol payload
     * @return {@code true} when at least one listener received the event
     */
    public boolean emit(String event, Object payload) {
        return events.emit(Assert.notBlank(event, "event"), payload);
    }

    /**
     * Returns the listener count.
     *
     * @param event event type
     * @return listener count value
     */
    public int listenerCount(String event) {
        return events.listenerCount(Assert.notBlank(event, "event"));
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            Logger.debug(true, "Protocol", "CDP connection dispose requested: sessions={}", sessions.size());
            callbacks.clear(new IllegalStateException("CDP connection has been closed."));
            for (CDPSession session : sessions.values()) {
                session.onClosed();
            }
            sessions.clear();
            emit(CDPSession.Events.DISCONNECTED, CdpPayload.NULL);
            transport.close();
            Logger.debug(false, "Protocol", "CDP connection disposed.");
        }
    }

    /**
     * Handles on closed.
     *
     * @param statusCode status code value
     * @param reason     reason value
     */
    public void onClosed(int statusCode, String reason) {
        Logger.debug(false, "Protocol", "CDP connection closed: status={}, reason={}", statusCode, reason);
        dispose();
    }

    /**
     * Handles on error.
     *
     * @param throwable throwable value
     */
    public void onError(Throwable throwable) {
        Logger.warn(false, "Protocol", throwable, "CDP connection error.");
        callbacks.clear(throwable);
    }

    /**
     * Handles route command result.
     *
     * @param envelope envelope value
     */
    private void routeCommandResult(CdpEnvelope envelope) {
        if (envelope.getSessionId() != null) {
            CDPSession session = sessions.get(envelope.getSessionId());
            if (session != null) {
                session.onMessage(envelope);
                return;
            }
        }
        if (callbacks.contains(envelope.getId())) {
            if (envelope.getError().isNull()) {
                callbacks.resolve(envelope.getId(), envelope.getResult());
            } else {
                callbacks.reject(envelope.getId(), envelope.getError().get("message").asText());
            }
            return;
        }
        for (CDPSession session : sessions.values()) {
            if (session.tryResolve(envelope)) {
                return;
            }
        }
    }

    /**
     * Applies the configured slow motion delay.
     */
    private void delay() {
        Awaitable.sleep(delayMillis, "CDP delay interrupted.");
    }

    /**
     * Transports noop protocol messages.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class NoopTransport implements Transport {

        /**
         * Sends a protocol command.
         *
         * @param message message text
         */
        @Override
        public void send(String message) {
            throw new IllegalStateException("CDP transport is not configured.");
        }

        /**
         * Updates connection.
         *
         * @param connection protocol connection
         */
        @Override
        public void setConnection(Object connection) {
        }

        /**
         * Closes this object and releases its resources.
         */
        @Override
        public void close() {
        }
    }

}
