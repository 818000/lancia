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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.net.url.UrlDecoder;
import org.miaixz.bus.core.net.url.UrlEncoder;
import org.miaixz.lancia.options.PDFOptions;
import org.miaixz.lancia.shared.protocol.ProtocolStreams;

/**
 * Global Lancia builder utilities.
 *
 * <p>
 * This class only carries shared constants and lightweight helpers. Browser session state does not belong here.
 * </p>
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Builder {

    /**
     * Default Lancia product name.
     */
    public static final String PRODUCT_NAME = "lancia";

    /**
     * Default protocol timeout in milliseconds.
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = Normal._30 * Normal.KILO;

    /**
     * Blank page URL.
     */
    public static final String ABOUT_BLANK = "about" + Symbol.COLON + "blank";

    /**
     * Default viewport settings.
     */
    public static final Map<String, Object> DEFAULT_VIEWPORT = Map.of("width", 800, "height", 600);

    /**
     * sourceURL comment pattern.
     */
    public static final Pattern SOURCE_URL_REGEX = Pattern
            .compile("^[\\x20\\t]*//[@#] sourceURL=\\s{0,10}(\\S*?)\\s{0,10}$", Pattern.MULTILINE);

    /**
     * Cached normalized JavaScript function sources.
     */
    private static final Map<String, String> CREATED_FUNCTIONS = new ConcurrentHashMap<>();

    /**
     * JavaScript function declaration pattern.
     */
    private static final Pattern FUNCTION_DECLARATION = Pattern
            .compile("^(async\\s+)*function(\\(|\\s).*", Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * JavaScript generator function declaration pattern.
     */
    private static final Pattern FUNCTION_GENERATOR = Pattern
            .compile("^(async\\s+)*function\\s*\\*\\s*.*", Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * JavaScript async arrow function pattern.
     */
    private static final Pattern ASYNC_ARROW = Pattern
            .compile("^async\\s*\\(.*", Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * JavaScript identifier arrow function pattern.
     */
    private static final Pattern IDENTIFIER_ARROW = Pattern.compile(
            "^(async)*\\s*[$_\\p{L}][$_\\p{L}\\p{N}\\u200C\\u200D]*\\s*=>.*",
            Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Prefix for async shorthand functions.
     */
    private static final String ASYNC_PREFIX = "async ";

    /**
     * Network idle time in milliseconds.
     */
    public static final long NETWORK_IDLE_TIME = 500L;

    /**
     * Hides the utility constructor.
     */
    private Builder() {
        // No initialization required.
    }

    /**
     * Returns whether the supplied object is a string.
     *
     * @param object object to test
     * @return {@code true} when the object behaves like a string
     */
    public static boolean isString(Object object) {
        return object instanceof CharSequence;
    }

    /**
     * Returns whether the supplied object is a number.
     *
     * @param object object to test
     * @return {@code true} when the object is a number
     */
    public static boolean isNumber(Object object) {
        return object instanceof Number;
    }

    /**
     * Returns whether the supplied object is a plain object.
     *
     * @param object object to test
     * @return {@code true} when the object is a plain map
     */
    public static boolean isPlainObject(Object object) {
        return object instanceof Map<?, ?>;
    }

    /**
     * Returns whether the supplied object is a regular expression.
     *
     * @param object object to test
     * @return {@code true} when the object is a regular expression
     */
    public static boolean isRegExp(Object object) {
        return object instanceof Pattern;
    }

    /**
     * Returns whether the supplied object is a date.
     *
     * @param object object to test
     * @return {@code true} when the object is date-like
     */
    public static boolean isDate(Object object) {
        return object instanceof Date || object instanceof java.time.temporal.Temporal;
    }

    /**
     * Asserts that a value is JavaScript-truthy.
     *
     * @param value to assert
     */
    public static void assertThat(Object value) {
        assertThat(value, Normal.EMPTY);
    }

    /**
     * Asserts that a value is JavaScript-truthy.
     *
     * @param value   value to assert
     * @param message failure message
     */
    public static void assertThat(Object value, String message) {
        if (!isTruthy(value)) {
            throw new InternalException(message == null ? Normal.EMPTY : message);
        }
    }

    /**
     * Guards an operation against disposed state.
     *
     * @param disposed disposed state
     * @param message  failure message
     */
    public static void guardDisposed(boolean disposed, String message) {
        if (disposed) {
            throw new InternalException(message == null ? "Object is disposed." : message);
        }
    }

    /**
     * Guards an operation against disconnected state.
     *
     * @param connected connected state
     * @param message   failure message
     */
    public static void guardConnected(boolean connected, String message) {
        if (!connected) {
            throw new InternalException(message == null ? "Object is disconnected." : message);
        }
    }

    /**
     * Guards a non-null value.
     *
     * @param <T>   value type
     * @param value to convert
     * @param name  value name
     * @return non-null value
     */
    public static <T> T guardNonNull(T value, String name) {
        if (value == null) {
            throw new InternalException((name == null || name.isBlank() ? "value" : name) + " is required.");
        }
        return value;
    }

    /**
     * Guards a protocol result.
     *
     * @param <T>     result type
     * @param result  protocol result
     * @param message failure message
     * @return protocol result
     */
    public static <T> T guardProtocolResult(T result, String message) {
        if (result == null) {
            throw new InternalException(message == null ? "Protocol command returned no result." : message);
        }
        if (result instanceof Payload payload && payload.isNull()) {
            throw new InternalException(message == null ? "Protocol command returned no result." : message);
        }
        return result;
    }

    /**
     * Returns whether the value is JavaScript-truthy.
     *
     * @param value to inspect
     * @return {@code true} when the value is truthy
     */
    public static boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            double numeric = number.doubleValue();
            return numeric != 0D && !Double.isNaN(numeric);
        }
        if (value instanceof CharSequence text) {
            return !text.isEmpty();
        }
        return true;
    }

    /**
     * Creates a cached JavaScript function source from a function source string.
     *
     * @param functionValue JavaScript function source
     * @return cached normalized function source
     */
    public static String createFunction(String functionValue) {
        String source = Assert.notBlank(functionValue, "functionValue");
        return CREATED_FUNCTIONS.computeIfAbsent(source, Builder::stringifyFunction);
    }

    /**
     * Returns a JavaScript function source that can be evaluated as a standalone function.
     *
     * @param functionSource JavaScript function source
     * @return standalone JavaScript function source
     */
    public static String stringifyFunction(String functionSource) {
        String value = Assert.notBlank(functionSource, "functionSource").trim();
        if (FUNCTION_DECLARATION.matcher(value).matches() || FUNCTION_GENERATOR.matcher(value).matches()) {
            return value;
        }
        if (isArrowFunction(value)) {
            return value;
        }
        String prefix = "function ";
        if (value.startsWith(ASYNC_PREFIX)) {
            prefix = ASYNC_PREFIX + prefix;
            value = value.substring(ASYNC_PREFIX.length());
        }
        return prefix + value;
    }

    /**
     * Replaces PLACEHOLDER calls with valid JavaScript replacement snippets.
     *
     * @param functionSource JavaScript function source
     * @param replacements   replacement snippets keyed by placeholder name
     * @return interpolated JavaScript function source
     */
    public static String interpolateFunction(String functionSource, Map<String, String> replacements) {
        Assert.notNull(replacements, "replacements");
        String value = stringifyFunction(functionSource);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String name = Assert.notBlank(entry.getKey(), "replacement name");
            String jsValue = entry.getValue() == null ? Normal.EMPTY : entry.getValue();
            Pattern placeholder = Pattern.compile(
                    "PLACEHOLDER\\(\\s*(?:'" + Pattern.quote(name) + "'|\"" + Pattern.quote(name) + "\")\\s*\\)");
            value = placeholder.matcher(value)
                    .replaceAll(Matcher.quoteReplacement(Symbol.PARENTHESE_LEFT + jsValue + Symbol.PARENTHESE_RIGHT));
        }
        return createFunction(value);
    }

    /**
     * Returns the current number of cached JavaScript function sources.
     *
     * @return cached function source count
     */
    public static int functionCacheSize() {
        return CREATED_FUNCTIONS.size();
    }

    /**
     * Clears cached JavaScript function sources.
     */
    public static void clearFunctionCache() {
        CREATED_FUNCTIONS.clear();
    }

    /**
     * Validates a JavaScript dialog type.
     *
     * @param type dialog type
     * @return valid dialog type
     */
    public static String validateDialogType(String type) {
        return switch (type) {
            case "alert", "confirm", "prompt", "beforeunload" -> type;
            default -> throw new InternalException("Unknown javascript dialog type: " + type);
        };
    }

    /**
     * Returns a sourceURL comment.
     *
     * @param url sourceURL
     * @return sourceURL comment
     */
    public static String getSourceUrlComment(String url) {
        return "//# sourceURL=" + (url == null ? Normal.EMPTY : url);
    }

    /**
     * Parses PDF options.
     *
     * @param options PDF options
     * @return parsed PDF options
     */
    public static PDFOptions.ParsedPDFOptions parsePDFOptions(PDFOptions options) {
        return (options == null ? new PDFOptions() : options).parse();
    }

    /**
     * Reads a protocol IO stream.
     *
     * @param session protocol session
     * @param handle  stream handle
     * @return stream bytes
     */
    public static byte[] readProtocolStream(Session session, String handle) {
        return ProtocolStreams.read(session, handle);
    }

    /**
     * Returns whether arrow function is enabled.
     *
     * @param value to use
     * @return {@code true} when the condition matches
     */
    private static boolean isArrowFunction(String value) {
        return value.startsWith(Symbol.PARENTHESE_LEFT) || ASYNC_ARROW.matcher(value).matches()
                || IDENTIFIER_ARROW.matcher(value).matches();
    }

    /**
     * Puppeteer sourceURL.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class URL {

        /**
         * Internal URL.
         */
        public static final String INTERNAL_URL = "pptr:internal";

        /**
         * Function name.
         */
        private final String functionName;

        /**
         * Call site string.
         */
        private final String siteString;

        /**
         * Creates a Puppeteer sourceURL.
         *
         * @param functionName function name
         * @param siteString   call site string
         */
        private URL(String functionName, String siteString) {
            this.functionName = functionName == null ? Normal.EMPTY : functionName;
            this.siteString = siteString == null ? Normal.EMPTY : siteString;
        }

        /**
         * Creates a Puppeteer sourceURL from the current call site.
         *
         * @param functionName function name
         * @return Puppeteer sourceURL
         */
        public static URL fromCallSite(String functionName) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String site = stack.length > 2 ? stack[2].toString() : Normal.EMPTY;
            return new URL(functionName, site);
        }

        /**
         * Parses a Puppeteer sourceURL.
         *
         * @param url sourceURL
         * @return Puppeteer sourceURL
         */
        public static URL parse(String url) {
            String value = url == null ? Normal.EMPTY : url;
            if (value.startsWith("pptr:")) {
                value = value.substring("pptr:".length());
            }
            String[] parts = value.split(Symbol.SEMICOLON, 2);
            String functionName = parts.length > 0 ? parts[0] : Normal.EMPTY;
            String site = parts.length > 1 ? decode(parts[1]) : Normal.EMPTY;
            return new URL(functionName, site);
        }

        /**
         * Returns whether the URL is Puppeteer URL.
         *
         * @param url target URL
         * @return {@code true} when the URL is a Puppeteer sourceURL
         */
        public static boolean isPuppeteerURL(String url) {
            return url != null && url.startsWith("pptr:");
        }

        /**
         * Returns the function name.
         *
         * @return function name
         */
        public String functionName() {
            return functionName;
        }

        /**
         * Returns the call site string.
         *
         * @return call site string
         */
        public String siteString() {
            return siteString;
        }

        /**
         * Converts this URL to a string.
         *
         * @return string value
         */
        @Override
        public String toString() {
            return "pptr:" + functionName + Symbol.SEMICOLON + encode(siteString);
        }

        /**
         * Encodes a URL segment.
         *
         * @param value raw value
         * @return encoded value
         */
        private static String encode(String value) {
            return UrlEncoder.encodeAll(value == null ? Normal.EMPTY : value, Charset.UTF_8);
        }

        /**
         * Decodes a URL segment.
         *
         * @param value encoded value
         * @return raw value
         */
        private static String decode(String value) {
            return UrlDecoder.decode(value == null ? Normal.EMPTY : value, Charset.UTF_8);
        }
    }

}
