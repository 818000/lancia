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
package org.miaixz.lancia.kernel.cdp.target;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;

/**
 * Enumerates target manager event names.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum CdpTargetManagerEvent {

    /**
     * Emitted when a target is discovered.
     */
    TARGET_DISCOVERED("targetDiscovered"),

    /**
     * Emitted when a target becomes available.
     */
    TARGET_AVAILABLE("targetAvailable"),

    /**
     * Emitted when a target is removed.
     */
    TARGET_GONE("targetGone"),

    /**
     * Emitted when target metadata changes.
     */
    TARGET_CHANGED("targetChanged");

    /**
     * Protocol event name.
     */
    private final String eventName;

    /**
     * Creates a target manager event.
     *
     * @param eventName event name
     */
    CdpTargetManagerEvent(String eventName) {
        this.eventName = Assert.notBlank(eventName, "eventName");
    }

    /**
     * Returns the protocol event name.
     *
     * @return protocol event name
     */
    public String eventName() {
        return eventName;
    }

    /**
     * Returns the event name.
     *
     * @return protocol event name
     */
    public String getEventName() {
        return eventName();
    }

    /**
     * Resolves an enum value by protocol event name.
     *
     * @param eventName event name
     * @return matching event
     */
    public static Optional<CdpTargetManagerEvent> fromEventName(String eventName) {
        for (CdpTargetManagerEvent type : values()) {
            if (type.eventName.equals(eventName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

}
