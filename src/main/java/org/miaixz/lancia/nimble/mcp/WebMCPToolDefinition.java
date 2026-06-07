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
package org.miaixz.lancia.nimble.mcp;

import java.util.Map;

/**
 * Represents a web MCP tool definition value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class WebMCPToolDefinition {

    /**
     * Current name.
     */
    private final String name;
    /**
     * Current description.
     */
    private final String description;
    /**
     * Mapped input schema values.
     */
    private final Map<String, Object> inputSchema;

    /**
     * Creates a web MCP tool definition.
     *
     * @param name        name to use
     * @param description description
     * @param inputSchema input schema
     */
    public WebMCPToolDefinition(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        return Map.of("name", name, "description", description, "inputSchema", inputSchema);
    }

}
