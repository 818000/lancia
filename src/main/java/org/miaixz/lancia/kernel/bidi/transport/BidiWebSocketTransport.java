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
package org.miaixz.lancia.kernel.bidi.transport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.bus.core.xyz.UrlKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.bidi.session.BidiConnection;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Transports bidi web socket protocol messages.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiWebSocketTransport implements BidiTransport, WebSocket.Listener {

    /**
     * Current web socket.
     */
    private final WebSocket webSocket;
    /**
     * Current buffer.
     */
    private final StringBuilder buffer = new StringBuilder();
    /**
     * Thread-safe closed state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);
    /**
     * Current connection.
     */
    private BidiConnection connection;
    /**
     * Resource limits.
     */
    private final ResourceLimits resourceLimits;
    /**
     * Current send timeout millis.
     */
    private final long sendTimeoutMillis;
    /**
     * Send queue permits.
     */
    private final Semaphore sendPermits = new Semaphore(1024);

    /**
     * Creates a bidi web socket transport.
     *
     * @param endpoint endpoint
     */
    public BidiWebSocketTransport(URI endpoint) {
        this(endpoint, SecurityPolicy.defaultPolicy(), ResourceLimits.defaults(), 30_000L, 30_000L);
    }

    /**
     * Creates a bidi web socket transport.
     *
     * @param endpoint       endpoint
     * @param timeoutMillis  connect timeout millis
     * @param resourceLimits resource limits
     */
    public BidiWebSocketTransport(URI endpoint, long timeoutMillis, ResourceLimits resourceLimits) {
        this(endpoint, SecurityPolicy.defaultPolicy(), resourceLimits, timeoutMillis, timeoutMillis);
    }

    /**
     * Creates a bidi web socket transport.
     *
     * @param endpoint       endpoint
     * @param securityPolicy security policy
     * @param resourceLimits resource limits
     * @param timeoutMillis  connect timeout millis
     */
    public BidiWebSocketTransport(URI endpoint, SecurityPolicy securityPolicy, ResourceLimits resourceLimits,
            long timeoutMillis) {
        this(endpoint, securityPolicy, resourceLimits, timeoutMillis, timeoutMillis);
    }

    /**
     * Creates a bidi web socket transport.
     *
     * @param endpoint             endpoint
     * @param securityPolicy       security policy
     * @param resourceLimits       resource limits
     * @param connectTimeoutMillis connect timeout millis
     * @param sendTimeoutMillis    send timeout millis
     */
    public BidiWebSocketTransport(URI endpoint, SecurityPolicy securityPolicy, ResourceLimits resourceLimits,
            long connectTimeoutMillis, long sendTimeoutMillis) {
        SecurityPolicy actualSecurityPolicy = securityPolicy == null ? SecurityPolicy.defaultPolicy() : securityPolicy;
        actualSecurityPolicy.validateWebSocketUrl(endpoint);
        this.resourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
        long actualConnectTimeout = connectTimeoutMillis <= 0 ? 30_000L : connectTimeoutMillis;
        this.sendTimeoutMillis = sendTimeoutMillis <= 0 ? actualConnectTimeout : sendTimeoutMillis;
        Logger.debug(
                true,
                "Protocol",
                "BiDi WebSocket transport create requested: endpoint={}",
                endpoint == null ? Normal.EMPTY : endpoint.toString().replaceAll("[?#].*$", "?<redacted>"));
        try {
            this.webSocket = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(actualConnectTimeout)).build()
                    .newWebSocketBuilder().buildAsync(endpoint, this).get(actualConnectTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create BiDi WebSocket transport.", ex);
        }
        Logger.debug(
                false,
                "Protocol",
                "BiDi WebSocket transport created: endpoint={}",
                endpoint == null ? Normal.EMPTY : endpoint.toString().replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Opens a BiDi WebSocket transport.
     *
     * @param endpoint endpoint URL
     * @return WebSocket transport
     */
    public static BidiWebSocketTransport connect(String endpoint) {
        return connect(endpoint, 30_000L, SecurityPolicy.defaultPolicy(), ResourceLimits.defaults());
    }

    /**
     * Opens a BiDi WebSocket transport with runtime policies.
     *
     * @param endpoint              endpoint URL
     * @param protocolTimeoutMillis protocol timeout in milliseconds
     * @param securityPolicy        security policy
     * @param resourceLimits        resource limits
     * @return WebSocket transport
     */
    public static BidiWebSocketTransport connect(
            String endpoint,
            long protocolTimeoutMillis,
            SecurityPolicy securityPolicy,
            ResourceLimits resourceLimits) {
        Logger.debug(
                true,
                "Protocol",
                "BiDi WebSocket connect requested: endpoint={}",
                endpoint == null ? Normal.EMPTY : endpoint.replaceAll("[?#].*$", "?<redacted>"));
        return new BidiWebSocketTransport(UrlKit.toURI(endpoint), securityPolicy, resourceLimits, protocolTimeoutMillis,
                protocolTimeoutMillis);
    }

    /**
     * Binds this transport to the protocol connection that receives messages.
     *
     * @param connection protocol connection
     */
    public void bind(BidiConnection connection) {
        this.connection = connection;
        Logger.debug(false, "Protocol", "BiDi WebSocket transport bound: hasConnection={}", connection != null);
    }

    /**
     * Sends a protocol command.
     *
     * @param message message text
     */
    @Override
    public void send(String message) {
        if (closed.get()) {
            throw new IllegalStateException("BiDi WebSocket transport has been closed.");
        }
        String actualMessage = message == null ? Normal.EMPTY : message;
        resourceLimits.validateProtocolMessageBytes(ByteKit.toBytes(actualMessage, Charset.UTF_8).length);
        if (!sendPermits.tryAcquire()) {
            throw new IllegalStateException("BiDi WebSocket send queue is full.");
        }
        Logger.debug(true, "Protocol", "BiDi WebSocket send requested: chars={}", actualMessage.length());
        try {
            webSocket.sendText(actualMessage, true).orTimeout(sendTimeoutMillis, TimeUnit.MILLISECONDS).join();
            Logger.debug(false, "Protocol", "BiDi WebSocket send completed.");
        } catch (CompletionException ex) {
            throw new IllegalStateException("Failed to send BiDi WebSocket message.", ex);
        } finally {
            sendPermits.release();
        }
    }

    /**
     * Handles on open.
     *
     * @param webSocket web socket value
     */
    @Override
    public void onOpen(WebSocket webSocket) {
        Logger.debug(false, "Protocol", "BiDi WebSocket opened.");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    /**
     * Returns the on text.
     *
     * @param webSocket web socket value
     * @param data      data to use
     * @param last      last value
     * @return completion future
     */
    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            resourceLimits.validateProtocolMessageBytes(buffer.length() + data.length());
        } catch (RuntimeException ex) {
            Logger.warn(
                    false,
                    "Protocol",
                    ex,
                    "BiDi WebSocket payload rejected: chars={}",
                    buffer.length() + data.length());
            if (connection != null) {
                connection.onError(ex);
            }
            webSocket.abort();
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        buffer.append(data);
        if (last) {
            String message = buffer.toString();
            buffer.setLength(0);
            Logger.debug(false, "Protocol", "BiDi WebSocket message received: chars={}", message.length());
            if (connection != null) {
                connection.onMessage(message);
            } else {
                Logger.warn(
                        false,
                        "Protocol",
                        "BiDi WebSocket message dropped before binding: chars={}",
                        message.length());
            }
        }
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the on close.
     *
     * @param webSocket  web socket value
     * @param statusCode status code value
     * @param reason     reason value
     * @return completion future
     */
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        closed.set(true);
        Logger.debug(false, "Protocol", "BiDi WebSocket closed: status={}, reason={}", statusCode, reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    /**
     * Handles on error.
     *
     * @param webSocket web socket value
     * @param error     error to propagate
     */
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Logger.warn(false, "Protocol", error, "BiDi WebSocket error.");
        if (connection != null) {
            connection.onError(error);
        }
        WebSocket.Listener.super.onError(webSocket, error);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Logger.debug(true, "Protocol", "BiDi WebSocket close requested.");
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "closed");
            Logger.debug(false, "Protocol", "BiDi WebSocket close sent.");
        }
    }

}
