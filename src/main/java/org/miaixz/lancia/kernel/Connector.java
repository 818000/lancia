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
package org.miaixz.lancia.kernel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.kernel.bidi.session.BidiConnection;
import org.miaixz.lancia.kernel.bidi.transport.BidiTransport;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.protocol.TextWriter;

/**
 * Connects BiDi through a CDP-backed transport.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Connector {

    /**
     * Default session ID.
     */
    private static final String DEFAULT_SESSION_ID = "bidi-over-cdp-session";

    /**
     * Creates a connector.
     */
    private Connector() {
        // No initialization required.
    }

    /**
     * Creates a BiDi connection backed by an existing CDP connection.
     *
     * @param cdp CDP connection
     * @return BiDi connection
     */
    public static BidiConnection connect(Connection cdp) {
        return connect(cdp, Builder.DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Creates a BiDi connection backed by an existing CDP connection.
     *
     * @param cdp                  CDP connection
     * @param defaultTimeoutMillis default command timeout in milliseconds
     * @return BiDi connection
     */
    public static BidiConnection connect(Connection cdp, long defaultTimeoutMillis) {
        CdpBackedBidiTransport transport = new CdpBackedBidiTransport(Assert.notNull(cdp, "cdp"));
        BidiConnection connection = new BidiConnection("bidi+cdp://local", transport, defaultTimeoutMillis);
        transport.bind(connection);
        return connection;
    }

    /**
     * Transports BiDi protocol messages through CDP commands.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class CdpBackedBidiTransport implements BidiTransport {

        /**
         * CDP connection used to forward compatible commands.
         */
        private final Connection cdp;
        /**
         * JSON writer used for BiDi response messages.
         */
        private final TextWriter writer = new TextWriter();
        /**
         * Bound BiDi connection that receives synthetic responses.
         */
        private BidiConnection bidi;
        /**
         * Thread-safe closed state.
         */
        private final AtomicBoolean closed = new AtomicBoolean();

        /**
         * Creates an instance.
         *
         * @param cdp CDP value
         */
        private CdpBackedBidiTransport(Connection cdp) {
            this.cdp = cdp;
        }

        /**
         * Binds the transport to the BiDi connection it feeds.
         *
         * @param bidi BiDi value
         */
        private void bind(BidiConnection bidi) {
            this.bidi = bidi;
        }

        /**
         * Sends a protocol command.
         *
         * @param message message text
         */
        @Override
        public void send(String message) {
            if (closed.get()) {
                throw new InternalException("Connector transport has been closed.");
            }
            CdpPayload command = CdpPayload.parse(message);
            int id = command.get("id").asInt();
            String method = PayloadReader.text(command.get("method"));
            CdpPayload params = command.get("params");
            switch (method) {
                case "session.status" -> success(id, Map.of("ready", true, "message", "ready"));
                case "session.new" -> success(id, Map.of("sessionId", DEFAULT_SESSION_ID, "capabilities", Map.of()));
                case "session.subscribe", "session.unsubscribe", "session.end" -> success(id, Map.of());
                case "cdp.sendCommand", "goog:cdp.sendCommand" -> forwardCdpCommand(id, params);
                default -> success(id, Map.of());
            }
        }

        /**
         * Closes this object and releases its resources.
         */
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                cdp.dispose();
            }
        }

        /**
         * Handles forward CDP command.
         *
         * @param id     identifier
         * @param params protocol parameters
         */
        private void forwardCdpCommand(int id, CdpPayload params) {
            String method = PayloadReader.text(params.get("method"));
            String sessionId = PayloadReader.text(params.get("session"));
            Map<String, Object> commandParams = object(params.get("params"));
            CompletableFuture<CdpPayload> future = sessionId.isEmpty() ? cdp.send(method, commandParams)
                    : cdp.rawSend(sessionId, method, commandParams);
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    error(id, "cdp error", throwable.getMessage());
                    return;
                }
                success(id, Map.of("result", PayloadReader.value(result)));
            });
        }

        /**
         * Handles success.
         *
         * @param id     identifier
         * @param result result value
         */
        private void success(int id, Map<String, Object> result) {
            bidi.onMessage(writer.writeValue(Map.of("type", "success", "id", id, "result", result)));
        }

        /**
         * Handles error.
         *
         * @param id      identifier
         * @param error   error to propagate
         * @param message message text
         */
        private void error(int id, String error, String message) {
            bidi.onMessage(
                    writer.writeValue(
                            Map.of(
                                    "type",
                                    "error",
                                    "id",
                                    id,
                                    "error",
                                    error,
                                    "message",
                                    message == null ? Normal.EMPTY : message)));
        }

        /**
         * Converts a CDP payload object to Java map values.
         *
         * @param payload protocol payload
         * @return mapped values
         */
        private Map<String, Object> object(CdpPayload payload) {
            if (!PayloadReader.present(payload)) {
                return Map.of();
            }
            if (!payload.isObject()) {
                throw new InternalException("Connector CDP parameters must be an object.");
            }
            return PayloadReader.object(payload);
        }

    }

}
