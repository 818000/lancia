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
package org.miaixz.lancia.kernel.cdp.runtime;

import org.miaixz.bus.core.lang.Normal;

/**
 * Represents isolated worlds.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CdpIsolatedWorlds {

    /**
     * Shared constant for main world.
     */
    public static final WorldKey MAIN_WORLD = new WorldKey("mainWorld");

    /**
     * Shared constant for puppeteer world.
     */
    public static final WorldKey PUPPETEER_WORLD = new WorldKey("puppeteerWorld");

    /**
     * Creates an CdpIsolatedWorlds instance.
     */
    private CdpIsolatedWorlds() {
        // No initialization required.
    }

    /**
     * Returns whether this world is the main execution world.
     *
     * @param worldId world id
     * @return {@code true} when the condition matches
     */
    public static boolean isMainWorld(Object worldId) {
        return MAIN_WORLD == worldId || MAIN_WORLD.name().equals(worldId);
    }

    /**
     * Returns whether this world is the Puppeteer execution world.
     *
     * @param worldId world id
     * @return {@code true} when the condition matches
     */
    public static boolean isPuppeteerWorld(Object worldId) {
        return PUPPETEER_WORLD == worldId || PUPPETEER_WORLD.name().equals(worldId);
    }

    /**
     * Carries the WorldKey data.
     *
     * @param name name
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public record WorldKey(String name) {

        /**
         * Creates an instance.
         *
         * @param name name to use
         */
        public WorldKey {
            if (name == null || name.isBlank()) {
                name = Normal.EMPTY;
            }
        }

        /**
         * Converts this value to string.
         *
         * @return string
         */
        @Override
        public String toString() {
            return name;
        }
    }

}
