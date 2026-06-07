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

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;

/**
 * Represents a pending call in the Lancia-only CdpWebMCP extension domain.
 * <p>
 * Tool-call response helpers are retained as Lancia extension APIs and are not part of the Puppeteer parity surface.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class WebMCPToolCall {

    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current tool name.
     */
    private final String toolName;
    /**
     * Current tool.
     */
    private final WebMCPTool tool;
    /**
     * Current arguments.
     */
    private final CdpPayload arguments;

    /**
     * Creates a web MCP tool call.
     *
     * @param session   protocol session
     * @param id        identifier
     * @param toolName  tool name
     * @param arguments arguments to pass
     */
    public WebMCPToolCall(CDPSession session, String id, String toolName, CdpPayload arguments) {
        this(session, id, null, toolName, arguments);
    }

    /**
     * Creates a web MCP tool call.
     *
     * @param session   protocol session
     * @param id        identifier
     * @param tool      tool
     * @param toolName  tool name
     * @param arguments arguments to pass
     */
    public WebMCPToolCall(CDPSession session, String id, WebMCPTool tool, String toolName, CdpPayload arguments) {
        this.session = session;
        this.id = id;
        this.tool = tool;
        this.toolName = toolName;
        this.arguments = arguments;
    }

    /**
     * Responds to a Lancia CdpWebMCP tool call.
     * <p>
     * This helper is extension-specific and has no Puppeteer public API equivalent.
     *
     * @param result evaluated result
     * @return response result
     */
    public CompletableFuture<CdpPayload> respond(Object result) {
        return session.send("CdpWebMCP.respondToToolCall", Map.of("id", id, "result", result));
    }

    /**
     * Cancels a Lancia CdpWebMCP tool call.
     * <p>
     * This helper is extension-specific and has no Puppeteer public API equivalent.
     *
     * @param errorText error text
     * @return {@code true} when the condition matches
     */
    public CompletableFuture<CdpPayload> cancel(String errorText) {
        return session.send(
                "CdpWebMCP.respondToToolCall",
                Map.of("id", id, "status", "Canceled", "errorText", errorText == null ? Normal.EMPTY : errorText));
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
     * Converts this value to ol name.
     *
     * @return ol name
     */
    public String toolName() {
        return toolName;
    }

    /**
     * Converts this value to ol.
     *
     * @return optional value
     */
    public Optional<WebMCPTool> tool() {
        return Optional.ofNullable(tool);
    }

    /**
     * Returns the arguments.
     *
     * @return arguments value
     */
    public CdpPayload arguments() {
        return arguments;
    }

}
