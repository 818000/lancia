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
package org.miaixz.lancia;

/**
 * Public view of a protocol payload.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Payload {

    /**
     * Returns a named child payload.
     *
     * @param name child name
     * @return child payload
     */
    Payload get(String name);

    /**
     * Returns the payload as text.
     *
     * @return text value
     */
    String asText();

    /**
     * Returns the payload as an integer.
     *
     * @return integer value
     */
    int asInt();

    /**
     * Returns the payload as a double.
     *
     * @return double value
     */
    double asDouble();

    /**
     * Returns the payload as a boolean.
     *
     * @return boolean value
     */
    boolean asBoolean();

    /**
     * Returns whether object is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isObject();

    /**
     * Returns whether array is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isArray();

    /**
     * Returns whether null is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isNull();

    /**
     * Returns the raw payload content.
     *
     * @return raw payload content
     */
    Object raw();

}
