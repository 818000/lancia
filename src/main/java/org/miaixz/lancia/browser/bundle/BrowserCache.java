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
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.Browser;

/**
 * Manages cached browser downloads.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BrowserCache {

    /**
     * Shared constant for metadata file.
     */
    private static final String METADATA_FILE = Symbol.DOT + "metadata";
    /**
     * Shared constant for alias prefix.
     */
    private static final String ALIAS_PREFIX = "alias" + Symbol.DOT;
    /**
     * Shared constant for executable prefix.
     */
    private static final String EXECUTABLE_PREFIX = "executable" + Symbol.DOT;
    /**
     * Browser variants and artifacts supported by the cache.
     */
    private static final Set<String> SUPPORTED_BROWSERS = Set
            .of(ExecutableResolver.CHROME, "chrome-headless-shell", "chromium", "firefox", "chromedriver");
    /**
     * Shared constant for latest alias.
     */
    private static final String LATEST_ALIAS = "latest";
    /**
     * Shared constant for delete max retries.
     */
    private static final int DELETE_MAX_RETRIES = 10;
    /**
     * Shared constant for delete retry delay millis.
     */
    private static final long DELETE_RETRY_DELAY_MILLIS = 500L;
    /**
     * Current root dir.
     */
    private final Path rootDir;

    /**
     * Creates a browser cache.
     *
     * @param rootDir root dir
     */
    public BrowserCache(Path rootDir) {
        this.rootDir = Assert.notNull(rootDir, "rootDir");
        Logger.debug(false, "Launcher", "Browser cache initialized: rootDir={}", this.rootDir);
    }

    /**
     * Returns the root dir.
     *
     * @return root dir value
     */
    public Path rootDir() {
        return rootDir;
    }

    /**
     * Returns the browser root.
     *
     * @param browser browser instance
     * @return browser root value
     */
    public Path browserRoot(String browser) {
        return rootDir.resolve(normalizeBrowser(browser));
    }

    /**
     * Returns the metadata file.
     *
     * @param browser browser instance
     * @return metadata file value
     */
    public Path metadataFile(String browser) {
        return browserRoot(browser).resolve(METADATA_FILE);
    }

    /**
     * Returns the installation dir.
     *
     * @param browser  browser instance
     * @param platform platform value
     * @param buildId  build ID value
     * @return installation dir value
     */
    public Path installationDir(String browser, BrowserPlatform platform, String buildId) {
        return browserRoot(browser).resolve(platform.cacheId() + Symbol.MINUS + buildId);
    }

    /**
     * Returns the compute executable path.
     *
     * @param options operation options
     * @return compute executable path value
     */
    public Path computeExecutablePath(FetcherOptions options) {
        BrowserPlatform platform = options.getPlatformOrDetect();
        String buildId;
        try {
            buildId = resolveAlias(options.getBrowser(), options.getBuildId()).orElse(options.getBuildId());
        } catch (RuntimeException ex) {
            buildId = options.getBuildId();
            Logger.warn(
                    false,
                    "Launcher",
                    "Browser cache alias resolve failed, using requested build: browser={}, buildId={}, message={}",
                    options.getBrowser(),
                    options.getBuildId(),
                    ex.getMessage());
        }
        String resolvedBuildId = buildId;
        Path installationDir = installationDir(options.getBrowser(), platform, resolvedBuildId);
        Optional<Path> stored = readExecutablePath(options.getBrowser(), platform, resolvedBuildId);
        Path executable = stored.map(installationDir::resolve).orElseGet(
                () -> installationDir.resolve(relativeExecutablePath(options.getBrowser(), platform, resolvedBuildId)));
        Logger.debug(
                false,
                "Launcher",
                "Browser cache executable path computed: browser={}, platform={}, buildId={}, stored={}",
                options.getBrowser(),
                platform,
                resolvedBuildId,
                stored.isPresent());
        return executable;
    }

    /**
     * Returns whether the browser is already installed.
     *
     * @param options operation options
     * @return {@code true} when the condition matches
     */
    public boolean isInstalled(FetcherOptions options) {
        return FileKit.isFile(computeExecutablePath(options).toFile());
    }

    /**
     * Reads the executable path.
     *
     * @param browser  browser instance
     * @param platform platform
     * @param buildId  build id
     * @return read executable path value
     */
    public Optional<Path> readExecutablePath(String browser, BrowserPlatform platform, String buildId) {
        String value = readMetadata(browser).getProperty(EXECUTABLE_PREFIX + key(platform, buildId));
        return StringKit.isBlank(value) ? Optional.empty() : Optional.of(Path.of(value));
    }

    /**
     * Handles write executable path.
     *
     * @param browser        browser instance
     * @param platform       platform value
     * @param buildId        build ID value
     * @param executablePath executable path value
     */
    public void writeExecutablePath(String browser, BrowserPlatform platform, String buildId, Path executablePath) {
        Properties metadata = readMetadata(browser);
        metadata.setProperty(EXECUTABLE_PREFIX + key(platform, buildId), executablePath.toString());
        writeMetadata(browser, metadata);
        Logger.debug(
                false,
                "Launcher",
                "Browser cache executable path stored: browser={}, platform={}, buildId={}",
                normalizeBrowser(browser),
                platform,
                buildId);
    }

    /**
     * Resolves alias.
     *
     * @param browser browser instance
     * @param alias   alias
     * @return resolve alias value
     */
    public Optional<String> resolveAlias(String browser, String alias) {
        Properties metadata = readMetadata(browser);
        if (LATEST_ALIAS.equals(alias)) {
            Optional<String> latest = Optional.of(aliasValues(metadata).stream().max(BrowserCache::compareBuildIds));
            Logger.debug(
                    true,
                    "Launcher",
                    "Browser cache latest alias resolved: browser={}, present={}",
                    normalizeBrowser(browser),
                    latest.isPresent());
            return latest;
        }
        String value = metadata.getProperty(ALIAS_PREFIX + alias);
        Optional<String> resolved = StringKit.isBlank(value) ? Optional.empty() : Optional.of(value);
        Logger.debug(
                true,
                "Launcher",
                "Browser cache alias resolved: browser={}, alias={}, present={}",
                normalizeBrowser(browser),
                alias,
                resolved.isPresent());
        return resolved;
    }

    /**
     * Handles write alias.
     *
     * @param browser browser instance
     * @param alias   alias value
     * @param buildId build ID value
     */
    public void writeAlias(String browser, String alias, String buildId) {
        Properties metadata = readMetadata(browser);
        metadata.setProperty(ALIAS_PREFIX + alias, buildId);
        writeMetadata(browser, metadata);
        Logger.debug(
                true,
                "Launcher",
                "Browser cache alias stored: browser={}, alias={}, buildId={}",
                normalizeBrowser(browser),
                alias,
                buildId);
    }

    /**
     * Returns the installed browsers.
     *
     * @param browser browser instance
     * @return values
     */
    public List<InstalledBrowser> installedBrowsers(String browser) {
        Path root = browserRoot(browser);
        if (!Files.isDirectory(root)) {
            Logger.debug(
                    true,
                    "Launcher",
                    "Browser cache list skipped, browser root missing: browser={}",
                    normalizeBrowser(browser));
            return List.of();
        }
        List<InstalledBrowser> result = new ArrayList<>();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(path -> parseInstallation(browser, path).ifPresent(result::add));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read browser cache directory: " + root, ex);
        }
        result.sort(
                Comparator.comparing(InstalledBrowser::browser).thenComparing(item -> item.platform().cacheId())
                        .thenComparing(InstalledBrowser::buildId, BrowserCache::compareBuildIds));
        Logger.debug(
                false,
                "Launcher",
                "Browser cache installed list loaded: browser={}, count={}",
                normalizeBrowser(browser),
                result.size());
        return List.copyOf(result);
    }

    /**
     * Returns the installed browsers.
     *
     * @return values
     */
    public List<InstalledBrowser> getInstalledBrowsers() {
        if (!Files.isDirectory(rootDir)) {
            Logger.debug(true, "Launcher", "Browser cache root missing: rootDir={}", rootDir);
            return List.of();
        }
        List<InstalledBrowser> result = new ArrayList<>();
        try (var stream = Files.list(rootDir)) {
            stream.filter(Files::isDirectory).forEach(path -> {
                String browser = path.getFileName().toString();
                if (SUPPORTED_BROWSERS.contains(browser)) {
                    result.addAll(installedBrowsers(browser));
                }
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read browser cache root directory: " + rootDir, ex);
        }
        result.sort(
                Comparator.comparing(InstalledBrowser::browser).thenComparing(item -> item.platform().cacheId())
                        .thenComparing(InstalledBrowser::buildId, BrowserCache::compareBuildIds));
        Logger.debug(false, "Launcher", "Browser cache installed browsers loaded: count={}", result.size());
        return List.copyOf(result);
    }

    /**
     * Handles uninstall.
     *
     * @param browser  browser instance
     * @param platform platform value
     * @param buildId  build ID value
     */
    public void uninstall(String browser, BrowserPlatform platform, String buildId) {
        try {
            Logger.debug(
                    true,
                    "Launcher",
                    "Browser cache uninstall requested: browser={}, platform={}, buildId={}",
                    normalizeBrowser(browser),
                    platform,
                    buildId);
            Properties metadata = readMetadata(browser);
            boolean changed = removeAliases(metadata, buildId);
            Object removed = metadata.remove(EXECUTABLE_PREFIX + key(platform, buildId));
            if (changed || removed != null) {
                writeMetadata(browser, metadata);
            }
            deleteRecursively(installationDir(browser, platform, buildId));
            Logger.debug(
                    false,
                    "Launcher",
                    "Browser cache uninstall completed: browser={}, platform={}, buildId={}",
                    normalizeBrowser(browser),
                    platform,
                    buildId);
        } catch (IOException ex) {
            Logger.error(
                    false,
                    "Launcher",
                    "Browser cache uninstall failed: buildId={}, message={}",
                    buildId,
                    ex.getMessage());
            throw new IllegalStateException("Failed to delete browser cache: " + buildId, ex);
        }
    }

    /**
     * Handles clear.
     */
    public void clear() {
        try {
            Logger.debug(true, "Launcher", "Browser cache clear requested: rootDir={}", rootDir);
            deleteRecursively(rootDir);
            Logger.debug(false, "Launcher", "Browser cache cleared: rootDir={}", rootDir);
        } catch (IOException ex) {
            Logger.error(false, "Launcher", "Browser cache clear failed: message={}", ex.getMessage());
            throw new IllegalStateException("Failed to clear browser cache: " + rootDir, ex);
        }
    }

    /**
     * Reads the metadata.
     *
     * @param browser browser instance
     * @return read metadata value
     */
    public Properties readMetadata(String browser) {
        Properties properties = new Properties();
        Path metadata = metadataFile(browser);
        if (!FileKit.isFile(metadata.toFile())) {
            Logger.debug(true, "Launcher", "Browser cache metadata missing: browser={}", normalizeBrowser(browser));
            return properties;
        }
        try (Reader reader = Files.newBufferedReader(metadata)) {
            properties.load(reader);
            Logger.debug(
                    false,
                    "Launcher",
                    "Browser cache metadata loaded: browser={}, entries={}",
                    normalizeBrowser(browser),
                    properties.size());
            return properties;
        } catch (IOException ex) {
            Logger.error(
                    false,
                    "Launcher",
                    "Browser cache metadata read failed: browser={}, message={}",
                    normalizeBrowser(browser),
                    ex.getMessage());
            throw new IllegalStateException("Failed to read browser cache metadata: " + metadata, ex);
        }
    }

    /**
     * Handles write metadata.
     *
     * @param browser  browser instance
     * @param metadata metadata value
     */
    public void writeMetadata(String browser, Properties metadata) {
        Path file = metadataFile(browser);
        try {
            FileKit.mkdir(file.getParent().toFile());
            try (Writer writer = Files.newBufferedWriter(file)) {
                Properties actualMetadata = metadata == null ? new Properties() : metadata;
                actualMetadata.store(writer, "lancia browser cache");
                Logger.debug(
                        false,
                        "Launcher",
                        "Browser cache metadata written: browser={}, entries={}",
                        normalizeBrowser(browser),
                        actualMetadata.size());
            }
        } catch (IOException ex) {
            Logger.error(
                    false,
                    "Launcher",
                    "Browser cache metadata write failed: browser={}, message={}",
                    normalizeBrowser(browser),
                    ex.getMessage());
            throw new IllegalStateException("Failed to write browser cache metadata: " + file, ex);
        }
    }

    /**
     * Parses installation.
     *
     * @param browser browser instance
     * @param path    file path
     * @return optional value
     */
    private Optional<InstalledBrowser> parseInstallation(String browser, Path path) {
        String name = path.getFileName().toString();
        int index = name.indexOf(Symbol.C_MINUS);
        if (index <= 0 || index == name.length() - 1 || index != name.lastIndexOf(Symbol.C_MINUS)) {
            return Optional.empty();
        }
        String platformId = name.substring(0, index);
        String buildId = name.substring(index + 1);
        for (BrowserPlatform platform : BrowserPlatform.values()) {
            if (platform.cacheId().equals(platformId)) {
                return Optional.of(new InstalledBrowser(this, browser, platform, buildId));
            }
        }
        return Optional.empty();
    }

    /**
     * Handles delete recursively.
     *
     * @param path file path
     * @throws IOException if the operation fails
     */
    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            Logger.debug(true, "Launcher", "Browser cache delete skipped, path missing: path={}", path);
            return;
        }
        IOException failure = null;
        for (int i = 0; i <= DELETE_MAX_RETRIES; i++) {
            try {
                Logger.debug(true, "Launcher", "Browser cache delete attempt: path={}, attempt={}", path, i + 1);
                FileKit.remove(path.toFile());
                if (!Files.exists(path)) {
                    Logger.debug(false, "Launcher", "Browser cache delete completed: path={}", path);
                    return;
                }
            } catch (RuntimeException ex) {
                failure = new IOException("Failed to delete directory: " + path, ex);
                Logger.warn(
                        false,
                        "Launcher",
                        "Browser cache delete attempt failed: path={}, attempt={}, message={}",
                        path,
                        i + 1,
                        ex.getMessage());
            }
            if (i < DELETE_MAX_RETRIES) {
                sleepBeforeRetry();
            }
        }
        if (failure != null) {
            throw failure;
        }
        throw new IOException("Directory still exists after deletion: " + path);
    }

    /**
     * Returns the key.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return key value
     */
    private String key(BrowserPlatform platform, String buildId) {
        return platform.cacheId() + Symbol.MINUS + buildId;
    }

    /**
     * Returns the normalize browser.
     *
     * @param browser browser instance
     * @return normalize browser value
     */
    private String normalizeBrowser(String browser) {
        return StringKit.isBlank(browser) ? ExecutableResolver.CHROME : browser;
    }

    /**
     * Returns the alias values.
     *
     * @param metadata metadata value
     * @return values
     */
    private static Set<String> aliasValues(Properties metadata) {
        Set<String> values = new LinkedHashSet<>();
        for (String name : metadata.stringPropertyNames()) {
            if (!name.startsWith(ALIAS_PREFIX)) {
                continue;
            }
            String value = metadata.getProperty(name);
            if (!StringKit.isBlank(value)) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Returns the remove aliases.
     *
     * @param metadata metadata value
     * @param buildId  build ID value
     * @return {@code true} when the condition matches
     */
    private static boolean removeAliases(Properties metadata, String buildId) {
        boolean changed = false;
        for (String name : List.copyOf(metadata.stringPropertyNames())) {
            if (name.startsWith(ALIAS_PREFIX) && java.util.Objects.equals(metadata.getProperty(name), buildId)) {
                metadata.remove(name);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Returns the compare build ids.
     *
     * @param left  left value
     * @param right right value
     * @return compare build ids value
     */
    private static int compareBuildIds(String left, String right) {
        String[] leftParts = String.valueOf(left).split("[._-]");
        String[] rightParts = String.valueOf(right).split("[._-]");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            String leftPart = i < leftParts.length ? leftParts[i] : "0";
            String rightPart = i < rightParts.length ? rightParts[i] : "0";
            int result = compareBuildPart(leftPart, rightPart);
            if (result != 0) {
                return result;
            }
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    /**
     * Returns the compare build part.
     *
     * @param left  left value
     * @param right right value
     * @return compare build part value
     */
    private static int compareBuildPart(String left, String right) {
        boolean leftNumber = left.chars().allMatch(Character::isDigit);
        boolean rightNumber = right.chars().allMatch(Character::isDigit);
        if (leftNumber && rightNumber) {
            return BrowserData.compareNumericText(left, right);
        }
        return left.compareTo(right);
    }

    /**
     * Handles sleep before retry.
     *
     * @throws IOException if the operation fails
     */
    private static void sleepBeforeRetry() throws IOException {
        if (!ThreadKit.sleep(DELETE_RETRY_DELAY_MILLIS)) {
            throw new IOException("Interrupted while waiting to retry deletion.");
        }
    }

    /**
     * Returns the relative executable path.
     *
     * @param browser  browser instance
     * @param platform platform value
     * @param buildId  build ID value
     * @return relative executable path value
     */
    static Path relativeExecutablePath(String browser, BrowserPlatform platform, String buildId) {
        String actualBrowser = StringKit.isBlank(browser) ? ExecutableResolver.CHROME : browser;
        return BrowserData.relativeExecutablePath(Browser.fromValue(actualBrowser), platform, buildId);
    }

    /**
     * Coordinates installed browser operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class InstalledBrowser {

        /**
         * Current browser.
         */
        private final String browser;
        /**
         * Current platform.
         */
        private final BrowserPlatform platform;
        /**
         * Current build ID.
         */
        private final String buildId;
        /**
         * Current path.
         */
        private final Path path;
        /**
         * Current executable path.
         */
        private final Path executablePath;
        /**
         * Current cache.
         */
        private final BrowserCache cache;

        /**
         * Creates an instance.
         *
         * @param cache    cache value
         * @param browser  browser instance
         * @param platform platform value
         * @param buildId  build ID value
         */
        public InstalledBrowser(BrowserCache cache, String browser, BrowserPlatform platform, String buildId) {
            this.cache = Assert.notNull(cache, "cache");
            this.browser = cache.normalizeBrowser(browser);
            this.platform = Assert.notNull(platform, "platform");
            this.buildId = Assert.notBlank(buildId, "buildId");
            this.path = cache.installationDir(this.browser, this.platform, this.buildId);
            this.executablePath = cache.readExecutablePath(this.browser, this.platform, this.buildId)
                    .map(this.path::resolve).orElseGet(
                            () -> this.path.resolve(relativeExecutablePath(this.browser, this.platform, this.buildId)));
        }

        /**
         * Creates an instance.
         *
         * @param browser  browser instance
         * @param platform platform value
         * @param buildId  build ID value
         * @param path     file path
         */
        public InstalledBrowser(String browser, BrowserPlatform platform, String buildId, Path path) {
            Path actualPath = Assert.notNull(path, "path");
            this.cache = new BrowserCache(actualPath.getParent().getParent());
            this.browser = cache.normalizeBrowser(browser);
            this.platform = Assert.notNull(platform, "platform");
            this.buildId = Assert.notBlank(buildId, "buildId");
            this.path = actualPath;
            this.executablePath = cache.readExecutablePath(this.browser, this.platform, this.buildId)
                    .map(this.path::resolve).orElseGet(
                            () -> this.path.resolve(relativeExecutablePath(this.browser, this.platform, this.buildId)));
        }

        /**
         * Returns the browser.
         *
         * @return browser value
         */
        public String browser() {
            return browser;
        }

        /**
         * Returns the platform.
         *
         * @return platform value
         */
        public BrowserPlatform platform() {
            return platform;
        }

        /**
         * Builds ID.
         *
         * @return built ID
         */
        public String buildId() {
            return buildId;
        }

        /**
         * Returns the path.
         *
         * @return path value
         */
        public Path path() {
            return path;
        }

        /**
         * Returns the executable path.
         *
         * @return executable path value
         */
        public Path executablePath() {
            return executablePath;
        }

        /**
         * Reads the metadata.
         *
         * @return read metadata value
         */
        public Properties readMetadata() {
            return cache.readMetadata(browser);
        }

        /**
         * Handles write metadata.
         *
         * @param metadata metadata value
         */
        public void writeMetadata(Properties metadata) {
            cache.writeMetadata(browser, metadata);
        }
    }

}
