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
package org.miaixz.lancia.kernel.cdp.emulation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.kernel.Bluetooth;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.nimble.device.SimulatedPeripheral;

/**
 * Controls Web Bluetooth emulation for a page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpBluetooth implements Bluetooth {

    /**
     * Current session.
     */
    private final CDPSession session;

    /**
     * Creates a bluetooth emulation.
     *
     * @param session protocol session
     */
    public CdpBluetooth(CDPSession session) {
        this.session = session;
    }

    /**
     * Returns the enable.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> enable() {
        return requireSession().send("CdpBluetooth.enable");
    }

    /**
     * Returns the emulate adapter.
     *
     * @param state state value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateAdapter(AdapterState state) {
        return emulateAdapter(state, true);
    }

    /**
     * Returns the emulate adapter.
     *
     * @param state       state value
     * @param leSupported le supported value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateAdapter(AdapterState state, boolean leSupported) {
        return emulateAdapter(state, Boolean.valueOf(leSupported));
    }

    /**
     * Returns the emulate adapter.
     *
     * @param state       state value
     * @param leSupported le supported value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateAdapter(AdapterState state, Boolean leSupported) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("state", Assert.notNull(state, "state").id());
        params.put("leSupported", leSupported == null || leSupported);
        return disable().thenCompose(ignored -> requireSession().send("CdpBluetooth.enable", params));
    }

    /**
     * Returns the simulate adapter.
     *
     * @param state       state value
     * @param leSupported le supported value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> simulateAdapter(AdapterState state, Boolean leSupported) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("state", Assert.notNull(state, "state").id());
        if (leSupported != null) {
            params.put("leSupported", leSupported);
        }
        return requireSession().send("CdpBluetooth.simulateAdapter", params);
    }

    /**
     * Returns the disable.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> disable() {
        return requireSession().send("CdpBluetooth.disable");
    }

    /**
     * Returns the disable emulation.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> disableEmulation() {
        return disable();
    }

    /**
     * Returns the simulate preconnected peripheral.
     *
     * @param peripheral peripheral value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> simulatePreconnectedPeripheral(SimulatedPeripheral peripheral) {
        return requireSession()
                .send("CdpBluetooth.simulatePreconnectedPeripheral", Assert.notNull(peripheral, "peripheral").toMap());
    }

    /**
     * Returns the simulate preconnected peripheral.
     *
     * @param peripheral peripheral value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> simulatePreconnectedPeripheral(PreconnectedPeripheral peripheral) {
        return requireSession()
                .send("CdpBluetooth.simulatePreconnectedPeripheral", Assert.notNull(peripheral, "peripheral").toMap());
    }

    /**
     * Returns the require session.
     *
     * @return require session value
     */
    private CDPSession requireSession() {
        return Assert.notNull(session, "session");
    }

    /**
     * Enumerates AdapterStates.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public enum AdapterState {

        /**
         * Represents the absent enum member.
         */
        ABSENT("absent"),

        /**
         * Represents the powered off enum member.
         */
        POWERED_OFF("powered-off"),

        /**
         * Represents the powered on enum member.
         */
        POWERED_ON("powered-on");

        /**
         * Current identifier.
         */
        private final String id;

        /**
         * Creates an instance.
         *
         * @param id identifier
         */
        AdapterState(String id) {
            this.id = id;
        }

        /**
         * Returns the ID.
         *
         * @return ID value
         */
        public String id() {
            return id;
        }

        /**
         * Creates this value from value.
         *
         * @param value to use
         * @return from value
         */
        public static AdapterState fromValue(String value) {
            String actualValue = Assert.notBlank(value, "value").toLowerCase(Locale.ROOT);
            for (AdapterState state : values()) {
                if (state.id.equals(actualValue)) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid bluetooth adapter state: " + value);
        }
    }

    /**
     * Describes bluetooth manufacturer metadata.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class BluetoothManufacturerData {

        /**
         * Current key.
         */
        private final int key;
        /**
         * Current data.
         */
        private final String data;

        /**
         * Creates an instance.
         *
         * @param key  key value
         * @param data data to use
         */
        public BluetoothManufacturerData(int key, String data) {
            this.key = key;
            this.data = Assert.notBlank(data, "data");
        }

        /**
         * Converts this value to protocol parameters.
         *
         * @return protocol parameters
         */
        public Map<String, Object> toMap() {
            return Map.of("key", key, "data", data);
        }

        /**
         * Returns the key.
         *
         * @return key value
         */
        public int key() {
            return key;
        }

        /**
         * Returns the data.
         *
         * @return data value
         */
        public String data() {
            return data;
        }
    }

    /**
     * Represents preconnected peripheral.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class PreconnectedPeripheral {

        /**
         * Current address.
         */
        private final String address;
        /**
         * Current name.
         */
        private final String name;
        /**
         * Registered manufacturer data values.
         */
        private final List<BluetoothManufacturerData> manufacturerData;
        /**
         * Registered known service uuids values.
         */
        private final List<String> knownServiceUuids;

        /**
         * Creates an instance.
         *
         * @param address           address value
         * @param name              name to use
         * @param manufacturerData  manufacturer data value
         * @param knownServiceUuids known service uuids value
         */
        public PreconnectedPeripheral(String address, String name, List<BluetoothManufacturerData> manufacturerData,
                List<String> knownServiceUuids) {
            this.address = Assert.notBlank(address, "address");
            this.name = Assert.notBlank(name, "name");
            this.manufacturerData = manufacturerData == null ? List.of() : List.copyOf(manufacturerData);
            this.knownServiceUuids = knownServiceUuids == null ? List.of() : List.copyOf(knownServiceUuids);
        }

        /**
         * Converts this value to protocol parameters.
         *
         * @return protocol parameters
         */
        public Map<String, Object> toMap() {
            return Map.of(
                    "address",
                    address,
                    "name",
                    name,
                    "manufacturerData",
                    manufacturerData.stream().map(BluetoothManufacturerData::toMap).toList(),
                    "knownServiceUuids",
                    knownServiceUuids);
        }

        /**
         * Returns the address.
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
         * Returns the manufacturer data.
         *
         * @return values
         */
        public List<BluetoothManufacturerData> manufacturerData() {
            return manufacturerData;
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

}
