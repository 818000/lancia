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
package org.miaixz.lancia;

import java.nio.file.Path;
import java.util.List;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.nimble.browser.BrowserVariant;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.options.LaunchOptions;
import org.miaixz.lancia.runtime.BrowserRuntime;
import org.miaixz.lancia.runtime.Configuration;
import org.miaixz.lancia.runtime.QueryHandlers;

/**
 * Public Lancia entry point.
 *
 * <p>
 * This class maps Puppeteer's default entry point and exposes launch, connect, and global query handler APIs.
 * </p>
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Puppeteer {

    /**
     * Runtime configuration.
     */
    private static volatile Configuration configuration = Configuration.fromEnvironment(System.getenv());

    /**
     * Cached launcher.
     */
    private static volatile Launcher launcher;

    /**
     * Browser variant for the cached launcher.
     */
    private static volatile BrowserVariant launcherBrowser;

    /**
     * Last launched browser variant.
     */
    private static volatile BrowserVariant lastLaunchedBrowser;

    /**
     * Hides the entry point constructor.
     */
    private Puppeteer() {
        // No initialization required.
    }

    /**
     * Launches a browser with default options.
     *
     * @return browser instance selected by the resolved protocol
     */
    public static Browser launch() {
        return launch(new LaunchOptions());
    }

    /**
     * Launches a browser with custom options and routes by protocol.
     *
     * @param options launch options
     * @return browser instance selected by the resolved protocol
     */
    public static Browser launch(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        BrowserVariant browser = actualOptions.getBrowser() == null ? defaultBrowser() : actualOptions.getBrowser();
        actualOptions.setBrowser(browser);
        if (browser == BrowserVariant.FIREFOX && !actualOptions.isProtocolConfigured()) {
            actualOptions.setProtocol(ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI);
        }
        Logger.debug(
                true,
                "Launcher",
                "Launch route requested: browser={}, protocol={}, pipe={}",
                browser,
                actualOptions.getProtocol(),
                actualOptions.isPipe());
        try {
            Browser result;
            if (ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI.equals(actualOptions.getProtocol())) {
                result = launchBidi(actualOptions);
            } else {
                result = launchCdp(actualOptions);
            }
            Logger.debug(
                    false,
                    "Launcher",
                    "Launch route completed: browser={}, protocol={}",
                    browser,
                    actualOptions.getProtocol());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Launcher",
                    ex,
                    "Launch route failed: browser={}, protocol={}",
                    browser,
                    actualOptions.getProtocol());
            throw ex;
        }
    }

    /**
     * Connects to an existing browser and routes by protocol.
     *
     * @param options connect options
     * @return browser instance selected by the resolved protocol
     */
    public static Browser connect(ConnectOptions options) {
        ConnectOptions actualOptions = options == null ? new ConnectOptions() : options;
        Logger.debug(true, "Launcher", "Connect route requested: protocol={}", actualOptions.getProtocol());
        try {
            Browser result;
            if (ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI.equals(actualOptions.getProtocol())) {
                result = connectBidi(actualOptions);
            } else {
                result = connectCdp(actualOptions);
            }
            Logger.debug(false, "Launcher", "Connect route completed: protocol={}", actualOptions.getProtocol());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "Connect route failed: protocol={}", actualOptions.getProtocol());
            throw ex;
        }
    }

    /**
     * Returns the current Lancia package version.
     *
     * @return package version
     */
    public static String packageVersion() {
        return Version.all();
    }

    /**
     * Downloads browsers using the active runtime configuration.
     *
     * <p>
     * Lancia runtime extension.
     * </p>
     *
     * @return user-facing install messages
     */
    public static List<String> downloadBrowsers() {
        Logger.debug(true, "Browser", "Browser download requested.");
        try {
            List<String> messages = BrowserRuntime.download(configuration);
            Logger.debug(false, "Browser", "Browser download completed: messages={}", messages.size());
            return messages;
        } catch (RuntimeException ex) {
            Logger.error(false, "Browser", ex, "Browser download failed.");
            throw ex;
        }
    }

    /**
     * Returns the executable path for the last launched or default browser.
     *
     * @return executable path
     */
    public static Path executablePath() {
        return executablePath(new LaunchOptions());
    }

    /**
     * Returns the executable path for a Chrome channel.
     *
     * @param channel Chrome channel
     * @return executable path
     */
    public static Path executablePath(String channel) {
        LaunchOptions options = new LaunchOptions();
        options.setBrowser(BrowserVariant.CHROME);
        options.setChannel(channel);
        return executablePath(options);
    }

    /**
     * Returns the executable path for launch options.
     *
     * @param options launch options
     * @return executable path
     */
    public static Path executablePath(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        if (actualOptions.getExecutablePath() != null) {
            return actualOptions.getExecutablePath();
        }
        if (configuration.getExecutablePath() != null) {
            return configuration.getExecutablePath();
        }
        BrowserVariant browser = actualOptions.getBrowser() == null ? lastLaunchedBrowser()
                : actualOptions.getBrowser();
        return launcher(browser).executable(actualOptions);
    }

    /**
     * Returns the browser version used by the current configuration.
     *
     * <p>
     * Lancia runtime extension.
     * </p>
     *
     * @return browser version
     */
    public static String browserVersion() {
        return BrowserRuntime.version();
    }

    /**
     * Returns the default browser download path.
     *
     * <p>
     * Lancia runtime extension.
     * </p>
     *
     * @return default download path
     */
    public static Path defaultDownloadPath() {
        return configuration.getCacheDirectory();
    }

    /**
     * Creates a browser cache entry equivalent to Puppeteer's browser fetcher API.
     *
     * <p>
     * Lancia runtime extension.
     * </p>
     *
     * @return browser cache entry
     */
    public static Object createBrowserFetcher() {
        return BrowserRuntime.fetcher(configuration);
    }

    /**
     * Returns the browser variant that was last launched or the configured default browser.
     *
     * <p>
     * Lancia runtime extension.
     * </p>
     *
     * @return browser variant
     */
    public static BrowserVariant lastLaunchedBrowser() {
        return lastLaunchedBrowser == null ? defaultBrowser() : lastLaunchedBrowser;
    }

    /**
     * Returns the configured default browser.
     *
     * @return default browser variant
     */
    public static BrowserVariant defaultBrowser() {
        return BrowserVariant.fromValue(configuration.getDefaultBrowser()).orElse(BrowserVariant.CHROME);
    }

    /**
     * Returns default launch arguments for the default browser.
     *
     * @return default launch arguments
     */
    public static List<String> defaultArgs() {
        return defaultArgs(new LaunchOptions());
    }

    /**
     * Returns default launch arguments for the requested browser.
     *
     * @param options launch options
     * @return default launch arguments
     */
    public static List<String> defaultArgs(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        BrowserVariant browser = actualOptions.getBrowser() == null ? lastLaunchedBrowser()
                : actualOptions.getBrowser();
        return launcher(browser).args(actualOptions);
    }

    /**
     * Removes cached Chrome and Firefox builds that do not match the current Lancia revision.
     */
    public static void trimCache() {
        BrowserRuntime.trim(configuration);
    }

    /**
     * Registers a custom query handler.
     *
     * @param name         query handler name
     * @param queryHandler custom query handler
     */
    public static void registerCustomQueryHandler(String name, Handler queryHandler) {
        QueryHandlers.register(name, queryHandler);
    }

    /**
     * Unregisters a custom query handler.
     *
     * @param name query handler name
     */
    public static void unregisterCustomQueryHandler(String name) {
        QueryHandlers.unregister(name);
    }

    /**
     * Returns custom query handler names.
     *
     * @return custom query handler names
     */
    public static List<String> customQueryHandlerNames() {
        return QueryHandlers.names();
    }

    /**
     * Clears custom query handlers.
     */
    public static void clearCustomQueryHandlers() {
        QueryHandlers.clear();
    }

    /**
     * Creates a single-element custom query handler.
     *
     * <p>
     * Lancia runtime extension.
     * </p>
     *
     * @param queryOne queryOne JavaScript source
     * @return custom query handler
     */
    public static Handler queryOne(String queryOne) {
        return QueryHandlers.queryOne(queryOne);
    }

    /**
     * Creates a multi-element custom query handler.
     *
     * <p>
     * Lancia runtime extension.
     * </p>
     *
     * @param queryAll queryAll JavaScript source
     * @return custom query handler
     */
    public static Handler queryAll(String queryAll) {
        return QueryHandlers.queryAll(queryAll);
    }

    /**
     * Returns a registered custom query handler.
     *
     * <p>
     * Lancia runtime extension.
     * </p>
     *
     * @param name query handler name
     * @return registered custom query handler
     */
    public static Optional<? extends Handler> customQueryHandler(String name) {
        return QueryHandlers.get(name);
    }

    /**
     * Launches a browser through the CDP protocol.
     *
     * @param options launch options
     * @return CDP browser instance
     */
    private static Browser launchCdp(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        BrowserVariant browser = actualOptions.getBrowser() == null ? defaultBrowser() : actualOptions.getBrowser();
        actualOptions.setBrowser(browser);
        if (browser == BrowserVariant.FIREFOX && actualOptions.isProtocolConfigured()
                && ConnectOptions.PROTOCOL_CDP.equals(actualOptions.getProtocol())) {
            throw new IllegalArgumentException("Firefox launch with cdp protocol is disabled by configuration.");
        }
        actualOptions.setProtocol(ConnectOptions.PROTOCOL_CDP);
        lastLaunchedBrowser = browser;
        Logger.debug(true, "Launcher", "CDP launch requested: browser={}, pipe={}", browser, actualOptions.isPipe());
        try {
            Browser result = launcher(browser).launch(actualOptions);
            Logger.debug(false, "Launcher", "CDP launch completed: browser={}", browser);
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "CDP launch failed: browser={}", browser);
            throw ex;
        }
    }

    /**
     * Connects to an existing browser through the CDP protocol.
     *
     * @param options connect options
     * @return CDP browser instance
     */
    private static Browser connectCdp(ConnectOptions options) {
        ConnectOptions actualOptions = options == null ? new ConnectOptions() : options;
        actualOptions.setProtocol(ConnectOptions.PROTOCOL_CDP);
        Logger.debug(true, "Launcher", "CDP connect requested.");
        try {
            Browser result = launcher(defaultBrowser()).connect(actualOptions);
            Logger.debug(false, "Launcher", "CDP connect completed.");
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "CDP connect failed.");
            throw ex;
        }
    }

    /**
     * Launches a browser through the WebDriver BiDi protocol.
     *
     * @param options launch options
     * @return BiDi browser instance
     */
    private static Browser launchBidi(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        BrowserVariant browser = actualOptions.getBrowser() == null ? defaultBrowser() : actualOptions.getBrowser();
        actualOptions.setBrowser(browser);
        actualOptions.setProtocol(ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI);
        lastLaunchedBrowser = browser;
        Logger.debug(true, "Launcher", "BiDi launch requested: browser={}", browser);
        try {
            Browser result = launcher(browser).launch(actualOptions);
            Logger.debug(false, "Launcher", "BiDi launch completed: browser={}", browser);
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "BiDi launch failed: browser={}", browser);
            throw ex;
        }
    }

    /**
     * Connects to an existing browser through the WebDriver BiDi protocol.
     *
     * @param options connect options
     * @return BiDi browser instance
     */
    private static Browser connectBidi(ConnectOptions options) {
        ConnectOptions actualOptions = options == null ? new ConnectOptions() : options;
        actualOptions.setProtocol(ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI);
        Logger.debug(true, "Launcher", "BiDi connect requested.");
        try {
            Browser result = launcher(defaultBrowser()).connect(actualOptions);
            Logger.debug(false, "Launcher", "BiDi connect completed.");
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "BiDi connect failed.");
            throw ex;
        }
    }

    /**
     * Returns a cached launcher for the browser variant.
     *
     * @param browser browser variant
     * @return launcher
     */
    private static Launcher launcher(BrowserVariant browser) {
        Launcher current = launcher;
        if (current != null && (launcherBrowser == null || launcherBrowser == browser)) {
            return current;
        }
        Launcher created = Launcher.of(browser);
        launcher = created;
        launcherBrowser = browser;
        return created;
    }

}
