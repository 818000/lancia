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
package org.miaixz.lancia.shared.protocol;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.lancia.Payload;

/**
 * JSON text writer and encoding helper.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class TextWriter {

    /**
     * Creates a JSON text writer.
     */
    public TextWriter() {
        // No initialization required.
    }

    /**
     * Converts a string into a byte array using UTF-8 or Base64 decoding.
     *
     * @param string        source string
     * @param base64Encoded whether the source is Base64 encoded
     * @return byte array
     */
    public static byte[] stringToTypedArray(String string, boolean base64Encoded) {
        String value = string == null ? Normal.EMPTY : string;
        return base64Encoded ? Base64.decode(value) : ByteKit.toBytes(value, Charset.UTF_8);
    }

    /**
     * Converts a UTF-8 string into a byte array.
     *
     * @param string source string
     * @return byte array
     */
    public static byte[] stringToTypedArray(String string) {
        return stringToTypedArray(string, false);
    }

    /**
     * Converts a UTF-8 string into Base64.
     *
     * @param string source string
     * @return Base64 text
     */
    public static String stringToBase64(String string) {
        return typedArrayToBase64(stringToTypedArray(string));
    }

    /**
     * Converts a byte array into Base64.
     *
     * @param typedArray byte array
     * @return Base64 text
     */
    public static String typedArrayToBase64(byte[] typedArray) {
        return Base64.encode(typedArray == null ? Normal.EMPTY_BYTE_ARRAY : typedArray);
    }

    /**
     * Merges byte arrays into a single byte array.
     *
     * @param items byte arrays
     * @return merged byte array
     */
    public static byte[] mergeUint8Arrays(byte[]... items) {
        if (items == null || items.length == 0) {
            return Normal.EMPTY_BYTE_ARRAY;
        }
        int length = 0;
        for (byte[] item : items) {
            length += item == null ? 0 : item.length;
        }
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] item : items) {
            if (item == null || item.length == 0) {
                continue;
            }
            System.arraycopy(item, 0, result, offset, item.length);
            offset += item.length;
        }
        return result;
    }

    /**
     * Merges byte arrays into a single byte array.
     *
     * @param items byte arrays
     * @return merged byte array
     */
    public static byte[] mergeUint8Arrays(Collection<byte[]> items) {
        Assert.notNull(items, "items");
        return mergeUint8Arrays(items.toArray(new byte[0][]));
    }

    /**
     * Writes a Java value.
     *
     * @param value Java value
     * @return JSON text
     */
    public String writeValue(Object value) {
        StringBuilder builder = new StringBuilder();
        write(ValueCodec.encode(value), builder);
        return builder.toString();
    }

    /**
     * Writes a supported value.
     *
     * @param value   Java value
     * @param builder text builder
     */
    private void write(Object value, StringBuilder builder) {
        if (value == null) {
            builder.append(Normal.NULL);
        } else if (value instanceof Payload payload) {
            writePayload(payload, builder);
        } else if (value instanceof String text) {
            writeString(text, builder);
        } else if (value instanceof Character character) {
            writeString(String.valueOf(character), builder);
        } else if (value instanceof Boolean bool) {
            builder.append(bool ? Normal.TRUE : Normal.FALSE);
        } else if (value instanceof Number number) {
            writeNumber(number, builder);
        } else if (value instanceof Map<?, ?> map) {
            writeMap(map, builder);
        } else if (value instanceof Iterable<?> iterable) {
            writeIterable(iterable, builder);
        } else if (value.getClass().isArray()) {
            writeArray(value, builder);
        } else {
            write(ValueCodec.encode(value), builder);
        }
    }

    /**
     * Writes a payload.
     *
     * @param payload protocol payload
     * @param builder text builder
     */
    private void writePayload(Payload payload, StringBuilder builder) {
        if (payload == null || payload.isNull()) {
            builder.append(Normal.NULL);
        } else {
            write(payload.raw(), builder);
        }
    }

    /**
     * Writes an object.
     *
     * @param map     field map
     * @param builder text builder
     */
    private void writeMap(Map<?, ?> map, StringBuilder builder) {
        builder.append(Symbol.C_BRACE_LEFT);
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            writeString(String.valueOf(entry.getKey()), builder);
            builder.append(Symbol.C_COLON);
            write(entry.getValue(), builder);
            if (iterator.hasNext()) {
                builder.append(Symbol.C_COMMA);
            }
        }
        builder.append(Symbol.C_BRACE_RIGHT);
    }

    /**
     * Writes an iterable.
     *
     * @param iterable iterable
     * @param builder  text builder
     */
    private void writeIterable(Iterable<?> iterable, StringBuilder builder) {
        builder.append(Symbol.C_BRACKET_LEFT);
        Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            write(iterator.next(), builder);
            if (iterator.hasNext()) {
                builder.append(Symbol.C_COMMA);
            }
        }
        builder.append(Symbol.C_BRACKET_RIGHT);
    }

    /**
     * Writes an array.
     *
     * @param array   array
     * @param builder text builder
     */
    private void writeArray(Object array, StringBuilder builder) {
        builder.append(Symbol.C_BRACKET_LEFT);
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                builder.append(Symbol.C_COMMA);
            }
            write(Array.get(array, i), builder);
        }
        builder.append(Symbol.C_BRACKET_RIGHT);
    }

    /**
     * Writes a number.
     *
     * @param number  number
     * @param builder text builder
     */
    private void writeNumber(Number number, StringBuilder builder) {
        if (number instanceof BigDecimal decimal) {
            builder.append(decimal.toPlainString());
        } else if (number instanceof BigInteger integer) {
            builder.append(integer);
        } else {
            builder.append(number);
        }
    }

    /**
     * Writes a string.
     *
     * @param text    string
     * @param builder text builder
     */
    private void writeString(String text, StringBuilder builder) {
        builder.append(Symbol.C_DOUBLE_QUOTES);
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            switch (current) {
                case Symbol.C_DOUBLE_QUOTES -> builder.append(Symbol.BACKSLASH).append(Symbol.C_DOUBLE_QUOTES);
                case Symbol.C_BACKSLASH -> builder.append(Symbol.BACKSLASH).append(Symbol.C_BACKSLASH);
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case Symbol.C_LF -> builder.append("\\n");
                case Symbol.C_CR -> builder.append("\\r");
                case Symbol.C_HT -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        builder.append(Symbol.C_DOUBLE_QUOTES);
    }

}
