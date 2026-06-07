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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;

/**
 * WebDriver BiDi protocol session.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiProtocolSession {

    /**
     * Current connection.
     */
    private final BidiConnection connection;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current capabilities.
     */
    private final CdpPayload capabilities;
    /**
     * Thread-safe ended state.
     */
    private final AtomicBoolean ended = new AtomicBoolean(false);

    /**
     * Creates a bidi protocol session.
     *
     * @param connection   protocol connection
     * @param id           identifier
     * @param capabilities capabilities
     */
    public BidiProtocolSession(BidiConnection connection, String id, CdpPayload capabilities) {
        this.connection = connection;
        this.id = id;
        this.capabilities = capabilities == null ? CdpPayload.NULL : capabilities;
        Logger.debug(false, "Protocol", "BiDi protocol session initialized: session={}", id);
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        if (ended.get()) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new IllegalStateException("BiDi protocol session has ended."));
            Logger.warn(
                    false,
                    "Protocol",
                    "BiDi session command rejected after end: session={}, method={}",
                    id,
                    method);
            return rejected;
        }
        Logger.debug(
                true,
                "Protocol",
                "BiDi session command requested: session={}, method={}, params={}",
                id,
                method,
                params == null ? Normal._0 : params.size());
        return connection.send(method, params);
    }

    /**
     * Returns the subscribe.
     *
     * @param events events value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> subscribe(List<String> events) {
        Logger.debug(
                true,
                "Protocol",
                "BiDi session subscribe requested: session={}, events={}",
                id,
                events == null ? Normal._0 : events.size());
        return send("session.subscribe", Map.of("events", events == null ? List.of() : events));
    }

    /**
     * Returns the subscribe.
     *
     * @param events   events value
     * @param contexts contexts value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> subscribe(List<String> events, List<String> contexts) {
        Logger.debug(
                true,
                "Protocol",
                "BiDi session subscribe requested: session={}, events={}, contexts={}",
                id,
                events == null ? Normal._0 : events.size(),
                contexts == null ? Normal._0 : contexts.size());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("events", events == null ? List.of() : events);
        if (contexts != null && !contexts.isEmpty()) {
            params.put("contexts", contexts);
        }
        return send("session.subscribe", params);
    }

    /**
     * Returns the end.
     *
     * @return completion future
     */
    public CompletableFuture<Void> end() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Logger.debug(true, "Protocol", "BiDi session end requested: session={}", id);
        send("session.end", Map.of()).whenComplete((value, throwable) -> {
            ended.set(true);
            if (throwable != null) {
                Logger.warn(false, "Protocol", throwable, "BiDi session end failed: session={}", id);
                result.completeExceptionally(throwable);
                return;
            }
            Logger.debug(false, "Protocol", "BiDi session ended: session={}", id);
            result.complete(null);
        });
        return result;
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
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
    }

    /**
     * Returns the capabilities.
     *
     * @return capabilities value
     */
    public CdpPayload capabilities() {
        return capabilities;
    }

    /**
     * Returns the ended.
     *
     * @return {@code true} when the condition matches
     */
    public boolean ended() {
        return ended.get();
    }

}
