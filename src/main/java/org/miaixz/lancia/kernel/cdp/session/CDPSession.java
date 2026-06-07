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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.cdp.protocol.CdpEnvelope;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;

/**
 * Represents a Chrome DevTools Protocol session attached to a target.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CDPSession implements Session {

    /**
     * Shared constant for command timer.
     */
    private static final ScheduledExecutorService COMMAND_TIMER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "lancia-cdp-session-command-timer");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Current connection.
     */
    private final Connection connection;

    /**
     * Current session ID.
     */
    private final String sessionId;

    /**
     * Current target info.
     */
    private final TargetInfo targetInfo;

    /**
     * Current parent session.
     */
    private final CDPSession parentSession;

    /**
     * Current callbacks.
     */
    private final CallbackRegistry callbacks = new CallbackRegistry();

    /**
     * Thread-safe closed state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Current events.
     */
    private final EventEmitter<String> events = new EventEmitter<>();

    /**
     * Creates a session without a parent session.
     *
     * @param connection connection
     * @param sessionId  session id
     * @param targetInfo target info
     */
    public CDPSession(Connection connection, String sessionId, TargetInfo targetInfo) {
        this(connection, sessionId, targetInfo, null);
    }

    /**
     * Creates a session bound to a connection and target.
     *
     * @param connection    connection
     * @param sessionId     session id
     * @param targetInfo    target info
     * @param parentSession parent session
     */
    public CDPSession(Connection connection, String sessionId, TargetInfo targetInfo, CDPSession parentSession) {
        this.connection = Assert.notNull(connection, "connection");
        this.sessionId = Assert.notBlank(sessionId, "sessionId");
        this.targetInfo = Assert.notNull(targetInfo, "targetInfo");
        this.parentSession = parentSession;
        Logger.debug(
                false,
                "Protocol",
                "CDP session initialized: session={}, target={}, type={}",
                sessionId,
                targetInfo.getTargetId(),
                targetInfo.getType());
    }

    /**
     * Sends a command when a session is available.
     *
     * @param session session
     * @param method  method
     * @param params  params
     * @return response payload
     */
    public static CompletableFuture<CdpPayload> sendIfPresent(
            CDPSession session,
            String method,
            Map<String, Object> params) {
        return session == null ? CompletableFuture.completedFuture(CdpPayload.NULL) : session.send(method, params);
    }

    /**
     * Sends a command without parameters.
     *
     * @param method method
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method) {
        return send(method, Map.of());
    }

    /**
     * Sends a command through this session.
     *
     * @param method method
     * @param params params
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        if (detached()) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new InternalException(closedMessage(method)));
            Logger.warn(
                    false,
                    "Protocol",
                    "CDP session command rejected: session={}, method={}, detached=true",
                    sessionId,
                    method);
            return rejected;
        }
        String actualMethod = Assert.notBlank(method, "method");
        Logger.trace(
                true,
                "Protocol",
                "CDP session command requested: session={}, method={}, params={}",
                sessionId,
                actualMethod,
                params == null ? Normal._0 : params.size());
        CompletableFuture<CdpPayload> sent = connection
                .rawSend(sessionId, actualMethod, params == null ? Map.of() : params, callbacks);
        CompletableFuture<CdpPayload> guarded = new CompletableFuture<>();
        sent.whenComplete((value, error) -> {
            if (error == null) {
                guarded.complete(value);
                Logger.trace(
                        false,
                        "Protocol",
                        "CDP session command completed: session={}, method={}",
                        sessionId,
                        actualMethod);
                return;
            }
            Throwable actualError = error instanceof java.util.concurrent.CompletionException completion
                    ? completion.getCause()
                    : error;
            if (actualError != null
                    && String.valueOf(actualError.getMessage()).contains("Session with given id not found")) {
                onClosed();
                guarded.completeExceptionally(
                        new InternalException("Protocol error (" + actualMethod + "): Session with given id not found.",
                                actualError));
                Logger.warn(
                        false,
                        "Protocol",
                        actualError,
                        "CDP session command failed because session is missing: session={}, method={}",
                        sessionId,
                        actualMethod);
                return;
            }
            guarded.completeExceptionally(actualError == null ? error : actualError);
            Logger.warn(
                    false,
                    "Protocol",
                    actualError == null ? error : actualError,
                    "CDP session command failed: session={}, method={}",
                    sessionId,
                    actualMethod);
        });
        return guarded;
    }

    /**
     * Sends a command and applies an optional timeout.
     *
     * @param method  method
     * @param params  params
     * @param options options
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method, Map<String, Object> params, CommandOptions options) {
        CompletableFuture<CdpPayload> future = send(method, params);
        CommandOptions actualOptions = options == null ? new CommandOptions() : options;
        if (actualOptions.getTimeoutMillis() <= Normal._0) {
            return future;
        }
        CompletableFuture<CdpPayload> timed = new CompletableFuture<>();
        ScheduledFuture<?> timeout = COMMAND_TIMER.schedule(
                () -> timed.completeExceptionally(new TimeoutException("CDP command timed out: " + method)),
                actualOptions.getTimeoutMillis(),
                TimeUnit.MILLISECONDS);
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
     * Detaches this session from its target.
     *
     * @return detach result
     */
    public CompletableFuture<CdpPayload> detach() {
        if (detached()) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(
                    new InternalException(
                            "Session already detached. Most likely the " + targetInfo.getType() + " has been closed."));
            return rejected;
        }
        Logger.debug(
                true,
                "Protocol",
                "CDP session detach requested: session={}, target={}",
                sessionId,
                targetInfo.getTargetId());
        return connection.send("Target.detachFromTarget", Map.of("sessionId", sessionId))
                .whenComplete((value, throwable) -> {
                    if (throwable == null) {
                        Logger.debug(false, "Protocol", "CDP session detached: session={}", sessionId);
                        connection.removeSession(sessionId);
                    } else {
                        Logger.warn(false, "Protocol", throwable, "CDP session detach failed: session={}", sessionId);
                        onClosed();
                    }
                });
    }

    /**
     * Registers a listener for a CDP event.
     *
     * @param method   method
     * @param listener listener
     * @return listener binding
     */
    public Binding on(String method, Consumer<CdpPayload> listener) {
        return EventHooks.onPayload(events, method, listener, CdpPayload.class);
    }

    /**
     * Registers a one-shot listener for a CDP event.
     *
     * @param method   method
     * @param listener listener
     * @return listener binding
     */
    public Binding once(String method, Consumer<CdpPayload> listener) {
        return EventHooks.oncePayload(events, method, listener, CdpPayload.class);
    }

    /**
     * Removes a listener from a CDP event.
     *
     * @param method   method
     * @param listener listener
     */
    public void off(String method, Consumer<Object> listener) {
        events.off(Assert.notNull(method, "method"), Assert.notNull(listener, "listener"));
    }

    /**
     * Returns the number of listeners registered for a CDP event.
     *
     * @param method method
     * @return listener count
     */
    public int listenerCount(String method) {
        return events.listenerCount(Assert.notNull(method, "method"));
    }

    /**
     * Removes all listeners for a CDP event.
     *
     * @param method method
     */
    public void removeAllListeners(String method) {
        events.removeAllListeners(Assert.notNull(method, "method"));
    }

    /**
     * Emits a CDP event payload to local listeners.
     *
     * @param method method
     * @param params params
     * @return {@code true} when at least one listener received the event
     */
    public boolean emit(String method, CdpPayload params) {
        Assert.notNull(method, "method");
        return events.emit(method, params == null ? CdpPayload.NULL : params);
    }

    /**
     * Returns the parent session when this session was created by another session.
     *
     * @return parent session
     */
    public Optional<CDPSession> parentSession() {
        return Optional.ofNullable(parentSession);
    }

    /**
     * Returns the parent session or this session when no parent exists.
     *
     * @return parent session or this session
     */
    CDPSession parentSessionOrSelf() {
        return parentSession == null ? this : parentSession;
    }

    /**
     * Returns the owning CDP connection.
     *
     * @return connection
     */
    public Connection connection() {
        return connection;
    }

    /**
     * Returns the detached.
     *
     * @return {@code true} when the condition matches
     */
    public boolean detached() {
        return closed.get() || connection.isClosed();
    }

    /**
     * Returns whether this object is detached.
     *
     * @return {@code true} when the condition matches
     */
    boolean isDetached() {
        return detached();
    }

    /**
     * Returns the CDP session id.
     *
     * @return session id
     */
    public String id() {
        return sessionId;
    }

    /**
     * Routes a raw CDP envelope to a callback or event listener.
     *
     * @param envelope envelope
     */
    void onMessage(CdpEnvelope envelope) {
        Logger.trace(
                false,
                "Protocol",
                "CDP session message received: session={}, id={}, method={}",
                sessionId,
                envelope.hasId() ? envelope.getId() : Normal._0,
                envelope.getMethod());
        if (envelope.hasId()) {
            if (envelope.getError().isNull()) {
                callbacks.resolve(envelope.getId(), envelope.getResult());
            } else {
                callbacks.reject(envelope.getId(), envelope.getError().get("message").asText());
            }
            return;
        }
        if (envelope.isEvent()) {
            emit(envelope.getMethod(), envelope.getParams());
        }
    }

    /**
     * Marks the session closed and rejects pending callbacks.
     */
    void onClosed() {
        if (closed.compareAndSet(false, true)) {
            Logger.debug(
                    false,
                    "Protocol",
                    "CDP session closed: session={}, target={}",
                    sessionId,
                    targetInfo.getTargetId());
            callbacks.clear(new InternalException("CDP session has been closed."));
            emit(Events.DISCONNECTED, CdpPayload.NULL);
        }
    }

    /**
     * Emits a session replacement event.
     *
     * @param replacement replacement
     */
    void swapped(CDPSession replacement) {
        emit(Events.SWAPPED, CdpPayload.of(Map.of("sessionId", Assert.notNull(replacement, "replacement").id())));
    }

    /**
     * Emits the ready event for this session.
     */
    void ready() {
        emit(Events.READY, CdpPayload.of(Map.of("sessionId", id())));
    }

    /**
     * Emits a child-session attached event.
     *
     * @param session session
     */
    void sessionAttached(CDPSession session) {
        emit(Events.SESSION_ATTACHED, CdpPayload.of(Map.of("sessionId", Assert.notNull(session, "session").id())));
    }

    /**
     * Emits a child-session detached event.
     *
     * @param session session
     */
    void sessionDetached(CDPSession session) {
        emit(Events.SESSION_DETACHED, CdpPayload.of(Map.of("sessionId", Assert.notNull(session, "session").id())));
    }

    /**
     * Resolves an envelope when it belongs to a pending callback.
     *
     * @param envelope envelope
     * @return {@code true} when the envelope was handled
     */
    boolean tryResolve(CdpEnvelope envelope) {
        if (!envelope.hasId()) {
            return false;
        }
        if (!callbacks.contains(envelope.getId())) {
            return false;
        }
        onMessage(envelope);
        return true;
    }

    /**
     * Returns whether callback is available.
     *
     * @param id identifier
     * @return {@code true} when the condition matches
     */
    boolean hasCallback(int id) {
        return callbacks.contains(id);
    }

    /**
     * Returns errors recorded for pending callbacks.
     *
     * @return pending protocol errors
     */
    java.util.List<String> getPendingProtocolErrors() {
        return callbacks.getPendingProtocolErrors();
    }

    /**
     * Returns the CDP session id for internal package users.
     *
     * @return session id
     */
    String getSessionId() {
        return sessionId;
    }

    /**
     * Returns target information for internal package users.
     *
     * @return target info
     */
    TargetInfo getTargetInfo() {
        return targetInfo;
    }

    /**
     * Returns target information for this session.
     *
     * @return target info
     */
    TargetInfo target() {
        return targetInfo;
    }

    /**
     * Builds the protocol error message used after a session closes.
     *
     * @param method method
     * @return closed-session error message
     */
    private String closedMessage(String method) {
        return "Protocol error (" + method + "): Session closed. Most likely the " + targetInfo.getType()
                + " has been closed.";
    }

    /**
     * CDP session event names emitted by this class.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Events {

        /**
         * Shared constant for disconnected.
         */
        public static final String DISCONNECTED = "CDPSession.Disconnected";

        /**
         * Shared constant for swapped.
         */
        public static final String SWAPPED = "CDPSession.Swapped";

        /**
         * Shared constant for ready.
         */
        public static final String READY = "CDPSession.Ready";

        /**
         * Shared constant for session attached.
         */
        public static final String SESSION_ATTACHED = "sessionattached";

        /**
         * Shared constant for session detached.
         */
        public static final String SESSION_DETACHED = "sessiondetached";

        /**
         * Creates an instance.
         */
        private Events() {
            // No initialization required.
        }
    }

    /**
     * Provides internal session collaboration without creating a standalone helper type.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Internal {

        /**
         * Creates no Internal instance.
         */
        private Internal() {
            // No initialization required.
        }

        /**
         * Returns target information attached to a session.
         *
         * @param session session
         * @return target information
         */
        public static TargetInfo targetInfo(CDPSession session) {
            return Assert.notNull(session, "session").getTargetInfo();
        }

        /**
         * Marks a session closed.
         *
         * @param session session
         */
        public static void onClosed(CDPSession session) {
            Assert.notNull(session, "session").onClosed();
        }
    }

    /**
     * Options applied to a single CDP command.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class CommandOptions {

        /**
         * Current timeout millis.
         */
        private long timeoutMillis;

        /**
         * Creates an instance.
         */
        public CommandOptions() {
            // No initialization required.
        }

        /**
         * Returns the command timeout.
         *
         * @return timeout in milliseconds
         */
        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        /**
         * Updates timeout millis.
         *
         * @param timeoutMillis timeout in milliseconds
         */
        public void setTimeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
    }

}
