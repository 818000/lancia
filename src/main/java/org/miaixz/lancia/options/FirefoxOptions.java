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
package org.miaixz.lancia.options;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Firefox-specific launch options.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class FirefoxOptions extends LaunchOptions {

    /**
     * Additional Firefox preferences.
     */
    private final Map<String, Object> preferences = new LinkedHashMap<>();

    /**
     * Creates Firefox launch options.
     */
    public FirefoxOptions() {
        // No initialization required.
    }

    /**
     * Returns Firefox preferences.
     *
     * @return immutable Firefox preferences
     */
    public Map<String, Object> getPreferences() {
        return Map.copyOf(preferences);
    }

    /**
     * Adds one Firefox preference.
     *
     * @param name  preference name
     * @param value preference value
     */
    public void putPreference(String name, Object value) {
        preferences.put(name, value);
    }

}
