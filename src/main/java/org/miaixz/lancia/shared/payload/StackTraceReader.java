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

import java.util.ArrayList;
import java.util.List;

import org.miaixz.lancia.Payload;

/**
 * Reads protocol stack trace payloads.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class StackTraceReader {

    /**
     * Prevents instantiation.
     */
    private StackTraceReader() {
        // No initialization required.
    }

    /**
     * Reads stack trace locations.
     *
     * @param stackTrace stack trace
     * @param factory    location factory
     * @param <T>        location type
     * @return locations
     */
    public static <T> List<T> locations(Payload stackTrace, LocationFactory<T> factory) {
        Payload frames = stackTrace == null ? null : stackTrace.get("callFrames");
        if (!PayloadReader.present(frames) || !frames.isArray() || factory == null) {
            return List.of();
        }
        List<T> locations = new ArrayList<>();
        for (Payload frame : PayloadReader.elements(frames)) {
            locations.add(
                    factory.create(
                            PayloadReader.text(frame.get("url")),
                            PayloadReader.number(frame.get("lineNumber")),
                            PayloadReader.number(frame.get("columnNumber"))));
        }
        return List.copyOf(locations);
    }

    /**
     * Creates a stack trace location.
     *
     * @param <T> location type
     */
    @FunctionalInterface
    public interface LocationFactory<T> {

        /**
         * Creates a location.
         *
         * @param url          url
         * @param lineNumber   line number
         * @param columnNumber column number
         * @return location
         */
        T create(String url, int lineNumber, int columnNumber);
    }

}
