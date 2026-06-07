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

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Deserializes BiDi protocol values.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiDeserializer {

    /**
     * Shared constant for undefined.
     */
    public static final Object UNDEFINED = new Object();

    /**
     * Creates a bidi deserializer.
     */
    private BidiDeserializer() {
        // No initialization required.
    }

    /**
     * Returns the deserialize.
     *
     * @param remoteValue remote value
     * @return deserialize value
     */
    public static Object deserialize(CdpPayload remoteValue) {
        if (remoteValue == null || remoteValue.isNull()) {
            return UNDEFINED;
        }
        String type = PayloadReader.text(remoteValue.get("type"));
        CdpPayload value = remoteValue.get("value");
        return switch (type) {
            case "array" -> array(value);
            case "set" -> set(value);
            case "object" -> object(value);
            case "map" -> map(value);
            case "promise" -> Map.of();
            case "regexp" -> regexp(value);
            case "date" -> date(value);
            case "undefined" -> UNDEFINED;
            case "null" -> null;
            case "number" -> deserializeNumber(value);
            case "bigint" -> new BigInteger(PayloadReader.text(value));
            case "boolean" -> value.asBoolean();
            case "string" -> PayloadReader.text(value);
            default -> UNDEFINED;
        };
    }

    /**
     * Returns the array.
     *
     * @param value to use
     * @return values
     */
    private static List<Object> array(CdpPayload value) {
        if (value == null || value.isNull()) {
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        for (CdpPayload element : value.elements()) {
            result.add(deserialize(element));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the set.
     *
     * @param value to use
     * @return values
     */
    private static Set<Object> set(CdpPayload value) {
        if (value == null || value.isNull()) {
            return Set.of();
        }
        Set<Object> result = new LinkedHashSet<>();
        for (CdpPayload element : value.elements()) {
            result.add(deserialize(element));
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns the object.
     *
     * @param value to use
     * @return mapped values
     */
    private static Map<Object, Object> object(CdpPayload value) {
        Map<Object, Object> result = new LinkedHashMap<>();
        if (value == null || value.isNull()) {
            return result;
        }
        for (CdpPayload tuple : value.elements()) {
            Tuple entry = tuple(tuple);
            result.put(entry.key(), entry.value());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the map.
     *
     * @param value to use
     * @return mapped values
     */
    private static Map<Object, Object> map(CdpPayload value) {
        return object(value);
    }

    /**
     * Returns the regexp.
     *
     * @param value to use
     * @return regexp value
     */
    private static Pattern regexp(CdpPayload value) {
        String pattern = PayloadReader.text(value.get("pattern"));
        String flags = PayloadReader.text(value.get("flags"));
        int javaFlags = flags.contains("i") ? Pattern.CASE_INSENSITIVE : 0;
        return Pattern.compile(pattern, javaFlags);
    }

    /**
     * Returns the date.
     *
     * @param value to use
     * @return date value
     */
    private static Instant date(CdpPayload value) {
        return Instant.parse(PayloadReader.text(value));
    }

    /**
     * Returns the number.
     *
     * @param value to use
     * @return number value
     */
    private static Number deserializeNumber(CdpPayload value) {
        Object raw = value.raw();
        if (raw instanceof Number number) {
            return number;
        }
        return switch (PayloadReader.text(value)) {
            case "-0" -> -0.0d;
            case "NaN" -> Double.NaN;
            case "Infinity" -> Double.POSITIVE_INFINITY;
            case "-Infinity" -> Double.NEGATIVE_INFINITY;
            default -> Double.valueOf(PayloadReader.text(value));
        };
    }

    /**
     * Returns the tuple.
     *
     * @param tuple tuple value
     * @return tuple value
     */
    private static Tuple tuple(CdpPayload tuple) {
        List<CdpPayload> elements = tuple.elements();
        CdpPayload keyPayload = elements.get(0);
        Object key = keyPayload.isObject() ? deserialize(keyPayload) : keyPayload.raw();
        Object value = deserialize(elements.get(1));
        return new Tuple(key, value);
    }

    /**
     * Represents tuple.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class Tuple {

        /**
         * Current key.
         */
        private final Object key;
        /**
         * Current value.
         */
        private final Object value;

        /**
         * Creates an instance.
         *
         * @param key   key value
         * @param value to use
         */
        private Tuple(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the key.
         *
         * @return key value
         */
        private Object key() {
            return key;
        }

        /**
         * Returns the value.
         *
         * @return value
         */
        private Object value() {
            return value;
        }
    }

}
