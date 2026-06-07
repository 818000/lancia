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
package org.miaixz.lancia.kernel.bidi.protocol.message;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi response protocol message.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiResponseMessage {

    /**
     * Current type.
     */
    private final String type;
    /**
     * Current identifier.
     */
    private final int id;

    /**
     * Current result.
     */
    private final CdpPayload result;
    /**
     * Current error.
     */
    private final String error;
    /**
     * Current message.
     */
    private final String message;

    /**
     * Creates a bidi response message.
     *
     * @param type    type name
     * @param id      identifier
     * @param result  result value
     * @param error   error to propagate
     * @param message message text
     */
    public BidiResponseMessage(String type, int id, CdpPayload result, String error, String message) {
        this.type = type;
        this.id = id;
        this.result = result == null ? CdpPayload.NULL : result;
        this.error = error == null ? Normal.EMPTY : error;
        this.message = message == null ? Normal.EMPTY : message;
    }

    /**
     * Returns the from.
     *
     * @param payload protocol payload
     * @return from value
     */
    public static BidiResponseMessage from(CdpPayload payload) {
        return new BidiResponseMessage(PayloadReader.text(payload.get("type")), payload.get("id").asInt(),
                payload.get("result"), PayloadReader.text(payload.get("error")),
                PayloadReader.text(payload.get("message")));
    }

    /**
     * Returns the success.
     *
     * @return {@code true} when the condition matches
     */
    public boolean success() {
        return "success".equals(type);
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public int id() {
        return id;
    }

    /**
     * Returns the result.
     *
     * @return result value
     */
    public CdpPayload result() {
        return result;
    }

    /**
     * Returns the error.
     *
     * @return error value
     */
    public String error() {
        return error;
    }

    /**
     * Returns the message.
     *
     * @return message value
     */
    public String message() {
        return message;
    }

}
