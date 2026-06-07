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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.ProtocolException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Payload;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.shared.protocol.ValueCodec;

/**
 * Represents a CDP payload.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CdpPayload implements Payload {

    /**
     * Creates a CDP payload.
     */
    public static final CdpPayload NULL = new CdpPayload(null);
    /**
     * Current value.
     */
    private final Object value;

    /**
     * Creates a CDP payload.
     *
     * @param value to use
     */
    private CdpPayload(Object value) {
        this.value = value;
    }

    /**
     * Returns the of.
     *
     * @param value to use
     * @return of value
     */
    public static CdpPayload of(Object value) {
        if (value == null) {
            return NULL;
        }
        if (value instanceof CdpPayload payload) {
            return payload;
        }
        return ofEncoded(ValueCodec.encode(value));
    }

    /**
     * Returns the of encoded.
     *
     * @param value to use
     * @return of encoded value
     */
    private static CdpPayload ofEncoded(Object value) {
        if (value == null) {
            return NULL;
        }
        if (value instanceof Map<?, ?> source) {
            Map<String, CdpPayload> object = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                object.put(StringKit.toString(entry.getKey()), ofEncoded(entry.getValue()));
            }
            return new CdpPayload(Collections.unmodifiableMap(object));
        }
        if (value instanceof Iterable<?> source) {
            List<CdpPayload> array = new ArrayList<>();
            for (Object element : source) {
                array.add(ofEncoded(element));
            }
            return new CdpPayload(Collections.unmodifiableList(array));
        }
        if (value.getClass().isArray()) {
            List<CdpPayload> array = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                array.add(ofEncoded(Array.get(value, i)));
            }
            return new CdpPayload(Collections.unmodifiableList(array));
        }
        if (value instanceof String || value instanceof Boolean || value instanceof Number) {
            return new CdpPayload(value);
        }
        throw new ProtocolException("Invalid CDP payload type: " + value.getClass().getName());
    }

    /**
     * Returns the parse.
     *
     * @param text text to use
     * @return parse value
     */
    public static CdpPayload parse(String text) {
        return parse(text, ResourceLimits.defaults());
    }

    /**
     * Returns the parse.
     *
     * @param text           text to use
     * @param resourceLimits resource limits
     * @return parse value
     */
    public static CdpPayload parse(String text, ResourceLimits resourceLimits) {
        return new Parser(text, resourceLimits).parse();
    }

    /**
     * Returns the get.
     *
     * @param name name to use
     * @return get value
     */
    public CdpPayload get(String name) {
        Assert.notNull(name, "name");
        if (!isObject()) {
            return NULL;
        }
        return objectValue().getOrDefault(name, NULL);
    }

    /**
     * Returns the elements.
     *
     * @return values
     */
    public List<CdpPayload> elements() {
        if (!isArray()) {
            throw new ProtocolException("Current CDP payload is not an array.");
        }
        return arrayValue();
    }

    /**
     * Returns the as text.
     *
     * @return as text value
     */
    public String asText() {
        if (value instanceof String text) {
            return text;
        }
        throw new ProtocolException("Current CDP payload is not a string.");
    }

    /**
     * Returns the as int.
     *
     * @return as int value
     */
    public int asInt() {
        if (value instanceof BigDecimal number) {
            return number.intValueExact();
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new ProtocolException("Current CDP payload is not a number.");
    }

    /**
     * Returns the as double.
     *
     * @return as double value
     */
    public double asDouble() {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new ProtocolException("Current CDP payload is not a number.");
    }

    /**
     * Returns the as boolean.
     *
     * @return {@code true} when the condition matches
     */
    public boolean asBoolean() {
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new ProtocolException("Current CDP payload is not a boolean.");
    }

    /**
     * Returns whether object is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isObject() {
        return value instanceof Map<?, ?>;
    }

    /**
     * Returns whether array is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isArray() {
        return value instanceof List<?>;
    }

    /**
     * Returns whether null is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isNull() {
        return value == null;
    }

    /**
     * Returns the raw.
     *
     * @return raw value
     */
    public Object raw() {
        return value;
    }

    /**
     * Returns the fields.
     *
     * @return mapped values
     */
    public Map<String, CdpPayload> fields() {
        if (!isObject()) {
            throw new ProtocolException("Current CDP payload is not an object.");
        }
        return objectValue();
    }

    /**
     * Returns the object value.
     *
     * @return mapped values
     */
    private Map<String, CdpPayload> objectValue() {
        return (Map<String, CdpPayload>) value;
    }

    /**
     * Returns the array value.
     *
     * @return values
     */
    private List<CdpPayload> arrayValue() {
        return (List<CdpPayload>) value;
    }

    /**
     * Parses parser values.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class Parser {

        /**
         * Current text.
         */
        private final String text;
        /**
         * Resource limits.
         */
        private final ResourceLimits resourceLimits;
        /**
         * Current index.
         */
        private int index;
        /**
         * Current parse depth.
         */
        private int depth;
        /**
         * Current token count.
         */
        private long tokens;

        /**
         * Creates an instance.
         *
         * @param text text to use
         */
        private Parser(String text) {
            this(text, ResourceLimits.defaults());
        }

        /**
         * Creates an instance.
         *
         * @param text           text to use
         * @param resourceLimits resource limits
         */
        private Parser(String text, ResourceLimits resourceLimits) {
            this.text = Assert.notNull(text, "text");
            this.resourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
        }

        /**
         * Returns the parse.
         *
         * @return parse value
         */
        private CdpPayload parse() {
            skipWhitespace();
            CdpPayload payload = parseValue();
            skipWhitespace();
            if (!isEnd()) {
                throw error("JSON text contains trailing content.");
            }
            return payload;
        }

        /**
         * Parses value.
         *
         * @return parse value
         */
        private CdpPayload parseValue() {
            if (isEnd()) {
                throw error("JSON text ended unexpectedly.");
            }
            char current = text.charAt(index);
            if (current == Symbol.C_BRACE_LEFT) {
                return parseObject();
            }
            if (current == Symbol.C_BRACKET_LEFT) {
                return parseArray();
            }
            if (current == Symbol.C_DOUBLE_QUOTES) {
                return new CdpPayload(parseString());
            }
            if (current == 't') {
                countToken();
                consumeLiteral(Normal.TRUE);
                return new CdpPayload(Boolean.TRUE);
            }
            if (current == 'f') {
                countToken();
                consumeLiteral(Normal.FALSE);
                return new CdpPayload(Boolean.FALSE);
            }
            if (current == 'n') {
                countToken();
                consumeLiteral(Normal.NULL);
                return NULL;
            }
            if (current == Symbol.C_MINUS || Character.isDigit(current)) {
                countToken();
                return new CdpPayload(parseNumber());
            }
            throw error("Unrecognized JSON value.");
        }

        /**
         * Parses object.
         *
         * @return parse object value
         */
        private CdpPayload parseObject() {
            countToken();
            enterDepth();
            expect(Symbol.C_BRACE_LEFT);
            try {
                skipWhitespace();
                Map<String, CdpPayload> object = new LinkedHashMap<>();
                if (peek(Symbol.C_BRACE_RIGHT)) {
                    index++;
                    return new CdpPayload(Collections.unmodifiableMap(object));
                }
                while (true) {
                    skipWhitespace();
                    if (!peek(Symbol.C_DOUBLE_QUOTES)) {
                        throw error("JSON object field names must be strings.");
                    }
                    String key = parseString();
                    skipWhitespace();
                    expect(Symbol.C_COLON);
                    skipWhitespace();
                    object.put(key, parseValue());
                    skipWhitespace();
                    if (peek(Symbol.C_BRACE_RIGHT)) {
                        index++;
                        return new CdpPayload(Collections.unmodifiableMap(object));
                    }
                    expect(Symbol.C_COMMA);
                }
            } finally {
                exitDepth();
            }
        }

        /**
         * Parses array.
         *
         * @return parse array value
         */
        private CdpPayload parseArray() {
            countToken();
            enterDepth();
            expect(Symbol.C_BRACKET_LEFT);
            try {
                skipWhitespace();
                List<CdpPayload> array = new ArrayList<>();
                if (peek(Symbol.C_BRACKET_RIGHT)) {
                    index++;
                    return new CdpPayload(Collections.unmodifiableList(array));
                }
                while (true) {
                    skipWhitespace();
                    array.add(parseValue());
                    skipWhitespace();
                    if (peek(Symbol.C_BRACKET_RIGHT)) {
                        index++;
                        return new CdpPayload(Collections.unmodifiableList(array));
                    }
                    expect(Symbol.C_COMMA);
                }
            } finally {
                exitDepth();
            }
        }

        /**
         * Parses string.
         *
         * @return parse string value
         */
        private String parseString() {
            countToken();
            expect(Symbol.C_DOUBLE_QUOTES);
            StringBuilder builder = new StringBuilder();
            while (!isEnd()) {
                char current = text.charAt(index++);
                if (current == Symbol.C_DOUBLE_QUOTES) {
                    return builder.toString();
                }
                if (current == Symbol.C_BACKSLASH) {
                    builder.append(parseEscape());
                } else {
                    if (current < 0x20) {
                        throw error("JSON string contains an invalid control character.");
                    }
                    builder.append(current);
                }
            }
            throw error("JSON string is unterminated.");
        }

        /**
         * Parses escape.
         *
         * @return parse escape value
         */
        private char parseEscape() {
            if (isEnd()) {
                throw error("JSON string escape is unterminated.");
            }
            char escaped = text.charAt(index++);
            return switch (escaped) {
                case Symbol.C_DOUBLE_QUOTES -> Symbol.C_DOUBLE_QUOTES;
                case Symbol.C_BACKSLASH -> Symbol.C_BACKSLASH;
                case Symbol.C_SLASH -> Symbol.C_SLASH;
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> Symbol.C_LF;
                case 'r' -> Symbol.C_CR;
                case 't' -> Symbol.C_HT;
                case 'u' -> parseUnicode();
                default -> throw error("Invalid JSON string escape.");
            };
        }

        /**
         * Parses unicode.
         *
         * @return parse unicode value
         */
        private char parseUnicode() {
            if (index + 4 > text.length()) {
                throw error("Unicode escape is too short.");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                char current = text.charAt(index++);
                int digit = Character.digit(current, 16);
                if (digit < 0) {
                    throw error("Unicode escape contains invalid characters.");
                }
                value = (value << 4) + digit;
            }
            return (char) value;
        }

        /**
         * Parses number.
         *
         * @return parse number value
         */
        private BigDecimal parseNumber() {
            int start = index;
            if (peek(Symbol.C_MINUS)) {
                index++;
            }
            consumeDigits();
            if (peek(Symbol.C_DOT)) {
                index++;
                consumeDigits();
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek(Symbol.C_PLUS) || peek(Symbol.C_MINUS)) {
                    index++;
                }
                consumeDigits();
            }
            try {
                return new BigDecimal(text.substring(start, index));
            } catch (NumberFormatException ex) {
                throw new ProtocolException("Invalid JSON number.", ex);
            }
        }

        /**
         * Handles consume digits.
         */
        private void consumeDigits() {
            int start = index;
            while (!isEnd() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("JSON number is missing a digit part.");
            }
        }

        /**
         * Handles consume literal.
         *
         * @param literal literal value
         */
        private void consumeLiteral(String literal) {
            if (!text.startsWith(literal, index)) {
                throw error("JSON literal does not match.");
            }
            index += literal.length();
        }

        /**
         * Handles expect.
         *
         * @param expected expected value
         */
        private void expect(char expected) {
            if (!peek(expected)) {
                throw error("Expected character: " + expected);
            }
            index++;
        }

        /**
         * Returns the peek.
         *
         * @param expected expected value
         * @return {@code true} when the condition matches
         */
        private boolean peek(char expected) {
            return !isEnd() && text.charAt(index) == expected;
        }

        /**
         * Handles skip whitespace.
         */
        private void skipWhitespace() {
            while (!isEnd()) {
                char current = text.charAt(index);
                if (current == Symbol.C_SPACE || current == Symbol.C_LF || current == Symbol.C_CR
                        || current == Symbol.C_TAB) {
                    index++;
                } else {
                    return;
                }
            }
        }

        /**
         * Returns whether end is enabled.
         *
         * @return {@code true} when the condition matches
         */
        private boolean isEnd() {
            return index >= text.length();
        }

        /**
         * Returns the error.
         *
         * @param message message text
         * @return error value
         */
        private ProtocolException error(String message) {
            return new ProtocolException(message + " position: " + index);
        }

        /**
         * Counts one JSON token.
         */
        private void countToken() {
            tokens++;
            resourceLimits.validateJsonTokens(tokens);
        }

        /**
         * Enters a nested JSON container.
         */
        private void enterDepth() {
            depth++;
            resourceLimits.validateJsonDepth(depth);
        }

        /**
         * Exits a nested JSON container.
         */
        private void exitDepth() {
            depth--;
        }
    }

}
