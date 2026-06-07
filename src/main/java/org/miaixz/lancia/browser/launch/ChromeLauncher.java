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
package org.miaixz.lancia.browser.launch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.browser.bundle.BrowserFetcher;
import org.miaixz.lancia.browser.bundle.ExecutableResolver;
import org.miaixz.lancia.browser.bundle.FetcherOptions;
import org.miaixz.lancia.browser.supervisor.Runner;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowser;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.options.LaunchOptions;

/**
 * Chrome and Chromium launcher.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ChromeLauncher extends BrowserLauncher {

    /**
     * Creates a Chrome launcher with the default runner.
     */
    public ChromeLauncher() {
        super();
    }

    /**
     * Creates a Chrome launcher with a custom runner.
     *
     * @param runner browser process runner
     */
    public ChromeLauncher(Runner runner) {
        super(runner);
    }

    /**
     * Launches Chrome or Chromium.
     *
     * @param options launch options
     * @return browser instance
     */
    @Override
    public Browser launch(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        Logger.debug(
                true,
                "Launcher",
                "Chrome launch requested: protocol={}, pipe={}",
                actualOptions.getProtocol(),
                actualOptions.isPipe());
        if (ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI.equals(actualOptions.getProtocol())) {
            return adapt(launchBidi(actualOptions));
        }
        try {
            ResolvedLaunchArgs launchArgs = computeLaunchArguments(actualOptions);
            ensureExecutableExists(launchArgs.getExecutablePath());
            Browser result = runner.launch(launchArgs.getExecutablePath(), launchArgs.getArgs(), actualOptions);
            Logger.debug(
                    false,
                    "Launcher",
                    "Chrome launch completed: executable={}, args={}",
                    launchArgs.getExecutablePath(),
                    launchArgs.getArgs().size());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Launcher",
                    ex,
                    "Chrome launch failed: protocol={}, pipe={}",
                    actualOptions.getProtocol(),
                    actualOptions.isPipe());
            throw ex;
        }
    }

    /**
     * Launches Chrome or Chromium and returns a BiDi browser backed by CDP.
     *
     * @param options launch options
     * @return BiDi browser instance
     */
    public BidiBrowser launchBidi(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        Logger.debug(true, "Launcher", "Chrome BiDi launch requested: pipe={}", actualOptions.isPipe());
        try {
            ResolvedLaunchArgs launchArgs = computeLaunchArguments(actualOptions);
            ensureExecutableExists(launchArgs.getExecutablePath());
            BidiBrowser result = runner
                    .launchBidiOverCdp(launchArgs.getExecutablePath(), launchArgs.getArgs(), actualOptions);
            Logger.debug(
                    false,
                    "Launcher",
                    "Chrome BiDi launch completed: executable={}, args={}",
                    launchArgs.getExecutablePath(),
                    launchArgs.getArgs().size());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "Chrome BiDi launch failed: pipe={}", actualOptions.isPipe());
            throw ex;
        }
    }

    /**
     * Computes final launch arguments.
     *
     * @param options launch options
     * @return resolved launch arguments
     */
    public ResolvedLaunchArgs computeLaunchArguments(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        List<String> chromeArguments = new ArrayList<>(defaultArgs(actualOptions));
        if (chromeArguments.stream().noneMatch(argument -> argument.startsWith("--remote-debugging-"))) {
            if (actualOptions.isPipe()) {
                if (actualOptions.getDebuggingPort() != null) {
                    throw new IllegalArgumentException(
                            "Browser should be launched with either pipe or debugging port - not both.");
                }
                chromeArguments.add("--remote-debugging-pipe");
            } else {
                int port = actualOptions.getDebuggingPort() == null ? 0 : actualOptions.getDebuggingPort();
                chromeArguments.add("--remote-debugging-port=" + port);
            }
        }

        boolean temporaryUserDataDir = false;
        Path userDataDir = findUserDataDir(chromeArguments);
        if (userDataDir == null) {
            temporaryUserDataDir = true;
            userDataDir = createProfileDirectory();
            chromeArguments.add("--user-data-dir=" + userDataDir);
        }
        actualOptions.setUserDataDir(userDataDir);
        actualOptions.setTemporaryUserDataDir(temporaryUserDataDir);

        Path executable = resolveExecutablePath(actualOptions);
        ResolvedLaunchArgs resolved = new ResolvedLaunchArgs();
        resolved.setExecutablePath(executable);
        resolved.setArgs(chromeArguments);
        resolved.setTempUserDataDir(temporaryUserDataDir);
        resolved.setUserDataDir(userDataDir);
        Logger.debug(
                false,
                "Launcher",
                "Chrome launch arguments resolved: executable={}, args={}, userDataDir={}, tempProfile={}",
                executable,
                chromeArguments.size(),
                userDataDir,
                temporaryUserDataDir);
        return resolved;
    }

    /**
     * Removes a temporary user data directory created by this launcher.
     *
     * @param path   user data directory
     * @param isTemp whether the directory is temporary
     */
    protected void cleanUserDataDir(Path path, boolean isTemp) {
        if (!isTemp || path == null || !FileKit.exists(path.toFile())) {
            return;
        }
        try {
            FileKit.remove(path.toFile());
            Logger.debug(false, "Launcher", "Chrome temporary user data directory removed: {}", path);
        } catch (Exception ex) {
            Logger.warn(false, "Launcher", ex, "Failed to remove Chrome temporary user data directory: {}", path);
            throw new InternalException("Failed to remove temporary Chrome user data directory: " + path, ex);
        }
    }

    /**
     * Generates Chrome default launch arguments.
     *
     * @param options launch options
     * @return Chrome default launch arguments
     */
    public List<String> defaultArgs(LaunchOptions options) {
        Assert.notNull(options, "options");
        List<String> userArgs = new ArrayList<>(options.getArgs());
        List<String> userDisabledFeatures = getFeatures("--disable-features", userArgs);
        if (!userDisabledFeatures.isEmpty()) {
            removeMatchingFlags(userArgs, "--disable-features");
        }
        List<String> userEnabledFeatures = getFeatures("--enable-features", userArgs);
        if (!userEnabledFeatures.isEmpty()) {
            removeMatchingFlags(userArgs, "--enable-features");
        }

        List<String> enabledFeatures = new ArrayList<>();
        enabledFeatures.add("PdfOopif");
        enabledFeatures.addAll(userEnabledFeatures);

        List<String> disabledFeatures = new ArrayList<>();
        disabledFeatures.add("Translate");
        disabledFeatures.add("AcceptCHFrame");
        disabledFeatures.add("MediaRouter");
        disabledFeatures.add("OptimizationHints");
        disabledFeatures.add("WebUIReloadButton");
        if (!isExperimentalChromeFeaturesEnabled()) {
            disabledFeatures.add("ProcessPerSiteUpToMainFrameThreshold");
            disabledFeatures.add("IsolateSandboxedIframes");
        }
        disabledFeatures.addAll(userDisabledFeatures);
        disabledFeatures.removeIf(feature -> StringKit.isBlank(feature) || enabledFeatures.contains(feature));
        enabledFeatures.removeIf(StringKit::isBlank);

        List<String> args = new ArrayList<>();
        args.add("--allow-pre-commit-input");
        args.add("--disable-background-networking");
        args.add("--disable-background-timer-throttling");
        args.add("--disable-backgrounding-occluded-windows");
        args.add("--disable-breakpad");
        args.add("--disable-client-side-phishing-detection");
        args.add("--disable-component-extensions-with-background-pages");
        args.add("--disable-crash-reporter");
        args.add("--disable-default-apps");
        args.add("--disable-dev-shm-usage");
        args.add("--disable-hang-monitor");
        args.add("--disable-infobars");
        args.add("--disable-ipc-flooding-protection");
        args.add("--disable-popup-blocking");
        args.add("--disable-prompt-on-repost");
        args.add("--disable-renderer-backgrounding");
        args.add("--disable-search-engine-choice-screen");
        args.add("--disable-sync");
        args.add("--enable-automation");
        args.add("--export-tagged-pdf");
        args.add("--force-color-profile=srgb");
        args.add("--generate-pdf-document-outline");
        args.add("--metrics-recording-only");
        args.add("--no-first-run");
        args.add("--password-store=basic");
        args.add("--use-mock-keychain");
        args.add("--disable-features=" + String.join(Symbol.COMMA, disabledFeatures));
        args.add("--enable-features=" + String.join(Symbol.COMMA, enabledFeatures));
        if (isNoSandboxEnabled() && !userArgs.contains("--no-sandbox")) {
            args.add("--no-sandbox");
        }
        if (options.isHeadless()) {
            args.add(options.isHeadlessShell() ? "--headless" : "--headless=new");
            args.add("--hide-scrollbars");
            args.add("--mute-audio");
        }
        if (options.getUserDataDir() != null) {
            args.add("--user-data-dir=" + options.getUserDataDir().toAbsolutePath().normalize());
        }
        if (options.isDevtools()) {
            args.add("--auto-open-devtools-for-tabs");
        }
        if (!options.isEnableExtensions()) {
            args.add("--disable-extensions");
        }
        if (userArgs.stream().allMatch(arg -> arg.startsWith(Symbol.MINUS))) {
            args.add(Builder.ABOUT_BLANK);
        }
        args.addAll(userArgs);
        return args;
    }

    /**
     * Resolves the Chrome executable path.
     *
     * @param executablePath explicit executable path
     * @return executable path
     */
    public Path resolveExecutablePath(Path executablePath) {
        if (executablePath != null) {
            ensureExecutableExists(executablePath);
            return executablePath;
        }
        return BrowserFetcher.resolveExecutablePath();
    }

    /**
     * Resolves the Chrome executable path from launch options.
     *
     * @param options launch options
     * @return executable path
     */
    public Path resolveExecutablePath(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        if (actualOptions.getExecutablePath() != null) {
            return resolveExecutablePath(actualOptions.getExecutablePath());
        }
        if (!actualOptions.isHeadlessShell()) {
            return resolveExecutablePath((Path) null);
        }
        FetcherOptions fetcherOptions = new FetcherOptions();
        fetcherOptions.setBrowser(ExecutableResolver.CHROME_HEADLESS_SHELL);
        fetcherOptions.setPreferSystemExecutable(false);
        try {
            return BrowserFetcher.resolveExecutablePath(fetcherOptions);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(BrowserFetcher.missingBrowserMessage(
                    fetcherOptions.getBrowser(),
                    fetcherOptions.getBuildId(),
                    fetcherOptions.getCacheDir()), ex);
        }
    }

    /**
     * Resolves the Chrome executable path.
     *
     * @param options launch options
     * @return executable path
     */
    @Override
    public Path executable(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        return resolveExecutablePath(actualOptions);
    }

    /**
     * Resolves Chrome command line arguments.
     *
     * @param options launch options
     * @return command line arguments
     */
    @Override
    public List<String> args(LaunchOptions options) {
        return defaultArgs(options == null ? new LaunchOptions() : options);
    }

    /**
     * Extracts features from a Chrome feature flag.
     *
     * @param flag command line flag
     * @param args command line arguments
     * @return feature names
     */
    public static List<String> getFeatures(String flag, List<String> args) {
        String prefix = flag.endsWith(Symbol.EQUAL) ? flag : flag + Symbol.EQUAL;
        List<String> features = new ArrayList<>();
        for (String arg : args == null ? List.<String>of() : args) {
            if (!arg.startsWith(prefix)) {
                continue;
            }
            String[] parts = arg.substring(arg.indexOf(Symbol.C_EQUAL) + 1).trim().split(Symbol.COMMA);
            for (String part : parts) {
                if (StringKit.isNotBlank(part)) {
                    features.add(part.trim());
                }
            }
        }
        return features;
    }

    /**
     * Removes arguments that match a Chrome feature flag.
     *
     * @param args command line arguments
     * @param flag command line flag
     * @return updated argument list
     */
    public static List<String> removeMatchingFlags(List<String> args, String flag) {
        String prefix = flag.endsWith(Symbol.EQUAL) ? flag : flag + Symbol.EQUAL;
        if (args != null) {
            args.removeIf(arg -> arg.startsWith(prefix));
        }
        return args;
    }

    /**
     * Creates a temporary Chrome profile directory.
     *
     * @return temporary profile directory
     */
    private Path createProfileDirectory() {
        try {
            Path profile = Files.createTempDirectory("lancia-profile-");
            FileKit.mkdir(profile.toFile());
            return profile;
        } catch (IOException | RuntimeException ex) {
            throw new InternalException("Failed to create Chrome temporary user data directory.", ex);
        }
    }

    /**
     * Finds the user data directory from computed Chrome arguments.
     *
     * @param args computed Chrome arguments
     * @return user data directory or {@code null}
     */
    private Path findUserDataDir(List<String> args) {
        for (String arg : args) {
            if (!arg.startsWith("--user-data-dir")) {
                continue;
            }
            int index = arg.indexOf(Symbol.C_EQUAL);
            if (index < 0 || index == arg.length() - 1) {
                throw new IllegalArgumentException("`--user-data-dir` is malformed");
            }
            return Path.of(arg.substring(index + 1));
        }
        return null;
    }

    /**
     * Returns whether experimental Chrome features are enabled.
     *
     * @return {@code true} when the condition matches
     */
    private boolean isExperimentalChromeFeaturesEnabled() {
        return "true".equalsIgnoreCase(System.getenv("PUPPETEER_TEST_EXPERIMENTAL_CHROME_FEATURES"));
    }

    /**
     * Returns whether the no-sandbox flag is enabled.
     *
     * @return {@code true} when the condition matches
     */
    private boolean isNoSandboxEnabled() {
        return "true".equals(
                System.getenv("PUPPETEER_DANGEROUS_NO_SANDBOX") == null ? null
                        : System.getenv("PUPPETEER_DANGEROUS_NO_SANDBOX").toLowerCase(Locale.ROOT));
    }

}
