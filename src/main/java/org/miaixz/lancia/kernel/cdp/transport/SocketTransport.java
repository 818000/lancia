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
package org.miaixz.lancia.kernel.cdp.transport;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Transport;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.runtime.ResourceLimits;

/**
 * CDP transport backed by the JDK WebSocket client.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class SocketTransport implements Transport {

    /**
     * Normal WebSocket close status code.
     */
    public static final int NORMAL_CLOSE = 1000;

    /**
     * Maximum WebSocket payload accepted by Puppeteer.
     */
    public static final int MAX_PAYLOAD_BYTES = 256 * 1024 * 1024;

    /**
     * Default send timeout.
     */
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Maximum concurrent send operations.
     */
    private static final int MAX_IN_FLIGHT_SENDS = 1024;

    /**
     * JDK WebSocket instance.
     */
    private final WebSocket webSocket;

    /**
     * Connection reference used by the public transport binding.
     */
    private final AtomicReference<Connection> connectionRef;

    /**
     * Message callback reference.
     */
    private final AtomicReference<Consumer<String>> messageHandlerRef;

    /**
     * Close callback reference.
     */
    private final AtomicReference<Runnable> closeHandlerRef;

    /**
     * Send concurrency limiter.
     */
    private final Semaphore sendPermits = new Semaphore(MAX_IN_FLIGHT_SENDS);

    /**
     * Whether the transport is closed.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a WebSocket transport.
     *
     * @param webSocket JDK WebSocket instance
     */
    public SocketTransport(WebSocket webSocket) {
        this(webSocket, new AtomicReference<>(), ResourceLimits.defaults());
    }

    /**
     * Creates a WebSocket transport.
     *
     * @param webSocket     JDK WebSocket instance
     * @param connectionRef connection reference
     */
    public SocketTransport(WebSocket webSocket, AtomicReference<Connection> connectionRef) {
        this(webSocket, connectionRef, ResourceLimits.defaults());
    }

    /**
     * Creates a WebSocket transport.
     *
     * @param webSocket      JDK WebSocket instance
     * @param connectionRef  connection reference
     * @param resourceLimits resource limits
     */
    public SocketTransport(WebSocket webSocket, AtomicReference<Connection> connectionRef,
            ResourceLimits resourceLimits) {
        this(webSocket, connectionRef, new AtomicReference<>(), new AtomicReference<>(), resourceLimits);
    }

    /**
     * Creates a WebSocket transport.
     *
     * @param webSocket         JDK WebSocket instance
     * @param connectionRef     connection reference
     * @param messageHandlerRef message handler reference
     * @param closeHandlerRef   close handler reference
     */
    public SocketTransport(WebSocket webSocket, AtomicReference<Connection> connectionRef,
            AtomicReference<Consumer<String>> messageHandlerRef, AtomicReference<Runnable> closeHandlerRef) {
        this(webSocket, connectionRef, messageHandlerRef, closeHandlerRef, ResourceLimits.defaults());
    }

    /**
     * Creates a WebSocket transport.
     *
     * @param webSocket         JDK WebSocket instance
     * @param connectionRef     connection reference
     * @param messageHandlerRef message handler reference
     * @param closeHandlerRef   close handler reference
     * @param resourceLimits    resource limits
     */
    public SocketTransport(WebSocket webSocket, AtomicReference<Connection> connectionRef,
            AtomicReference<Consumer<String>> messageHandlerRef, AtomicReference<Runnable> closeHandlerRef,
            ResourceLimits resourceLimits) {
        this.webSocket = Assert.notNull(webSocket, "webSocket");
        this.connectionRef = Assert.notNull(connectionRef, "connectionRef");
        this.messageHandlerRef = Assert.notNull(messageHandlerRef, "messageHandlerRef");
        this.closeHandlerRef = Assert.notNull(closeHandlerRef, "closeHandlerRef");
    }

    /**
     * Creates a WebSocket transport.
     *
     * @param wsEndpoint WebSocket endpoint
     * @return WebSocket transport
     */
    public static SocketTransport create(String wsEndpoint) {
        return SocketTransportFactory.of(wsEndpoint);
    }

    /**
     * Creates a WebSocket transport with request headers.
     *
     * @param wsEndpoint WebSocket endpoint
     * @param headers    request headers
     * @return WebSocket transport
     */
    public static SocketTransport create(String wsEndpoint, Map<String, String> headers) {
        return SocketTransportFactory.of(wsEndpoint, headers);
    }

    /**
     * Sends one CDP text message.
     *
     * @param message CDP text message
     */
    @Override
    public void send(String message) {
        Assert.notNull(message, "message");
        if (closed.get() || webSocket.isOutputClosed()) {
            throw new IllegalStateException("WebSocket transport is already closed.");
        }
        if (!sendPermits.tryAcquire()) {
            throw new IllegalStateException("WebSocket send queue is full.");
        }
        Logger.trace(true, "Protocol", "CDP WebSocket message send requested: chars={}", message.length());
        try {
            webSocket.sendText(message, true).orTimeout(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).join();
            Logger.trace(false, "Protocol", "CDP WebSocket message sent: chars={}", message.length());
        } finally {
            sendPermits.release();
        }
    }

    /**
     * Updates connection.
     *
     * @param connection connection callback target
     */
    @Override
    public void setConnection(Object connection) {
        connectionRef.set((Connection) Assert.notNull(connection, "connection"));
    }

    /**
     * Updates message handler.
     *
     * @param handler message handler
     */
    @Override
    public void setMessageHandler(Consumer<String> handler) {
        messageHandlerRef.set(handler);
    }

    /**
     * Updates close handler.
     *
     * @param handler close handler
     */
    @Override
    public void setCloseHandler(Runnable handler) {
        closeHandlerRef.set(handler);
    }

    /**
     * Closes the WebSocket transport.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Logger.debug(true, "Protocol", "CDP WebSocket transport close requested.");
            try {
                if (!webSocket.isOutputClosed()) {
                    webSocket.sendClose(NORMAL_CLOSE, "normal close").join();
                }
            } catch (RuntimeException ex) {
                Logger.warn(false, "Protocol", ex, "CDP WebSocket close handshake failed; aborting.");
                webSocket.abort();
            }
            notifyClosed(NORMAL_CLOSE, "normal close", connectionRef, closeHandlerRef);
            Logger.debug(false, "Protocol", "CDP WebSocket transport closed.");
        }
    }

    /**
     * Returns whether this object is closed.
     *
     * @return {@code true} when closed
     */
    public boolean closed() {
        return closed.get();
    }

    /**
     * Dispatches a complete text message.
     *
     * @param message           complete message
     * @param connectionRef     connection reference
     * @param messageHandlerRef message handler reference
     */
    private static void notifyMessage(
            String message,
            AtomicReference<Connection> connectionRef,
            AtomicReference<Consumer<String>> messageHandlerRef) {
        Consumer<String> handler = messageHandlerRef.get();
        if (handler != null) {
            handler.accept(message);
            return;
        }
        Connection connection = connectionRef.get();
        if (connection != null) {
            connection.onMessage(message);
        }
    }

    /**
     * Dispatches a close event.
     *
     * @param statusCode      WebSocket close status code
     * @param reason          WebSocket close reason
     * @param connectionRef   connection reference
     * @param closeHandlerRef close handler reference
     */
    private static void notifyClosed(
            int statusCode,
            String reason,
            AtomicReference<Connection> connectionRef,
            AtomicReference<Runnable> closeHandlerRef) {
        Connection connection = connectionRef.get();
        if (connection != null) {
            connection.onClosed(statusCode, reason);
        }
        Runnable handler = closeHandlerRef.get();
        if (handler != null) {
            handler.run();
        }
    }

    /**
     * WebSocket inbound message listener.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class MessageListener implements WebSocket.Listener {

        /**
         * Connection reference.
         */
        private final AtomicReference<Connection> connectionRef;

        /**
         * Message handler reference.
         */
        private final AtomicReference<Consumer<String>> messageHandlerRef;

        /**
         * Close handler reference.
         */
        private final AtomicReference<Runnable> closeHandlerRef;
        /**
         * Resource limits.
         */
        private final ResourceLimits resourceLimits;

        /**
         * Fragmented text message buffer.
         */
        private final StringBuilder fragments = new StringBuilder();

        /**
         * Current payload size in UTF-16 code units.
         */
        private int payloadSize;

        /**
         * Creates a WebSocket inbound listener.
         *
         * @param connectionRef connection reference
         */
        public MessageListener(AtomicReference<Connection> connectionRef) {
            this(connectionRef, new AtomicReference<>(), new AtomicReference<>(), ResourceLimits.defaults());
        }

        /**
         * Creates a WebSocket inbound listener.
         *
         * @param connectionRef     connection reference
         * @param messageHandlerRef message handler reference
         * @param closeHandlerRef   close handler reference
         */
        public MessageListener(AtomicReference<Connection> connectionRef,
                AtomicReference<Consumer<String>> messageHandlerRef, AtomicReference<Runnable> closeHandlerRef) {
            this(connectionRef, messageHandlerRef, closeHandlerRef, ResourceLimits.defaults());
        }

        /**
         * Creates a WebSocket inbound listener.
         *
         * @param connectionRef     connection reference
         * @param messageHandlerRef message handler reference
         * @param closeHandlerRef   close handler reference
         * @param resourceLimits    resource limits
         */
        public MessageListener(AtomicReference<Connection> connectionRef,
                AtomicReference<Consumer<String>> messageHandlerRef, AtomicReference<Runnable> closeHandlerRef,
                ResourceLimits resourceLimits) {
            this.connectionRef = Assert.notNull(connectionRef, "connectionRef");
            this.messageHandlerRef = Assert.notNull(messageHandlerRef, "messageHandlerRef");
            this.closeHandlerRef = Assert.notNull(closeHandlerRef, "closeHandlerRef");
            this.resourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
        }

        /**
         * Requests the first message after opening.
         *
         * @param webSocket JDK WebSocket instance
         */
        @Override
        public void onOpen(WebSocket webSocket) {
            Logger.debug(false, "Protocol", "CDP WebSocket opened.");
            webSocket.request(1);
        }

        /**
         * Handles text message fragments.
         *
         * @param webSocket JDK WebSocket instance
         * @param data      text fragment
         * @param last      whether this is the last fragment
         * @return completion signal
         */
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            payloadSize += data.length();
            try {
                resourceLimits.validateProtocolMessageBytes(payloadSize);
            } catch (RuntimeException ex) {
                IllegalStateException error = new IllegalStateException(
                        "WebSocket payload exceeds the configured limit.", ex);
                Logger.warn(false, "Protocol", error, "CDP WebSocket payload rejected: chars={}", payloadSize);
                Connection connection = connectionRef.get();
                if (connection != null) {
                    connection.onError(error);
                }
                webSocket.abort();
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            fragments.append(data);
            if (last) {
                Logger.trace(false, "Protocol", "CDP WebSocket message received: chars={}", fragments.length());
                notifyMessage(fragments.toString(), connectionRef, messageHandlerRef);
                fragments.setLength(0);
                payloadSize = 0;
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        /**
         * Ignores binary messages and requests the next message.
         *
         * @param webSocket JDK WebSocket instance
         * @param data      binary data
         * @param last      whether this is the last fragment
         * @return completion signal
         */
        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        /**
         * Handles WebSocket close events.
         *
         * @param webSocket  JDK WebSocket instance
         * @param statusCode close status code
         * @param reason     close reason
         * @return completion signal
         */
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            Logger.debug(false, "Protocol", "CDP WebSocket closed: status={}, reason={}", statusCode, reason);
            notifyClosed(statusCode, reason, connectionRef, closeHandlerRef);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        /**
         * Handles WebSocket errors.
         *
         * @param webSocket JDK WebSocket instance
         * @param error     error
         */
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            Logger.warn(false, "Protocol", error, "CDP WebSocket error.");
            Connection connection = connectionRef.get();
            if (connection != null) {
                connection.onError(error);
            }
        }
    }

}
