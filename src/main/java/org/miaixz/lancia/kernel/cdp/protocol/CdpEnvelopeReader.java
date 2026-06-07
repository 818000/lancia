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

import org.miaixz.bus.core.lang.exception.ProtocolException;

/**
 * Reads CDP envelope protocol payload values.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpEnvelopeReader {

    /**
     * Creates a CDP envelope reader.
     */
    public CdpEnvelopeReader() {
        // No initialization required.
    }

    /**
     * Returns the read.
     *
     * @param text text to use
     * @return read value
     */
    public CdpEnvelope read(String text) {
        CdpPayload payload = CdpPayload.parse(text);
        if (!payload.isObject()) {
            throw new ProtocolException("CDP message must be a JSON object.");
        }
        CdpPayload idPayload = payload.get("id");
        Integer id = idPayload.isNull() ? null : idPayload.asInt();
        CdpPayload methodPayload = payload.get("method");
        String method = methodPayload.isNull() ? null : methodPayload.asText();
        CdpPayload sessionPayload = payload.get("sessionId");
        String sessionId = sessionPayload.isNull() ? null : sessionPayload.asText();
        return new CdpEnvelope(id, method, sessionId, payload.get("params"), payload.get("result"),
                payload.get("error"));
    }

}
