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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;

/**
 * Represents a BiDi connection object.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiConnection implements AutoCloseable {

    /**
     * Current connection.
     */
    private final org.miaixz.lancia.kernel.bidi.session.BidiConnection connection;

    /**
     * Returns the from.
     *
     * @param connection protocol connection
     * @return from value
     */
    public static BidiConnection from(org.miaixz.lancia.kernel.bidi.session.BidiConnection connection) {
        return new BidiConnection(connection);
    }

    /**
     * Creates a BiDi connection.
     *
     * @param connection protocol connection
     */
    public BidiConnection(org.miaixz.lancia.kernel.bidi.session.BidiConnection connection) {
        this.connection = Assert.notNull(connection, "connection");
        Logger.debug(
                false,
                "Protocol",
                "BiDi connection initialized: url={}",
                connection.url().replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        String actualMethod = Assert.notBlank(method, "method");
        Logger.debug(true, "Protocol", "BiDi command requested: method={}", actualMethod);
        return connection.send(actualMethod, params == null ? Map.of() : params).whenComplete((payload, throwable) -> {
            if (throwable == null) {
                Logger.debug(false, "Protocol", "BiDi command completed: method={}", actualMethod);
            } else {
                Logger.error(
                        false,
                        "Protocol",
                        "BiDi command failed: method={}, message={}",
                        actualMethod,
                        throwable.getMessage());
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
     * Returns the send with result.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return completion future
     */
    public CompletableFuture<CdpPayload> sendWithResult(String method, Map<String, Object> params) {
        Logger.debug(true, "Protocol", "BiDi result command requested: method={}", method);
        return send(method, params).thenApply(result -> CdpPayload.of(Map.of("result", result)));
    }

    /**
     * Registers an event listener.
     *
     * @param method   protocol method
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String method, Consumer<CdpPayload> listener) {
        String actualMethod = Assert.notBlank(method, "method");
        Logger.debug(true, "Protocol", "BiDi listener added: method={}", actualMethod);
        return connection.on(actualMethod, Assert.notNull(listener, "listener"));
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param method   protocol method
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String method, Consumer<CdpPayload> listener) {
        String actualMethod = Assert.notBlank(method, "method");
        Logger.debug(true, "Protocol", "BiDi one-time listener added: method={}", actualMethod);
        return connection.once(actualMethod, Assert.notNull(listener, "listener"));
    }

    /**
     * Returns the listener count.
     *
     * @param method protocol method
     * @return listener count value
     */
    public int listenerCount(String method) {
        return connection.listenerCount(Assert.notBlank(method, "method"));
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return connection.url();
    }

    /**
     * Returns whether this object is closed.
     *
     * @return whether this object is closed
     */
    public boolean closed() {
        return connection.closed();
    }

    /**
     * Returns the underlying connection.
     *
     * @return underlying connection
     */
    public org.miaixz.lancia.kernel.bidi.session.BidiConnection unwrap() {
        return connection;
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        Logger.debug(true, "Protocol", "BiDi connection dispose requested: closed={}", connection.closed());
        connection.dispose();
        Logger.debug(false, "Protocol", "BiDi connection disposed");
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        dispose();
    }

}
