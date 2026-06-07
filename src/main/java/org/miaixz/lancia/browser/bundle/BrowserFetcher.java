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

import java.nio.file.Path;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.browser.BrowserPlatform;

/**
 * Fetches browser archives.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BrowserFetcher {

    /**
     * Creates a browser fetcher.
     */
    private BrowserFetcher() {
        // No initialization required.
    }

    /**
     * Resolves executable path.
     *
     * @return resolve executable path value
     */
    public static Path resolveExecutablePath() {
        return resolveExecutablePath(new FetcherOptions());
    }

    /**
     * Resolves executable path.
     *
     * @param options operation options
     * @return resolve executable path value
     */
    public static Path resolveExecutablePath(FetcherOptions options) {
        FetcherOptions actualOptions = requireOptions(options);
        Logger.debug(
                true,
                "Launcher",
                "Browser executable resolve requested: browser={}, buildId={}, preferSystem={}, installIfMissing={}",
                actualOptions.getBrowser(),
                actualOptions.getBuildId(),
                actualOptions.isPreferSystemExecutable(),
                actualOptions.isInstallIfMissing());
        if (actualOptions.getExecutablePath() != null) {
            Path executable = requireExecutable(actualOptions.getExecutablePath());
            Logger.debug(false, "Launcher", "Browser executable resolved from explicit path: exists={}", true);
            return executable;
        }
        BrowserPlatform platform = actualOptions.getPlatformOrDetect();
        if (actualOptions.isPreferSystemExecutable()) {
            var system = ExecutableResolver.resolveSystemExecutablePath(platform, actualOptions.getSystemCandidates());
            if (system.isPresent()) {
                Path executable = system.getOrThrow();
                Logger.debug(false, "Launcher", "Browser executable resolved from system path: platform={}", platform);
                return executable;
            }
        }
        BrowserCache cache = new BrowserCache(actualOptions.getCacheDir());
        Path cached = cache.computeExecutablePath(actualOptions);
        if (FileKit.isFile(cached.toFile())) {
            Logger.debug(
                    false,
                    "Launcher",
                    "Browser executable resolved from cache: platform={}, buildId={}",
                    platform,
                    actualOptions.getBuildId());
            return cached;
        }
        if (actualOptions.isInstallIfMissing()) {
            Logger.debug(
                    true,
                    "Launcher",
                    "Browser executable missing, install requested: platform={}, buildId={}",
                    platform,
                    actualOptions.getBuildId());
            return install(actualOptions);
        }
        Logger.warn(
                false,
                "Launcher",
                "Browser executable resolve failed: platform={}, buildId={}",
                platform,
                actualOptions.getBuildId());
        throw new IllegalStateException(missingBrowserMessage(
                actualOptions.getBrowser(),
                actualOptions.getBuildId(),
                actualOptions.getCacheDir()));
    }

    /**
     * Builds the Puppeteer-aligned message for a missing browser artifact.
     *
     * @param browser  browser artifact
     * @param buildId  browser build id
     * @param cacheDir browser cache directory
     * @return missing browser message
     */
    public static String missingBrowserMessage(String browser, String buildId, Path cacheDir) {
        String actualBrowser = browser == null ? ExecutableResolver.CHROME : browser;
        Path actualCacheDir = cacheDir == null ? Path.of(System.getProperty("user.home"), ".cache", "puppeteer")
                : cacheDir;
        return "Could not find " + browserDisplayName(actualBrowser) + " (ver. " + buildId
                + "). This can occur if either\n"
                + " 1. you did not perform an installation before running the script (e.g. `npx puppeteer browsers install "
                + actualBrowser + "`), or\n" + " 2. your cache path is incorrectly configured (which is: "
                + actualCacheDir + ").\n"
                + "For (2), check out our guide on configuring puppeteer at https://pptr.dev/guides/configuration.";
    }

    /**
     * Returns a human-readable browser artifact name.
     *
     * @param browser browser artifact
     * @return browser display name
     */
    public static String browserDisplayName(String browser) {
        return switch (browser == null ? ExecutableResolver.CHROME : browser) {
            case ExecutableResolver.CHROME -> "Chrome";
            case ExecutableResolver.CHROME_HEADLESS_SHELL -> "Chrome Headless Shell";
            case "firefox" -> "Firefox";
            case "chromedriver" -> "ChromeDriver";
            case "chromium" -> "Chromium";
            default -> browser;
        };
    }

    /**
     * Returns the compute executable path.
     *
     * @param options operation options
     * @return compute executable path value
     */
    public static Path computeExecutablePath(FetcherOptions options) {
        return new BrowserCache(requireOptions(options).getCacheDir()).computeExecutablePath(requireOptions(options));
    }

    /**
     * Returns the install.
     *
     * @param options operation options
     * @return install value
     */
    public static Path install(FetcherOptions options) {
        FetcherOptions actualOptions = requireOptions(options);
        BrowserPlatform platform = actualOptions.getPlatformOrDetect();
        Logger.debug(
                true,
                "Launcher",
                "Browser install requested: browser={}, platform={}, buildId={}",
                actualOptions.getBrowser(),
                platform,
                actualOptions.getBuildId());
        try {
            BrowserInstaller.InstallOptions installOptions = installOptions(actualOptions, platform);
            BrowserCache.InstalledBrowser installed = BrowserInstaller.installAndUnpack(installOptions);
            Path executable = installed.executablePath();
            if (FileKit.isFile(executable.toFile())) {
                Logger.debug(
                        false,
                        "Launcher",
                        "Browser install completed from Java installer: platform={}, buildId={}",
                        platform,
                        actualOptions.getBuildId());
                return executable;
            }
            throw new IllegalStateException("Browser executable was still not found after installation.");
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Launcher",
                    "Browser install failed: platform={}, message={}",
                    platform,
                    ex.getMessage());
            throw new IllegalStateException("Failed to install browser.", ex);
        }
    }

    /**
     * Returns the require executable.
     *
     * @param executablePath executable path value
     * @return require executable value
     */
    private static Path requireExecutable(Path executablePath) {
        if (!FileKit.isFile(executablePath.toFile())) {
            Logger.warn(false, "Launcher", "Explicit browser executable missing: hasPath={}", executablePath != null);
            throw new IllegalArgumentException("Chrome executable does not exist: " + executablePath);
        }
        return executablePath;
    }

    /**
     * Returns the require options.
     *
     * @param options operation options
     * @return require options value
     */
    private static FetcherOptions requireOptions(FetcherOptions options) {
        return Assert.notNull(options, "options");
    }

    /**
     * Converts fetcher options to installer options.
     *
     * @param options  fetcher options
     * @param platform browser platform
     * @return installer options
     */
    private static BrowserInstaller.InstallOptions installOptions(FetcherOptions options, BrowserPlatform platform) {
        BrowserInstaller.InstallOptions installOptions = new BrowserInstaller.InstallOptions();
        installOptions.setBrowser(options.getBrowser());
        installOptions.setPlatform(platform);
        installOptions.setBuildId(options.getBuildId());
        installOptions.setCacheDir(options.getCacheDir());
        installOptions.setBaseUrl(options.getDownloadBaseUrl());
        installOptions.setDownloadUrl(options.getDownloadUrl());
        installOptions.setExpectedArchiveSha256(options.getExpectedArchiveSha256());
        installOptions.setAllowUnverifiedDownload(options.isAllowUnverifiedDownload());
        installOptions.setSecurityPolicy(options.getSecurityPolicy());
        installOptions.setResourceLimits(options.getResourceLimits());
        return installOptions;
    }

}
