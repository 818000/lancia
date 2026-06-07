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
package org.miaixz.lancia.shared.frame;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;

/**
 * Enumerates frame manager event names.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum FrameManager {

    /**
     * Emitted when a frame is attached.
     */
    FRAME_ATTACHED("FrameManager.FrameAttached"),

    /**
     * Emitted when a frame navigates.
     */
    FRAME_NAVIGATED("FrameManager.FrameNavigated"),

    /**
     * Emitted when a frame is detached.
     */
    FRAME_DETACHED("FrameManager.FrameDetached"),

    /**
     * Emitted when a frame is swapped.
     */
    FRAME_SWAPPED("FrameManager.FrameSwapped"),

    /**
     * Emitted when a frame lifecycle event occurs.
     */
    LIFECYCLE_EVENT("FrameManager.LifecycleEvent"),

    /**
     * Emitted when a frame navigates within the current document.
     */
    FRAME_NAVIGATED_WITHIN_DOCUMENT("FrameManager.FrameNavigatedWithinDocument"),

    /**
     * Emitted when a frame execution context calls a console API.
     */
    CONSOLE_API_CALLED("FrameManager.ConsoleApiCalled"),

    /**
     * Emitted when a frame binding is called.
     */
    BINDING_CALLED("FrameManager.BindingCalled");

    /**
     * Protocol event name.
     */
    private final String eventName;

    /**
     * Creates a frame manager event.
     *
     * @param eventName event name
     */
    FrameManager(String eventName) {
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
    public static Optional<FrameManager> fromEventName(String eventName) {
        for (FrameManager type : values()) {
            if (type.eventName.equals(eventName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

}
