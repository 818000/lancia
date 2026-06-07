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

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.lancia.kernel.bidi.page.BidiElementHandle;
import org.miaixz.lancia.kernel.bidi.page.BidiJSHandle;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.protocol.ValueCodec;

/**
 * Serializes BiDi values for protocol transport.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiSerializer {

    /**
     * Creates a bidi serializer.
     */
    private BidiSerializer() {
        // No initialization required.
    }

    /**
     * Returns the serialize.
     *
     * @param value to use
     * @return serialize value
     */
    public static CdpPayload serialize(Object value) {
        return CdpPayload.of(serializeValue(value, new IdentityHashMap<>(), Symbol.DOLLAR));
    }

    /**
     * Returns the serialize value.
     *
     * @param value   value to use
     * @param visited visited value
     * @param path    file path
     * @return mapped values
     */
    private static Map<String, Object> serializeValue(
            Object value,
            IdentityHashMap<Object, String> visited,
            String path) {
        if (value == null) {
            return Map.of("type", "null");
        }
        if (value == BidiDeserializer.UNDEFINED) {
            return Map.of("type", "undefined");
        }
        if (value instanceof BidiJSHandle handle) {
            return handle.optionalId().<Map<String, Object>>map(id -> Map.of("handle", id))
                    .orElseGet(() -> serializeValue(handle.jsonValue(), visited, path));
        }
        if (value instanceof BidiElementHandle handle) {
            return Map.of("handle", handle.id());
        }
        if (value instanceof Boolean bool) {
            return Map.of("type", "boolean", "value", bool);
        }
        if (value instanceof BigInteger integer) {
            return Map.of("type", "bigint", "value", integer.toString());
        }
        if (value instanceof Number number) {
            return serializeNumber(number);
        }
        if (value instanceof Character character) {
            return Map.of("type", "string", "value", String.valueOf(character));
        }
        if (value instanceof CharSequence text) {
            return Map.of("type", "string", "value", text.toString());
        }
        if (value instanceof Pattern pattern) {
            return Map.of("type", "regexp", "value", Map.of("pattern", pattern.pattern(), "flags", flags(pattern)));
        }
        if (value instanceof Instant instant) {
            return Map.of("type", "date", "value", instant.toString());
        }
        if (value instanceof Date date) {
            return Map.of("type", "date", "value", date.toInstant().toString());
        }
        if (value instanceof byte[] bytes) {
            return serializeValue(ValueCodec.encode(bytes), visited, path);
        }
        if (value instanceof Map<?, ?> map) {
            String reference = guardCircular(value, visited, path);
            if (reference != null) {
                return reference(reference);
            }
            List<List<Map<String, Object>>> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.add(
                        List.of(
                                serializeValue(String.valueOf(entry.getKey()), visited, path + ".key"),
                                serializeValue(entry.getValue(), visited, path + Symbol.DOT + entry.getKey())));
            }
            visited.remove(value);
            return Map.of("type", "object", "value", entries);
        }
        if (value instanceof Set<?> set) {
            String reference = guardCircular(value, visited, path);
            if (reference != null) {
                return reference(reference);
            }
            List<Map<String, Object>> values = new ArrayList<>();
            int index = Normal._0;
            for (Object item : set) {
                values.add(serializeValue(item, visited, path + Symbol.BRACKET_LEFT + index++ + Symbol.BRACKET_RIGHT));
            }
            visited.remove(value);
            return Map.of("type", "set", "value", values);
        }
        if (value instanceof Iterable<?> iterable) {
            String reference = guardCircular(value, visited, path);
            if (reference != null) {
                return reference(reference);
            }
            List<Map<String, Object>> values = new ArrayList<>();
            int index = Normal._0;
            for (Object item : iterable) {
                values.add(serializeValue(item, visited, path + Symbol.BRACKET_LEFT + index++ + Symbol.BRACKET_RIGHT));
            }
            visited.remove(value);
            return Map.of("type", "array", "value", values);
        }
        if (value.getClass().isArray()) {
            String reference = guardCircular(value, visited, path);
            if (reference != null) {
                return reference(reference);
            }
            List<Map<String, Object>> values = new ArrayList<>();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                values.add(
                        serializeValue(
                                Array.get(value, index),
                                visited,
                                path + Symbol.BRACKET_LEFT + index + Symbol.BRACKET_RIGHT));
            }
            visited.remove(value);
            return Map.of("type", "array", "value", values);
        }
        return serializeEncoded(ValueCodec.encode(value), visited, path);
    }

    /**
     * Returns the serialize encoded.
     *
     * @param value   value to use
     * @param visited visited value
     * @param path    file path
     * @return mapped values
     */
    private static Map<String, Object> serializeEncoded(
            Object value,
            IdentityHashMap<Object, String> visited,
            String path) {
        if (value instanceof Map<?, ?> map) {
            if (map.containsKey("type") && map.containsKey("unserializableValue")) {
                return Map.of(
                        "type",
                        String.valueOf(map.get("type")),
                        "value",
                        String.valueOf(map.get("unserializableValue")));
            }
            return serializeValue(map, visited, path);
        }
        return serializeValue(value, visited, path);
    }

    /**
     * Returns the number.
     *
     * @param number number value
     * @return mapped values
     */
    private static Map<String, Object> serializeNumber(Number number) {
        if (number instanceof Double value) {
            if (Double.isNaN(value)) {
                return Map.of("type", "number", "value", "NaN");
            }
            if (value == Double.POSITIVE_INFINITY) {
                return Map.of("type", "number", "value", "Infinity");
            }
            if (value == Double.NEGATIVE_INFINITY) {
                return Map.of("type", "number", "value", "-Infinity");
            }
            if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(-0.0d)) {
                return Map.of("type", "number", "value", "-0");
            }
        }
        if (number instanceof Float value) {
            if (Float.isNaN(value)) {
                return Map.of("type", "number", "value", "NaN");
            }
            if (value == Float.POSITIVE_INFINITY) {
                return Map.of("type", "number", "value", "Infinity");
            }
            if (value == Float.NEGATIVE_INFINITY) {
                return Map.of("type", "number", "value", "-Infinity");
            }
        }
        return Map.of("type", "number", "value", number);
    }

    /**
     * Returns the flags.
     *
     * @param pattern pattern value
     * @return flags value
     */
    private static String flags(Pattern pattern) {
        StringBuilder flags = new StringBuilder();
        if ((pattern.flags() & Pattern.CASE_INSENSITIVE) != 0) {
            flags.append('i');
        }
        if ((pattern.flags() & Pattern.MULTILINE) != 0) {
            flags.append('m');
        }
        if ((pattern.flags() & Pattern.DOTALL) != 0) {
            flags.append('s');
        }
        return flags.toString();
    }

    /**
     * Returns the guard circular.
     *
     * @param value   value to use
     * @param visited visited value
     * @param path    file path
     * @return guard circular value
     */
    private static String guardCircular(Object value, IdentityHashMap<Object, String> visited, String path) {
        if (visited.containsKey(value)) {
            return visited.get(value);
        }
        visited.put(value, path);
        return null;
    }

    /**
     * Returns the reference.
     *
     * @param reference reference value
     * @return mapped values
     */
    private static Map<String, Object> reference(String reference) {
        List<List<Map<String, Object>>> entries = new ArrayList<>();
        entries.add(List.of(Map.of("type", "string", "value", "$ref"), Map.of("type", "string", "value", reference)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "object");
        result.put("value", entries);
        return result;
    }

}
