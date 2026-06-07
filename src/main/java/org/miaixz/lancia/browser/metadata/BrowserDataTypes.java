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
package org.miaixz.lancia.browser.metadata;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.StringKit;

/**
 * Defines browser data type metadata.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BrowserDataTypes {

    /**
     * Creates a browser data types.
     */
    private BrowserDataTypes() {
        // No initialization required.
    }

    /**
     * Enumerates Browsers.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public enum Browser {

        /**
         * Represents the chrome enum member.
         */
        CHROME("chrome"),

        /**
         * Represents the chrome headless shell enum member.
         */
        CHROME_HEADLESS_SHELL("chrome-headless-shell"),

        /**
         * Represents the chromium enum member.
         */
        CHROMIUM("chromium"),

        /**
         * Represents the firefox enum member.
         */
        FIREFOX("firefox"),

        /**
         * Represents the chromedriver enum member.
         */
        CHROMEDRIVER("chromedriver");

        /**
         * Current identifier.
         */
        private final String id;

        /**
         * Creates an instance.
         *
         * @param id identifier
         */
        Browser(String id) {
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
        public static Browser fromValue(String value) {
            String actualValue = StringKit.isBlank(value) ? CHROME.id : value.toLowerCase(Locale.ROOT);
            for (Browser browser : values()) {
                if (browser.id.equals(actualValue)) {
                    return browser;
                }
            }
            throw new IllegalArgumentException("Invalid browser type: " + value);
        }
    }

    /**
     * Enumerates BrowserTags.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public enum BrowserTag {

        /**
         * Represents the canary enum member.
         */
        CANARY("canary"),

        /**
         * Represents the nightly enum member.
         */
        NIGHTLY("nightly"),

        /**
         * Represents the beta enum member.
         */
        BETA("beta"),

        /**
         * Represents the dev enum member.
         */
        DEV("dev"),

        /**
         * Represents the devedition enum member.
         */
        DEVEDITION("devedition"),

        /**
         * Represents the stable enum member.
         */
        STABLE("stable"),

        /**
         * Represents the esr enum member.
         */
        ESR("esr"),

        /**
         * Represents the latest enum member.
         */
        LATEST("latest");

        /**
         * Current identifier.
         */
        private final String id;

        /**
         * Creates an instance.
         *
         * @param id identifier
         */
        BrowserTag(String id) {
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
         * Creates this value from value or null.
         *
         * @param value to use
         * @return from value or null value
         */
        public static BrowserTag fromValueOrNull(String value) {
            if (StringKit.isBlank(value)) {
                return null;
            }
            String actualValue = value.toLowerCase(Locale.ROOT);
            for (BrowserTag tag : values()) {
                if (tag.id.equals(actualValue)) {
                    return tag;
                }
            }
            return null;
        }
    }

    /**
     * Enumerates ChromeReleaseChannels.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public enum ChromeReleaseChannel {

        /**
         * Represents the stable enum member.
         */
        STABLE("stable", "Stable"),

        /**
         * Represents the dev enum member.
         */
        DEV("dev", "Dev"),

        /**
         * Represents the canary enum member.
         */
        CANARY("canary", "Canary"),

        /**
         * Represents the beta enum member.
         */
        BETA("beta", "Beta");

        /**
         * Current identifier.
         */
        private final String id;
        /**
         * Current api name.
         */
        private final String apiName;

        /**
         * Creates an instance.
         *
         * @param id      identifier
         * @param apiName api name value
         */
        ChromeReleaseChannel(String id, String apiName) {
            this.id = id;
            this.apiName = apiName;
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
         * Returns the api name.
         *
         * @return api name value
         */
        public String apiName() {
            return apiName;
        }

        /**
         * Creates this value from value.
         *
         * @param value to use
         * @return from value
         */
        public static ChromeReleaseChannel fromValue(String value) {
            String actualValue = Assert.notBlank(value, "value").toLowerCase(Locale.ROOT);
            for (ChromeReleaseChannel channel : values()) {
                if (channel.id.equals(actualValue)) {
                    return channel;
                }
            }
            throw new IllegalArgumentException("Invalid Chrome release channel: " + value);
        }
    }

    /**
     * Enumerates FirefoxChannels.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public enum FirefoxChannel {

        /**
         * Represents the stable enum member.
         */
        STABLE("stable", "LATEST_FIREFOX_VERSION"),

        /**
         * Represents the esr enum member.
         */
        ESR("esr", "FIREFOX_ESR"),

        /**
         * Represents the devedition enum member.
         */
        DEVEDITION("devedition", "FIREFOX_DEVEDITION"),

        /**
         * Represents the beta enum member.
         */
        BETA("beta", "FIREFOX_DEVEDITION"),

        /**
         * Represents the nightly enum member.
         */
        NIGHTLY("nightly", "FIREFOX_NIGHTLY");

        /**
         * Current identifier.
         */
        private final String id;
        /**
         * Current version key.
         */
        private final String versionKey;

        /**
         * Creates an instance.
         *
         * @param id         identifier
         * @param versionKey version key value
         */
        FirefoxChannel(String id, String versionKey) {
            this.id = id;
            this.versionKey = versionKey;
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
         * Returns the version key.
         *
         * @return version key value
         */
        public String versionKey() {
            return versionKey;
        }
    }

    /**
     * Defines options for profile operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class ProfileOptions {

        /**
         * Mapped preferences values.
         */
        private final Map<String, Object> preferences;
        /**
         * Current path.
         */
        private final Path path;

        /**
         * Creates an instance.
         *
         * @param path        file path
         * @param preferences preferences value
         */
        public ProfileOptions(Path path, Map<String, Object> preferences) {
            this.path = Assert.notNull(path, "path");
            this.preferences = preferences == null ? new LinkedHashMap<>() : new LinkedHashMap<>(preferences);
        }

        /**
         * Returns the preferences.
         *
         * @return mapped values
         */
        public Map<String, Object> preferences() {
            return new LinkedHashMap<>(preferences);
        }

        /**
         * Returns the path.
         *
         * @return path value
         */
        public Path path() {
            return path;
        }
    }

}
