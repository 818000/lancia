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
import java.util.List;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.browser.BrowserPlatform;

/**
 * Describes chromium browser metadata.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ChromiumBrowserData {

    /**
     * Default download base URL.
     */
    public static final String DEFAULT_DOWNLOAD_BASE_URL = "https://storage.googleapis.com/chromium-browser-snapshots";

    /**
     * Creates a chromium browser data.
     */
    private ChromiumBrowserData() {
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
        String actualBaseUrl = StringKit.isBlank(baseUrl) ? DEFAULT_DOWNLOAD_BASE_URL
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
        return List.of(folder(platform), Assert.notBlank(buildId, "buildId"), archive(platform, buildId) + ".zip");
    }

    /**
     * Returns the relative executable path.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return relative executable path value
     */
    public static Path relativeExecutablePath(BrowserPlatform platform, String buildId) {
        return switch (Assert.notNull(platform, "platform")) {
            case MAC, MAC_ARM64 -> Path.of("chrome-mac", "Chromium.app", "Contents", "MacOS", "Chromium");
            case LINUX, LINUX_ARM64 -> Path.of("chrome-linux", "chrome");
            case WIN32, WIN64 -> Path.of("chrome-win", "chrome.exe");
        };
    }

    /**
     * Resolves build ID.
     *
     * @param platform platform
     * @return resolve build ID value
     */
    public static String resolveBuildId(BrowserPlatform platform) {
        String value = BrowserData
                .getText(BrowserData.uri(DEFAULT_DOWNLOAD_BASE_URL + Symbol.SLASH + folder(platform) + "/LAST_CHANGE"));
        return value == null ? Normal.EMPTY : value.trim();
    }

    /**
     * Returns the compare versions.
     *
     * @param left  left value
     * @param right right value
     * @return compare versions value
     */
    public static int compareVersions(String left, String right) {
        return BrowserData.compareNumericText(left, right);
    }

    /**
     * Returns the archive.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return archive value
     */
    public static String archive(BrowserPlatform platform, String buildId) {
        return switch (Assert.notNull(platform, "platform")) {
            case LINUX, LINUX_ARM64 -> "chrome-linux";
            case MAC, MAC_ARM64 -> "chrome-mac";
            case WIN32, WIN64 -> windowsArchive(buildId);
        };
    }

    /**
     * Returns the folder.
     *
     * @param platform platform value
     * @return folder value
     */
    static String folder(BrowserPlatform platform) {
        return switch (Assert.notNull(platform, "platform")) {
            case LINUX, LINUX_ARM64 -> "Linux_x64";
            case MAC_ARM64 -> "Mac_Arm";
            case MAC -> "Mac";
            case WIN32 -> "Win";
            case WIN64 -> "Win_x64";
        };
    }

    /**
     * Returns the windows archive.
     *
     * @param buildId build ID value
     * @return windows archive value
     */
    private static String windowsArchive(String buildId) {
        try {
            return Long.parseLong(buildId) > 591479L ? "chrome-win" : "chrome-win32";
        } catch (NumberFormatException ex) {
            return "chrome-win";
        }
    }

}
