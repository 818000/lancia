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
import java.util.Locale;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.xyz.StringKit;

/**
 * Browser variant that can be launched or downloaded.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum BrowserVariant {

    /**
     * Chrome browser variant.
     */
    CHROME("chrome"),

    /**
     * Firefox browser variant.
     */
    FIREFOX("firefox");

    /**
     * Stable browser variant value.
     */
    private final String value;

    /**
     * Creates a browser variant.
     *
     * @param value to use
     */
    BrowserVariant(String value) {
        this.value = Assert.notBlank(value, "value");
    }

    /**
     * Returns the value.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    /**
     * Returns the value.
     *
     * @return value
     */
    public String getValue() {
        return value();
    }

    /**
     * Creates this value from value.
     *
     * @param value to use
     * @return optional value
     */
    public static Optional<BrowserVariant> fromValue(String value) {
        if (StringKit.isBlank(value)) {
            return Optional.empty();
        }
        String actualValue = value.toLowerCase(Locale.ROOT);
        for (BrowserVariant browser : values()) {
            if (browser.value.equals(actualValue)) {
                return Optional.of(browser);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether this browser variant is recognized.
     *
     * @param value to use
     * @return {@code true} when the condition matches
     */
    public static boolean isSupported(String value) {
        return fromValue(value).isPresent();
    }

    /**
     * Returns the values list.
     *
     * @return values
     */
    public static List<String> valuesList() {
        return List.of(CHROME.value, FIREFOX.value);
    }

}
