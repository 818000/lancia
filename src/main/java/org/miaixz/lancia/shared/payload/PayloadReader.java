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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Payload;

/**
 * Reads protocol neutral payload values.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class PayloadReader {

    /**
     * Prevents instantiation.
     */
    private PayloadReader() {
        // No initialization required.
    }

    /**
     * Returns the present.
     *
     * @param payload protocol payload
     * @return {@code true} when the condition matches
     */
    public static boolean present(Payload payload) {
        return payload != null && !payload.isNull();
    }

    /**
     * Reads payload array elements.
     *
     * @param payload payload
     * @return payload elements
     */
    public static List<Payload> elements(Payload payload) {
        if (!present(payload) || !payload.isArray()) {
            return List.of();
        }
        Object raw = payload.raw();
        if (!(raw instanceof Iterable<?> source)) {
            return List.of();
        }
        List<Payload> elements = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Payload child) {
                elements.add(child);
            }
        }
        return List.copyOf(elements);
    }

    /**
     * Reads typed payload array elements.
     *
     * @param payload payload
     * @param type    payload type
     * @param <T>     payload type
     * @return payload elements
     */
    public static <T extends Payload> List<T> elements(Payload payload, Class<T> type) {
        if (type == null) {
            return List.of();
        }
        List<T> elements = new ArrayList<>();
        for (Payload item : elements(payload)) {
            if (type.isInstance(item)) {
                elements.add(type.cast(item));
            }
        }
        return List.copyOf(elements);
    }

    /**
     * Reads text from a payload.
     *
     * @param payload payload
     * @return text value
     */
    public static String text(Payload payload) {
        if (!present(payload)) {
            return Normal.EMPTY;
        }
        return StringKit.toString(payload.raw());
    }

    /**
     * Reads text from a payload with a null fallback.
     *
     * @param payload      payload
     * @param defaultValue default value
     * @return text value
     */
    public static String text(Payload payload, String defaultValue) {
        return present(payload) ? text(payload) : defaultValue;
    }

    /**
     * Reads non-blank text from a payload.
     *
     * @param payload      payload
     * @param defaultValue default value
     * @return text value
     */
    public static String nonBlankText(Payload payload, String defaultValue) {
        String value = text(payload);
        return StringKit.isBlank(value) ? defaultValue : value;
    }

    /**
     * Reads nullable text from a payload.
     *
     * @param payload payload
     * @return text value
     */
    public static String nullableText(Payload payload) {
        return present(payload) ? text(payload) : null;
    }

    /**
     * Reads an integer from a payload.
     *
     * @param payload payload
     * @return integer value
     */
    public static int number(Payload payload) {
        return number(payload, 0);
    }

    /**
     * Reads an integer from a payload.
     *
     * @param payload      payload
     * @param defaultValue default value
     * @return integer value
     */
    public static int number(Payload payload, int defaultValue) {
        return present(payload) ? payload.asInt() : defaultValue;
    }

    /**
     * Reads a nullable integer from a payload.
     *
     * @param payload payload
     * @return integer value
     */
    public static Integer numberObject(Payload payload) {
        return present(payload) ? payload.asInt() : null;
    }

    /**
     * Reads a backend DOM node id from a DOM.describeNode payload.
     *
     * @param payload describe node payload
     * @return backend node id
     */
    public static int backendNodeId(Payload payload) {
        return number(payload.get("node").get("backendNodeId"));
    }

    /**
     * Reads and stores a cached backend DOM node id.
     *
     * @param cached cached value
     * @param source describe node source
     * @param cache  cache setter
     * @return backend node id
     */
    public static int backendNodeId(Integer cached, Supplier<? extends Payload> source, IntConsumer cache) {
        if (cached != null) {
            return cached;
        }
        int value = backendNodeId(source.get());
        cache.accept(value);
        return value;
    }

    /**
     * Reads a double from a payload.
     *
     * @param payload payload
     * @return double value
     */
    public static double decimal(Payload payload) {
        return decimal(payload, 0);
    }

    /**
     * Reads a double from a payload.
     *
     * @param payload      payload
     * @param defaultValue default value
     * @return double value
     */
    public static double decimal(Payload payload, double defaultValue) {
        return present(payload) ? payload.asDouble() : defaultValue;
    }

    /**
     * Reads a boolean from a payload.
     *
     * @param payload payload
     * @return boolean value
     */
    public static boolean bool(Payload payload) {
        return present(payload) && payload.asBoolean();
    }

    /**
     * Reads a nullable boolean from a payload.
     *
     * @param payload payload
     * @return boolean value
     */
    public static Boolean boolObject(Payload payload) {
        return present(payload) ? payload.asBoolean() : null;
    }

    /**
     * Returns the first non-null payload.
     *
     * @param fallback fallback payload
     * @param values   candidate payloads
     * @param <T>      payload type
     * @return first present payload
     */
    @SafeVarargs
    public static <T extends Payload> T first(T fallback, T... values) {
        if (values != null) {
            for (T value : values) {
                if (present(value)) {
                    return value;
                }
            }
        }
        return fallback;
    }

    /**
     * Reads payload as a recursively unwrapped object map.
     *
     * @param payload payload
     * @return object value
     */
    public static Map<String, Object> object(Payload payload) {
        if (!present(payload) || !payload.isObject() || !(payload.raw() instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object item = entry.getValue();
            result.put(StringKit.toString(entry.getKey()), item instanceof Payload child ? value(child) : item);
        }
        return Map.copyOf(result);
    }

    /**
     * Reads payload as a recursively unwrapped array.
     *
     * @param payload payload
     * @return array value
     */
    public static List<Object> array(Payload payload) {
        if (!present(payload) || !payload.isArray() || !(payload.raw() instanceof Iterable<?> source)) {
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        for (Object item : source) {
            result.add(item instanceof Payload child ? value(child) : item);
        }
        return List.copyOf(result);
    }

    /**
     * Reads payload as a recursively unwrapped value.
     *
     * @param payload payload
     * @return value
     */
    public static Object value(Payload payload) {
        if (!present(payload)) {
            return null;
        }
        if (payload.isObject()) {
            return object(payload);
        }
        if (payload.isArray()) {
            return array(payload);
        }
        return payload.raw();
    }

}
