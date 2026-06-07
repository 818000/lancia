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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.lancia.Payload;

/**
 * Encodes Java values into protocol writable values.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ValueCodec {

    /**
     * Creates a value codec.
     */
    private ValueCodec() {
        // No initialization required.
    }

    /**
     * Returns the encode.
     *
     * @param value to use
     * @return encode value
     */
    public static Object encode(Object value) {
        return encode(value, new IdentityHashMap<>(), Symbol.DOLLAR);
    }

    /**
     * Returns the encode.
     *
     * @param value   value to use
     * @param visited visited value
     * @param path    file path
     * @return encode value
     */
    private static Object encode(Object value, IdentityHashMap<Object, String> visited, String path) {
        if (value == null) {
            return null;
        }
        if (value instanceof Payload payload) {
            return encodePayload(payload, visited, path);
        }
        if (value instanceof String || value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte || value instanceof BigDecimal) {
            return value;
        }
        if (value instanceof Character character) {
            return String.valueOf(character);
        }
        if (value instanceof BigInteger integer) {
            return unserializable("bigint", integer + "n");
        }
        if (value instanceof Double number) {
            return encodeDouble(number);
        }
        if (value instanceof Float number) {
            return encodeFloat(number);
        }
        if (value instanceof Number) {
            return value;
        }
        if (value instanceof byte[] bytes) {
            return Base64.encode(bytes);
        }
        if (value instanceof Enum<?> item) {
            return item.name();
        }
        if (value instanceof Path || value instanceof URI || value instanceof URL) {
            return String.valueOf(value);
        }
        if (value instanceof Date date) {
            return date.toInstant().toString();
        }
        if (value instanceof TemporalAccessor) {
            return String.valueOf(value);
        }
        if (visited.containsKey(value)) {
            return Map.of("$ref", visited.get(value));
        }
        if (value instanceof Map<?, ?> map) {
            visited.put(value, path);
            Map<String, Object> result = new LinkedHashMap<>();
            int index = Normal._0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(
                        String.valueOf(entry.getKey()),
                        encode(entry.getValue(), visited, path + Symbol.DOT + index++));
            }
            visited.remove(value);
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            visited.put(value, path);
            List<Object> result = new ArrayList<>();
            int index = Normal._0;
            for (Object item : iterable) {
                result.add(encode(item, visited, path + Symbol.BRACKET_LEFT + index++ + Symbol.BRACKET_RIGHT));
            }
            visited.remove(value);
            return result;
        }
        if (value.getClass().isArray()) {
            visited.put(value, path);
            List<Object> result = new ArrayList<>();
            int length = Array.getLength(value);
            for (int index = Normal._0; index < length; index++) {
                result.add(
                        encode(
                                Array.get(value, index),
                                visited,
                                path + Symbol.BRACKET_LEFT + index + Symbol.BRACKET_RIGHT));
            }
            visited.remove(value);
            return result;
        }
        return encodeBean(value, visited, path);
    }

    /**
     * Returns the encode payload.
     *
     * @param payload protocol payload
     * @param visited visited value
     * @param path    file path
     * @return encode payload value
     */
    private static Object encodePayload(Payload payload, IdentityHashMap<Object, String> visited, String path) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        return encode(payload.raw(), visited, path);
    }

    /**
     * Returns the encode double.
     *
     * @param value to use
     * @return encode double value
     */
    private static Object encodeDouble(Double value) {
        if (Double.isNaN(value)) {
            return unserializable("number", "NaN");
        }
        if (value == Double.POSITIVE_INFINITY) {
            return unserializable("number", "Infinity");
        }
        if (value == Double.NEGATIVE_INFINITY) {
            return unserializable("number", "-Infinity");
        }
        if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(-0.0d)) {
            return unserializable("number", "-0");
        }
        return value;
    }

    /**
     * Returns the encode float.
     *
     * @param value to use
     * @return encode float value
     */
    private static Object encodeFloat(Float value) {
        if (Float.isNaN(value)) {
            return unserializable("number", "NaN");
        }
        if (value == Float.POSITIVE_INFINITY) {
            return unserializable("number", "Infinity");
        }
        if (value == Float.NEGATIVE_INFINITY) {
            return unserializable("number", "-Infinity");
        }
        return value;
    }

    /**
     * Returns the encode bean.
     *
     * @param value   value to use
     * @param visited visited value
     * @param path    file path
     * @return encode bean value
     */
    private static Object encodeBean(Object value, IdentityHashMap<Object, String> visited, String path) {
        visited.put(value, path);
        Map<String, Object> result = new LinkedHashMap<>();
        for (Method method : value.getClass().getMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != Normal._0
                    || method.getDeclaringClass() == Object.class) {
                continue;
            }
            String name = propertyName(method);
            if (name == null) {
                continue;
            }
            try {
                method.trySetAccessible();
                result.put(name, encode(method.invoke(value), visited, path + Symbol.DOT + name));
            } catch (ReflectiveOperationException ignored) {
                result.put(name, null);
            }
        }
        visited.remove(value);
        if (result.isEmpty()) {
            result.put("value", String.valueOf(value));
        }
        return result;
    }

    /**
     * Returns the property name.
     *
     * @param method protocol method
     * @return property name value
     */
    private static String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == Boolean.TYPE || method.getReturnType() == Boolean.class)) {
            return decapitalize(name.substring(2));
        }
        return null;
    }

    /**
     * Returns the decapitalize.
     *
     * @param value to use
     * @return decapitalize value
     */
    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return Normal.EMPTY;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * Returns the unserializable.
     *
     * @param type  type to use
     * @param value to use
     * @return mapped values
     */
    private static Map<String, Object> unserializable(String type, String value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("unserializableValue", value);
        return result;
    }

}
