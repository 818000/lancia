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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.mcp.WebMCPToolDefinition;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a tool in the Lancia-only CdpWebMCP extension domain.
 * <p>
 * This type is extension-specific and should not be treated as a Puppeteer parity API.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class WebMCPTool {

    /**
     * Current definition.
     */
    private final WebMCPToolDefinition definition;
    /**
     * Current CdpWebMCP facade.
     */
    private final CdpWebMCP webmcp;
    /**
     * Current frame.
     */
    private final Frame frame;
    /**
     * Current backend node ID.
     */
    private final Integer backendNodeId;
    /**
     * Mapped annotations values.
     */
    private final Map<String, Object> annotations;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();

    /**
     * Creates a web MCP tool.
     *
     * @param name        name to use
     * @param description description
     * @param inputSchema input schema
     */
    public WebMCPTool(String name, String description, Map<String, Object> inputSchema) {
        this.definition = new WebMCPToolDefinition(name, description, inputSchema);
        this.webmcp = null;
        this.frame = null;
        this.backendNodeId = null;
        this.annotations = Map.of();
    }

    /**
     * Creates a web MCP tool.
     *
     * @param webmcp webmcp
     * @param tool   tool
     * @param frame  frame instance
     */
    public WebMCPTool(CdpWebMCP webmcp, CdpPayload tool, Frame frame) {
        this.definition = new WebMCPToolDefinition(PayloadReader.text(tool.get("name")),
                PayloadReader.text(tool.get("description")), PayloadReader.object(tool.get("inputSchema")));
        this.webmcp = webmcp;
        this.frame = frame;
        this.backendNodeId = tool.get("backendNodeId").isNull() ? null : tool.get("backendNodeId").asInt();
        this.annotations = PayloadReader.object(tool.get("annotations"));
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        return definition.toMap();
    }

    /**
     * Returns the name.
     *
     * @return name value
     */
    public String name() {
        return String.valueOf(toMap().getOrDefault("name", Normal.EMPTY));
    }

    /**
     * Returns the description.
     *
     * @return description value
     */
    public String description() {
        return String.valueOf(toMap().getOrDefault("description", Normal.EMPTY));
    }

    /**
     * Returns the input schema.
     *
     * @return mapped values
     */
    public Map<String, Object> inputSchema() {
        Object schema = toMap().get("inputSchema");
        return schema instanceof Map<?, ?> map ? stringMap(map) : Map.of();
    }

    /**
     * Returns the annotations.
     *
     * @return mapped values
     */
    public Map<String, Object> annotations() {
        return annotations;
    }

    /**
     * Returns the frame.
     *
     * @return optional value
     */
    public Optional<Frame> frame() {
        return Optional.ofNullable(frame);
    }

    /**
     * Returns the backend node ID.
     *
     * @return optional value
     */
    public Optional<Integer> backendNodeId() {
        return Optional.ofNullable(backendNodeId);
    }

    /**
     * Invokes this tool through the Lancia CdpWebMCP extension domain.
     * <p>
     * This helper is extension-specific and has no Puppeteer public API equivalent.
     *
     * @param input input
     * @return execute value
     */
    public CompletableFuture<CdpPayload> execute(Map<String, Object> input) {
        if (webmcp == null) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return webmcp.invokeTool(this, input);
    }

    /**
     * Registers a listener for extension tool invocation events.
     * <p>
     * This helper is extension-specific and has no Puppeteer public API equivalent.
     *
     * @param listener listener
     * @return on tool invoked value
     */
    public Binding onToolInvoked(Consumer<WebMCPToolCall> listener) {
        Consumer<Object> bridge = payload -> listener.accept((WebMCPToolCall) payload);
        emitter.on(CdpWebMCP.TOOL_INVOKED, bridge);
        return new EventBinding(() -> emitter.off(CdpWebMCP.TOOL_INVOKED, bridge));
    }

    /**
     * Handles emit invoked.
     *
     * @param call call value
     */
    public void emitInvoked(WebMCPToolCall call) {
        emitter.emit(CdpWebMCP.TOOL_INVOKED, call);
    }

    /**
     * Returns the string map.
     *
     * @param map map value
     * @return mapped values
     */
    private static Map<String, Object> stringMap(Map<?, ?> map) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            result.put(String.valueOf(entry.getKey()), value instanceof CdpPayload payload ? payload.raw() : value);
        }
        return Map.copyOf(result);
    }

}
