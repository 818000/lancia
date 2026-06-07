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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.net.url.RFC3986;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.UrlKit;
import org.miaixz.lancia.browser.BrowserNetwork;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.Browser;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.BrowserTag;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ChromeReleaseChannel;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.FirefoxChannel;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ProfileOptions;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Describes browser metadata.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BrowserData {

    /**
     * Default browser product name.
     */
    public static final String DEFAULT_BROWSER = Browser.CHROME.id();
    /**
     * Default browser build IDs.
     */
    private static final Map<Browser, String> DEFAULT_BUILD_IDS = Map.of(
            Browser.CHROME,
            "149.0.7827.22",
            Browser.CHROME_HEADLESS_SHELL,
            "149.0.7827.22",
            Browser.CHROMEDRIVER,
            "149.0.7827.22",
            Browser.CHROMIUM,
            "1500000",
            Browser.FIREFOX,
            "stable_151.0");

    /**
     * Creates a browser data.
     */
    private BrowserData() {
        // No initialization required.
    }

    /**
     * Resolves download URL.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param buildId  build id
     * @return resolve download URL value
     */
    public static String resolveDownloadUrl(Browser browser, BrowserPlatform platform, String buildId) {
        return resolveDownloadUrl(browser, platform, buildId, null);
    }

    /**
     * Returns the default build ID.
     *
     * @param browser browser instance
     * @return default build ID
     */
    public static String defaultBuildId(Browser browser) {
        Browser actualBrowser = browser == null ? Browser.CHROME : browser;
        String buildId = DEFAULT_BUILD_IDS.get(actualBrowser);
        if (StringKit.isBlank(buildId)) {
            throw new IllegalArgumentException("Invalid browser type: " + browser);
        }
        return buildId;
    }

    /**
     * Returns the default build ID.
     *
     * @param browser browser instance
     * @return default build ID
     */
    public static String defaultBuildId(String browser) {
        return defaultBuildId(Browser.fromValue(browser));
    }

    /**
     * Resolves download URL.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param buildId  build id
     * @param baseUrl  base url
     * @return resolve download URL value
     */
    public static String resolveDownloadUrl(String browser, BrowserPlatform platform, String buildId, String baseUrl) {
        return resolveDownloadUrl(Browser.fromValue(browser), platform, buildId, baseUrl);
    }

    /**
     * Resolves download URL.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param buildId  build id
     * @param baseUrl  base url
     * @return resolve download URL value
     */
    public static String resolveDownloadUrl(Browser browser, BrowserPlatform platform, String buildId, String baseUrl) {
        return switch (Assert.notNull(browser, "browser")) {
            case CHROME -> ChromeBrowserData.resolveDownloadUrl(platform, buildId, baseUrl);
            case CHROME_HEADLESS_SHELL -> ChromeHeadlessShellBrowserData.resolveDownloadUrl(platform, buildId, baseUrl);
            case CHROMEDRIVER -> ChromeDriverBrowserData.resolveDownloadUrl(platform, buildId, baseUrl);
            case CHROMIUM -> ChromiumBrowserData.resolveDownloadUrl(platform, buildId, baseUrl);
            case FIREFOX -> FirefoxBrowserData.resolveDownloadUrl(platform, buildId, baseUrl);
        };
    }

    /**
     * Resolves download path.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param buildId  build id
     * @return resolve download path value
     */
    public static List<String> resolveDownloadPath(Browser browser, BrowserPlatform platform, String buildId) {
        return switch (Assert.notNull(browser, "browser")) {
            case CHROME -> ChromeBrowserData.resolveDownloadPath(platform, buildId);
            case CHROME_HEADLESS_SHELL -> ChromeHeadlessShellBrowserData.resolveDownloadPath(platform, buildId);
            case CHROMEDRIVER -> ChromeDriverBrowserData.resolveDownloadPath(platform, buildId);
            case CHROMIUM -> ChromiumBrowserData.resolveDownloadPath(platform, buildId);
            case FIREFOX -> FirefoxBrowserData.resolveDownloadPath(platform, buildId);
        };
    }

    /**
     * Returns the relative executable path.
     *
     * @param browser  browser instance
     * @param platform platform value
     * @param buildId  build ID value
     * @return relative executable path value
     */
    public static Path relativeExecutablePath(Browser browser, BrowserPlatform platform, String buildId) {
        return switch (Assert.notNull(browser, "browser")) {
            case CHROME -> ChromeBrowserData.relativeExecutablePath(platform, buildId);
            case CHROME_HEADLESS_SHELL -> ChromeHeadlessShellBrowserData.relativeExecutablePath(platform, buildId);
            case CHROMEDRIVER -> ChromeDriverBrowserData.relativeExecutablePath(platform, buildId);
            case CHROMIUM -> ChromiumBrowserData.relativeExecutablePath(platform, buildId);
            case FIREFOX -> FirefoxBrowserData.relativeExecutablePath(platform, buildId);
        };
    }

    /**
     * Resolves build ID.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param tag      tag
     * @return resolve build ID value
     */
    public static String resolveBuildId(Browser browser, BrowserPlatform platform, String tag) {
        BrowserTag browserTag = BrowserTag.fromValueOrNull(tag);
        if (browserTag != null) {
            return resolveBuildIdForTag(browser, platform, browserTag);
        }
        return switch (Assert.notNull(browser, "browser")) {
            case FIREFOX, CHROMIUM -> tag;
            case CHROME -> ChromeBrowserData.resolveBuildId(tag).orElse(tag);
            case CHROMEDRIVER -> ChromeDriverBrowserData.resolveBuildId(tag).orElse(tag);
            case CHROME_HEADLESS_SHELL -> ChromeHeadlessShellBrowserData.resolveBuildId(tag).orElse(tag);
        };
    }

    /**
     * Creates profile.
     *
     * @param browser browser instance
     * @param options operation options
     */
    public static void createProfile(Browser browser, ProfileOptions options) {
        ProfileOptions actualOptions = Assert.notNull(options, "options");
        switch (Assert.notNull(browser, "browser")) {
            case FIREFOX -> FirefoxBrowserData.createProfile(actualOptions);
            case CHROME, CHROMIUM, CHROME_HEADLESS_SHELL -> createChromiumProfile(actualOptions);
            case CHROMEDRIVER -> createChromeDriverProfile(actualOptions);
        }
    }

    /**
     * Resolves system executable path.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param channel  channel
     * @return resolve system executable path value
     */
    public static Path resolveSystemExecutablePath(
            Browser browser,
            BrowserPlatform platform,
            ChromeReleaseChannel channel) {
        List<Path> paths = resolveSystemExecutablePaths(browser, platform, channel);
        if (paths.isEmpty()) {
            throw new IllegalStateException("No system executable path resolved for " + browser.id());
        }
        return paths.get(0);
    }

    /**
     * Resolves system executable paths.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param channel  channel
     * @return resolve system executable paths value
     */
    public static List<Path> resolveSystemExecutablePaths(
            Browser browser,
            BrowserPlatform platform,
            ChromeReleaseChannel channel) {
        return switch (Assert.notNull(browser, "browser")) {
            case CHROME -> ChromeBrowserData.resolveSystemExecutablePaths(platform, channel);
            case CHROME_HEADLESS_SHELL -> List.of();
            case CHROMEDRIVER -> chromeDriverSystemExecutablePaths(platform);
            case CHROMIUM -> chromiumSystemExecutablePaths(platform);
            case FIREFOX -> firefoxSystemExecutablePaths(platform);
        };
    }

    /**
     * Resolves default user data dir.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param channel  channel
     * @return resolve default user data dir value
     */
    public static Path resolveDefaultUserDataDir(
            Browser browser,
            BrowserPlatform platform,
            ChromeReleaseChannel channel) {
        return switch (Assert.notNull(browser, "browser")) {
            case CHROME -> ChromeBrowserData.resolveDefaultUserDataDir(platform, channel);
            case CHROME_HEADLESS_SHELL -> lanciaStateDir(platform, "chrome-headless-shell");
            case CHROMEDRIVER -> lanciaStateDir(platform, "chromedriver");
            case CHROMIUM -> chromiumDefaultUserDataDir(platform);
            case FIREFOX -> firefoxDefaultUserDataDir(platform);
        };
    }

    /**
     * Returns the version comparator.
     *
     * @param browser browser instance
     * @return version comparator
     */
    public static Comparator<String> getVersionComparator(Browser browser) {
        return (left, right) -> compareVersions(browser, left, right);
    }

    /**
     * Returns the compare versions.
     *
     * @param browser browser instance
     * @param left    left value
     * @param right   right value
     * @return compare versions value
     */
    public static int compareVersions(Browser browser, String left, String right) {
        return switch (Assert.notNull(browser, "browser")) {
            case CHROME -> ChromeBrowserData.compareVersions(left, right);
            case CHROME_HEADLESS_SHELL -> ChromeHeadlessShellBrowserData.compareVersions(left, right);
            case CHROMEDRIVER -> ChromeDriverBrowserData.compareVersions(left, right);
            case CHROMIUM -> ChromiumBrowserData.compareVersions(left, right);
            case FIREFOX -> FirefoxBrowserData.compareVersions(left, right);
        };
    }

    /**
     * Returns the resolve build ID for tag.
     *
     * @param browser  browser instance
     * @param platform platform value
     * @param tag      tag value
     * @return resolve build ID for tag value
     */
    private static String resolveBuildIdForTag(Browser browser, BrowserPlatform platform, BrowserTag tag) {
        return switch (Assert.notNull(browser, "browser")) {
            case FIREFOX -> resolveFirefoxBuildIdForTag(tag);
            case CHROME -> resolveChromeBuildIdForTag(tag, "Chrome");
            case CHROMEDRIVER -> resolveChromeBuildIdForTag(tag, "ChromeDriver");
            case CHROME_HEADLESS_SHELL -> resolveChromeBuildIdForTag(tag, "chrome-headless-shell");
            case CHROMIUM -> resolveChromiumBuildIdForTag(platform, tag);
        };
    }

    /**
     * Returns the resolve firefox build ID for tag.
     *
     * @param tag tag value
     * @return resolve firefox build ID for tag value
     */
    private static String resolveFirefoxBuildIdForTag(BrowserTag tag) {
        return switch (tag) {
            case LATEST, NIGHTLY -> FirefoxBrowserData.resolveBuildId(FirefoxChannel.NIGHTLY);
            case BETA -> FirefoxBrowserData.resolveBuildId(FirefoxChannel.BETA);
            case DEVEDITION -> FirefoxBrowserData.resolveBuildId(FirefoxChannel.DEVEDITION);
            case STABLE -> FirefoxBrowserData.resolveBuildId(FirefoxChannel.STABLE);
            case ESR -> FirefoxBrowserData.resolveBuildId(FirefoxChannel.ESR);
            case CANARY, DEV -> throw new IllegalArgumentException(
                    tag.id().toUpperCase() + " does not apply to Firefox.");
        };
    }

    /**
     * Returns the resolve chrome build ID for tag.
     *
     * @param tag         tag value
     * @param browserName browser name value
     * @return resolve chrome build ID for tag value
     */
    private static String resolveChromeBuildIdForTag(BrowserTag tag, String browserName) {
        return switch (tag) {
            case LATEST, CANARY -> ChromeBrowserData.resolveBuildId(ChromeReleaseChannel.CANARY);
            case BETA -> ChromeBrowserData.resolveBuildId(ChromeReleaseChannel.BETA);
            case DEV -> ChromeBrowserData.resolveBuildId(ChromeReleaseChannel.DEV);
            case STABLE -> ChromeBrowserData.resolveBuildId(ChromeReleaseChannel.STABLE);
            case NIGHTLY, DEVEDITION, ESR -> throw new IllegalArgumentException(
                    tag.id().toUpperCase() + " does not apply to " + browserName + ".");
        };
    }

    /**
     * Returns the resolve chromium build ID for tag.
     *
     * @param platform platform value
     * @param tag      tag value
     * @return resolve chromium build ID for tag value
     */
    private static String resolveChromiumBuildIdForTag(BrowserPlatform platform, BrowserTag tag) {
        if (tag == BrowserTag.LATEST) {
            return ChromiumBrowserData.resolveBuildId(platform);
        }
        throw new IllegalArgumentException(tag.id() + " does not apply to Chromium, use latest.");
    }

    /**
     * Creates chromium profile.
     *
     * @param options operation options
     */
    private static void createChromiumProfile(ProfileOptions options) {
        try {
            FileKit.mkdir(options.path().toFile());
            Files.writeString(options.path().resolve("Preferences"), toJsonObject(options.preferences()));
            Files.writeString(options.path().resolve("Local State"), jsonObjectLine(Map.of()));
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Failed to create browser profile: " + options.path(), ex);
        }
    }

    /**
     * Creates chrome driver profile.
     *
     * @param options operation options
     */
    private static void createChromeDriverProfile(ProfileOptions options) {
        try {
            FileKit.mkdir(options.path().toFile());
            Files.writeString(
                    options.path().resolve("chromedriver-profile.json"),
                    jsonObjectLine(Map.of("browser", Browser.CHROMEDRIVER.id())));
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Failed to create browser profile: " + options.path(), ex);
        }
    }

    /**
     * Returns the chromium system executable paths.
     *
     * @param platform platform value
     * @return values
     */
    private static List<Path> chromiumSystemExecutablePaths(BrowserPlatform platform) {
        return switch (Assert.notNull(platform, "platform")) {
            case MAC, MAC_ARM64 -> List.of(Path.of("/Applications/Chromium.app/Contents/MacOS/Chromium"));
            case LINUX, LINUX_ARM64 -> List.of(
                    Path.of("/usr/bin/chromium"),
                    Path.of("/usr/bin/chromium-browser"),
                    Path.of("/snap/bin/chromium"));
            case WIN32, WIN64 -> List.of(
                    Path.of(programFilesWin(), "Chromium", "Application", "chrome.exe"),
                    Path.of(localAppDataWin(), "Chromium", "Application", "chrome.exe"));
        };
    }

    /**
     * Returns the firefox system executable paths.
     *
     * @param platform platform value
     * @return values
     */
    private static List<Path> firefoxSystemExecutablePaths(BrowserPlatform platform) {
        return switch (Assert.notNull(platform, "platform")) {
            case MAC, MAC_ARM64 -> List.of(
                    Path.of("/Applications/Firefox.app/Contents/MacOS/firefox"),
                    Path.of("/Applications/Firefox Nightly.app/Contents/MacOS/firefox"));
            case LINUX, LINUX_ARM64 -> List.of(Path.of("/usr/bin/firefox"), Path.of("/snap/bin/firefox"));
            case WIN32, WIN64 -> List.of(
                    Path.of(programFilesWin(), "Mozilla Firefox", "firefox.exe"),
                    Path.of(localAppDataWin(), "Mozilla Firefox", "firefox.exe"));
        };
    }

    /**
     * Returns the chrome driver system executable paths.
     *
     * @param platform platform value
     * @return values
     */
    private static List<Path> chromeDriverSystemExecutablePaths(BrowserPlatform platform) {
        return switch (Assert.notNull(platform, "platform")) {
            case MAC, MAC_ARM64, LINUX, LINUX_ARM64 -> List.of(Path.of("/usr/bin/chromedriver"));
            case WIN32, WIN64 -> List.of(Path.of(programFilesWin(), "ChromeDriver", "chromedriver.exe"));
        };
    }

    /**
     * Returns the chromium default user data dir.
     *
     * @param platform platform value
     * @return chromium default user data dir value
     */
    private static Path chromiumDefaultUserDataDir(BrowserPlatform platform) {
        return switch (Assert.notNull(platform, "platform")) {
            case WIN32, WIN64 -> Path.of(localAppDataWin(), "Chromium", "User Data");
            case MAC, MAC_ARM64 -> Path.of(homeDir(), "Library", "Application Support", "Chromium");
            case LINUX, LINUX_ARM64 -> Path.of(configHomeLinux(), "chromium");
        };
    }

    /**
     * Returns the firefox default user data dir.
     *
     * @param platform platform value
     * @return firefox default user data dir value
     */
    private static Path firefoxDefaultUserDataDir(BrowserPlatform platform) {
        return switch (Assert.notNull(platform, "platform")) {
            case WIN32, WIN64 -> Path.of(roamingAppDataWin(), "Mozilla", "Firefox", "Profiles");
            case MAC, MAC_ARM64 -> Path.of(homeDir(), "Library", "Application Support", "Firefox", "Profiles");
            case LINUX, LINUX_ARM64 -> Path.of(homeDir(), ".mozilla", "firefox");
        };
    }

    /**
     * Returns the lancia state dir.
     *
     * @param platform platform value
     * @param name     name to use
     * @return lancia state dir value
     */
    private static Path lanciaStateDir(BrowserPlatform platform, String name) {
        return switch (Assert.notNull(platform, "platform")) {
            case WIN32, WIN64 -> Path.of(localAppDataWin(), "Lancia", name);
            case MAC, MAC_ARM64 -> Path.of(homeDir(), "Library", "Application Support", "Lancia", name);
            case LINUX, LINUX_ARM64 -> Path.of(configHomeLinux(), "lancia", name);
        };
    }

    /**
     * Returns the json object line.
     *
     * @param values values value
     * @return json object line value
     */
    private static String jsonObjectLine(Map<String, Object> values) {
        return toJsonObject(values) + System.lineSeparator();
    }

    /**
     * Converts this value to json object.
     *
     * @param values values value
     * @return json object
     */
    private static String toJsonObject(Map<String, Object> values) {
        StringBuilder builder = new StringBuilder(Symbol.BRACE_LEFT);
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                builder.append(Symbol.C_COMMA);
            }
            first = false;
            builder.append(quoteJson(entry.getKey())).append(Symbol.C_COLON).append(toJsonValue(entry.getValue()));
        }
        return builder.append(Symbol.C_BRACE_RIGHT).toString();
    }

    /**
     * Converts this value to json value.
     *
     * @param value to use
     * @return JSON value
     */
    private static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            return toJsonObject((Map<String, Object>) map);
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder(Symbol.BRACKET_LEFT);
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(Symbol.C_COMMA);
                }
                first = false;
                builder.append(toJsonValue(item));
            }
            return builder.append(Symbol.C_BRACKET_RIGHT).toString();
        }
        return quoteJson(String.valueOf(value));
    }

    /**
     * Returns the quote json.
     *
     * @param value to use
     * @return quote json value
     */
    private static String quoteJson(String value) {
        return Symbol.DOUBLE_QUOTES + String.valueOf(value).replace(Symbol.BACKSLASH, "\\\\")
                .replace(Symbol.DOUBLE_QUOTES, "\\\"").replace(Symbol.LF, "\\n").replace(Symbol.CR, "\\r")
                + Symbol.DOUBLE_QUOTES;
    }

    /**
     * Returns the program files win.
     *
     * @return program files win value
     */
    private static String programFilesWin() {
        String value = System.getenv("PROGRAMFILES");
        return StringKit.isBlank(value) ? "C:\\Program Files" : value;
    }

    /**
     * Returns the local app data win.
     *
     * @return local app data win value
     */
    static String localAppDataWin() {
        return appDataWin("LOCALAPPDATA", "Local");
    }

    /**
     * Returns the roaming app data win.
     *
     * @return roaming app data win value
     */
    private static String roamingAppDataWin() {
        return appDataWin("APPDATA", "Roaming");
    }

    /**
     * Returns the app data win.
     *
     * @param variable variable value
     * @param folder   folder value
     * @return app data win value
     */
    private static String appDataWin(String variable, String folder) {
        String value = System.getenv(variable);
        return StringKit.isBlank(value) ? Path.of(homeDir(), "AppData", folder).toString() : value;
    }

    /**
     * Returns the config home linux.
     *
     * @return config home linux value
     */
    private static String configHomeLinux() {
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        return StringKit.isBlank(xdgConfigHome) ? Path.of(homeDir(), ".config").toString() : xdgConfigHome;
    }

    /**
     * Returns the home dir.
     *
     * @return home dir value
     */
    private static String homeDir() {
        return System.getProperty("user.home");
    }

    /**
     * Returns the join URL.
     *
     * @param baseUrl  base URL value
     * @param segments segments value
     * @return join URL value
     */
    static String joinUrl(String baseUrl, List<String> segments) {
        StringBuilder builder = new StringBuilder(trimTrailingSlash(baseUrl));
        for (String segment : segments) {
            builder.append(Symbol.SLASH).append(encodePathSegment(segment));
        }
        return builder.toString();
    }

    /**
     * Returns the encode path segment.
     *
     * @param segment segment value
     * @return encode path segment value
     */
    static String encodePathSegment(String segment) {
        return RFC3986.SEGMENT.encode(String.valueOf(segment), Charset.UTF_8);
    }

    /**
     * Returns the trim trailing slash.
     *
     * @param value to use
     * @return trim trailing slash value
     */
    public static String trimTrailingSlash(String value) {
        String actualValue = value == null ? Normal.EMPTY : value;
        while (actualValue.endsWith(Symbol.SLASH)) {
            actualValue = actualValue.substring(0, actualValue.length() - 1);
        }
        return actualValue;
    }

    /**
     * Returns the text.
     *
     * @param uri target URI
     * @return text
     */
    static String getText(URI uri) {
        return BrowserNetwork
                .getText(Assert.notNull(uri, "uri"), SecurityPolicy.defaultPolicy(), ResourceLimits.defaults());
    }

    /**
     * Returns the find json string.
     *
     * @param json json value
     * @param key  key value
     * @return optional value
     */
    static Optional<String> findJsonString(String json, String key) {
        if (StringKit.isBlank(json) || StringKit.isBlank(key)) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile(
                Symbol.DOUBLE_QUOTES + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /**
     * Returns the URI.
     *
     * @param value to use
     * @return URI value
     */
    static URI uri(String value) {
        return UrlKit.toURI(value);
    }

    /**
     * Returns the compare numeric text.
     *
     * @param left  left value
     * @param right right value
     * @return compare numeric text value
     */
    public static int compareNumericText(String left, String right) {
        String normalizedLeft = trimLeadingZeros(String.valueOf(left));
        String normalizedRight = trimLeadingZeros(String.valueOf(right));
        int lengthCompare = Integer.compare(normalizedLeft.length(), normalizedRight.length());
        if (lengthCompare != 0) {
            return lengthCompare;
        }
        return normalizedLeft.compareTo(normalizedRight);
    }

    /**
     * Returns the trim leading zeros.
     *
     * @param value to use
     * @return trim leading zeros value
     */
    public static String trimLeadingZeros(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }

    /**
     * Returns the copy preferences.
     *
     * @param source source value
     * @return mapped values
     */
    static Map<String, Object> copyPreferences(Map<String, Object> source) {
        return source == null ? Map.of() : Map.copyOf(source);
    }

}
