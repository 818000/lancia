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
package org.miaixz.lancia.shared.payload;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.lancia.Payload;

/**
 * Provides a null payload object.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
enum NullPayload implements Payload {

    /**
     * null payload instance.
     */
    INSTANCE;

    /**
     * Returns a named child payload.
     *
     * @param name child name
     * @return child payload
     */
    public Payload get(String name) {
        return this;
    }

    /**
     * Returns the payload as text.
     *
     * @return text value
     */
    public String asText() {
        return Normal.EMPTY;
    }

    /**
     * Returns the payload as an integer.
     *
     * @return integer value
     */
    public int asInt() {
        return 0;
    }

    /**
     * Returns the payload as a double.
     *
     * @return double value
     */
    public double asDouble() {
        return 0;
    }

    /**
     * Returns the payload as a boolean.
     *
     * @return boolean value
     */
    public boolean asBoolean() {
        return false;
    }

    /**
     * Returns whether this payload is an object.
     *
     * @return object state
     */
    public boolean isObject() {
        return false;
    }

    /**
     * Returns whether this payload is an array.
     *
     * @return array state
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Returns whether this payload is a null.
     *
     * @return null state
     */
    public boolean isNull() {
        return true;
    }

    /**
     * Returns raw payload.
     *
     * @return raw value
     */
    public Object raw() {
        return null;
    }

}
