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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a BiDi command protocol message.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiCommandMessage {

    /**
     * Current identifier.
     */
    private final int id;
    /**
     * Current method.
     */
    private final String method;
    /**
     * Mapped params values.
     */
    private final Map<String, Object> params;

    /**
     * Creates a bidi command message.
     *
     * @param id     identifier
     * @param method protocol method
     * @param params protocol parameters
     */
    public BidiCommandMessage(int id, String method, Map<String, Object> params) {
        this.id = id;
        this.method = method;
        this.params = params == null ? Map.of() : Map.copyOf(params);
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
     * Returns the method.
     *
     * @return method value
     */
    public String method() {
        return method;
    }

    /**
     * Returns the params.
     *
     * @return mapped values
     */
    public Map<String, Object> params() {
        return params;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", id);
        value.put("method", method);
        value.put("params", params);
        return value;
    }

}
