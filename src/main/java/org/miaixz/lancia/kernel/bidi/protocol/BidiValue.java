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
package org.miaixz.lancia.kernel.bidi.protocol;

import org.miaixz.lancia.Payload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Reads WebDriver BiDi remote values.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiValue {

    /**
     * Prevents instantiation.
     */
    private BidiValue() {
        // No initialization required.
    }

    /**
     * Returns the primitive.
     *
     * @param remoteValue remote value
     * @return {@code true} when the condition matches
     */
    public static boolean primitive(Payload remoteValue) {
        return switch (PayloadReader.text(remoteValue.get("type"))) {
            case "string", "number", "bigint", "boolean", "undefined", "null" -> true;
            default -> false;
        };
    }

}
