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
 * Enumerates request interception decisions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum Verdict {

    /**
     * Aborts the intercepted request.
     */
    ABORT("abort"),

    /**
     * Responds to the intercepted request.
     */
    RESPOND("respond"),

    /**
     * Continues the intercepted request.
     */
    CONTINUE("continue"),

    /**
     * Marks interception as disabled.
     */
    DISABLED("disabled"),

    /**
     * Marks no interception action.
     */
    NONE("none"),

    /**
     * Marks the interception as already handled.
     */
    ALREADY_HANDLED("already-handled");

    /**
     * Protocol value sent with an interception decision.
     */
    private final String value;

    /**
     * Creates a verdict.
     *
     * @param value protocol value
     */
    Verdict(String value) {
        this.value = value;
    }

    /**
     * Returns the protocol token sent with this interception decision.
     *
     * @return protocol token
     */
    public String value() {
        return value;
    }

}
