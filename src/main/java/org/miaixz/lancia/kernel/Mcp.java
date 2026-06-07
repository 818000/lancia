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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.miaixz.lancia.Payload;

/**
 * Controls the Lancia WebMCP integration.
 * <p>
 * This interface is intentionally narrow and is not part of the Puppeteer parity surface. WebMCP is retained as a
 * Lancia-only extension; implementation-specific tool registration and tool-call response helpers live on
 * {@code CdpWebMCP}.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Mcp {

    /**
     * Initializes the Lancia WebMCP protocol domain.
     *
     * @return protocol result
     */
    CompletableFuture<? extends Payload> initialize();

    /**
     * Returns registered MCP tools exposed by the Lancia extension domain.
     *
     * @return registered MCP tools
     */
    List<?> tools();

}
