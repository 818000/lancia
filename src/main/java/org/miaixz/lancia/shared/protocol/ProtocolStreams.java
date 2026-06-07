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
package org.miaixz.lancia.shared.protocol;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.lancia.Payload;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Provides protocol stream reading helpers.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ProtocolStreams {

    /**
     * Hides the protocol streams constructor.
     */
    private ProtocolStreams() {
        // No initialization required.
    }

    /**
     * Reads a protocol IO stream.
     *
     * @param session protocol session
     * @param handle  stream handle
     * @return stream bytes
     */
    public static byte[] read(Session session, String handle) {
        return read(session, handle, ResourceLimits.defaults());
    }

    /**
     * Reads a protocol IO stream.
     *
     * @param session        protocol session
     * @param handle         stream handle
     * @param resourceLimits resource limits
     * @return stream bytes
     */
    public static byte[] read(Session session, String handle, ResourceLimits resourceLimits) {
        ResourceLimits actualResourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            boolean eof = false;
            while (!eof) {
                Payload chunk = session.send("IO.read", Map.of("handle", handle)).get(5, TimeUnit.SECONDS);
                String data = PayloadReader.text(chunk.get("data"));
                boolean base64Encoded = PayloadReader.bool(chunk.get("base64Encoded"));
                if (!data.isEmpty()) {
                    output.writeBytes(base64Encoded ? Base64.decode(data) : ByteKit.toBytes(data, Charset.UTF_8));
                    actualResourceLimits.validateProtocolStreamBytes(output.size());
                }
                eof = !chunk.get("eof").isNull() && chunk.get("eof").asBoolean();
            }
            return output.toByteArray();
        } catch (Exception ex) {
            throw new InternalException("Failed to read protocol stream: " + handle, ex);
        } finally {
            session.send("IO.close", Map.of("handle", handle));
        }
    }

}
