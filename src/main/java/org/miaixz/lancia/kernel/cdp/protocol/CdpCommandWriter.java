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
package org.miaixz.lancia.kernel.cdp.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.shared.protocol.TextWriter;

/**
 * Writes CDP command values for protocol transport.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpCommandWriter {

    /**
     * Creates a CDP command writer.
     */
    public CdpCommandWriter() {
        // No initialization required.
    }

    /**
     * Current text writer.
     */
    private final TextWriter textWriter = new TextWriter();

    /**
     * Returns the command.
     *
     * @param id        identifier
     * @param method    protocol method
     * @param sessionId session ID value
     * @param params    protocol parameters
     * @return command value
     */
    public String command(int id, String method, String sessionId, Map<String, Object> params) {
        Assert.notNull(method, "method");
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("id", id);
        command.put("method", method);
        if (StringKit.isNotBlank(sessionId)) {
            command.put("sessionId", sessionId);
        }
        if (params != null) {
            command.put("params", params);
        }
        return textWriter.writeValue(command);
    }

}
