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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.FirefoxChannel;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ProfileOptions;

/**
 * Describes firefox browser metadata.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class FirefoxBrowserData {

    /**
     * Default version base URL.
     */
    private static final String DEFAULT_VERSION_BASE_URL = "https://product-details.mozilla.org/1.0";
    /**
     * Shared constant for nightly base URL.
     */
    private static final String NIGHTLY_BASE_URL = "https://archive.mozilla.org/pub/firefox/nightly/latest-mozilla-central";
    /**
     * Shared constant for dev edition base URL.
     */
    private static final String DEV_EDITION_BASE_URL = "https://archive.mozilla.org/pub/devedition/releases";
    /**
     * Shared constant for release base URL.
     */
    private static final String RELEASE_BASE_URL = "https://archive.mozilla.org/pub/firefox/releases";
    /**
     * Shared constant for nightly prefix.
     */
    private static final String NIGHTLY_PREFIX = "nightly_";
    /**
     * Current base version URL.
     */
    private static String baseVersionUrl = DEFAULT_VERSION_BASE_URL;

    /**
     * Creates a firefox browser data.
     */
    private FirefoxBrowserData() {
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
        FirefoxBuild build = FirefoxBuild.parse(buildId);
        String actualBaseUrl = StringKit.isBlank(baseUrl) ? defaultDownloadBaseUrl(build.channel())
                : BrowserData.trimTrailingSlash(baseUrl);
        return BrowserData.joinUrl(actualBaseUrl, resolveDownloadPath(platform, buildId));
    }

    /**
     * Resolves download path.
     *
     * @param platform platform
     * @param buildId  build id
     * @return resolve download path value
     */
    public static List<String> resolveDownloadPath(BrowserPlatform platform, String buildId) {
        FirefoxBuild build = FirefoxBuild.parse(buildId);
        return switch (build.channel()) {
            case NIGHTLY -> List.of(nightlyArchive(platform, build.version()));
            case DEVEDITION, BETA, STABLE, ESR -> List
                    .of(build.version(), platformName(platform), "en-US", releaseArchive(platform, build.version()));
        };
    }

    /**
     * Returns the relative executable path.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return relative executable path value
     */
    public static Path relativeExecutablePath(BrowserPlatform platform, String buildId) {
        FirefoxBuild build = FirefoxBuild.parse(buildId);
        return switch (build.channel()) {
            case NIGHTLY -> switch (Assert.notNull(platform, "platform")) {
                case MAC, MAC_ARM64 -> Path.of("Firefox Nightly.app", "Contents", "MacOS", "firefox");
                case LINUX, LINUX_ARM64 -> Path.of("firefox", "firefox");
                case WIN32, WIN64 -> Path.of("firefox", "firefox.exe");
            };
            case BETA, DEVEDITION, ESR, STABLE -> switch (Assert.notNull(platform, "platform")) {
                case MAC, MAC_ARM64 -> Path.of("Firefox.app", "Contents", "MacOS", "firefox");
                case LINUX, LINUX_ARM64 -> Path.of("firefox", "firefox");
                case WIN32, WIN64 -> Path.of("core", "firefox.exe");
            };
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
     * Resolves build ID.
     *
     * @param channel channel
     * @return resolve build ID value
     */
    public static String resolveBuildId(FirefoxChannel channel) {
        String json = BrowserData.getText(BrowserData.uri(baseVersionUrl + "/firefox_versions.json"));
        String version = BrowserData.findJsonString(json, Assert.notNull(channel, "channel").versionKey())
                .orElseThrow(() -> new IllegalStateException("Missing Firefox channel version: " + channel.id()));
        return channel.id() + Symbol.UNDERLINE + version;
    }

    /**
     * Creates profile.
     *
     * @param options operation options
     */
    public static void createProfile(ProfileOptions options) {
        ProfileOptions actualOptions = Assert.notNull(options, "options");
        try {
            FileKit.mkdir(actualOptions.path().toFile());
            syncPreferences(actualOptions);
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Failed to create Firefox profile: " + actualOptions.path(), ex);
        }
    }

    /**
     * Returns the compare versions.
     *
     * @param left  left value
     * @param right right value
     * @return compare versions value
     */
    public static int compareVersions(String left, String right) {
        return Integer.compare(hexVersion(left), hexVersion(right));
    }

    /**
     * Handles sync preferences.
     *
     * @param options operation options
     * @throws IOException if the operation fails
     */
    private static void syncPreferences(ProfileOptions options) throws IOException {
        Map<String, Object> preferences = defaultProfilePreferences();
        preferences.putAll(options.preferences());
        Path prefsPath = options.path().resolve("prefs.js");
        Path userPath = options.path().resolve("user.js");
        backupFile(userPath);
        backupFile(prefsPath);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : preferences.entrySet()) {
            builder.append("user_pref(").append(quote(entry.getKey())).append(", ")
                    .append(formatPreferenceValue(entry.getValue())).append(");").append(System.lineSeparator());
        }
        Files.writeString(userPath, builder.toString());
    }

    /**
     * Handles backup file.
     *
     * @param input input source
     * @throws IOException if the operation fails
     */
    private static void backupFile(Path input) throws IOException {
        if (Files.exists(input)) {
            Files.copy(
                    input,
                    Path.of(input.toString() + ".puppeteer"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Returns the default profile preferences.
     *
     * @return mapped values
     */
    private static Map<String, Object> defaultProfilePreferences() {
        String server = "dummy.test";
        Map<String, Object> prefs = new LinkedHashMap<>();
        prefs.put("app.normandy.api_url", Normal.EMPTY);
        prefs.put("app.update.checkInstallTime", false);
        prefs.put("app.update.disabledForTesting", true);
        prefs.put("apz.content_response_timeout", 60000);
        prefs.put("browser.pagethumbnails.capturing_disabled", true);
        prefs.put("browser.safebrowsing.blockedURIs.enabled", false);
        prefs.put("browser.safebrowsing.downloads.enabled", false);
        prefs.put("browser.safebrowsing.malware.enabled", false);
        prefs.put("browser.safebrowsing.phishing.enabled", false);
        prefs.put("browser.search.update", false);
        prefs.put("browser.sessionstore.resume_from_crash", false);
        prefs.put("browser.shell.checkDefaultBrowser", false);
        prefs.put("browser.startup.homepage", "about:blank");
        prefs.put("browser.startup.homepage_override.mstone", "ignore");
        prefs.put("browser.startup.page", 0);
        prefs.put("browser.tabs.warnOnCloseOtherTabs", false);
        prefs.put("browser.tabs.warnOnOpen", false);
        prefs.put("browser.urlbar.suggest.searches", false);
        prefs.put("browser.warnOnQuit", false);
        prefs.put("datareporting.healthreport.documentServerURI", "http://" + server + "/dummy/healthreport/");
        prefs.put("datareporting.healthreport.uploadEnabled", false);
        prefs.put("datareporting.policy.dataSubmissionEnabled", false);
        prefs.put("devtools.jsonview.enabled", false);
        prefs.put("dom.disable_open_during_load", false);
        prefs.put("dom.file.createInChild", true);
        prefs.put("dom.ipc.reportProcessHangs", false);
        prefs.put("dom.max_chrome_script_run_time", 0);
        prefs.put("dom.max_script_run_time", 0);
        prefs.put("extensions.autoDisableScopes", 0);
        prefs.put("extensions.enabledScopes", 5);
        prefs.put("extensions.getAddons.cache.enabled", false);
        prefs.put("extensions.installDistroAddons", false);
        prefs.put("extensions.update.enabled", false);
        prefs.put("extensions.webservice.discoverURL", "http://" + server + "/dummy/discoveryURL");
        prefs.put("focusmanager.testmode", true);
        prefs.put("general.useragent.updates.enabled", false);
        prefs.put("geo.provider.testing", true);
        prefs.put("geo.wifi.scan", false);
        prefs.put("hangmonitor.timeout", 0);
        prefs.put("javascript.options.showInConsole", true);
        prefs.put("media.gmp-manager.updateEnabled", false);
        prefs.put("media.sanity-test.disabled", true);
        prefs.put("network.cookie.sameSite.laxByDefault", false);
        prefs.put("network.http.prompt-temp-redirect", false);
        prefs.put("network.http.speculative-parallel-limit", 0);
        prefs.put("network.manage-offline-status", false);
        prefs.put("network.sntp.pools", server);
        prefs.put("plugin.state.flash", 0);
        prefs.put("privacy.trackingprotection.enabled", false);
        prefs.put("remote.enabled", true);
        prefs.put("remote.bidi.dismiss_file_pickers.enabled", true);
        prefs.put("screenshots.browser.component.enabled", false);
        prefs.put("security.certerrors.mitm.priming.enabled", false);
        prefs.put("security.fileuri.strict_origin_policy", false);
        prefs.put("security.notification_enable_delay", 0);
        prefs.put("services.settings.server", "http://" + server + "/dummy/blocklist/");
        prefs.put("signon.autofillForms", false);
        prefs.put("signon.rememberSignons", false);
        prefs.put("startup.homepage_welcome_url", "about:blank");
        prefs.put("startup.homepage_welcome_url.additional", Normal.EMPTY);
        prefs.put("toolkit.cosmeticAnimations.enabled", false);
        prefs.put("toolkit.startup.max_resumed_crashes", -1);
        return prefs;
    }

    /**
     * Returns the format preference value.
     *
     * @param value to use
     * @return format preference value
     */
    private static String formatPreferenceValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value));
    }

    /**
     * Returns the quote.
     *
     * @param value to use
     * @return quote value
     */
    private static String quote(String value) {
        return Symbol.DOUBLE_QUOTES
                + String.valueOf(value).replace(Symbol.BACKSLASH, "\\\\").replace(Symbol.DOUBLE_QUOTES, "\\\"")
                + Symbol.DOUBLE_QUOTES;
    }

    /**
     * Returns the default download base URL.
     *
     * @param channel channel value
     * @return default download base URL value
     */
    private static String defaultDownloadBaseUrl(FirefoxChannel channel) {
        return switch (Assert.notNull(channel, "channel")) {
            case NIGHTLY -> NIGHTLY_BASE_URL;
            case DEVEDITION -> DEV_EDITION_BASE_URL;
            case BETA, STABLE, ESR -> RELEASE_BASE_URL;
        };
    }

    /**
     * Returns the nightly archive.
     *
     * @param platform platform value
     * @param version  version value
     * @return nightly archive value
     */
    private static String nightlyArchive(BrowserPlatform platform, String version) {
        return switch (Assert.notNull(platform, "platform")) {
            case LINUX -> "firefox-" + version + ".en-US.linux-x86_64.tar." + format(version);
            case LINUX_ARM64 -> "firefox-" + version + ".en-US.linux-aarch64.tar." + format(version);
            case MAC, MAC_ARM64 -> "firefox-" + version + ".en-US.mac.dmg";
            case WIN32, WIN64 -> "firefox-" + version + ".en-US." + platform.cacheId() + ".zip";
        };
    }

    /**
     * Returns the release archive.
     *
     * @param platform platform value
     * @param version  version value
     * @return release archive value
     */
    private static String releaseArchive(BrowserPlatform platform, String version) {
        return switch (Assert.notNull(platform, "platform")) {
            case LINUX, LINUX_ARM64 -> "firefox-" + version + ".tar." + format(version);
            case MAC, MAC_ARM64 -> "Firefox " + version + ".dmg";
            case WIN32, WIN64 -> "Firefox Setup " + version + ".exe";
        };
    }

    /**
     * Returns the platform name.
     *
     * @param platform platform value
     * @return platform name value
     */
    private static String platformName(BrowserPlatform platform) {
        return switch (Assert.notNull(platform, "platform")) {
            case LINUX -> "linux-x86_64";
            case LINUX_ARM64 -> "linux-aarch64";
            case MAC, MAC_ARM64 -> "mac";
            case WIN32, WIN64 -> platform.cacheId();
        };
    }

    /**
     * Returns the format.
     *
     * @param buildId build ID value
     * @return format value
     */
    private static String format(String buildId) {
        String major = String.valueOf(buildId).split("\\.")[0];
        try {
            return Integer.parseInt(major) >= 135 ? "xz" : "bz2";
        } catch (NumberFormatException ex) {
            return "bz2";
        }
    }

    /**
     * Returns the hex version.
     *
     * @param value to use
     * @return hex version value
     */
    private static int hexVersion(String value) {
        return Integer.parseInt(String.valueOf(value).replace(Symbol.DOT, Normal.EMPTY), 16);
    }

    /**
     * Carries the FirefoxBuild data.
     *
     * @param channel channel
     * @param version version
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private record FirefoxBuild(FirefoxChannel channel, String version) {

        /**
         * Returns the parse.
         *
         * @param buildId build ID value
         * @return parse value
         */
        private static FirefoxBuild parse(String buildId) {
            String actualBuildId = StringKit.isBlank(buildId) ? Normal.EMPTY : buildId;
            for (FirefoxChannel channel : FirefoxChannel.values()) {
                String prefix = channel.id() + Symbol.UNDERLINE;
                if (actualBuildId.startsWith(prefix)) {
                    return new FirefoxBuild(channel, actualBuildId.substring(prefix.length()));
                }
            }
            if (actualBuildId.startsWith(NIGHTLY_PREFIX)) {
                return new FirefoxBuild(FirefoxChannel.NIGHTLY, actualBuildId.substring(NIGHTLY_PREFIX.length()));
            }
            return new FirefoxBuild(FirefoxChannel.NIGHTLY, actualBuildId);
        }
    }

}
