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
package org.miaixz.lancia.runtime;

import java.net.URI;
import java.util.List;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Launcher;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.bundle.BrowserCache;
import org.miaixz.lancia.browser.bundle.BrowserInstaller;
import org.miaixz.lancia.browser.bundle.BrowserManager;
import org.miaixz.lancia.browser.launch.Launchers;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.Browser;
import org.miaixz.lancia.nimble.browser.BrowserVariant;

/**
 * Provides runtime browser installation, cache, and revision operations for the public entry point.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BrowserRuntime {

    /**
     * Hides the browser runtime constructor.
     */
    private BrowserRuntime() {
        // No initialization required.
    }

    /**
     * Downloads configured browsers.
     *
     * @param configuration runtime configuration
     * @return user-facing install messages
     */
    public static List<String> download(Configuration configuration) {
        Configuration actual = actual(configuration);
        Logger.debug(
                true,
                "Browser",
                "Runtime download requested: cacheDir={}, skipDownload={}",
                actual.getCacheDirectory(),
                actual.getSkipDownload());
        try {
            List<String> result = BrowserInstaller.downloadBrowsers(downloadOptions(actual));
            Logger.debug(
                    false,
                    "Browser",
                    "Runtime download completed: cacheDir={}, messages={}",
                    actual.getCacheDirectory(),
                    result.size());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Browser", ex, "Runtime download failed: cacheDir={}", actual.getCacheDirectory());
            throw ex;
        }
    }

    /**
     * Returns the bundled browser revision.
     *
     * @return bundled browser revision
     */
    public static String version() {
        return BrowserData.defaultBuildId(Browser.CHROME);
    }

    /**
     * Creates the browser cache entry used by the public browser fetcher API.
     *
     * @param configuration runtime configuration
     * @return browser cache entry
     */
    public static Object fetcher(Configuration configuration) {
        return BrowserManager.cache(actual(configuration).getCacheDirectory());
    }

    /**
     * Creates a launcher for the browser variant.
     *
     * @param browser browser variant
     * @return launcher instance
     */
    public static Launcher launcher(BrowserVariant browser) {
        Logger.debug(true, "Launcher", "Runtime launcher requested: browser={}", browser);
        Launcher result = Launchers.of(browser);
        Logger.debug(false, "Launcher", "Runtime launcher resolved: browser={}", browser);
        return result;
    }

    /**
     * Removes cached Chrome and Firefox builds that do not match the current bundled revision.
     *
     * @param configuration runtime configuration
     */
    public static void trim(Configuration configuration) {
        Configuration actual = actual(configuration);
        Logger.debug(true, "Browser", "Runtime trim requested: cacheDir={}", actual.getCacheDirectory());
        try {
            BrowserPlatform platform = BrowserManager.detectBrowserPlatform()
                    .orElseThrow(() -> new InternalException("Invalid current platform."));
            BrowserInstaller.GetInstalledBrowsersOptions options = new BrowserInstaller.GetInstalledBrowsersOptions();
            options.setCacheDir(actual.getCacheDirectory());
            int removed = 0;
            for (BrowserCache.InstalledBrowser browser : BrowserManager.getInstalledBrowsers(options)) {
                if (!Browser.CHROME.id().equals(browser.browser()) && !Browser.FIREFOX.id().equals(browser.browser())) {
                    continue;
                }
                if (BrowserData.defaultBuildId(Browser.fromValue(browser.browser())).equals(browser.buildId())) {
                    continue;
                }
                BrowserInstaller.UninstallOptions uninstall = new BrowserInstaller.UninstallOptions();
                uninstall.setCacheDir(actual.getCacheDirectory());
                uninstall.setPlatform(platform);
                uninstall.setBrowser(browser.browser());
                uninstall.setBuildId(browser.buildId());
                BrowserManager.uninstall(uninstall);
                removed++;
            }
            Logger.debug(
                    false,
                    "Browser",
                    "Runtime trim completed: cacheDir={}, removed={}",
                    actual.getCacheDirectory(),
                    removed);
        } catch (RuntimeException ex) {
            Logger.error(false, "Browser", ex, "Runtime trim failed: cacheDir={}", actual.getCacheDirectory());
            throw ex;
        }
    }

    /**
     * Returns a non-null runtime configuration.
     *
     * @param configuration runtime configuration
     * @return non-null runtime configuration
     */
    private static Configuration actual(Configuration configuration) {
        return configuration == null ? Configuration.create() : configuration;
    }

    /**
     * Converts runtime configuration into kernel browser download options.
     *
     * @param configuration runtime configuration
     * @return kernel browser download options
     */
    private static BrowserInstaller.DownloadBrowsersOptions downloadOptions(Configuration configuration) {
        BrowserInstaller.DownloadBrowsersOptions options = new BrowserInstaller.DownloadBrowsersOptions();
        options.setSkipDownload(configuration.getSkipDownload());
        options.setCacheDirectory(configuration.getCacheDirectory());
        options.setChrome(downloadSettings(configuration.getChrome()));
        options.setChromeHeadlessShell(downloadSettings(configuration.getChromeHeadlessShell()));
        options.setFirefox(downloadSettings(configuration.getFirefox()));
        return options;
    }

    /**
     * Converts one runtime browser configuration into kernel browser settings.
     *
     * @param configuration runtime browser configuration
     * @return kernel browser settings
     */
    private static BrowserInstaller.BrowserSettings downloadSettings(Configuration.Browser configuration) {
        BrowserInstaller.BrowserSettings settings = new BrowserInstaller.BrowserSettings();
        if (configuration == null) {
            return settings;
        }
        settings.setSkipDownload(configuration.getSkipDownload());
        settings.setVersion(configuration.getVersion());
        settings.setExpectedArchiveSha256(configuration.getExpectedArchiveSha256());
        settings.setAllowUnverifiedDownload(configuration.isAllowUnverifiedDownload());
        if (StringKit.isNotBlank(configuration.getDownloadBaseUrl())) {
            settings.setDownloadBaseUrl(URI.create(configuration.getDownloadBaseUrl()));
        }
        return settings;
    }

}
