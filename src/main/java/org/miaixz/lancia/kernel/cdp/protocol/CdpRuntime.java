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
package org.miaixz.lancia.kernel.cdp.protocol;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.payload.StackTraceReader;
import org.miaixz.lancia.shared.protocol.TextWriter;

/**
 * Builds CDP runtime values, errors, and page binding bootstrap scripts.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CdpRuntime {

    /**
     * Shared constant for CDP binding prefix.
     */
    public static final String CDP_BINDING_PREFIX = "puppeteer_";
    /**
     * Shared constant for add page binding source.
     */
    private static final String ADD_PAGE_BINDING_SOURCE = Normal.EMPTY + "function addPageBinding(type,name,prefix){"
            + "if(globalThis[name]){return;}" + "Object.assign(globalThis,{[name](...args){"
            + "const callPuppeteer=globalThis[name];" + "callPuppeteer.args??=new Map();"
            + "callPuppeteer.callbacks??=new Map();" + "const seq=(callPuppeteer.lastSeq??0)+1;"
            + "callPuppeteer.lastSeq=seq;" + "callPuppeteer.args.set(seq,args);"
            + "globalThis[prefix+name](JSON.stringify({"
            + "type,name,seq,args,isTrivial:!args.some(value=>value instanceof Node)" + "}));"
            + "return new Promise((resolve,reject)=>{" + "callPuppeteer.callbacks.set(seq,{"
            + "resolve(value){callPuppeteer.args.delete(seq);resolve(value);},"
            + "reject(value){callPuppeteer.args.delete(seq);reject(value);}" + "});" + "});" + "}});"
            + Symbol.BRACE_RIGHT;
    /**
     * Shared constant for text writer.
     */
    private static final TextWriter TEXT_WRITER = new TextWriter();

    /**
     * Creates a CDP runtime.
     */
    private CdpRuntime() {
        // No initialization required.
    }

    /**
     * Creates evaluation error.
     *
     * @param details exception details
     * @return created evaluation error
     */
    public static Object createEvaluationError(CdpPayload details) {
        return createErrorOrPrimitive(details, false);
    }

    /**
     * Creates client error.
     *
     * @param details exception details
     * @return created client error
     */
    public static Object createClientError(CdpPayload details) {
        return createErrorOrPrimitive(details, true);
    }

    /**
     * Returns the value from primitive remote object.
     *
     * @param remoteObject remote object payload
     * @return value from primitive remote object value
     */
    public static Object valueFromPrimitiveRemoteObject(CdpPayload remoteObject) {
        CdpPayload actualObject = remoteObject == null ? CdpPayload.NULL : remoteObject;
        if (StringKit.isNotBlank(PayloadReader.text(actualObject.get("objectId")))) {
            throw new InternalException("Cannot extract value when objectId is given");
        }
        CdpPayload unserializable = actualObject.get("unserializableValue");
        if (!unserializable.isNull()) {
            return unserializableValue(
                    PayloadReader.text(actualObject.get("type")),
                    PayloadReader.text(unserializable));
        }
        CdpPayload value = actualObject.get("value");
        return value.isNull() ? null : value.raw();
    }

    /**
     * Returns the page binding init string.
     *
     * @param type type to use
     * @param name name to use
     * @return page binding init string value
     */
    public static String pageBindingInitString(String type, String name) {
        return evaluationString(ADD_PAGE_BINDING_SOURCE, type, name, CDP_BINDING_PREFIX);
    }

    /**
     * Converts console message level.
     *
     * @param method protocol method
     * @return converted console message level
     */
    public static String convertConsoleMessageLevel(String method) {
        return switch (method == null ? Normal.EMPTY : method) {
            case "warning" -> "warn";
            default -> method == null ? Normal.EMPTY : method;
        };
    }

    /**
     * Returns the stack trace locations.
     *
     * @param stackTrace stack trace
     * @return stack trace locations
     */
    public static List<StackTraceLocation> getStackTraceLocations(CdpPayload stackTrace) {
        return StackTraceReader.locations(stackTrace, StackTraceLocation::new);
    }

    /**
     * Creates error or primitive.
     *
     * @param details    exception details
     * @param clientMode client mode value
     * @return create error or primitive value
     */
    private static Object createErrorOrPrimitive(CdpPayload details, boolean clientMode) {
        CdpPayload actualDetails = details == null ? CdpPayload.NULL : details;
        CdpPayload exception = actualDetails.get("exception");
        if (!exception.isNull()
                && (!"object".equals(PayloadReader.text(exception.get("type")))
                        || !"error".equals(PayloadReader.text(exception.get("subtype"))))
                && StringKit.isBlank(PayloadReader.text(exception.get("objectId")))) {
            return valueFromPrimitiveRemoteObject(exception);
        }
        ErrorDetails errorDetails = getErrorDetails(actualDetails);
        String message = StringKit.isBlank(errorDetails.message()) ? PayloadReader.text(actualDetails.get("text"))
                : errorDetails.message();
        String name = StringKit.isBlank(errorDetails.name()) ? "Error" : errorDetails.name();
        return new InternalException(name + ": " + message + stackSuffix(actualDetails.get("stackTrace"), clientMode));
    }

    /**
     * Returns the error details.
     *
     * @param details exception details
     * @return error details
     */
    private static ErrorDetails getErrorDetails(CdpPayload details) {
        CdpPayload exception = details == null ? CdpPayload.NULL : details.get("exception");
        String name = PayloadReader.text(exception.get("className"));
        String description = PayloadReader.text(exception.get("description"));
        if (StringKit.isBlank(description)) {
            return new ErrorDetails(name, PayloadReader.text(details.get("text")));
        }
        List<String> lines = new ArrayList<>(List.of(description.split("\\n    at ")));
        int stackSize = Math
                .min(getStackTraceLocations(details.get("stackTrace")).size(), Math.max(Normal._0, lines.size() - 1));
        for (int index = Normal._0; index < stackSize; index++) {
            lines.remove(lines.size() - 1);
        }
        String message = String.join(Symbol.LF, lines);
        if (StringKit.isNotBlank(name) && message.startsWith(name + ": ")) {
            message = message.substring(name.length() + 2);
        }
        return new ErrorDetails(name, message);
    }

    /**
     * Returns the stack suffix.
     *
     * @param stackTrace stack trace value
     * @param clientMode client mode value
     * @return stack suffix value
     */
    private static String stackSuffix(CdpPayload stackTrace, boolean clientMode) {
        List<StackTraceLocation> locations = getStackTraceLocations(stackTrace);
        if (locations.isEmpty()) {
            return Normal.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        for (StackTraceLocation location : locations) {
            int line = clientMode ? location.lineNumber() + 1 : location.lineNumber();
            int column = clientMode ? location.columnNumber() + 1 : location.columnNumber();
            builder.append(Symbol.C_LF).append("    at ").append(location.url()).append(Symbol.C_COLON).append(line)
                    .append(Symbol.C_COLON).append(column);
        }
        return builder.toString();
    }

    /**
     * Returns the evaluation string.
     *
     * @param functionSource function source
     * @param args           arguments to pass
     * @return evaluation string value
     */
    private static String evaluationString(String functionSource, Object... args) {
        StringBuilder builder = new StringBuilder();
        builder.append(Symbol.C_PARENTHESE_LEFT).append(functionSource).append(")(");
        for (int index = Normal._0; index < args.length; index++) {
            if (index > Normal._0) {
                builder.append(Symbol.C_COMMA);
            }
            builder.append(TEXT_WRITER.writeValue(args[index]));
        }
        builder.append(Symbol.C_PARENTHESE_RIGHT);
        return builder.toString();
    }

    /**
     * Returns the unserializable value.
     *
     * @param type  type to use
     * @param value to use
     * @return unserializable value
     */
    private static Object unserializableValue(String type, String value) {
        if ("bigint".equals(type) && value.endsWith("n")) {
            return new BigInteger(value.substring(Normal._0, value.length() - 1));
        }
        return unserializableValue(value);
    }

    /**
     * Returns the unserializable value.
     *
     * @param value to use
     * @return unserializable value
     */
    private static Object unserializableValue(String value) {
        return switch (value) {
            case "-0" -> -0.0d;
            case "NaN" -> Double.NaN;
            case "Infinity" -> Double.POSITIVE_INFINITY;
            case "-Infinity" -> Double.NEGATIVE_INFINITY;
            default -> value;
        };
    }

    /**
     * Stores normalized runtime exception details.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class ErrorDetails {

        /**
         * Current name.
         */
        private final String name;
        /**
         * Current message.
         */
        private final String message;

        /**
         * Creates an instance.
         *
         * @param name    name to use
         * @param message message text
         */
        private ErrorDetails(String name, String message) {
            this.name = name == null ? Normal.EMPTY : name;
            this.message = message == null ? Normal.EMPTY : message;
        }

        /**
         * Returns the name.
         *
         * @return name value
         */
        private String name() {
            return name;
        }

        /**
         * Returns the message.
         *
         * @return message value
         */
        private String message() {
            return message;
        }
    }

    /**
     * Represents a runtime stack trace location.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class StackTraceLocation {

        /**
         * Current URL.
         */
        private final String url;
        /**
         * Current line number.
         */
        private final int lineNumber;
        /**
         * Current column number.
         */
        private final int columnNumber;

        /**
         * Creates an instance.
         *
         * @param url          target URL
         * @param lineNumber   line number value
         * @param columnNumber column number value
         */
        public StackTraceLocation(String url, int lineNumber, int columnNumber) {
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
        public int lineNumber() {
            return lineNumber;
        }

        /**
         * Returns the column number.
         *
         * @return column number value
         */
        public int columnNumber() {
            return columnNumber;
        }
    }

}
