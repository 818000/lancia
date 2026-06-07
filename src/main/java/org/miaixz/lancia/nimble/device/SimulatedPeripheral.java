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
package org.miaixz.lancia.nimble.device;

import java.util.List;
import java.util.Map;

/**
 * Represents a simulated peripheral value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class SimulatedPeripheral {

    /**
     * Current address.
     */
    private final String address;
    /**
     * Current name.
     */
    private final String name;
    /**
     * Registered known service uuids values.
     */
    private final List<String> knownServiceUuids;

    /**
     * Creates a simulated peripheral.
     *
     * @param address           address
     * @param name              name to use
     * @param knownServiceUuids known service uuids
     */
    public SimulatedPeripheral(String address, String name, List<String> knownServiceUuids) {
        this.address = address;
        this.name = name;
        this.knownServiceUuids = knownServiceUuids == null ? List.of() : List.copyOf(knownServiceUuids);
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        return Map.of("address", address, "name", name, "knownServiceUuids", knownServiceUuids);
    }

    /**
     * Adds ress.
     *
     * @return address value
     */
    public String address() {
        return address;
    }

    /**
     * Returns the name.
     *
     * @return name value
     */
    public String name() {
        return name;
    }

    /**
     * Returns the known service uuids.
     *
     * @return values
     */
    public List<String> knownServiceUuids() {
        return knownServiceUuids;
    }

}
