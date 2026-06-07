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
package org.miaixz.lancia.browser.bundle;

import java.net.URI;
import java.nio.file.Path;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.net.url.RFC3986;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.UrlKit;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.Browser;
import org.miaixz.lancia.browser.metadata.ChromiumBrowserData;

/**
 * Represents default browser provider.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class DefaultBrowserProvider implements BrowserProvider {

    /**
     * Shared constant for name.
     */
    public static final String NAME = "DefaultProvider";
    /**
     * Shared constant for chrome testing base URL.
     */
    private static final String CHROME_TESTING_BASE_URL = "https://storage.googleapis.com/chrome-for-testing-public";
    /**
     * Shared constant for chrome testing npmmirror base URL.
     */
    private static final String CHROME_TESTING_NPMMIRROR_BASE_URL = "https://cdn.npmmirror.com/binaries/chrome-for-testing";
    /**
     * Shared constant for npmmirror provider name.
     */
    private static final String NPMMIRROR_NAME = "NpmMirrorProvider";
    /**
     * Shared constant for chromium base URL.
     */
    private static final String CHROMIUM_BASE_URL = "https://storage.googleapis.com/chromium-browser-snapshots";
    /**
     * Shared constant for firefox nightly base URL.
     */
    private static final String FIREFOX_NIGHTLY_BASE_URL = "https://archive.mozilla.org/pub/firefox/nightly/latest-mozilla-central";
    /**
     * Shared constant for firefox dev edition base URL.
     */
    private static final String FIREFOX_DEV_EDITION_BASE_URL = "https://archive.mozilla.org/pub/devedition/releases";
    /**
     * Shared constant for firefox release base URL.
     */
    private static final String FIREFOX_RELEASE_BASE_URL = "https://archive.mozilla.org/pub/firefox/releases";
    /**
     * Shared constant for firefox stable prefix.
     */
    private static final String FIREFOX_STABLE_PREFIX = "stable_";
    /**
     * Shared constant for firefox esr prefix.
     */
    private static final String FIREFOX_ESR_PREFIX = "esr_";
    /**
     * Shared constant for firefox dev edition prefix.
     */
    private static final String FIREFOX_DEV_EDITION_PREFIX = "devedition_";
    /**
     * Shared constant for firefox beta prefix.
     */
    private static final String FIREFOX_BETA_PREFIX = "beta_";
    /**
     * Current base URL.
     */
    private final String baseUrl;
    /**
     * Whether this provider only supports Chrome for Testing artifacts.
     */
    private final boolean chromeTestingOnly;
    /**
     * Current provider name.
     */
    private final String name;

    /**
     * Creates a default browser provider.
     */
    public DefaultBrowserProvider() {
        this(null);
    }

    /**
     * Creates a default browser provider.
     *
     * @param baseUrl base url
     */
    public DefaultBrowserProvider(String baseUrl) {
        this(baseUrl, false, NAME);
    }

    /**
     * Creates a default browser provider.
     *
     * @param baseUrl           base url
     * @param chromeTestingOnly whether only Chrome for Testing artifacts are supported
     * @param name              provider name
     */
    private DefaultBrowserProvider(String baseUrl, boolean chromeTestingOnly, String name) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.chromeTestingOnly = chromeTestingOnly;
        this.name = StringKit.isBlank(name) ? NAME : name;
    }

    /**
     * Creates an npmmirror Chrome for Testing provider.
     *
     * @return provider
     */
    public static DefaultBrowserProvider npmMirror() {
        return new DefaultBrowserProvider(CHROME_TESTING_NPMMIRROR_BASE_URL, true, NPMMIRROR_NAME);
    }

    /**
     * Returns the supports.
     *
     * @param options operation options
     * @return {@code true} when the condition matches
     */
    @Override
    public boolean supports(FetcherOptions options) {
        if (!chromeTestingOnly) {
            return true;
        }
        return switch (Browser.fromValue(normalizeBrowser(Assert.notNull(options, "options").getBrowser()))) {
            case CHROME, CHROME_HEADLESS_SHELL, CHROMEDRIVER -> true;
            case CHROMIUM, FIREFOX -> false;
        };
    }

    /**
     * Returns the download URL.
     *
     * @param options operation options
     * @return download URL
     */
    @Override
    public URI getDownloadUrl(FetcherOptions options) {
        FetcherOptions actualOptions = Assert.notNull(options, "options");
        String browser = normalizeBrowser(actualOptions.getBrowser());
        BrowserPlatform platform = actualOptions.getPlatformOrDetect();
        String buildId = actualOptions.getBuildId();
        return UrlKit.toURI(BrowserData.resolveDownloadUrl(Browser.fromValue(browser), platform, buildId, baseUrl));
    }

    /**
     * Returns expected archive SHA-256.
     *
     * @param options operation options
     * @return expected archive SHA-256
     */
    @Override
    public String expectedArchiveSha256(FetcherOptions options) {
        return Assert.notNull(options, "options").getExpectedArchiveSha256();
    }

    /**
     * Returns the executable path.
     *
     * @param options operation options
     * @return executable path
     */
    @Override
    public Path getExecutablePath(FetcherOptions options) {
        FetcherOptions actualOptions = Assert.notNull(options, "options");
        return BrowserData.relativeExecutablePath(
                Browser.fromValue(actualOptions.getBrowser()),
                actualOptions.getPlatformOrDetect(),
                actualOptions.getBuildId());
    }

    /**
     * Returns the name.
     *
     * @return name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the chrome testing URL.
     *
     * @param browser  browser instance
     * @param platform platform value
     * @param buildId  build ID value
     * @return chrome testing URL value
     */
    private String chromeTestingUrl(String browser, BrowserPlatform platform, String buildId) {
        String folder = chromeTestingFolder(platform);
        return joinUrl(chromeTestingBaseUrl(), buildId, folder, browser + Symbol.MINUS + folder + ".zip");
    }

    /**
     * Returns the chromium URL.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return chromium URL value
     */
    private String chromiumUrl(BrowserPlatform platform, String buildId) {
        return joinUrl(chromiumBaseUrl(), chromiumFolder(platform), buildId, chromiumArchive(platform, buildId));
    }

    /**
     * Returns the firefox URL.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return firefox URL value
     */
    private String firefoxUrl(BrowserPlatform platform, String buildId) {
        FirefoxBuild firefoxBuild = FirefoxBuild.parse(buildId);
        return switch (firefoxBuild.channel()) {
            case NIGHTLY -> joinUrl(
                    firefoxBaseUrl(FirefoxChannel.NIGHTLY),
                    firefoxNightlyArchive(platform, firefoxBuild.version()));
            case DEVEDITION, BETA, STABLE, ESR -> joinUrl(
                    firefoxBaseUrl(firefoxBuild.channel()),
                    firefoxBuild.version(),
                    firefoxPlatformName(platform),
                    "en-US",
                    firefoxReleaseArchive(platform, firefoxBuild.version()));
        };
    }

    /**
     * Returns the chrome testing base URL.
     *
     * @return chrome testing base URL value
     */
    private String chromeTestingBaseUrl() {
        return StringKit.isBlank(baseUrl) ? CHROME_TESTING_BASE_URL : baseUrl;
    }

    /**
     * Returns the chromium base URL.
     *
     * @return chromium base URL value
     */
    private String chromiumBaseUrl() {
        return StringKit.isBlank(baseUrl) ? CHROMIUM_BASE_URL : baseUrl;
    }

    /**
     * Returns the firefox base URL.
     *
     * @param channel channel value
     * @return firefox base URL value
     */
    private String firefoxBaseUrl(FirefoxChannel channel) {
        if (!StringKit.isBlank(baseUrl)) {
            return baseUrl;
        }
        return switch (channel) {
            case NIGHTLY -> FIREFOX_NIGHTLY_BASE_URL;
            case DEVEDITION -> FIREFOX_DEV_EDITION_BASE_URL;
            case BETA, STABLE, ESR -> FIREFOX_RELEASE_BASE_URL;
        };
    }

    /**
     * Returns the chrome testing folder.
     *
     * @param platform platform value
     * @return chrome testing folder value
     */
    private static String chromeTestingFolder(BrowserPlatform platform) {
        return platformFolder(platform, "linux64", "mac-x64", "mac-arm64", "win32", "win64");
    }

    /**
     * Returns the chromium folder.
     *
     * @param platform platform value
     * @return chromium folder value
     */
    private static String chromiumFolder(BrowserPlatform platform) {
        return platformFolder(platform, "Linux_x64", "Mac", "Mac_Arm", "Win", "Win_x64");
    }

    /**
     * Resolves a browser platform folder.
     *
     * @param platform platform
     * @param linux    Linux folder
     * @param mac      Mac folder
     * @param macArm   Mac ARM folder
     * @param win32    Win32 folder
     * @param win64    Win64 folder
     * @return platform folder value
     */
    private static String platformFolder(
            BrowserPlatform platform,
            String linux,
            String mac,
            String macArm,
            String win32,
            String win64) {
        return switch (platform) {
            case LINUX, LINUX_ARM64 -> linux;
            case MAC -> mac;
            case MAC_ARM64 -> macArm;
            case WIN32 -> win32;
            case WIN64 -> win64;
        };
    }

    /**
     * Returns the chromium archive.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return chromium archive value
     */
    private static String chromiumArchive(BrowserPlatform platform, String buildId) {
        return ChromiumBrowserData.archive(platform, buildId) + ".zip";
    }

    /**
     * Returns the firefox nightly archive.
     *
     * @param platform platform value
     * @param version  version value
     * @return firefox nightly archive value
     */
    private static String firefoxNightlyArchive(BrowserPlatform platform, String version) {
        return switch (platform) {
            case LINUX -> "firefox-" + version + ".en-US.linux-x86_64.tar." + firefoxCompression(version);
            case LINUX_ARM64 -> "firefox-" + version + ".en-US.linux-aarch64.tar." + firefoxCompression(version);
            case MAC, MAC_ARM64 -> "firefox-" + version + ".en-US.mac.dmg";
            case WIN32, WIN64 -> "firefox-" + version + ".en-US." + platform.cacheId() + ".zip";
        };
    }

    /**
     * Returns the firefox release archive.
     *
     * @param platform platform value
     * @param version  version value
     * @return firefox release archive value
     */
    private static String firefoxReleaseArchive(BrowserPlatform platform, String version) {
        return switch (platform) {
            case LINUX, LINUX_ARM64 -> "firefox-" + version + ".tar." + firefoxCompression(version);
            case MAC, MAC_ARM64 -> "Firefox " + version + ".dmg";
            case WIN32, WIN64 -> "Firefox Setup " + version + ".exe";
        };
    }

    /**
     * Returns the firefox platform name.
     *
     * @param platform platform value
     * @return firefox platform name value
     */
    private static String firefoxPlatformName(BrowserPlatform platform) {
        return switch (platform) {
            case LINUX -> "linux-x86_64";
            case LINUX_ARM64 -> "linux-aarch64";
            case MAC, MAC_ARM64 -> "mac";
            case WIN32, WIN64 -> platform.cacheId();
        };
    }

    /**
     * Returns the firefox compression.
     *
     * @param version version value
     * @return firefox compression value
     */
    private static String firefoxCompression(String version) {
        String major = version == null ? Normal.EMPTY : version.split("\\.")[0];
        try {
            return Integer.parseInt(major) >= 135 ? "xz" : "bz2";
        } catch (NumberFormatException ex) {
            return "bz2";
        }
    }

    /**
     * Returns the join URL.
     *
     * @param root     root value
     * @param segments segments value
     * @return join URL value
     */
    private static String joinUrl(String root, String... segments) {
        StringBuilder builder = new StringBuilder(BrowserData.trimTrailingSlash(root));
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
    private static String encodePathSegment(String segment) {
        return RFC3986.SEGMENT.encode(segment, Charset.UTF_8);
    }

    /**
     * Returns the normalize browser.
     *
     * @param browser browser instance
     * @return normalize browser value
     */
    private static String normalizeBrowser(String browser) {
        return StringKit.isBlank(browser) ? ExecutableResolver.CHROME : browser;
    }

    /**
     * Returns the normalize base URL.
     *
     * @param value to use
     * @return normalize base URL value
     */
    private static String normalizeBaseUrl(String value) {
        return StringKit.isBlank(value) ? null : BrowserData.trimTrailingSlash(value);
    }

    /**
     * Enumerates FirefoxChannels.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private enum FirefoxChannel {

        /**
         * Represents the stable enum member.
         */
        STABLE,

        /**
         * Represents the esr enum member.
         */
        ESR,

        /**
         * Represents the devedition enum member.
         */
        DEVEDITION,

        /**
         * Represents the beta enum member.
         */
        BETA,

        /**
         * Carries the FirefoxBuild data.
         */
        NIGHTLY
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
            if (actualBuildId.startsWith(FIREFOX_STABLE_PREFIX)) {
                return new FirefoxBuild(FirefoxChannel.STABLE, actualBuildId.substring(FIREFOX_STABLE_PREFIX.length()));
            }
            if (actualBuildId.startsWith(FIREFOX_ESR_PREFIX)) {
                return new FirefoxBuild(FirefoxChannel.ESR, actualBuildId.substring(FIREFOX_ESR_PREFIX.length()));
            }
            if (actualBuildId.startsWith(FIREFOX_DEV_EDITION_PREFIX)) {
                return new FirefoxBuild(FirefoxChannel.DEVEDITION,
                        actualBuildId.substring(FIREFOX_DEV_EDITION_PREFIX.length()));
            }
            if (actualBuildId.startsWith(FIREFOX_BETA_PREFIX)) {
                return new FirefoxBuild(FirefoxChannel.BETA, actualBuildId.substring(FIREFOX_BETA_PREFIX.length()));
            }
            if (actualBuildId.startsWith("nightly_")) {
                return new FirefoxBuild(FirefoxChannel.NIGHTLY, actualBuildId.substring("nightly_".length()));
            }
            return new FirefoxBuild(FirefoxChannel.NIGHTLY, actualBuildId);
        }
    }

}
