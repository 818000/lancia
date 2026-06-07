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
import java.util.ArrayList;
import java.util.List;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ChromeReleaseChannel;

/**
 * Describes chrome browser metadata.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ChromeBrowserData {

    /**
     * Default download base URL.
     */
    public static final String DEFAULT_DOWNLOAD_BASE_URL = "https://storage.googleapis.com/chrome-for-testing-public";
    /**
     * Default version base URL.
     */
    private static final String DEFAULT_VERSION_BASE_URL = "https://googlechromelabs.github.io/chrome-for-testing";
    /**
     * Shared constant for windows env param names.
     */
    private static final List<String> WINDOWS_ENV_PARAM_NAMES = List
            .of("PROGRAMFILES", "ProgramW6432", "ProgramFiles(x86)", "LOCALAPPDATA");
    /**
     * Current base version URL.
     */
    private static String baseVersionUrl = DEFAULT_VERSION_BASE_URL;

    /**
     * Creates a chrome browser data.
     */
    private ChromeBrowserData() {
        // No initialization required.
    }

    /**
     * Resolves download URL.
     *
     * @param platform platform
     * @param buildId  build id
     * @param baseUrl  base url
     * @return resolve download URL value
     */
    public static String resolveDownloadUrl(BrowserPlatform platform, String buildId, String baseUrl) {
        return BrowserData.joinUrl(downloadBaseUrl(baseUrl), resolveDownloadPath(platform, buildId));
    }

    /**
     * Resolves download path.
     *
     * @param platform platform
     * @param buildId  build id
     * @return resolve download path value
     */
    public static List<String> resolveDownloadPath(BrowserPlatform platform, String buildId) {
        return resolveArtifactDownloadPath(platform, buildId, "chrome");
    }

    /**
     * Returns the relative executable path.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return relative executable path value
     */
    public static Path relativeExecutablePath(BrowserPlatform platform, String buildId) {
        String folder = folder(platform);
        return switch (Assert.notNull(platform, "platform")) {
            case MAC, MAC_ARM64 -> Path.of(
                    "chrome-" + folder,
                    "Google Chrome for Testing.app",
                    "Contents",
                    "MacOS",
                    "Google Chrome for Testing");
            case LINUX, LINUX_ARM64 -> Path.of("chrome-linux64", "chrome");
            case WIN32, WIN64 -> Path.of("chrome-" + folder, "chrome.exe");
        };
    }

    /**
     * Handles change base version URL for testing.
     *
     * @param url target URL
     */
    public static void changeBaseVersionUrlForTesting(String url) {
        baseVersionUrl = BrowserData.trimTrailingSlash(url);
    }

    /**
     * Handles reset base version URL for testing.
     */
    public static void resetBaseVersionUrlForTesting() {
        baseVersionUrl = DEFAULT_VERSION_BASE_URL;
    }

    /**
     * Returns the last known good release for channel.
     *
     * @param channel channel
     * @return last known good release for channel
     */
    public static Release getLastKnownGoodReleaseForChannel(ChromeReleaseChannel channel) {
        String json = BrowserData.getText(BrowserData.uri(baseVersionUrl + "/last-known-good-versions.json"));
        String section = findObjectSection(json, channel.apiName());
        return new Release(
                BrowserData.findJsonString(section, "version").orElseThrow(
                        () -> new IllegalStateException("Missing Chrome channel version: " + channel.id())),
                BrowserData.findJsonString(section, "revision").orElse(Normal.EMPTY));
    }

    /**
     * Returns the last known good release for milestone.
     *
     * @param milestone milestone
     * @return last known good release for milestone
     */
    public static Optional<Release> getLastKnownGoodReleaseForMilestone(String milestone) {
        String json = BrowserData.getText(BrowserData.uri(baseVersionUrl + "/latest-versions-per-milestone.json"));
        String section = findObjectSection(json, milestone);
        return BrowserData.findJsonString(section, "version").map(
                version -> new Release(version, BrowserData.findJsonString(section, "revision").orElse(Normal.EMPTY)));
    }

    /**
     * Returns the last known good release for build.
     *
     * @param buildPrefix build prefix
     * @return last known good release for build
     */
    public static Optional<Release> getLastKnownGoodReleaseForBuild(String buildPrefix) {
        String json = BrowserData.getText(BrowserData.uri(baseVersionUrl + "/latest-patch-versions-per-build.json"));
        String section = findObjectSection(json, buildPrefix);
        return BrowserData.findJsonString(section, "version").map(
                version -> new Release(version, BrowserData.findJsonString(section, "revision").orElse(Normal.EMPTY)));
    }

    /**
     * Resolves build ID.
     *
     * @param channel channel
     * @return resolve build ID value
     */
    public static String resolveBuildId(ChromeReleaseChannel channel) {
        return getLastKnownGoodReleaseForChannel(channel).version();
    }

    /**
     * Resolves build ID.
     *
     * @param channelOrPrefix channel or prefix
     * @return resolve build ID value
     */
    public static Optional<String> resolveBuildId(String channelOrPrefix) {
        if (StringKit.isBlank(channelOrPrefix)) {
            return Optional.empty();
        }
        try {
            return Optional.of(resolveBuildId(ChromeReleaseChannel.fromValue(channelOrPrefix)));
        } catch (IllegalArgumentException ex) {
            // Implementation note.
        }
        if (channelOrPrefix.matches("^\\d+$")) {
            return getLastKnownGoodReleaseForMilestone(channelOrPrefix).map(Release::version);
        }
        if (channelOrPrefix.matches("^\\d+\\.\\d+\\.\\d+$")) {
            return getLastKnownGoodReleaseForBuild(channelOrPrefix).map(Release::version);
        }
        return Optional.empty();
    }

    /**
     * Resolves system executable paths.
     *
     * @param platform platform
     * @param channel  channel
     * @return resolve system executable paths value
     */
    public static List<Path> resolveSystemExecutablePaths(BrowserPlatform platform, ChromeReleaseChannel channel) {
        return switch (Assert.notNull(platform, "platform")) {
            case WIN32, WIN64 -> chromeWindowsLocations(channel);
            case MAC, MAC_ARM64 -> chromeMacLocations(channel);
            case LINUX, LINUX_ARM64 -> chromeLinuxLocations(channel);
        };
    }

    /**
     * Resolves default user data dir.
     *
     * @param platform platform
     * @param channel  channel
     * @return resolve default user data dir value
     */
    public static Path resolveDefaultUserDataDir(BrowserPlatform platform, ChromeReleaseChannel channel) {
        return switch (Assert.notNull(platform, "platform")) {
            case WIN32, WIN64 -> switch (Assert.notNull(channel, "channel")) {
                case STABLE -> Path.of(BrowserData.localAppDataWin(), "Google", "Chrome", "User Data");
                case BETA -> Path.of(BrowserData.localAppDataWin(), "Google", "Chrome Beta", "User Data");
                case CANARY -> Path.of(BrowserData.localAppDataWin(), "Google", "Chrome SxS", "User Data");
                case DEV -> Path.of(BrowserData.localAppDataWin(), "Google", "Chrome Dev", "User Data");
            };
            case MAC, MAC_ARM64 -> switch (Assert.notNull(channel, "channel")) {
                case STABLE -> Path.of(homeDir(), "Library", "Application Support", "Google", "Chrome");
                case BETA -> Path.of(homeDir(), "Library", "Application Support", "Google", "Chrome Beta");
                case CANARY -> Path.of(homeDir(), "Library", "Application Support", "Google", "Chrome Canary");
                case DEV -> Path.of(homeDir(), "Library", "Application Support", "Google", "Chrome Dev");
            };
            case LINUX, LINUX_ARM64 -> switch (Assert.notNull(channel, "channel")) {
                case STABLE -> Path.of(configHomeLinux(), "google-chrome");
                case BETA -> Path.of(configHomeLinux(), "google-chrome-beta");
                case CANARY -> Path.of(configHomeLinux(), "google-chrome-canary");
                case DEV -> Path.of(configHomeLinux(), "google-chrome-unstable");
            };
        };
    }

    /**
     * Returns the compare versions.
     *
     * @param left  left value
     * @param right right value
     * @return compare versions value
     */
    public static int compareVersions(String left, String right) {
        int[] leftParts = parseChromeVersion(left);
        int[] rightParts = parseChromeVersion(right);
        for (int i = 0; i < 4; i++) {
            int result = Integer.compare(leftParts[i], rightParts[i]);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /**
     * Returns the folder.
     *
     * @param platform platform value
     * @return folder value
     */
    static String folder(BrowserPlatform platform) {
        return switch (Assert.notNull(platform, "platform")) {
            case LINUX, LINUX_ARM64 -> "linux64";
            case MAC -> "mac-x64";
            case MAC_ARM64 -> "mac-arm64";
            case WIN32 -> "win32";
            case WIN64 -> "win64";
        };
    }

    /**
     * Returns the download base URL.
     *
     * @param baseUrl base URL value
     * @return download base URL value
     */
    static String downloadBaseUrl(String baseUrl) {
        return StringKit.isBlank(baseUrl) ? DEFAULT_DOWNLOAD_BASE_URL : BrowserData.trimTrailingSlash(baseUrl);
    }

    /**
     * Resolves a Chrome for Testing artifact download URL.
     *
     * @param platform platform
     * @param buildId  build id
     * @param baseUrl  base url
     * @param artifact artifact name
     * @return download URL
     */
    static String resolveArtifactDownloadUrl(
            BrowserPlatform platform,
            String buildId,
            String baseUrl,
            String artifact) {
        return BrowserData.joinUrl(downloadBaseUrl(baseUrl), resolveArtifactDownloadPath(platform, buildId, artifact));
    }

    /**
     * Resolves a Chrome for Testing artifact download path.
     *
     * @param platform platform
     * @param buildId  build id
     * @param artifact artifact name
     * @return download path
     */
    static List<String> resolveArtifactDownloadPath(BrowserPlatform platform, String buildId, String artifact) {
        String folder = folder(platform);
        return List.of(Assert.notBlank(buildId, "buildId"), folder, artifact + Symbol.MINUS + folder + ".zip");
    }

    /**
     * Returns the find object section.
     *
     * @param json json value
     * @param key  key value
     * @return find object section value
     */
    private static String findObjectSection(String json, String key) {
        String marker = Symbol.DOUBLE_QUOTES + key + Symbol.DOUBLE_QUOTES;
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return Normal.EMPTY;
        }
        int start = json.indexOf(Symbol.C_BRACE_LEFT, keyIndex + marker.length());
        if (start < 0) {
            return Normal.EMPTY;
        }
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == Symbol.C_BRACE_LEFT) {
                depth++;
            } else if (ch == Symbol.C_BRACE_RIGHT) {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return Normal.EMPTY;
    }

    /**
     * Parses chrome version.
     *
     * @param value to use
     * @return parse chrome version value
     */
    private static int[] parseChromeVersion(String value) {
        String actualValue = Assert.notBlank(value, "value").trim();
        if (!actualValue.matches("^\\d+(?:\\.\\d+){0,3}$")) {
            throw new IllegalArgumentException("Invalid Chrome version: " + value);
        }
        String[] parts = actualValue.split("\\.");
        int[] result = new int[] { 0, 0, 0, 0 };
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }

    /**
     * Returns the chrome windows locations.
     *
     * @param channel channel value
     * @return values
     */
    private static List<Path> chromeWindowsLocations(ChromeReleaseChannel channel) {
        List<String> roots = new ArrayList<>();
        for (String name : WINDOWS_ENV_PARAM_NAMES) {
            String value = System.getenv(name);
            if (!StringKit.isBlank(value) && !roots.contains(value)) {
                roots.add(value);
            }
        }
        addIfMissing(roots, "C:\\Program Files");
        addIfMissing(roots, "C:\\Program Files (x86)");
        addIfMissing(roots, "D:\\Program Files");
        addIfMissing(roots, "D:\\Program Files (x86)");
        String suffix = switch (Assert.notNull(channel, "channel")) {
            case STABLE -> "Google\\Chrome\\Application\\chrome.exe";
            case BETA -> "Google\\Chrome Beta\\Application\\chrome.exe";
            case CANARY -> "Google\\Chrome SxS\\Application\\chrome.exe";
            case DEV -> "Google\\Chrome Dev\\Application\\chrome.exe";
        };
        return roots.stream().map(root -> Path.of(root, suffix)).toList();
    }

    /**
     * Returns the chrome mac locations.
     *
     * @param channel channel value
     * @return values
     */
    private static List<Path> chromeMacLocations(ChromeReleaseChannel channel) {
        return switch (Assert.notNull(channel, "channel")) {
            case STABLE -> List.of(Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"));
            case BETA -> List.of(Path.of("/Applications/Google Chrome Beta.app/Contents/MacOS/Google Chrome Beta"));
            case CANARY -> List
                    .of(Path.of("/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary"));
            case DEV -> List.of(Path.of("/Applications/Google Chrome Dev.app/Contents/MacOS/Google Chrome Dev"));
        };
    }

    /**
     * Returns the chrome linux locations.
     *
     * @param channel channel value
     * @return values
     */
    private static List<Path> chromeLinuxLocations(ChromeReleaseChannel channel) {
        return switch (Assert.notNull(channel, "channel")) {
            case STABLE -> List.of(Path.of("/opt/google/chrome/chrome"));
            case BETA -> List.of(Path.of("/opt/google/chrome-beta/chrome"));
            case CANARY -> List.of(Path.of("/opt/google/chrome-canary/chrome"));
            case DEV -> List.of(Path.of("/opt/google/chrome-unstable/chrome"));
        };
    }

    /**
     * Handles add if missing.
     *
     * @param values values value
     * @param value  value to use
     */
    private static void addIfMissing(List<String> values, String value) {
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    /**
     * Returns the config home linux.
     *
     * @return config home linux value
     */
    private static String configHomeLinux() {
        String chromeConfigHome = System.getenv("CHROME_CONFIG_HOME");
        if (!StringKit.isBlank(chromeConfigHome)) {
            return chromeConfigHome;
        }
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
     * Carries the Release data.
     *
     * @param version  version
     * @param revision revision
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public record Release(String version, String revision) {
    }

}
