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
package org.miaixz.lancia.shared.page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Payload;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a console message emitted by the page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ConsoleMessage {

    /**
     * Current type.
     */
    private final String type;
    /**
     * Current text.
     */
    private final String text;
    /**
     * Registered args values.
     */
    private final List<Handle> args;
    /**
     * Registered stack trace locations values.
     */
    private final List<ConsoleMessageLocation> stackTraceLocations;
    /**
     * Current frame.
     */
    private final Frame frame;
    /**
     * Current raw stack trace.
     */
    private final Payload rawStackTrace;
    /**
     * Current target ID.
     */
    private final String targetId;

    /**
     * Creates a console message.
     *
     * @param type type name
     * @param text text to use
     * @param args arguments to pass
     */
    public ConsoleMessage(String type, String text, List<? extends Handle> args) {
        this(type, text, args, List.of(), null, null, null);
    }

    /**
     * Creates a console message.
     *
     * @param type                type name
     * @param text                text to use
     * @param args                arguments to pass
     * @param stackTraceLocations stack trace locations
     * @param frame               frame instance
     * @param rawStackTrace       raw stack trace
     * @param targetId            target id
     */
    public ConsoleMessage(String type, String text, List<? extends Handle> args,
            List<ConsoleMessageLocation> stackTraceLocations, Frame frame, Payload rawStackTrace, String targetId) {
        this.type = type == null ? Normal.EMPTY : type;
        this.text = text == null ? Normal.EMPTY : text;
        this.args = args == null ? List.of() : List.copyOf(args);
        this.stackTraceLocations = stackTraceLocations == null ? List.of() : List.copyOf(stackTraceLocations);
        this.frame = frame;
        this.rawStackTrace = rawStackTrace;
        this.targetId = StringKit.isBlank(targetId) ? null : targetId;
    }

    /**
     * Returns the locations from stack trace.
     *
     * @param stackTrace stack trace value
     * @return values
     */
    public static List<ConsoleMessageLocation> locationsFromStackTrace(Payload stackTrace) {
        if (stackTrace == null || stackTrace.isNull()) {
            return List.of();
        }
        List<ConsoleMessageLocation> locations = new ArrayList<>();
        for (Payload frame : PayloadReader.elements(stackTrace.get("callFrames"))) {
            locations.add(
                    new ConsoleMessageLocation(PayloadReader.text(frame.get("url")),
                            PayloadReader.numberObject(frame.get("lineNumber")),
                            PayloadReader.numberObject(frame.get("columnNumber"))));
        }
        return List.copyOf(locations);
    }

    /**
     * Handles dispose args.
     */
    public void disposeArgs() {
        for (Handle arg : args) {
            arg.dispose();
        }
    }

    /**
     * Returns the type.
     *
     * @return type value
     */
    public String type() {
        return type;
    }

    /**
     * Returns the text.
     *
     * @return text value
     */
    public String text() {
        return text;
    }

    /**
     * Returns the args.
     *
     * @return values
     */
    public List<Handle> args() {
        return args;
    }

    /**
     * Returns the location.
     *
     * @return location value
     */
    public ConsoleMessageLocation location() {
        if (!stackTraceLocations.isEmpty()) {
            return stackTraceLocations.get(Normal._0);
        }
        if (frame != null) {
            return new ConsoleMessageLocation(frame.url(), null, null);
        }
        return new ConsoleMessageLocation(Normal.EMPTY, null, null);
    }

    /**
     * Returns the stack trace.
     *
     * @return values
     */
    public List<ConsoleMessageLocation> stackTrace() {
        return stackTraceLocations;
    }

    /**
     * Returns the raw stack trace.
     *
     * @return raw stack trace value
     */
    public Payload rawStackTrace() {
        return rawStackTrace;
    }

    /**
     * Returns the raw stack trace.
     *
     * @return raw stack trace value
     */
    public Payload _rawStackTrace() {
        return rawStackTrace();
    }

    /**
     * Returns the target ID.
     *
     * @return target ID value
     */
    public String targetId() {
        return targetId;
    }

    /**
     * Returns the target ID.
     *
     * @return target ID value
     */
    public String _targetId() {
        return targetId();
    }

    /**
     * Returns the frame.
     *
     * @return frame value
     */
    public Frame frame() {
        return frame;
    }

    /**
     * Represents a console message location.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class ConsoleMessageLocation {

        /**
         * Current URL.
         */
        private final String url;
        /**
         * Current line number.
         */
        private final Integer lineNumber;
        /**
         * Current column number.
         */
        private final Integer columnNumber;

        /**
         * Creates an instance.
         *
         * @param url          target URL
         * @param lineNumber   line number value
         * @param columnNumber column number value
         */
        public ConsoleMessageLocation(String url, Integer lineNumber, Integer columnNumber) {
            this.url = url == null ? Normal.EMPTY : url;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        public String url() {
            return url;
        }

        /**
         * Returns the line number.
         *
         * @return line number value
         */
        public Integer lineNumber() {
            return lineNumber;
        }

        /**
         * Returns the column number.
         *
         * @return column number value
         */
        public Integer columnNumber() {
            return columnNumber;
        }

        /**
         * Converts this value to protocol parameters.
         *
         * @return protocol parameters
         */
        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            if (StringKit.isNotBlank(url)) {
                result.put("url", url);
            }
            if (lineNumber != null) {
                result.put("lineNumber", lineNumber);
            }
            if (columnNumber != null) {
                result.put("columnNumber", columnNumber);
            }
            return result;
        }
    }

}
