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

/**
 * Preferred display mode for an installed Progressive Web App.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum PWADisplayMode {

    /**
     * Opens the app in a standalone window.
     */
    STANDALONE("standalone"),

    /**
     * Opens the app in a browser tab.
     */
    BROWSER("browser");

    /**
     * Protocol value.
     */
    private final String value;

    /**
     * Creates a display mode.
     *
     * @param value protocol value
     */
    PWADisplayMode(String value) {
        this.value = value;
    }

    /**
     * Returns the protocol value.
     *
     * @return protocol value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves a display mode from a protocol value.
     *
     * @param value protocol value
     * @return display mode or {@code null}
     */
    public static PWADisplayMode from(String value) {
        for (PWADisplayMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return null;
    }

}
