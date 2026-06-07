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

import java.util.ArrayList;
import java.util.List;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.lancia.kernel.bidi.page.BidiElementHandle;
import org.miaixz.lancia.kernel.bidi.page.BidiJSHandle;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.payload.StackTraceReader;

/**
 * Maps WebDriver BiDi log payloads to runtime message data.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiLogMapper {

    /**
     * Creates a bidi log mapper.
     */
    private BidiLogMapper() {
        // No initialization required.
    }

    /**
     * Returns the message level.
     *
     * @param method protocol method
     * @return message level value
     */
    public static String messageLevel(String method) {
        return switch (method == null ? Normal.EMPTY : method) {
            case "group" -> "startGroup";
            case "groupCollapsed" -> "startGroupCollapsed";
            case "groupEnd" -> "endGroup";
            default -> method == null ? Normal.EMPTY : method;
        };
    }

    /**
     * Returns the stack trace locations.
     *
     * @param stackTrace stack trace value
     * @return values
     */
    public static List<StackTraceLocation> stackTraceLocations(CdpPayload stackTrace) {
        return StackTraceReader.locations(stackTrace, StackTraceLocation::new);
    }

    /**
     * Returns the console message.
     *
     * @param entry    entry value
     * @param args     arguments to pass
     * @param frame    frame instance
     * @param targetId target ID value
     * @return console message value
     */
    public static ConsoleMessageData consoleMessage(CdpPayload entry, List<?> args, Object frame, String targetId) {
        List<?> actualArgs = args == null ? List.of() : args;
        List<String> parts = new ArrayList<>();
        for (Object arg : actualArgs) {
            if (arg instanceof BidiJSHandle handle && handle.isPrimitiveValue()) {
                parts.add(String.valueOf(BidiDeserializer.deserialize(handle.remoteValue())));
            } else if (arg instanceof BidiElementHandle handle && handle.isPrimitiveValue()) {
                parts.add(String.valueOf(BidiDeserializer.deserialize(handle.remoteValue())));
            } else {
                parts.add(String.valueOf(arg));
            }
        }
        return new ConsoleMessageData(messageLevel(PayloadReader.text(entry.get("method"))),
                String.join(Symbol.SPACE, parts), actualArgs, stackTraceLocations(entry.get("stackTrace")), frame,
                targetId);
    }

    /**
     * Returns whether the log entry is a console message.
     *
     * @param event event name
     * @return {@code true} when the condition matches
     */
    public static boolean isConsoleLogEntry(CdpPayload event) {
        return "console".equals(PayloadReader.text(event.get("type")));
    }

    /**
     * Returns whether the log entry is a JavaScript exception.
     *
     * @param event log entry payload
     * @return {@code true} when the entry is a JavaScript exception
     */
    public static boolean isJavaScriptLogEntry(CdpPayload event) {
        return "javascript".equals(PayloadReader.text(event.get("type")));
    }

    /**
     * Carries the StackTraceLocation data.
     *
     * @param url          url
     * @param lineNumber   line number
     * @param columnNumber column number
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public record StackTraceLocation(String url, int lineNumber, int columnNumber) {
    }

    /**
     * Carries the ConsoleMessageData data.
     *
     * @param type      type
     * @param text      text
     * @param args      args
     * @param locations locations
     * @param frame     frame
     * @param targetId  target id
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public record ConsoleMessageData(String type, String text, List<?> args, List<StackTraceLocation> locations,
            Object frame, String targetId) {
    }

}
