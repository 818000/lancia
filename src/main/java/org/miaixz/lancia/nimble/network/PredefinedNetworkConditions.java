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
package org.miaixz.lancia.nimble.network;

import java.util.LinkedHashMap;
import java.util.Map;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;

/**
 * Represents a predefined network conditions value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class PredefinedNetworkConditions {

    /**
     * Shared constant for slow 3 g.
     */
    public static final NetworkConditions SLOW_3G = new NetworkConditions(false, 2000, 50000, 50000);

    /**
     * Shared constant for fast 3 g.
     */
    public static final NetworkConditions FAST_3G = new NetworkConditions(false, 562.5, 180000, 84375);

    /**
     * Shared constant for slow 4 g.
     */
    public static final NetworkConditions SLOW_4G = new NetworkConditions(false, 562.5, 180000, 84375);

    /**
     * Shared constant for fast 4 g.
     */
    public static final NetworkConditions FAST_4G = new NetworkConditions(false, 165, 1012500, 168750);

    /**
     * Predefined value registry.
     */
    public static final Map<String, NetworkConditions> VALUES = values();

    /**
     * Creates a predefined network conditions.
     */
    private PredefinedNetworkConditions() {
        // No initialization required.
    }

    /**
     * Returns the get.
     *
     * @param name name to use
     * @return optional value
     */
    public static Optional<NetworkConditions> get(String name) {
        return Optional.ofNullable(VALUES.get(Assert.notBlank(name, "name")));
    }

    /**
     * Returns the values.
     *
     * @return mapped values
     */
    private static Map<String, NetworkConditions> values() {
        Map<String, NetworkConditions> values = new LinkedHashMap<>();
        values.put("Slow 3G", SLOW_3G);
        values.put("Fast 3G", FAST_3G);
        values.put("Slow 4G", SLOW_4G);
        values.put("Fast 4G", FAST_4G);
        return Map.copyOf(values);
    }

}
