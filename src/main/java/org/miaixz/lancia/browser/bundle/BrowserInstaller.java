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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.net.url.UrlDecoder;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.UrlKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.browser.BrowserArchive;
import org.miaixz.lancia.browser.BrowserNetwork;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Installs browser binaries.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BrowserInstaller {

    /**
     * Last progress log timestamp by download key.
     */
    private static final ConcurrentMap<String, Long> TIMES = new ConcurrentHashMap<>();
    /**
     * Timeout for external dependency installation processes.
     */
    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(Normal._30);
    /**
     * Browser archive digest algorithm.
     */
    private static final String ARCHIVE_DIGEST_ALGORITHM = "SHA-256";

    /**
     * Creates a browser installer.
     */
    private BrowserInstaller() {
        // No initialization required.
    }

    /**
     * Returns the install.
     *
     * @param options operation options
     * @return install value
     */
    public static InstallResult install(InstallOptions options) {
        InstallOptions actualOptions = normalizedOptions(options);
        Logger.debug(
                true,
                "Browser",
                "Install requested: browser={}, buildId={}, platform={}, unpack={}",
                actualOptions.getBrowser(),
                actualOptions.getBuildId(),
                actualOptions.getPlatform(),
                actualOptions.isUnpack());
        try {
            InstallResult result = installWithProviders(actualOptions);
            Logger.debug(
                    false,
                    "Browser",
                    "Install completed: browser={}, buildId={}, installed={}, archive={}",
                    actualOptions.getBrowser(),
                    actualOptions.getBuildId(),
                    result.installedBrowser().isPresent(),
                    result.archivePath().isPresent());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "Install failed: browser={}, buildId={}, platform={}",
                    actualOptions.getBrowser(),
                    actualOptions.getBuildId(),
                    actualOptions.getPlatform());
            throw ex;
        }
    }

    /**
     * Downloads browsers using default kernel download options.
     *
     * @return user-facing install messages
     */
    public static List<String> downloadBrowsers() {
        return downloadBrowsers(new DownloadBrowsersOptions());
    }

    /**
     * Downloads configured browsers using Puppeteer's install.ts orchestration semantics.
     *
     * @param options browser download options
     * @return user-facing install messages
     */
    public static List<String> downloadBrowsers(DownloadBrowsersOptions options) {
        DownloadBrowsersOptions actualOptions = options == null ? new DownloadBrowsersOptions() : options;
        Logger.debug(
                true,
                "Browser",
                "Download orchestration requested: cacheDir={}, skipDownload={}",
                actualOptions.getCacheDirectory(),
                actualOptions.getSkipDownload());
        List<String> messages = new ArrayList<>();
        try {
            BrowserPlatform platform = BrowserPlatform.detectBrowserPlatform()
                    .orElseThrow(() -> new InternalException("Invalid current platform."));
            Path cacheDir = actualOptions.getCacheDirectory();
            List<RuntimeException> failures = new ArrayList<>();
            downloadBrowserIfNeeded(
                    BrowserDataTypes.Browser.CHROME,
                    actualOptions.getChrome(),
                    actualOptions.getSkipDownload(),
                    cacheDir,
                    platform,
                    "**INFO** Skipping Chrome download as instructed.",
                    messages,
                    failures);
            downloadBrowserIfNeeded(
                    BrowserDataTypes.Browser.CHROME_HEADLESS_SHELL,
                    actualOptions.getChromeHeadlessShell(),
                    actualOptions.getSkipDownload(),
                    cacheDir,
                    platform,
                    "**INFO** Skipping Chrome download as instructed.",
                    messages,
                    failures);
            downloadBrowserIfNeeded(
                    BrowserDataTypes.Browser.FIREFOX,
                    actualOptions.getFirefox(),
                    actualOptions.getSkipDownload(),
                    cacheDir,
                    platform,
                    "**INFO** Skipping Firefox download as instructed.",
                    messages,
                    failures);
            if (!failures.isEmpty()) {
                InternalException failure = new InternalException("Failed to download one or more browsers.");
                failures.forEach(failure::addSuppressed);
                throw failure;
            }
            Logger.debug(
                    false,
                    "Browser",
                    "Download orchestration completed: cacheDir={}, messages={}",
                    cacheDir,
                    messages.size());
            return messages;
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "Download orchestration failed: cacheDir={}",
                    actualOptions.getCacheDirectory());
            throw ex;
        }
    }

    /**
     * Returns the download archive.
     *
     * @param options operation options
     * @return download archive value
     */
    public static Path downloadArchive(InstallOptions options) {
        InstallOptions copy = new InstallOptions(options);
        copy.setUnpack(false);
        return install(copy).archivePath()
                .orElseThrow(() -> new InternalException("Browser archive download did not return a file path."));
    }

    /**
     * Returns the install and unpack.
     *
     * @param options operation options
     * @return install and unpack value
     */
    public static BrowserCache.InstalledBrowser installAndUnpack(InstallOptions options) {
        InstallOptions copy = new InstallOptions(options);
        copy.setUnpack(true);
        return install(copy).installedBrowser().orElseThrow(
                () -> new InternalException("Browser installation did not return installation information."));
    }

    /**
     * Returns the install URL.
     *
     * @param url      target URL
     * @param options  operation options
     * @param provider provider value
     * @return install URL value
     */
    public static InstallResult installUrl(URI url, InstallOptions options, BrowserProvider provider) {
        URI actualUrl = Assert.notNull(url, "url");
        InstallOptions actualOptions = normalizedOptions(options);
        BrowserProvider actualProvider = Assert.notNull(provider, "provider");
        FetcherOptions fetcherOptions = fetcherOptions(actualOptions);
        String expectedSha256 = actualProvider.expectedArchiveSha256(fetcherOptions);
        enforceArchiveTrust(actualUrl, actualOptions, actualProvider, expectedSha256);
        Logger.debug(
                true,
                "Browser",
                "Install URL requested: browser={}, buildId={}, provider={}, url={}",
                actualOptions.getBrowser(),
                actualOptions.getBuildId(),
                actualProvider.getName(),
                actualUrl.toString().replaceAll("[?#].*$", "?<redacted>"));
        String fileName = archiveFileName(actualUrl);
        BrowserCache cache = new BrowserCache(actualOptions.getCacheDir());
        Path browserRoot = cache.browserRoot(actualOptions.getBrowser());
        Path archivePath = browserRoot.resolve(actualOptions.getBuildId() + Symbol.MINUS + fileName);
        ensureDirectory(browserRoot);

        if (!actualOptions.isUnpack()) {
            if (!Files.exists(archivePath)) {
                Logger.debug(
                        false,
                        "Browser",
                        "Downloading binary from {}",
                        actualUrl.toString().replaceAll("[?#].*$", "?<redacted>"));
                debugTime("download");
                try {
                    BrowserNetwork.downloadFile(
                            actualUrl,
                            archivePath,
                            progressCallback(actualOptions),
                            actualOptions.getSecurityPolicy(),
                            actualOptions.getResourceLimits());
                } finally {
                    debugTimeEnd("download");
                }
            }
            verifyArchiveSha256(archivePath, expectedSha256);
            InstallResult result = InstallResult.archive(archivePath);
            Logger.debug(
                    false,
                    "Browser",
                    "Install URL completed as archive: browser={}, buildId={}, archive={}",
                    actualOptions.getBrowser(),
                    actualOptions.getBuildId(),
                    archivePath);
            return result;
        }

        Path outputPath = cache
                .installationDir(actualOptions.getBrowser(), actualOptions.getPlatform(), actualOptions.getBuildId());
        Path relativeExecutablePath = actualProvider.getExecutablePath(fetcherOptions);
        if (!(actualProvider instanceof DefaultBrowserProvider)) {
            cache.writeExecutablePath(
                    actualOptions.getBrowser(),
                    actualOptions.getPlatform(),
                    actualOptions.getBuildId(),
                    relativeExecutablePath);
        }
        BrowserCache.InstalledBrowser installedBrowser = new BrowserCache.InstalledBrowser(cache,
                actualOptions.getBrowser(), actualOptions.getPlatform(), actualOptions.getBuildId());
        try {
            if (Files.exists(outputPath)) {
                if (!FileKit.isFile(installedBrowser.executablePath().toFile())) {
                    throw new InternalException("Browser directory exists but executable is missing: " + outputPath
                            + " -> " + installedBrowser.executablePath());
                }
                runSetup(installedBrowser);
                if (actualOptions.isInstallDeps()) {
                    installDeps(installedBrowser);
                }
                InstallResult result = InstallResult.installed(installedBrowser);
                Logger.debug(
                        false,
                        "Browser",
                        "Install URL reused existing browser: browser={}, buildId={}, path={}",
                        actualOptions.getBrowser(),
                        actualOptions.getBuildId(),
                        outputPath);
                return result;
            }

            if (!Files.exists(archivePath)) {
                Logger.debug(
                        false,
                        "Browser",
                        "Downloading binary from {}",
                        actualUrl.toString().replaceAll("[?#].*$", "?<redacted>"));
                debugTime("download");
                try {
                    BrowserNetwork.downloadFile(
                            actualUrl,
                            archivePath,
                            progressCallback(actualOptions),
                            actualOptions.getSecurityPolicy(),
                            actualOptions.getResourceLimits());
                } finally {
                    debugTimeEnd("download");
                }
            } else {
                Logger.debug(false, "Browser", "Using existing archive at {}", archivePath);
            }
            verifyArchiveSha256(archivePath, expectedSha256);

            Logger.debug(false, "Browser", "Installing {} to {}", archivePath, outputPath);
            debugTime("extract");
            try {
                BrowserArchive.unpackArchive(archivePath, outputPath);
            } finally {
                debugTimeEnd("extract");
            }

            if (StringKit.isNotBlank(actualOptions.getBuildIdAlias())) {
                cache.writeAlias(
                        actualOptions.getBrowser(),
                        actualOptions.getBuildIdAlias(),
                        actualOptions.getBuildId());
            }
            runSetup(installedBrowser);
            if (actualOptions.isInstallDeps()) {
                installDeps(installedBrowser);
            }
            InstallResult result = InstallResult.installed(installedBrowser);
            Logger.debug(
                    false,
                    "Browser",
                    "Install URL completed: browser={}, buildId={}, path={}",
                    actualOptions.getBrowser(),
                    actualOptions.getBuildId(),
                    outputPath);
            return result;
        } finally {
            deleteArchive(archivePath);
        }
    }

    /**
     * Handles uninstall.
     *
     * @param options operation options
     */
    public static void uninstall(UninstallOptions options) {
        UninstallOptions actualOptions = normalizedOptions(options);
        Logger.debug(
                true,
                "Browser",
                "Uninstall requested: browser={}, buildId={}, platform={}, cacheDir={}",
                actualOptions.getBrowser(),
                actualOptions.getBuildId(),
                actualOptions.getPlatform(),
                actualOptions.getCacheDir());
        new BrowserCache(actualOptions.getCacheDir())
                .uninstall(actualOptions.getBrowser(), actualOptions.getPlatform(), actualOptions.getBuildId());
        Logger.debug(
                false,
                "Browser",
                "Uninstall completed: browser={}, buildId={}, platform={}",
                actualOptions.getBrowser(),
                actualOptions.getBuildId(),
                actualOptions.getPlatform());
    }

    /**
     * Returns the installed browsers.
     *
     * @param options operation options
     * @return installed browsers
     */
    public static List<BrowserCache.InstalledBrowser> getInstalledBrowsers(GetInstalledBrowsersOptions options) {
        GetInstalledBrowsersOptions actualOptions = Assert.notNull(options, "options");
        return new BrowserCache(actualOptions.getCacheDir()).getInstalledBrowsers();
    }

    /**
     * Returns whether download is available.
     *
     * @param options operation options
     * @return {@code true} when the condition matches
     */
    public static boolean canDownload(InstallOptions options) {
        InstallOptions actualOptions = normalizedOptions(options);
        Logger.debug(
                true,
                "Browser",
                "Can-download probe requested: browser={}, buildId={}, platform={}",
                actualOptions.getBrowser(),
                actualOptions.getBuildId(),
                actualOptions.getPlatform());
        FetcherOptions downloadOptions = fetcherOptions(actualOptions);
        for (BrowserProvider provider : canDownloadProviders(actualOptions)) {
            if (!provider.supports(downloadOptions)) {
                continue;
            }
            URI url = provider.getDownloadUrl(downloadOptions);
            String expectedSha256 = provider.expectedArchiveSha256(downloadOptions);
            if (url != null) {
                enforceArchiveTrust(url, actualOptions, provider, expectedSha256);
            }
            if (url != null && BrowserNetwork.headHttpRequest(url)) {
                Logger.debug(
                        false,
                        "Browser",
                        "Can-download probe succeeded: browser={}, buildId={}, provider={}",
                        actualOptions.getBrowser(),
                        actualOptions.getBuildId(),
                        provider.getName());
                return true;
            }
        }
        Logger.debug(
                false,
                "Browser",
                "Can-download probe failed: browser={}, buildId={}",
                actualOptions.getBrowser(),
                actualOptions.getBuildId());
        return false;
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
        return UrlKit.toURI(
                BrowserData
                        .resolveDownloadUrl(BrowserDataTypes.Browser.fromValue(browser), platform, buildId, baseUrl));
    }

    /**
     * Returns the make progress callback.
     *
     * @param browser browser instance
     * @param buildId build ID value
     * @return make progress callback value
     */
    public static BrowserNetwork.ProgressCallback makeProgressCallback(String browser, String buildId) {
        final LongConsumer[] ticker = new LongConsumer[Normal._1];
        final long[] lastDownloadedBytes = new long[Normal._1];
        return (downloadedBytes, totalBytes) -> {
            if (ticker[Normal._0] == null) {
                ticker[Normal._0] = ProgressBar.createBarTicker(
                        "Downloading " + browser + Symbol.SPACE + buildId + " - " + toMegabytes(totalBytes),
                        totalBytes);
            }
            long delta = downloadedBytes - lastDownloadedBytes[Normal._0];
            lastDownloadedBytes[Normal._0] = downloadedBytes;
            ticker[Normal._0].accept(delta);
        };
    }

    /**
     * Returns the install with providers.
     *
     * @param options operation options
     * @return install with providers value
     */
    private static InstallResult installWithProviders(InstallOptions options) {
        BrowserCache cache = new BrowserCache(options.getCacheDir());
        ensureDirectory(cache.browserRoot(options.getBrowser()));
        FetcherOptions downloadOptions = fetcherOptions(options);
        if (options.getDownloadUrl() != null) {
            return installUrl(
                    options.getDownloadUrl(),
                    options,
                    new DefaultBrowserProvider(options.getBaseUrl() == null ? null : options.getBaseUrl().toString()));
        }
        List<ProviderFailure> errors = new ArrayList<>();
        for (BrowserProvider provider : installProviders(options)) {
            try {
                if (!provider.supports(downloadOptions)) {
                    Logger.debug(
                            false,
                            "Browser",
                            "Provider {} does not support {} on {}",
                            provider.getName(),
                            options.getBrowser(),
                            options.getPlatform());
                    continue;
                }
                if (!(provider instanceof DefaultBrowserProvider)) {
                    Logger.debug(false, "Browser", "Using custom downloader: {}", provider.getName());
                }
                Logger.debug(
                        false,
                        "Browser",
                        "Trying provider: {} for {} {}",
                        provider.getName(),
                        options.getBrowser(),
                        options.getBuildId());
                URI url = provider.getDownloadUrl(downloadOptions);
                if (url == null) {
                    Logger.debug(
                            false,
                            "Browser",
                            "Provider {} returned no URL for {} {}",
                            provider.getName(),
                            options.getBrowser(),
                            options.getBuildId());
                    continue;
                }
                Logger.debug(
                        false,
                        "Browser",
                        "Successfully got URL from {}: {}",
                        provider.getName(),
                        url.toString().replaceAll("[?#].*$", "?<redacted>"));
                return installUrl(url, options, provider);
            } catch (RuntimeException ex) {
                Logger.debug(false, "Browser", "Provider {} failed: {}", provider.getName(), ex.getMessage());
                errors.add(new ProviderFailure(provider.getName(), ex));
            }
        }
        throw new InternalException("All providers failed for " + options.getBrowser() + Symbol.SPACE
                + options.getBuildId() + Symbol.COLON + Symbol.LF + providerErrors(errors));
    }

    /**
     * Downloads a browser unless its configuration says to skip it.
     *
     * @param browser     browser type
     * @param settings    browser settings
     * @param globalSkip  global skip-download value
     * @param cacheDir    cache directory
     * @param platform    browser platform
     * @param skipMessage skip message
     * @param messages    install messages
     * @param failures    install failures
     */
    private static void downloadBrowserIfNeeded(
            BrowserDataTypes.Browser browser,
            BrowserSettings settings,
            Boolean globalSkip,
            Path cacheDir,
            BrowserPlatform platform,
            String skipMessage,
            List<String> messages,
            List<RuntimeException> failures) {
        BrowserSettings actualSettings = settings == null ? new BrowserSettings() : settings;
        Boolean skipDownload = actualSettings.getSkipDownload() == null ? globalSkip : actualSettings.getSkipDownload();
        if (Boolean.TRUE.equals(skipDownload)) {
            messages.add(skipMessage);
            return;
        }
        try {
            messages.add(downloadBrowser(browser, actualSettings, cacheDir, platform));
        } catch (RuntimeException ex) {
            failures.add(ex);
        }
    }

    /**
     * Downloads one browser and returns the success message.
     *
     * @param browser  browser type
     * @param settings browser settings
     * @param cacheDir cache directory
     * @param platform browser platform
     * @return success message
     */
    private static String downloadBrowser(
            BrowserDataTypes.Browser browser,
            BrowserSettings settings,
            Path cacheDir,
            BrowserPlatform platform) {
        String unresolvedBuildId = StringKit.isNotBlank(settings.getVersion()) ? settings.getVersion()
                : BrowserData.defaultBuildId(browser);
        String buildId = BrowserData.resolveBuildId(browser, platform, unresolvedBuildId);
        InstallOptions options = new InstallOptions();
        options.setBrowser(browser.id());
        options.setCacheDir(cacheDir);
        options.setPlatform(platform);
        options.setBuildId(buildId);
        options.setDefaultDownloadProgress(true);
        options.setExpectedArchiveSha256(settings.getExpectedArchiveSha256());
        options.setAllowUnverifiedDownload(settings.isAllowUnverifiedDownload());
        if (!buildId.equals(unresolvedBuildId)) {
            options.setBuildIdAlias(unresolvedBuildId);
        }
        if (settings.getDownloadBaseUrl() != null) {
            options.setBaseUrl(settings.getDownloadBaseUrl());
        }
        try {
            InstallResult result = install(options);
            BrowserCache.InstalledBrowser installed = result.installedBrowser()
                    .orElseThrow(() -> new InternalException("Browser install did not return installed browser info."));
            return browser.id() + " (" + installed.buildId() + ") downloaded to " + installed.path();
        } catch (RuntimeException ex) {
            throw new InternalException("ERROR: Failed to set up " + browser.id() + " v" + buildId
                    + "! Set \"PUPPETEER_SKIP_DOWNLOAD\" env variable to skip download.", ex);
        }
    }

    /**
     * Returns the install providers.
     *
     * @param options operation options
     * @return values
     */
    private static List<BrowserProvider> installProviders(InstallOptions options) {
        List<BrowserProvider> providers = new ArrayList<>(options.getProviders());
        if (options.getBaseUrl() != null) {
            providers.add(new DefaultBrowserProvider(options.getBaseUrl().toString()));
            if (options.isForceFallbackForTesting()) {
                addBuiltInProviders(providers);
            }
            return providers;
        }
        addBuiltInProviders(providers);
        return providers;
    }

    /**
     * Returns the can download providers.
     *
     * @param options operation options
     * @return values
     */
    private static List<BrowserProvider> canDownloadProviders(InstallOptions options) {
        List<BrowserProvider> providers = new ArrayList<>(options.getProviders());
        if (options.getBaseUrl() != null) {
            providers.add(new DefaultBrowserProvider(options.getBaseUrl().toString()));
            if (options.isForceFallbackForTesting()) {
                addBuiltInProviders(providers);
            }
            return providers;
        }
        addBuiltInProviders(providers);
        return providers;
    }

    /**
     * Adds built-in download providers.
     *
     * @param providers providers
     */
    private static void addBuiltInProviders(List<BrowserProvider> providers) {
        providers.add(new DefaultBrowserProvider());
        providers.add(DefaultBrowserProvider.npmMirror());
    }

    /**
     * Returns the normalized options.
     *
     * @param options operation options
     * @return normalized options value
     */
    private static InstallOptions normalizedOptions(InstallOptions options) {
        InstallOptions actualOptions = new InstallOptions(Assert.notNull(options, "options"));
        if (actualOptions.getPlatform() == null) {
            actualOptions.setPlatform(
                    BrowserPlatform.detectBrowserPlatform().orElseThrow(
                            () -> new InternalException("Could not detect the current browser platform.")));
        }
        if (actualOptions.getCacheDir() == null) {
            actualOptions.setCacheDir(defaultCacheDir());
        }
        if (StringKit.isBlank(actualOptions.getBrowser())) {
            actualOptions.setBrowser(ExecutableResolver.CHROME);
        }
        if (StringKit.isBlank(actualOptions.getBuildId())) {
            throw new InternalException("Browser build id must not be blank.");
        }
        return actualOptions;
    }

    /**
     * Returns the normalized options.
     *
     * @param options operation options
     * @return normalized options value
     */
    private static UninstallOptions normalizedOptions(UninstallOptions options) {
        UninstallOptions actualOptions = new UninstallOptions(Assert.notNull(options, "options"));
        if (actualOptions.getPlatform() == null) {
            actualOptions.setPlatform(
                    BrowserPlatform.detectBrowserPlatform().orElseThrow(
                            () -> new InternalException("Could not detect the current browser platform.")));
        }
        if (actualOptions.getCacheDir() == null) {
            actualOptions.setCacheDir(defaultCacheDir());
        }
        if (StringKit.isBlank(actualOptions.getBrowser())) {
            actualOptions.setBrowser(ExecutableResolver.CHROME);
        }
        if (StringKit.isBlank(actualOptions.getBuildId())) {
            throw new InternalException("Browser build id must not be blank.");
        }
        return actualOptions;
    }

    /**
     * Returns the fetcher options.
     *
     * @param options operation options
     * @return fetcher options value
     */
    private static FetcherOptions fetcherOptions(InstallOptions options) {
        FetcherOptions fetcherOptions = new FetcherOptions();
        fetcherOptions.setCacheDir(options.getCacheDir());
        fetcherOptions.setBrowser(options.getBrowser());
        fetcherOptions.setBuildId(options.getBuildId());
        fetcherOptions.setPlatform(options.getPlatform());
        fetcherOptions.setDownloadUrl(options.getDownloadUrl());
        fetcherOptions.setExpectedArchiveSha256(options.getExpectedArchiveSha256());
        fetcherOptions.setAllowUnverifiedDownload(options.isAllowUnverifiedDownload());
        fetcherOptions.setSecurityPolicy(options.getSecurityPolicy());
        fetcherOptions.setResourceLimits(options.getResourceLimits());
        return fetcherOptions;
    }

    /**
     * Handles delete archive.
     *
     * @param archivePath archive path value
     */
    private static void deleteArchive(Path archivePath) {
        try {
            Files.deleteIfExists(archivePath);
        } catch (IOException ex) {
            throw new InternalException("Failed to delete browser archive: " + archivePath, ex);
        }
    }

    /**
     * Returns the progress callback.
     *
     * @param options operation options
     * @return progress callback value
     */
    private static BrowserNetwork.ProgressCallback progressCallback(InstallOptions options) {
        if (options.getDownloadProgressCallback() != null) {
            return options.getDownloadProgressCallback();
        }
        if (options.isDefaultDownloadProgress()) {
            return makeProgressCallback(
                    options.getBrowser(),
                    StringKit.isBlank(options.getBuildIdAlias()) ? options.getBuildId() : options.getBuildIdAlias());
        }
        return null;
    }

    /**
     * Enforces browser archive trust policy before a network request.
     *
     * @param url            download URL
     * @param options        install options
     * @param provider       provider
     * @param expectedSha256 expected SHA-256
     */
    private static void enforceArchiveTrust(
            URI url,
            InstallOptions options,
            BrowserProvider provider,
            String expectedSha256) {
        boolean customSource = options.getDownloadUrl() != null || options.getBaseUrl() != null
                || !(provider instanceof DefaultBrowserProvider);
        if (StringKit.isNotBlank(expectedSha256) || !customSource) {
            return;
        }
        String sanitizedUrl = options.getSecurityPolicy().sanitizeUrl(url);
        if (!options.isAllowUnverifiedDownload()) {
            throw new InternalException("Unverified browser archive download is not allowed: " + sanitizedUrl);
        }
        Logger.warn(false, "Browser", "Unverified browser archive download is enabled: url={}", sanitizedUrl);
    }

    /**
     * Verifies browser archive SHA-256.
     *
     * @param archivePath    archive path
     * @param expectedSha256 expected SHA-256
     */
    private static void verifyArchiveSha256(Path archivePath, String expectedSha256) {
        if (StringKit.isBlank(expectedSha256)) {
            return;
        }
        String actualSha256 = archiveSha256(archivePath);
        if (!expectedSha256.equals(actualSha256)) {
            throw new InternalException("Browser archive SHA-256 mismatch: " + archivePath + ", expected="
                    + expectedSha256 + ", actual=" + actualSha256);
        }
    }

    /**
     * Returns browser archive SHA-256.
     *
     * @param archivePath archive path
     * @return SHA-256 value
     */
    private static String archiveSha256(Path archivePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ARCHIVE_DIGEST_ALGORITHM);
            byte[] buffer = new byte[Normal._8192];
            try (InputStream input = Files.newInputStream(archivePath)) {
                int read;
                while ((read = input.read(buffer)) >= Normal._0) {
                    if (read > Normal._0) {
                        digest.update(buffer, Normal._0, read);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException ex) {
            throw new InternalException("Failed to read browser archive for SHA-256: " + archivePath, ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalException("SHA-256 digest algorithm is unavailable.", ex);
        }
    }

    /**
     * Returns the archive file name.
     *
     * @param url target URL
     * @return archive file name value
     */
    private static String archiveFileName(URI url) {
        String path = String.valueOf(url.getRawPath());
        int index = path.lastIndexOf(Symbol.C_SLASH);
        String fileName = index >= Normal._0 ? path.substring(index + Normal._1) : path;
        String decoded = UrlDecoder.decode(fileName, Charset.UTF_8);
        if (StringKit.isBlank(decoded)) {
            throw new InternalException("Download URL is missing an archive file name: " + url);
        }
        return decoded;
    }

    /**
     * Handles ensure directory.
     *
     * @param path file path
     */
    private static void ensureDirectory(Path path) {
        try {
            FileKit.mkdir(path.toFile());
        } catch (RuntimeException ex) {
            throw new InternalException("Failed to create directory: " + path, ex);
        }
    }

    /**
     * Handles run setup.
     *
     * @param installedBrowser installed browser value
     */
    private static void runSetup(BrowserCache.InstalledBrowser installedBrowser) {
        if ((installedBrowser.platform() == BrowserPlatform.WIN32
                || installedBrowser.platform() == BrowserPlatform.WIN64)
                && ExecutableResolver.CHROME.equals(installedBrowser.browser())
                && BrowserPlatform.detectBrowserPlatform().filter(installedBrowser.platform()::equals).isPresent()) {
            debugTime("permissions");
            try {
                Path browserDir = installedBrowser.executablePath().getParent();
                Path setupExePath = browserDir.resolve("setup.exe");
                if (FileKit.isFile(setupExePath.toFile())) {
                    runCommand(List.of(setupExePath.toString(), "--configure-browser-in-directory=" + browserDir));
                }
            } finally {
                debugTimeEnd("permissions");
            }
        }
    }

    /**
     * Handles install deps.
     *
     * @param installedBrowser installed browser value
     */
    private static void installDeps(BrowserCache.InstalledBrowser installedBrowser) {
        if (!isLinux() || installedBrowser.platform() != BrowserPlatform.LINUX) {
            return;
        }
        Path depsPath = installedBrowser.executablePath().getParent().resolve("deb.deps");
        if (!FileKit.isFile(depsPath.toFile())) {
            Logger.debug(false, "Browser", "deb.deps file was not found at {}", depsPath);
            return;
        }
        if (!isRootUser()) {
            throw new InternalException("Installing system dependencies requires root privileges");
        }
        runCommand(List.of("apt-get", "-v"));
        List<String> dependencies = readDeps(depsPath);
        for (String dependency : dependencies) {
            validateDebDependency(dependency);
        }
        Logger.debug(false, "Browser", "Trying to install dependencies: count={}", dependencies.size());
        List<String> command = new ArrayList<>(List.of("apt-get", "satisfy", "-y"));
        command.addAll(dependencies);
        command.add("--no-install-recommends");
        runCommand(command);
        Logger.debug(false, "Browser", "Installed system dependencies: count={}", dependencies.size());
    }

    /**
     * Returns the read deps.
     *
     * @param depsPath deps path value
     * @return read deps value
     */
    private static List<String> readDeps(Path depsPath) {
        try {
            return Files.readString(depsPath).lines().map(String::trim).filter(StringKit::isNotBlank).toList();
        } catch (IOException ex) {
            throw new InternalException("Failed to read system dependency file: " + depsPath, ex);
        }
    }

    /**
     * Validates a Debian dependency token.
     *
     * @param dependency dependency token
     */
    private static void validateDebDependency(String dependency) {
        if (StringKit.isBlank(dependency) || !dependency.matches("[A-Za-z0-9.+:-]+")) {
            throw new InternalException("Invalid Debian dependency token.");
        }
    }

    /**
     * Handles run command.
     *
     * @param command command name
     */
    private static void runCommand(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            CompletableFuture<String> output = CompletableFuture
                    .supplyAsync(() -> readProcessOutput(process.getInputStream()));
            boolean finished = process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new InternalException("Command timed out: " + command);
            }
            if (process.exitValue() != Normal._0) {
                throw new InternalException("Command failed: " + command + ", exit code: " + process.exitValue()
                        + ", output: " + output.join());
            }
        } catch (IOException ex) {
            throw new InternalException("Command could not be started: " + command, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InternalException("Command execution was interrupted: " + command, ex);
        }
    }

    /**
     * Reads process output while retaining only bounded tail diagnostics.
     *
     * @param input process output stream
     * @return bounded output text
     */
    private static String readProcessOutput(InputStream input) {
        int limit = (int) Math.min(Integer.MAX_VALUE, ResourceLimits.defaults().getMaxProcessOutputBytes());
        byte[] retained = new byte[Math.max(0, limit)];
        byte[] buffer = new byte[8192];
        int start = 0;
        int size = 0;
        try (InputStream source = input) {
            int read;
            while ((read = source.read(buffer)) >= 0) {
                for (int index = 0; index < read && retained.length > 0; index++) {
                    int writeIndex;
                    if (size < retained.length) {
                        writeIndex = (start + size) % retained.length;
                        size++;
                    } else {
                        writeIndex = start;
                        start = (start + 1) % retained.length;
                    }
                    retained[writeIndex] = buffer[index];
                }
            }
        } catch (IOException ex) {
            throw new InternalException("Failed to read command output.", ex);
        }
        byte[] output = new byte[size];
        for (int index = 0; index < size; index++) {
            output[index] = retained[(start + index) % retained.length];
        }
        return new String(output, Charset.UTF_8);
    }

    /**
     * Returns whether linux is enabled.
     *
     * @return {@code true} when the condition matches
     */
    private static boolean isLinux() {
        return BrowserPlatform.isCurrentLinux();
    }

    /**
     * Returns whether root user is enabled.
     *
     * @return {@code true} when the condition matches
     */
    private static boolean isRootUser() {
        return "root".equals(System.getProperty("user.name"));
    }

    /**
     * Returns the default cache dir.
     *
     * @return default cache dir value
     */
    private static Path defaultCacheDir() {
        return Path.of(System.getProperty("user.home"), Symbol.DOT + "cache", Builder.PRODUCT_NAME);
    }

    /**
     * Handles debug time.
     *
     * @param label label value
     */
    private static void debugTime(String label) {
        TIMES.put(label, System.nanoTime());
    }

    /**
     * Handles debug time end.
     *
     * @param label label value
     */
    private static void debugTimeEnd(String label) {
        Long start = TIMES.remove(label);
        if (start == null) {
            return;
        }
        double duration = (System.nanoTime() - start) / 1_000_000.0D;
        Logger.debug(false, "Browser", "Duration for {}: {}ms", label, duration);
    }

    /**
     * Returns the provider errors.
     *
     * @param errors errors value
     * @return provider errors value
     */
    private static String providerErrors(List<ProviderFailure> errors) {
        StringBuilder builder = new StringBuilder();
        for (ProviderFailure error : errors) {
            builder.append("  - ").append(error.providerName).append(Symbol.COLON).append(Symbol.SPACE)
                    .append(error.error.getMessage()).append(Symbol.LF);
        }
        return builder.toString();
    }

    /**
     * Converts this value to megabytes.
     *
     * @param bytes bytes value
     * @return megabytes
     */
    private static String toMegabytes(long bytes) {
        double mb = bytes / 1000.0D / 1000.0D;
        return Math.round(mb * 10.0D) / 10.0D + " MB";
    }

    /**
     * Represents provider failure.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class ProviderFailure {

        /**
         * Current provider name.
         */
        private final String providerName;
        /**
         * Current error.
         */
        private final RuntimeException error;

        /**
         * Creates an instance.
         *
         * @param providerName provider name value
         * @param error        error to propagate
         */
        private ProviderFailure(String providerName, RuntimeException error) {
            this.providerName = providerName;
            this.error = error;
        }
    }

    /**
     * Represents a install result.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class InstallResult {

        /**
         * Current installed browser.
         */
        private final BrowserCache.InstalledBrowser installedBrowser;
        /**
         * Current archive path.
         */
        private final Path archivePath;

        /**
         * Creates an instance.
         *
         * @param installedBrowser installed browser value
         * @param archivePath      archive path value
         */
        private InstallResult(BrowserCache.InstalledBrowser installedBrowser, Path archivePath) {
            this.installedBrowser = installedBrowser;
            this.archivePath = archivePath;
        }

        /**
         * Returns the installed.
         *
         * @param installedBrowser installed browser value
         * @return installed value
         */
        private static InstallResult installed(BrowserCache.InstalledBrowser installedBrowser) {
            return new InstallResult(Assert.notNull(installedBrowser, "installedBrowser"), null);
        }

        /**
         * Returns the archive.
         *
         * @param archivePath archive path value
         * @return archive value
         */
        private static InstallResult archive(Path archivePath) {
            return new InstallResult(null, Assert.notNull(archivePath, "archivePath"));
        }

        /**
         * Returns the installed browser.
         *
         * @return optional value
         */
        public Optional<BrowserCache.InstalledBrowser> installedBrowser() {
            return Optional.ofNullable(installedBrowser);
        }

        /**
         * Returns the archive path.
         *
         * @return optional value
         */
        public Optional<Path> archivePath() {
            return Optional.ofNullable(archivePath);
        }

        /**
         * Returns whether unpacked is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isUnpacked() {
            return installedBrowser != null;
        }
    }

    /**
     * Defines options for download browsers operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class DownloadBrowsersOptions {

        /**
         * Whether skip download is enabled.
         */
        private Boolean skipDownload;
        /**
         * Current cache directory.
         */
        private Path cacheDirectory = defaultCacheDir();
        /**
         * Registered chrome values.
         */
        private BrowserSettings chrome = new BrowserSettings();
        /**
         * Registered chrome headless shell values.
         */
        private BrowserSettings chromeHeadlessShell = new BrowserSettings();
        /**
         * Registered firefox values.
         */
        private BrowserSettings firefox = skippedBrowserSettings();

        /**
         * Creates download browsers options.
         */
        public DownloadBrowsersOptions() {
            // No initialization required.
        }

        /**
         * Returns the skip download.
         *
         * @return {@code true} when the condition matches
         */
        public Boolean getSkipDownload() {
            return skipDownload;
        }

        /**
         * Updates skip download.
         *
         * @param skipDownload global skip download
         */
        public void setSkipDownload(Boolean skipDownload) {
            this.skipDownload = skipDownload;
        }

        /**
         * Returns the cache directory.
         *
         * @return cache directory
         */
        public Path getCacheDirectory() {
            return cacheDirectory;
        }

        /**
         * Updates cache directory.
         *
         * @param cacheDirectory cache directory value
         */
        public void setCacheDirectory(Path cacheDirectory) {
            this.cacheDirectory = cacheDirectory == null ? defaultCacheDir() : cacheDirectory;
        }

        /**
         * Returns the chrome.
         *
         * @return values
         */
        public BrowserSettings getChrome() {
            return chrome;
        }

        /**
         * Updates chrome.
         *
         * @param chrome Chrome download settings
         */
        public void setChrome(BrowserSettings chrome) {
            this.chrome = chrome == null ? new BrowserSettings() : chrome;
        }

        /**
         * Returns the chrome headless shell.
         *
         * @return values
         */
        public BrowserSettings getChromeHeadlessShell() {
            return chromeHeadlessShell;
        }

        /**
         * Updates chrome headless shell.
         *
         * @param chromeHeadlessShell Chrome Headless Shell download settings
         */
        public void setChromeHeadlessShell(BrowserSettings chromeHeadlessShell) {
            this.chromeHeadlessShell = chromeHeadlessShell == null ? new BrowserSettings() : chromeHeadlessShell;
        }

        /**
         * Returns the firefox.
         *
         * @return values
         */
        public BrowserSettings getFirefox() {
            return firefox;
        }

        /**
         * Updates firefox.
         *
         * @param firefox Firefox download settings
         */
        public void setFirefox(BrowserSettings firefox) {
            this.firefox = firefox == null ? skippedBrowserSettings() : firefox;
        }
    }

    /**
     * Stores browser settings.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class BrowserSettings {

        /**
         * Whether skip download is enabled.
         */
        private Boolean skipDownload;
        /**
         * Current download base URL.
         */
        private URI downloadBaseUrl;
        /**
         * Current version.
         */
        private String version;
        /**
         * Expected archive SHA-256.
         */
        private String expectedArchiveSha256;
        /**
         * Whether unverified downloads are allowed.
         */
        private boolean allowUnverifiedDownload;

        /**
         * Creates browser settings.
         */
        public BrowserSettings() {
            // No initialization required.
        }

        /**
         * Returns the skip download.
         *
         * @return {@code true} when the condition matches
         */
        public Boolean getSkipDownload() {
            return skipDownload;
        }

        /**
         * Updates skip download.
         *
         * @param skipDownload skip download value
         */
        public void setSkipDownload(Boolean skipDownload) {
            this.skipDownload = skipDownload;
        }

        /**
         * Returns the download base URL.
         *
         * @return download base URL
         */
        public URI getDownloadBaseUrl() {
            return downloadBaseUrl;
        }

        /**
         * Updates download base URL.
         *
         * @param downloadBaseUrl download base URL value
         */
        public void setDownloadBaseUrl(URI downloadBaseUrl) {
            this.downloadBaseUrl = downloadBaseUrl;
        }

        /**
         * Returns the version.
         *
         * @return version
         */
        public String getVersion() {
            return version;
        }

        /**
         * Updates version.
         *
         * @param version browser version
         */
        public void setVersion(String version) {
            this.version = version;
        }

        /**
         * Returns expected archive SHA-256.
         *
         * @return expected archive SHA-256
         */
        public String getExpectedArchiveSha256() {
            return expectedArchiveSha256;
        }

        /**
         * Updates expected archive SHA-256.
         *
         * @param expectedArchiveSha256 expected archive SHA-256 value
         */
        public void setExpectedArchiveSha256(String expectedArchiveSha256) {
            this.expectedArchiveSha256 = FetcherOptions.normalizeArchiveSha256(expectedArchiveSha256);
        }

        /**
         * Returns whether unverified downloads are allowed.
         *
         * @return {@code true} when unverified downloads are allowed
         */
        public boolean isAllowUnverifiedDownload() {
            return allowUnverifiedDownload;
        }

        /**
         * Updates whether unverified downloads are allowed.
         *
         * @param allowUnverifiedDownload allow unverified download value
         */
        public void setAllowUnverifiedDownload(boolean allowUnverifiedDownload) {
            this.allowUnverifiedDownload = allowUnverifiedDownload;
        }
    }

    /**
     * Creates skipped browser settings.
     *
     * @return skipped browser settings
     */
    private static BrowserSettings skippedBrowserSettings() {
        BrowserSettings settings = new BrowserSettings();
        settings.setSkipDownload(Boolean.TRUE);
        return settings;
    }

    /**
     * Defines options for install operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class InstallOptions {

        /**
         * Current cache dir.
         */
        private Path cacheDir = defaultCacheDir();
        /**
         * Current platform.
         */
        private BrowserPlatform platform;
        /**
         * Current browser.
         */
        private String browser = ExecutableResolver.CHROME;
        /**
         * Current build ID.
         */
        private String buildId;
        /**
         * Current build ID alias.
         */
        private String buildIdAlias;
        /**
         * Current download progress callback.
         */
        private BrowserNetwork.ProgressCallback downloadProgressCallback;
        /**
         * Whether default download progress is enabled.
         */
        private boolean defaultDownloadProgress;
        /**
         * Current base URL.
         */
        private URI baseUrl;
        /**
         * Current download URL.
         */
        private URI downloadUrl;
        /**
         * Expected archive SHA-256.
         */
        private String expectedArchiveSha256;
        /**
         * Whether unverified downloads are allowed.
         */
        private boolean allowUnverifiedDownload;
        /**
         * Whether unpack is enabled.
         */
        private boolean unpack = true;
        /**
         * Whether force fallback for testing is enabled.
         */
        private boolean forceFallbackForTesting;
        /**
         * Whether install deps is enabled.
         */
        private boolean installDeps;
        /**
         * Runtime security policy.
         */
        private SecurityPolicy securityPolicy = SecurityPolicy.defaultPolicy();
        /**
         * Runtime resource limits.
         */
        private ResourceLimits resourceLimits = ResourceLimits.defaults();
        /**
         * Registered providers values.
         */
        private final List<BrowserProvider> providers = new ArrayList<>();

        /**
         * Creates an instance.
         */
        public InstallOptions() {
            // No initialization required.
        }

        /**
         * Creates an instance.
         *
         * @param source source value
         */
        public InstallOptions(InstallOptions source) {
            InstallOptions actualSource = Assert.notNull(source, "source");
            this.cacheDir = actualSource.cacheDir;
            this.platform = actualSource.platform;
            this.browser = actualSource.browser;
            this.buildId = actualSource.buildId;
            this.buildIdAlias = actualSource.buildIdAlias;
            this.downloadProgressCallback = actualSource.downloadProgressCallback;
            this.defaultDownloadProgress = actualSource.defaultDownloadProgress;
            this.baseUrl = actualSource.baseUrl;
            this.downloadUrl = actualSource.downloadUrl;
            this.expectedArchiveSha256 = actualSource.expectedArchiveSha256;
            this.allowUnverifiedDownload = actualSource.allowUnverifiedDownload;
            this.unpack = actualSource.unpack;
            this.forceFallbackForTesting = actualSource.forceFallbackForTesting;
            this.installDeps = actualSource.installDeps;
            this.securityPolicy = actualSource.securityPolicy;
            this.resourceLimits = actualSource.resourceLimits;
            this.providers.addAll(actualSource.providers);
        }

        /**
         * Returns the cache dir.
         *
         * @return cache dir
         */
        public Path getCacheDir() {
            return cacheDir;
        }

        /**
         * Updates cache dir.
         *
         * @param cacheDir cache dir value
         */
        public void setCacheDir(Path cacheDir) {
            this.cacheDir = cacheDir;
        }

        /**
         * Returns the platform.
         *
         * @return platform
         */
        public BrowserPlatform getPlatform() {
            return platform;
        }

        /**
         * Updates platform.
         *
         * @param platform platform value
         */
        public void setPlatform(BrowserPlatform platform) {
            this.platform = platform;
        }

        /**
         * Returns the browser.
         *
         * @return browser
         */
        public String getBrowser() {
            return browser;
        }

        /**
         * Updates browser.
         *
         * @param browser browser instance
         */
        public void setBrowser(String browser) {
            this.browser = browser;
        }

        /**
         * Returns the build ID.
         *
         * @return build ID
         */
        public String getBuildId() {
            return buildId;
        }

        /**
         * Updates build ID.
         *
         * @param buildId build id
         */
        public void setBuildId(String buildId) {
            this.buildId = buildId;
        }

        /**
         * Returns the build ID alias.
         *
         * @return build ID alias
         */
        public String getBuildIdAlias() {
            return buildIdAlias;
        }

        /**
         * Updates build ID alias.
         *
         * @param buildIdAlias build id alias
         */
        public void setBuildIdAlias(String buildIdAlias) {
            this.buildIdAlias = buildIdAlias;
        }

        /**
         * Returns the download progress callback.
         *
         * @return download progress callback
         */
        public BrowserNetwork.ProgressCallback getDownloadProgressCallback() {
            return downloadProgressCallback;
        }

        /**
         * Updates download progress callback.
         *
         * @param downloadProgressCallback download progress callback value
         */
        public void setDownloadProgressCallback(BrowserNetwork.ProgressCallback downloadProgressCallback) {
            this.downloadProgressCallback = downloadProgressCallback;
        }

        /**
         * Returns whether default download progress is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isDefaultDownloadProgress() {
            return defaultDownloadProgress;
        }

        /**
         * Updates default download progress.
         *
         * @param defaultDownloadProgress default download progress value
         */
        public void setDefaultDownloadProgress(boolean defaultDownloadProgress) {
            this.defaultDownloadProgress = defaultDownloadProgress;
        }

        /**
         * Returns the base URL.
         *
         * @return base URL
         */
        public URI getBaseUrl() {
            return baseUrl;
        }

        /**
         * Updates base URL.
         *
         * @param baseUrl base url
         */
        public void setBaseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * Returns the download URL.
         *
         * @return download URL
         */
        public URI getDownloadUrl() {
            return downloadUrl;
        }

        /**
         * Updates download URL.
         *
         * @param downloadUrl download URL value
         */
        public void setDownloadUrl(URI downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        /**
         * Returns expected archive SHA-256.
         *
         * @return expected archive SHA-256
         */
        public String getExpectedArchiveSha256() {
            return expectedArchiveSha256;
        }

        /**
         * Updates expected archive SHA-256.
         *
         * @param expectedArchiveSha256 expected archive SHA-256 value
         */
        public void setExpectedArchiveSha256(String expectedArchiveSha256) {
            this.expectedArchiveSha256 = FetcherOptions.normalizeArchiveSha256(expectedArchiveSha256);
        }

        /**
         * Returns whether unverified downloads are allowed.
         *
         * @return {@code true} when unverified downloads are allowed
         */
        public boolean isAllowUnverifiedDownload() {
            return allowUnverifiedDownload;
        }

        /**
         * Updates whether unverified downloads are allowed.
         *
         * @param allowUnverifiedDownload allow unverified download value
         */
        public void setAllowUnverifiedDownload(boolean allowUnverifiedDownload) {
            this.allowUnverifiedDownload = allowUnverifiedDownload;
        }

        /**
         * Returns whether unpack is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isUnpack() {
            return unpack;
        }

        /**
         * Updates unpack.
         *
         * @param unpack unpack value
         */
        public void setUnpack(boolean unpack) {
            this.unpack = unpack;
        }

        /**
         * Returns whether force fallback for testing is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isForceFallbackForTesting() {
            return forceFallbackForTesting;
        }

        /**
         * Updates force fallback for testing.
         *
         * @param forceFallbackForTesting force fallback for testing value
         */
        public void setForceFallbackForTesting(boolean forceFallbackForTesting) {
            this.forceFallbackForTesting = forceFallbackForTesting;
        }

        /**
         * Returns whether install deps is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isInstallDeps() {
            return installDeps;
        }

        /**
         * Updates install deps.
         *
         * @param installDeps install deps value
         */
        public void setInstallDeps(boolean installDeps) {
            this.installDeps = installDeps;
        }

        /**
         * Returns security policy.
         *
         * @return security policy
         */
        public SecurityPolicy getSecurityPolicy() {
            return securityPolicy;
        }

        /**
         * Updates security policy.
         *
         * @param securityPolicy security policy value
         */
        public void setSecurityPolicy(SecurityPolicy securityPolicy) {
            this.securityPolicy = securityPolicy == null ? SecurityPolicy.defaultPolicy() : securityPolicy;
        }

        /**
         * Returns resource limits.
         *
         * @return resource limits
         */
        public ResourceLimits getResourceLimits() {
            return resourceLimits;
        }

        /**
         * Updates resource limits.
         *
         * @param resourceLimits resource limits value
         */
        public void setResourceLimits(ResourceLimits resourceLimits) {
            this.resourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
        }

        /**
         * Returns the providers.
         *
         * @return values
         */
        public List<BrowserProvider> getProviders() {
            return List.copyOf(providers);
        }

        /**
         * Adds provider.
         *
         * @param provider provider
         */
        public void addProvider(BrowserProvider provider) {
            if (provider != null) {
                providers.add(provider);
            }
        }
    }

    /**
     * Defines options for uninstall operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class UninstallOptions {

        /**
         * Current platform.
         */
        private BrowserPlatform platform;
        /**
         * Current cache dir.
         */
        private Path cacheDir = defaultCacheDir();
        /**
         * Current browser.
         */
        private String browser = ExecutableResolver.CHROME;
        /**
         * Current build ID.
         */
        private String buildId;

        /**
         * Creates an instance.
         */
        public UninstallOptions() {
            // No initialization required.
        }

        /**
         * Creates an instance.
         *
         * @param source source value
         */
        public UninstallOptions(UninstallOptions source) {
            UninstallOptions actualSource = Assert.notNull(source, "source");
            this.platform = actualSource.platform;
            this.cacheDir = actualSource.cacheDir;
            this.browser = actualSource.browser;
            this.buildId = actualSource.buildId;
        }

        /**
         * Returns the platform.
         *
         * @return platform
         */
        public BrowserPlatform getPlatform() {
            return platform;
        }

        /**
         * Updates platform.
         *
         * @param platform platform value
         */
        public void setPlatform(BrowserPlatform platform) {
            this.platform = platform;
        }

        /**
         * Returns the cache dir.
         *
         * @return cache dir
         */
        public Path getCacheDir() {
            return cacheDir;
        }

        /**
         * Updates cache dir.
         *
         * @param cacheDir cache dir value
         */
        public void setCacheDir(Path cacheDir) {
            this.cacheDir = cacheDir;
        }

        /**
         * Returns the browser.
         *
         * @return browser
         */
        public String getBrowser() {
            return browser;
        }

        /**
         * Updates browser.
         *
         * @param browser browser instance
         */
        public void setBrowser(String browser) {
            this.browser = browser;
        }

        /**
         * Returns the build ID.
         *
         * @return build ID
         */
        public String getBuildId() {
            return buildId;
        }

        /**
         * Updates build ID.
         *
         * @param buildId build id
         */
        public void setBuildId(String buildId) {
            this.buildId = buildId;
        }
    }

    /**
     * Defines options for get installed browsers operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class GetInstalledBrowsersOptions {

        /**
         * Current cache dir.
         */
        private Path cacheDir = defaultCacheDir();

        /**
         * Creates an instance.
         */
        public GetInstalledBrowsersOptions() {
            // No initialization required.
        }

        /**
         * Returns the cache dir.
         *
         * @return cache dir
         */
        public Path getCacheDir() {
            return cacheDir;
        }

        /**
         * Updates cache dir.
         *
         * @param cacheDir cache dir value
         */
        public void setCacheDir(Path cacheDir) {
            this.cacheDir = cacheDir;
        }
    }

}
