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
package org.miaixz.lancia.nimble.input;

import java.util.List;
import java.util.Map;

/**
 * Represents a drag data value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class DragData {

    /**
     * Current start.
     */
    private final DragPoint start;
    /**
     * Current end.
     */
    private final DragPoint end;

    /**
     * Creates a drag data.
     *
     * @param start start
     * @param end   end
     */
    public DragData(DragPoint start, DragPoint end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        return Map.of("items", List.of(), "dragOperationsMask", 1);
    }

    /**
     * Returns the start.
     *
     * @return start value
     */
    public DragPoint start() {
        return start;
    }

    /**
     * Returns the end.
     *
     * @return end value
     */
    public DragPoint end() {
        return end;
    }

}
