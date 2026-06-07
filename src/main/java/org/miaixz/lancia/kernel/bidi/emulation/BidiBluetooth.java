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
package org.miaixz.lancia.kernel.bidi.emulation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.kernel.bidi.session.BidiProtocolSession;
import org.miaixz.lancia.kernel.cdp.emulation.CdpBluetooth;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.device.SimulatedPeripheral;

/**
 * Represents BiDi bluetooth emulation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiBluetooth {

    /**
     * Current context ID.
     */
    private final String contextId;

    /**
     * BiDi session.
     */
    private final BidiProtocolSession session;

    /**
     * Creates a bidi bluetooth emulation.
     *
     * @param contextId context id
     * @param session   protocol session
     */
    public BidiBluetooth(String contextId, BidiProtocolSession session) {
        this.contextId = Assert.notBlank(contextId, "contextId");
        this.session = Assert.notNull(session, "session");
    }

    /**
     * Returns the emulate adapter.
     *
     * @param state state value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateAdapter(CdpBluetooth.AdapterState state) {
        return emulateAdapter(state, true);
    }

    /**
     * Returns the emulate adapter.
     *
     * @param state       state value
     * @param leSupported le supported value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateAdapter(CdpBluetooth.AdapterState state, boolean leSupported) {
        return session.send(
                "bluetooth.simulateAdapter",
                Map.of("context", contextId, "state", Assert.notNull(state, "state").id(), "leSupported", leSupported));
    }

    /**
     * Returns the disable emulation.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> disableEmulation() {
        return session.send("bluetooth.disableSimulation", Map.of("context", contextId));
    }

    /**
     * Returns the simulate preconnected peripheral.
     *
     * @param peripheral peripheral value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> simulatePreconnectedPeripheral(SimulatedPeripheral peripheral) {
        return simulatePreconnectedPeripheral(Assert.notNull(peripheral, "peripheral").toMap());
    }

    /**
     * Returns the simulate preconnected peripheral.
     *
     * @param peripheral peripheral value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> simulatePreconnectedPeripheral(
            CdpBluetooth.PreconnectedPeripheral peripheral) {
        return simulatePreconnectedPeripheral(Assert.notNull(peripheral, "peripheral").toMap());
    }

    /**
     * Returns the context ID.
     *
     * @return context ID value
     */
    public String contextId() {
        return contextId;
    }

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    public BidiProtocolSession session() {
        return session;
    }

    /**
     * Returns the simulate preconnected peripheral.
     *
     * @param peripheral peripheral value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> simulatePreconnectedPeripheral(Map<String, Object> peripheral) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("context", contextId);
        params.putAll(peripheral);
        return session.send("bluetooth.simulatePreconnectedPeripheral", params);
    }

}
