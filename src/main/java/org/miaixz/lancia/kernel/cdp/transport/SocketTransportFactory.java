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

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.UrlKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Represents socket transport factory.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class SocketTransportFactory {

    /**
     * Shared constant for connect timeout.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Creates a socket transport factory.
     */
    private SocketTransportFactory() {
        // No initialization required.
    }

    /**
     * Returns the of.
     *
     * @param wsEndpoint ws endpoint value
     * @return of value
     */
    public static SocketTransport of(String wsEndpoint) {
        return of(wsEndpoint, Collections.emptyMap());
    }

    /**
     * Returns the of.
     *
     * @param wsEndpoint ws endpoint value
     * @param headers    HTTP headers
     * @return of value
     */
    public static SocketTransport of(String wsEndpoint, Map<String, String> headers) {
        return of(wsEndpoint, headers, SecurityPolicy.defaultPolicy(), ResourceLimits.defaults());
    }

    /**
     * Returns the of.
     *
     * @param wsEndpoint     ws endpoint value
     * @param headers        HTTP headers
     * @param securityPolicy security policy
     * @param resourceLimits resource limits
     * @return of value
     */
    public static SocketTransport of(
            String wsEndpoint,
            Map<String, String> headers,
            SecurityPolicy securityPolicy,
            ResourceLimits resourceLimits) {
        Assert.notBlank(wsEndpoint, "wsEndpoint");
        Assert.notNull(headers, "headers");
        SecurityPolicy actualSecurityPolicy = securityPolicy == null ? SecurityPolicy.defaultPolicy() : securityPolicy;
        ResourceLimits actualResourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
        actualSecurityPolicy.validateWebSocketUrl(UrlKit.toURI(wsEndpoint));
        Logger.debug(
                true,
                "Protocol",
                "CDP WebSocket transport create requested: endpoint={}, headerCount={}",
                wsEndpoint.contains("?") ? wsEndpoint.substring(0, wsEndpoint.indexOf('?')) : wsEndpoint,
                headers.size());
        try {
            AtomicReference<Connection> connectionRef = new AtomicReference<>();
            AtomicReference<Consumer<String>> messageHandlerRef = new AtomicReference<>();
            AtomicReference<Runnable> closeHandlerRef = new AtomicReference<>();
            HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
            WebSocket.Builder builder = client.newWebSocketBuilder();
            builder.header("User-Agent", "Lancia");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
            WebSocket webSocket = builder.buildAsync(
                    UrlKit.toURI(wsEndpoint),
                    new SocketTransport.MessageListener(connectionRef, messageHandlerRef, closeHandlerRef,
                            actualResourceLimits))
                    .get(CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Logger.debug(
                    false,
                    "Protocol",
                    "CDP WebSocket transport created: endpoint={}",
                    wsEndpoint.contains("?") ? wsEndpoint.substring(0, wsEndpoint.indexOf('?')) : wsEndpoint);
            return new SocketTransport(webSocket, connectionRef, messageHandlerRef, closeHandlerRef,
                    actualResourceLimits);
        } catch (Exception ex) {
            Logger.error(
                    false,
                    "Protocol",
                    ex,
                    "CDP WebSocket transport create failed: endpoint={}",
                    wsEndpoint.contains("?") ? wsEndpoint.substring(0, wsEndpoint.indexOf('?')) : wsEndpoint);
            throw new IllegalStateException("Failed to create WebSocket transport: " + wsEndpoint, ex);
        }
    }

}
