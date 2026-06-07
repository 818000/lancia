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
import java.util.Comparator;
import java.util.List;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.Browser;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.BrowserTag;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ChromeReleaseChannel;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ProfileOptions;
import org.miaixz.lancia.browser.supervisor.BrowserProcess;

/**
 * Provides browser bundle and launch management entry points.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BrowserManager {

    /**
     * Creates a browser manager.
     */
    private BrowserManager() {
        // No initialization required.
    }

    /**
     * Returns the launch.
     *
     * @param options operation options
     * @return launch value
     */
    public static BrowserProcess launch(BrowserProcess.ProcessLaunchOptions options) {
        return BrowserProcess.launch(options);
    }

    /**
     * Returns the compute executable path.
     *
     * @param options operation options
     * @return compute executable path value
     */
    public static Path computeExecutablePath(BrowserProcess.ComputeExecutablePathOptions options) {
        return BrowserProcess.computeExecutablePath(options);
    }

    /**
     * Returns the compute system executable path.
     *
     * @param options operation options
     * @return compute system executable path value
     */
    public static Path computeSystemExecutablePath(BrowserProcess.SystemOptions options) {
        return BrowserProcess.computeSystemExecutablePath(options);
    }

    /**
     * Returns the install.
     *
     * @param options operation options
     * @return install value
     */
    public static BrowserInstaller.InstallResult install(BrowserInstaller.InstallOptions options) {
        return BrowserInstaller.install(options);
    }

    /**
     * Returns the installed browsers.
     *
     * @param options operation options
     * @return installed browsers
     */
    public static List<BrowserCache.InstalledBrowser> getInstalledBrowsers(
            BrowserInstaller.GetInstalledBrowsersOptions options) {
        return BrowserInstaller.getInstalledBrowsers(options);
    }

    /**
     * Returns whether download is available.
     *
     * @param options operation options
     * @return {@code true} when the condition matches
     */
    public static boolean canDownload(BrowserInstaller.InstallOptions options) {
        return BrowserInstaller.canDownload(options);
    }

    /**
     * Handles uninstall.
     *
     * @param options operation options
     */
    public static void uninstall(BrowserInstaller.UninstallOptions options) {
        BrowserInstaller.uninstall(options);
    }

    /**
     * Returns the download URL.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param buildId  build id
     * @param baseUrl  base url
     * @return download URL
     */
    public static URI getDownloadUrl(String browser, BrowserPlatform platform, String buildId, String baseUrl) {
        return BrowserInstaller.getDownloadUrl(browser, platform, buildId, baseUrl);
    }

    /**
     * Returns the detect browser platform.
     *
     * @return optional value
     */
    public static Optional<BrowserPlatform> detectBrowserPlatform() {
        return BrowserPlatform.detectBrowserPlatform();
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
        return BrowserData.resolveBuildId(browser, platform, tag);
    }

    /**
     * Creates profile.
     *
     * @param browser browser instance
     * @param options operation options
     */
    public static void createProfile(Browser browser, ProfileOptions options) {
        BrowserData.createProfile(browser, options);
    }

    /**
     * Returns the version comparator.
     *
     * @param browser browser instance
     * @return version comparator
     */
    public static Comparator<String> getVersionComparator(Browser browser) {
        return BrowserData.getVersionComparator(browser);
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
        return BrowserData.resolveDefaultUserDataDir(browser, platform, channel);
    }

    /**
     * Returns the cache.
     *
     * @param rootDir root dir value
     * @return cache value
     */
    public static BrowserCache cache(Path rootDir) {
        return new BrowserCache(rootDir);
    }

    /**
     * Returns the default provider.
     *
     * @return default provider value
     */
    public static DefaultBrowserProvider defaultProvider() {
        return new DefaultBrowserProvider();
    }

    /**
     * Returns the default provider.
     *
     * @param baseUrl base URL value
     * @return default provider value
     */
    public static DefaultBrowserProvider defaultProvider(String baseUrl) {
        return new DefaultBrowserProvider(baseUrl);
    }

    /**
     * Builds archive filename.
     *
     * @param browser   browser instance
     * @param platform  platform
     * @param buildId   build id
     * @param extension extension
     * @return built archive filename
     */
    public static String buildArchiveFilename(
            String browser,
            BrowserPlatform platform,
            String buildId,
            String extension) {
        return BrowserProvider.buildArchiveFilename(browser, platform, buildId, extension);
    }

    /**
     * Returns the browser tag.
     *
     * @param value to use
     * @return browser tag value
     */
    public static BrowserTag browserTag(String value) {
        BrowserTag tag = BrowserTag.fromValueOrNull(value);
        if (tag == null) {
            throw new IllegalArgumentException("Invalid browser tag: " + value);
        }
        return tag;
    }

}
