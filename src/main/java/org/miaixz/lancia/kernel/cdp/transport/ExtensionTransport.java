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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Transport;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.protocol.TextWriter;

/**
 * Defines the ExtensionTransport class.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ExtensionTransport implements Transport {

    /**
     * Defines the protocol version constant.
     */
    private static final String PROTOCOL_VERSION = "1.3";

    /**
     * Defines the tab target id constant.
     */
    private static final String TAB_TARGET_ID = "tabTargetId";

    /**
     * Defines the page target id constant.
     */
    private static final String PAGE_TARGET_ID = "pageTargetId";

    /**
     * Defines the tab target session id constant.
     */
    private static final String TAB_TARGET_SESSION_ID = "tabTargetSessionId";

    /**
     * Defines the page target session id constant.
     */
    private static final String PAGE_TARGET_SESSION_ID = "pageTargetSessionId";
    /**
     * Current tab ID.
     */
    private final int tabId;
    /**
     * Current bridge.
     */
    private final DebuggerBridge bridge;
    /**
     * Registered event listener values.
     */
    private final DebuggerEventListener eventListener;
    /**
     * Current writer.
     */
    private final TextWriter writer = new TextWriter();
    /**
     * Thread-safe closed state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);
    /**
     * Current connection.
     */
    private Connection connection;

    /**
     * Creates an ExtensionTransport instance.
     *
     * @param tabId  the tab id value
     * @param bridge the bridge value
     */
    public ExtensionTransport(int tabId, DebuggerBridge bridge) {
        this.tabId = tabId;
        this.bridge = Assert.notNull(bridge, "bridge");
        this.eventListener = this::onDebuggerEvent;
        this.bridge.addEventListener(eventListener);
    }

    /**
     * Returns the connect tab.
     *
     * @param tabId  tab ID value
     * @param bridge bridge value
     * @return completion future
     */
    public static CompletableFuture<ExtensionTransport> connectTab(int tabId, DebuggerBridge bridge) {
        Assert.notNull(bridge, "bridge");
        return bridge.attach(tabId, PROTOCOL_VERSION).thenApply(value -> new ExtensionTransport(tabId, bridge));
    }

    /**
     * Updates connection.
     *
     * @param connection the connection value
     */
    @Override
    public void setConnection(Object connection) {
        this.connection = (Connection) connection;
    }

    /**
     * Sends a protocol command.
     *
     * @param message the message value
     */
    @Override
    public void send(String message) {
        if (closed.get()) {
            return;
        }
        CdpPayload command = CdpPayload.parse(message);
        String method = PayloadReader.text(command.get("method"));
        int id = command.get("id").asInt();
        String sessionId = PayloadReader.text(command.get("sessionId"));
        switch (method) {
            case "Browser.getVersion" -> respond(id, sessionId, method, versionPayload());
            case "Target.getBrowserContexts" -> respond(
                    id,
                    sessionId,
                    method,
                    Map.of("browserContextIds", java.util.List.of()));
            case "Target.setDiscoverTargets" -> discoverTargets(id, sessionId, method);
            case "Target.setAutoAttach" -> {
                if (!autoAttach(id, sessionId, method)) {
                    delegate(command, id, method, sessionId);
                }
            }
            default -> delegate(command, id, method, sessionId);
        }
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            bridge.removeEventListener(eventListener);
            bridge.detach(tabId);
        }
    }

    /**
     * Handles dispatch debugger event.
     *
     * @param sessionId session ID value
     * @param method    protocol method
     * @param params    protocol parameters
     */
    public void dispatchDebuggerEvent(String sessionId, String method, CdpPayload params) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("sessionId", StringKit.isBlank(sessionId) ? PAGE_TARGET_SESSION_ID : sessionId);
        event.put("method", method);
        event.put("params", params == null ? CdpPayload.NULL : params);
        dispatch(event);
    }

    /**
     * Handles discover targets.
     *
     * @param id        identifier
     * @param sessionId session ID value
     * @param method    protocol method
     */
    private void discoverTargets(int id, String sessionId, String method) {
        dispatch(event("Target.targetCreated", Map.of("targetInfo", targetInfo(TAB_TARGET_ID, "tab"))));
        dispatch(event("Target.targetCreated", Map.of("targetInfo", targetInfo(PAGE_TARGET_ID, "page"))));
        respond(id, sessionId, method, Map.of());
    }

    /**
     * Returns the auto attach.
     *
     * @param id        identifier
     * @param sessionId session ID value
     * @param method    protocol method
     * @return {@code true} when the condition matches
     */
    private boolean autoAttach(int id, String sessionId, String method) {
        if (TAB_TARGET_SESSION_ID.equals(sessionId)) {
            dispatch(
                    event(
                            TAB_TARGET_SESSION_ID,
                            "Target.attachedToTarget",
                            Map.of(
                                    "targetInfo",
                                    targetInfo(PAGE_TARGET_ID, "page"),
                                    "sessionId",
                                    PAGE_TARGET_SESSION_ID)));
            respond(id, sessionId, method, Map.of());
            return true;
        } else if (StringKit.isBlank(sessionId)) {
            dispatch(
                    event(
                            "Target.attachedToTarget",
                            Map.of(
                                    "targetInfo",
                                    targetInfo(TAB_TARGET_ID, "tab"),
                                    "sessionId",
                                    TAB_TARGET_SESSION_ID)));
            respond(id, sessionId, method, Map.of());
            return true;
        }
        return false;
    }

    /**
     * Handles on debugger event.
     *
     * @param sourceTabId source tab ID value
     * @param sessionId   session ID value
     * @param method      protocol method
     * @param params      protocol parameters
     */
    private void onDebuggerEvent(int sourceTabId, String sessionId, String method, CdpPayload params) {
        if (sourceTabId != tabId) {
            return;
        }
        dispatchDebuggerEvent(sessionId, method, params);
    }

    /**
     * Handles delegate.
     *
     * @param command   command name
     * @param id        identifier
     * @param method    protocol method
     * @param sessionId session ID value
     */
    private void delegate(CdpPayload command, int id, String method, String sessionId) {
        String targetSessionId = PAGE_TARGET_SESSION_ID.equals(sessionId) ? Normal.EMPTY : sessionId;
        bridge.sendCommand(tabId, targetSessionId, method, command.get("params")).whenComplete((payload, throwable) -> {
            String responseSessionId = StringKit.isBlank(targetSessionId) ? PAGE_TARGET_SESSION_ID : targetSessionId;
            if (throwable == null) {
                respond(id, responseSessionId, method, payload == null ? Map.of() : payload);
            } else {
                reject(id, responseSessionId, method, throwable);
            }
        });
    }

    /**
     * Handles respond.
     *
     * @param id        identifier
     * @param sessionId session ID value
     * @param method    protocol method
     * @param result    result value
     */
    private void respond(int id, String sessionId, String method, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        putSession(response, sessionId);
        response.put("method", method);
        response.put("result", result == null ? Map.of() : result);
        dispatch(response);
    }

    /**
     * Handles reject.
     *
     * @param id        identifier
     * @param sessionId session ID value
     * @param method    protocol method
     * @param throwable throwable value
     */
    private void reject(int id, String sessionId, String method, Throwable throwable) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        putSession(response, sessionId);
        response.put("method", method);
        response.put(
                "error",
                Map.of(
                        "message",
                        throwable.getMessage() == null ? "CDP error had no message" : throwable.getMessage()));
        dispatch(response);
    }

    /**
     * Handles dispatch.
     *
     * @param message message text
     */
    private void dispatch(Map<String, Object> message) {
        if (connection == null || closed.get()) {
            return;
        }
        connection.onMessage(writer.writeValue(message));
    }

    /**
     * Returns the event.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return mapped values
     */
    private Map<String, Object> event(String method, Map<String, Object> params) {
        return event(Normal.EMPTY, method, params);
    }

    /**
     * Returns the event.
     *
     * @param sessionId session ID value
     * @param method    protocol method
     * @param params    protocol parameters
     * @return mapped values
     */
    private Map<String, Object> event(String sessionId, String method, Map<String, Object> params) {
        Map<String, Object> event = new LinkedHashMap<>();
        putSession(event, sessionId);
        event.put("method", method);
        event.put("params", params);
        return event;
    }

    /**
     * Returns the target info.
     *
     * @param targetId target ID value
     * @param type     type to use
     * @return mapped values
     */
    private Map<String, Object> targetInfo(String targetId, String type) {
        Map<String, Object> targetInfo = new LinkedHashMap<>();
        targetInfo.put("targetId", targetId);
        targetInfo.put("type", type);
        targetInfo.put("title", type);
        targetInfo.put("url", Builder.ABOUT_BLANK);
        targetInfo.put("attached", false);
        targetInfo.put("canAccessOpener", false);
        return targetInfo;
    }

    /**
     * Returns the version payload.
     *
     * @return mapped values
     */
    private Map<String, Object> versionPayload() {
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("protocolVersion", PROTOCOL_VERSION);
        version.put("product", "chrome");
        version.put("revision", Normal.UNKNOWN);
        version.put("userAgent", "chrome");
        version.put("jsVersion", Normal.UNKNOWN);
        return version;
    }

    /**
     * Handles put session.
     *
     * @param value     value to use
     * @param sessionId session ID value
     */
    private void putSession(Map<String, Object> value, String sessionId) {
        if (StringKit.isNotBlank(sessionId)) {
            value.put("sessionId", sessionId);
        }
    }

    /**
     * Defines the DebuggerBridge interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface DebuggerBridge {

        /**
         * Returns the attach.
         *
         * @param tabId           tab ID value
         * @param protocolVersion protocol version value
         * @return completion future
         */
        CompletableFuture<Void> attach(int tabId, String protocolVersion);

        /**
         * Handles add event listener.
         *
         * @param listener event listener
         */
        default void addEventListener(DebuggerEventListener listener) {
        }

        /**
         * Handles remove event listener.
         *
         * @param listener event listener
         */
        default void removeEventListener(DebuggerEventListener listener) {
        }

        /**
         * Returns the send command.
         *
         * @param tabId     tab ID value
         * @param sessionId session ID value
         * @param method    protocol method
         * @param params    protocol parameters
         * @return completion future
         */
        CompletableFuture<CdpPayload> sendCommand(int tabId, String sessionId, String method, CdpPayload params);

        /**
         * Returns the detach.
         *
         * @param tabId tab ID value
         * @return completion future
         */
        CompletableFuture<Void> detach(int tabId);
    }

    /**
     * Defines the DebuggerEventListener interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface DebuggerEventListener {

        /**
         * Handles on event.
         *
         * @param tabId     tab ID value
         * @param sessionId session ID value
         * @param method    protocol method
         * @param params    protocol parameters
         */
        void onEvent(int tabId, String sessionId, String method, CdpPayload params);
    }

}
