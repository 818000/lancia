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
package org.miaixz.lancia.kernel.cdp.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.Mcp;
import org.miaixz.lancia.kernel.cdp.page.CdpFrame;
import org.miaixz.lancia.kernel.cdp.page.CdpFrameManager;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Exposes page automation tools through MCP-style tool definitions.
 * <p>
 * CdpWebMCP is not declared by Puppeteer. It is deliberately kept outside the public Puppeteer parity contracts: the
 * public {@code Mcp} interface exposes only initialization and inspection, while this concrete class owns Lancia
 * extension operations such as tool registration, remote invocation, and tool-call response routing.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpWebMCP implements Mcp {

    /**
     * Shared constant for tools added.
     */
    public static final String TOOLS_ADDED = "toolsadded";

    /**
     * Shared constant for tools removed.
     */
    public static final String TOOLS_REMOVED = "toolsremoved";

    /**
     * Shared constant for tool invoked.
     */
    public static final String TOOL_INVOKED = "toolinvoked";

    /**
     * Shared constant for tool responded.
     */
    public static final String TOOL_RESPONDED = "toolresponded";
    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Current frame manager.
     */
    private final CdpFrameManager frameManager;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Mapped tools values.
     */
    private final Map<String, Map<String, WebMCPTool>> tools = new LinkedHashMap<>();
    /**
     * Mapped pending calls values.
     */
    private final Map<String, WebMCPToolCall> pendingCalls = new LinkedHashMap<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();

    /**
     * Creates an instance.
     *
     * @param session protocol session
     */
    public CdpWebMCP(CDPSession session) {
        this(session, null);
    }

    /**
     * Creates an instance.
     *
     * @param session      protocol session
     * @param frameManager frame manager value
     */
    public CdpWebMCP(CDPSession session, CdpFrameManager frameManager) {
        this.frameManager = frameManager;
        updateClient(session);
    }

    /**
     * Updates client.
     *
     * @param session protocol session
     */
    public void updateClient(CDPSession session) {
        binding.unbind();
        binding = new EventBinding();
        this.session = session;
        if (session == null) {
            return;
        }
        binding = binding.combine(session.on("CdpWebMCP.toolsAdded", this::onToolsAdded));
        binding = binding.combine(session.on("CdpWebMCP.toolsRemoved", this::onToolsRemoved));
        binding = binding.combine(session.on("CdpWebMCP.toolInvoked", this::onToolInvoked));
        binding = binding.combine(session.on("CdpWebMCP.toolResponded", this::onToolResponded));
    }

    /**
     * Initializes protocol state for this object.
     *
     * @return initialize value
     */
    public CompletableFuture<CdpPayload> initialize() {
        return CDPSession.sendIfPresent(session, "CdpWebMCP.enable", Map.of());
    }

    /**
     * Registers a listener for a Lancia CdpWebMCP extension event.
     *
     * @param event    event
     * @param listener listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        emitter.on(event, listener);
        return new EventBinding(() -> emitter.off(event, listener));
    }

    /**
     * Registers a tool through the Lancia CdpWebMCP extension domain.
     * <p>
     * This is intentionally a concrete {@code CdpWebMCP} method rather than a Puppeteer parity API.
     *
     * @param tool tool
     * @return add tool value
     */
    public CompletableFuture<CdpPayload> addTool(WebMCPTool tool) {
        return CDPSession.sendIfPresent(session, "CdpWebMCP.addTool", Map.of("tool", tool.toMap()));
    }

    /**
     * Removes a tool through the Lancia CdpWebMCP extension domain.
     * <p>
     * This is intentionally a concrete {@code CdpWebMCP} method rather than a Puppeteer parity API.
     *
     * @param name name
     * @return remove tool value
     */
    public CompletableFuture<CdpPayload> removeTool(String name) {
        return CDPSession.sendIfPresent(session, "CdpWebMCP.removeTool", Map.of("name", name));
    }

    /**
     * Replaces the registered tools through the Lancia CdpWebMCP extension domain.
     * <p>
     * This is intentionally a concrete {@code CdpWebMCP} method rather than a Puppeteer parity API.
     *
     * @param tools tools
     * @return set tools value
     */
    public CompletableFuture<CdpPayload> setTools(List<WebMCPTool> tools) {
        return CDPSession.sendIfPresent(
                session,
                "CdpWebMCP.setTools",
                Map.of("tools", tools.stream().map(WebMCPTool::toMap).toList()));
    }

    /**
     * Invokes a tool through the Lancia CdpWebMCP extension domain.
     * <p>
     * This is intentionally a concrete {@code CdpWebMCP} method rather than a Puppeteer parity API.
     *
     * @param tool  tool
     * @param input input
     * @return invoke tool value
     */
    public CompletableFuture<CdpPayload> invokeTool(WebMCPTool tool, Map<String, Object> input) {
        Assert.notNull(tool, "tool");
        return CDPSession.sendIfPresent(
                session,
                "CdpWebMCP.invokeTool",
                Map.of(
                        "frameId",
                        tool.frame().map(frame -> frame.id()).orElse(Normal.EMPTY),
                        "toolName",
                        tool.name(),
                        "input",
                        input == null ? Map.of() : input));
    }

    /**
     * Converts this value to ols.
     *
     * @return values
     */
    public List<WebMCPTool> tools() {
        List<WebMCPTool> result = new ArrayList<>();
        for (Map<String, WebMCPTool> frameTools : tools.values()) {
            result.addAll(frameTools.values());
        }
        return List.copyOf(result);
    }

    /**
     * Handles on tools added.
     *
     * @param params protocol parameters
     */
    private void onToolsAdded(CdpPayload params) {
        List<WebMCPTool> added = new ArrayList<>();
        if (params.get("tools").isArray()) {
            for (CdpPayload item : params.get("tools").elements()) {
                String frameId = PayloadReader.text(item.get("frameId"));
                CdpFrame frame = frame(frameId);
                if (frame == null) {
                    continue;
                }
                WebMCPTool tool = new WebMCPTool(this, item, frame);
                tools.computeIfAbsent(frame.id(), ignored -> new LinkedHashMap<>()).put(tool.name(), tool);
                added.add(tool);
            }
        }
        emitter.emit(TOOLS_ADDED, new WebMCPToolsEvent(added));
    }

    /**
     * Handles on tools removed.
     *
     * @param params protocol parameters
     */
    private void onToolsRemoved(CdpPayload params) {
        List<WebMCPTool> removed = new ArrayList<>();
        if (params.get("tools").isArray()) {
            for (CdpPayload item : params.get("tools").elements()) {
                String frameId = item.isObject() ? PayloadReader.text(item.get("frameId")) : Normal.EMPTY;
                String name = item.isObject() ? PayloadReader.text(item.get("name")) : PayloadReader.text(item);
                Map<String, WebMCPTool> frameTools = tools.get(frameId);
                if (frameTools == null && frameManager != null) {
                    frameTools = tools.get(frameManager.mainFrame().id());
                }
                WebMCPTool removedTool = frameTools == null ? null : frameTools.remove(name);
                if (removedTool != null) {
                    removed.add(removedTool);
                }
            }
        }
        emitter.emit(TOOLS_REMOVED, new WebMCPToolsEvent(removed));
    }

    /**
     * Handles on tool invoked.
     *
     * @param params protocol parameters
     */
    private void onToolInvoked(CdpPayload params) {
        String frameId = PayloadReader.text(params.get("frameId"));
        String toolName = PayloadReader.text(params.get("toolName"));
        WebMCPTool tool = tools.getOrDefault(frameId, Map.of()).get(toolName);
        if (tool == null && frameManager != null) {
            tool = tools.getOrDefault(frameManager.mainFrame().id(), Map.of()).get(toolName);
        }
        WebMCPToolCall call = new WebMCPToolCall(session, firstText(params, "invocationId", "id"), tool, toolName,
                firstPayload(params, "input", "arguments"));
        pendingCalls.put(call.id(), call);
        if (tool != null) {
            tool.emitInvoked(call);
        }
        emitter.emit(TOOL_INVOKED, call);
    }

    /**
     * Handles on tool responded.
     *
     * @param params protocol parameters
     */
    private void onToolResponded(CdpPayload params) {
        String invocationId = firstText(params, "invocationId", "id");
        WebMCPToolCall call = pendingCalls.remove(invocationId);
        emitter.emit(
                TOOL_RESPONDED,
                new WebMCPToolCallResult(invocationId, call, PayloadReader.text(params.get("status")),
                        params.get("output"), PayloadReader.text(params.get("errorText")), params.get("exception")));
    }

    /**
     * Returns the frame.
     *
     * @param frameId frame ID value
     * @return frame value
     */
    private CdpFrame frame(String frameId) {
        if (frameManager == null) {
            return null;
        }
        if (StringKit.isBlank(frameId)) {
            return frameManager.mainFrame();
        }
        return frameManager.frame(frameId);
    }

    /**
     * Returns the first text.
     *
     * @param params protocol parameters
     * @param names  names value
     * @return first text value
     */
    private String firstText(CdpPayload params, String... names) {
        return PayloadReader.text(firstPayload(params, names));
    }

    /**
     * Returns the first payload.
     *
     * @param params protocol parameters
     * @param names  names value
     * @return first payload value
     */
    private CdpPayload firstPayload(CdpPayload params, String... names) {
        for (String name : names) {
            CdpPayload value = params.get(name);
            if (!value.isNull()) {
                return value;
            }
        }
        return CdpPayload.NULL;
    }

    /**
     * Represents a web MCP tools event.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class WebMCPToolsEvent {

        /**
         * Registered tools values.
         */
        private final List<WebMCPTool> tools;

        /**
         * Creates an instance.
         *
         * @param tools tools value
         */
        public WebMCPToolsEvent(List<WebMCPTool> tools) {
            this.tools = tools == null ? List.of() : List.copyOf(tools);
        }

        /**
         * Converts this value to ols.
         *
         * @return values
         */
        public List<WebMCPTool> tools() {
            return tools;
        }
    }

    /**
     * Represents a web MCP tool call result.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class WebMCPToolCallResult {

        /**
         * Current identifier.
         */
        private final String id;
        /**
         * Current call.
         */
        private final WebMCPToolCall call;
        /**
         * Current status.
         */
        private final String status;
        /**
         * Current output.
         */
        private final CdpPayload output;
        /**
         * Current error text.
         */
        private final String errorText;
        /**
         * Current exception.
         */
        private final CdpPayload exception;

        /**
         * Creates an instance.
         *
         * @param id        identifier
         * @param call      call value
         * @param status    status value
         * @param output    output value
         * @param errorText error text value
         * @param exception exception value
         */
        public WebMCPToolCallResult(String id, WebMCPToolCall call, String status, CdpPayload output, String errorText,
                CdpPayload exception) {
            this.id = id == null ? Normal.EMPTY : id;
            this.call = call;
            this.status = StringKit.isBlank(status) ? "Completed" : status;
            this.output = output == null ? CdpPayload.NULL : output;
            this.errorText = errorText == null ? Normal.EMPTY : errorText;
            this.exception = exception == null ? CdpPayload.NULL : exception;
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
         * Returns the call.
         *
         * @return optional value
         */
        public Optional<WebMCPToolCall> call() {
            return Optional.ofNullable(call);
        }

        /**
         * Returns the status.
         *
         * @return status value
         */
        public String status() {
            return status;
        }

        /**
         * Returns the output.
         *
         * @return output value
         */
        public CdpPayload output() {
            return output;
        }

        /**
         * Returns the error text.
         *
         * @return error text value
         */
        public String errorText() {
            return errorText;
        }

        /**
         * Returns the exception.
         *
         * @return exception value
         */
        public CdpPayload exception() {
            return exception;
        }
    }

}
