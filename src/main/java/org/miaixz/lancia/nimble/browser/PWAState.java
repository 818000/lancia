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
package org.miaixz.lancia.nimble.browser;

import java.util.List;
import java.util.Map;

import org.miaixz.lancia.Payload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * OS integration state for an installed Progressive Web App.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class PWAState {

    /**
     * Current badge count.
     */
    private final int badgeCount;

    /**
     * File handlers registered by the app.
     */
    private final List<Object> fileHandlers;

    /**
     * Creates PWA state.
     *
     * @param badgeCount   badge count
     * @param fileHandlers file handlers
     */
    public PWAState(int badgeCount, List<Object> fileHandlers) {
        this.badgeCount = badgeCount;
        this.fileHandlers = fileHandlers == null ? List.of() : List.copyOf(fileHandlers);
    }

    /**
     * Creates PWA state from a CDP PWA.getOsAppState result.
     *
     * @param payload protocol payload
     * @return PWA state
     */
    public static PWAState from(Payload payload) {
        Payload actual = payload == null ? null : payload;
        return new PWAState(PayloadReader.number(actual == null ? null : actual.get("badgeCount")),
                PayloadReader.array(actual == null ? null : actual.get("fileHandlers")));
    }

    /**
     * Returns the badge count shown on the app icon.
     *
     * @return badge count
     */
    public int badgeCount() {
        return badgeCount;
    }

    /**
     * Returns the file handlers registered by the app.
     *
     * @return file handlers
     */
    public List<Object> fileHandlers() {
        return fileHandlers;
    }

    /**
     * Converts this value to a map.
     *
     * @return mapped value
     */
    public Map<String, Object> toMap() {
        return Map.of("badgeCount", badgeCount, "fileHandlers", fileHandlers);
    }

}
