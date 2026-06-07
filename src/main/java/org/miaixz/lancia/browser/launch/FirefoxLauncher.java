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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.bundle.BrowserFetcher;
import org.miaixz.lancia.browser.supervisor.FirefoxRunner;
import org.miaixz.lancia.browser.supervisor.Runner;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowser;
import org.miaixz.lancia.nimble.browser.BrowserVariant;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.options.FirefoxOptions;
import org.miaixz.lancia.options.LaunchOptions;

/**
 * Firefox launcher.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class FirefoxLauncher extends BrowserLauncher {

    /**
     * Preference backup suffix used for custom Firefox profiles.
     */
    private static final String BACKUP_SUFFIX = ".puppeteer";

    /**
     * Preference files that must be restored for custom profiles.
     */
    private static final List<String> BACKUP_FILES = List.of("prefs.js", "user.js");

    /**
     * Firefox BiDi runner.
     */
    @Getter
    private final FirefoxRunner firefoxRunner;

    /**
     * Creates a Firefox launcher with default runners.
     */
    public FirefoxLauncher() {
        this(new Runner(), new FirefoxRunner());
    }

    /**
     * Creates a Firefox launcher with a custom BiDi runner.
     *
     * @param firefoxRunner Firefox BiDi runner
     */
    public FirefoxLauncher(FirefoxRunner firefoxRunner) {
        this(new Runner(), firefoxRunner);
    }

    /**
     * Creates a Firefox launcher with custom runners.
     *
     * @param runner        shared browser process runner
     * @param firefoxRunner Firefox BiDi runner
     */
    public FirefoxLauncher(Runner runner, FirefoxRunner firefoxRunner) {
        super(BrowserVariant.FIREFOX, runner);
        this.firefoxRunner = firefoxRunner == null ? new FirefoxRunner() : firefoxRunner;
    }

    /**
     * Launches Firefox.
     *
     * @param options launch options
     * @return browser instance
     */
    @Override
    public Browser launch(LaunchOptions options) {
        FirefoxOptions actualOptions = toFirefoxOptions(options);
        Logger.debug(
                true,
                "Launcher",
                "Firefox launch requested: protocol={}, pipe={}",
                actualOptions.getProtocol(),
                actualOptions.isPipe());
        if (actualOptions.isProtocolConfigured() && ConnectOptions.PROTOCOL_CDP.equals(actualOptions.getProtocol())) {
            throw new IllegalArgumentException("Firefox launch with cdp protocol is disabled by configuration.");
        }
        if (!actualOptions.isProtocolConfigured()) {
            actualOptions.setProtocol(ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI);
        }
        try {
            Browser result = adapt(launchBidi(actualOptions));
            Logger.debug(false, "Launcher", "Firefox launch completed: protocol={}", actualOptions.getProtocol());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "Firefox launch failed: protocol={}", actualOptions.getProtocol());
            throw ex;
        }
    }

    /**
     * Launches Firefox and returns a BiDi browser.
     *
     * @param options launch options
     * @return BiDi browser
     */
    public BidiBrowser launchBidi(LaunchOptions options) {
        return launchBidi(toFirefoxOptions(options));
    }

    /**
     * Launches Firefox and returns a BiDi browser.
     *
     * @param options Firefox launch options
     * @return BiDi browser
     */
    public BidiBrowser launchBidi(FirefoxOptions options) {
        FirefoxOptions actualOptions = options == null ? new FirefoxOptions() : options;
        Logger.debug(true, "Launcher", "Firefox BiDi launch requested: pipe={}", actualOptions.isPipe());
        if (actualOptions.isPipe()) {
            throw new IllegalArgumentException("Firefox BiDi launch requires WebSocket transport configuration.");
        }
        if (actualOptions.isProtocolConfigured()
                && !ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI.equals(actualOptions.getProtocol())) {
            throw new IllegalArgumentException("Firefox BiDi launch requires webDriverBiDi protocol configuration.");
        }
        if (!actualOptions.isProtocolConfigured()) {
            actualOptions.setProtocol(ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI);
        }
        try {
            ResolvedLaunchArgs launchArgs = computeLaunchArguments(actualOptions);
            ensureExecutableExists(launchArgs.getExecutablePath());
            BidiBrowser result = firefoxRunner
                    .launch(launchArgs.getExecutablePath(), launchArgs.getArgs(), actualOptions);
            Logger.debug(
                    false,
                    "Launcher",
                    "Firefox BiDi launch completed: executable={}, args={}",
                    launchArgs.getExecutablePath(),
                    launchArgs.getArgs().size());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "Firefox BiDi launch failed.");
            throw ex;
        }
    }

    /**
     * Connects to an existing Firefox instance.
     *
     * @param options connect options
     * @return browser instance
     */
    @Override
    public Browser connect(ConnectOptions options) {
        ConnectOptions actualOptions = options == null ? new ConnectOptions() : options;
        Logger.debug(true, "Launcher", "Firefox connect requested: protocol={}", actualOptions.getProtocol());
        if (!ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI.equals(actualOptions.getProtocol())) {
            throw new IllegalArgumentException("Firefox connect requires the BiDi protocol route.");
        }
        try {
            Browser result = super.connect(actualOptions);
            Logger.debug(false, "Launcher", "Firefox connect completed.");
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Launcher", ex, "Firefox connect failed.");
            throw ex;
        }
    }

    /**
     * Returns Firefox preferences with Puppeteer-required defaults.
     *
     * @param extraPreferences extra Firefox preferences
     * @return merged Firefox preferences
     */
    public static Map<String, Object> getPreferences(Map<String, Object> extraPreferences) {
        Map<String, Object> preferences = new LinkedHashMap<>();
        if (extraPreferences != null) {
            preferences.putAll(extraPreferences);
        }
        preferences.put("fission.webContentIsolationStrategy", 0);
        return Map.copyOf(preferences);
    }

    /**
     * Computes final Firefox launch arguments.
     *
     * @param options launch options
     * @return resolved launch arguments
     */
    public ResolvedLaunchArgs computeLaunchArguments(LaunchOptions options) {
        FirefoxOptions actualOptions = toFirefoxOptions(options);
        List<String> firefoxArguments = new ArrayList<>(defaultArgs(actualOptions));
        if (firefoxArguments.stream().noneMatch(argument -> argument.startsWith("--remote-debugging-"))) {
            if (actualOptions.isPipe() && actualOptions.getDebuggingPort() != null) {
                throw new IllegalArgumentException(
                        "Browser should be launched with either pipe or debugging port - not both.");
            }
            int port = actualOptions.getDebuggingPort() == null ? 0 : actualOptions.getDebuggingPort();
            firefoxArguments.add("--remote-debugging-port=" + port);
            actualOptions.setPipe(false);
        }

        boolean temporaryUserDataDir = true;
        Path userDataDir = findProfile(firefoxArguments);
        if (userDataDir == null) {
            userDataDir = createProfileDirectory();
            firefoxArguments.add("--profile");
            firefoxArguments.add(userDataDir.toString());
        } else {
            temporaryUserDataDir = false;
        }
        actualOptions.setUserDataDir(userDataDir);
        actualOptions.setTemporaryUserDataDir(temporaryUserDataDir);
        createProfile(userDataDir, getPreferences(actualOptions.getPreferences()), temporaryUserDataDir);

        Path executable = resolveExecutablePath(actualOptions.getExecutablePath());
        ResolvedLaunchArgs resolved = new ResolvedLaunchArgs();
        resolved.setTempUserDataDir(temporaryUserDataDir);
        resolved.setUserDataDir(userDataDir);
        resolved.setArgs(firefoxArguments);
        resolved.setExecutablePath(executable);
        Logger.debug(
                false,
                "Launcher",
                "Firefox launch arguments resolved: executable={}, args={}, userDataDir={}, tempProfile={}",
                executable,
                firefoxArguments.size(),
                userDataDir,
                temporaryUserDataDir);
        return resolved;
    }

    /**
     * Cleans a Firefox user data directory after browser shutdown.
     *
     * @param userDataDir user data directory
     * @param isTemp      whether the directory is temporary
     */
    protected void cleanUserDataDir(Path userDataDir, boolean isTemp) {
        if (userDataDir == null) {
            return;
        }
        if (isTemp) {
            try {
                if (FileKit.exists(userDataDir.toFile())) {
                    FileKit.remove(userDataDir.toFile());
                    Logger.debug(false, "Launcher", "Firefox temporary user data directory removed: {}", userDataDir);
                }
            } catch (Exception ex) {
                Logger.warn(
                        false,
                        "Launcher",
                        ex,
                        "Failed to remove Firefox temporary user data directory: {}",
                        userDataDir);
                throw new InternalException("Failed to remove temporary Firefox user data directory: " + userDataDir,
                        ex);
            }
            return;
        }
        restoreProfileBackups(userDataDir);
    }

    /**
     * Generates Firefox default launch arguments.
     *
     * @param options launch options
     * @return Firefox default launch arguments
     */
    public List<String> defaultArgs(LaunchOptions options) {
        FirefoxOptions firefoxOptions = toFirefoxOptions(options);
        List<String> args = new ArrayList<>();
        if (BrowserPlatform.isCurrentMac()) {
            args.add("--foreground");
        } else if (BrowserPlatform.isCurrentWindows()) {
            args.add("--wait-for-browser");
        }
        if (firefoxOptions.getUserDataDir() != null) {
            args.add("--profile");
            args.add(firefoxOptions.getUserDataDir().toString());
        }
        if (firefoxOptions.isHeadless()) {
            args.add("--headless");
        }
        if (firefoxOptions.isDevtools()) {
            args.add("--devtools");
        }
        if (firefoxOptions.getArgs().stream().allMatch(arg -> arg.startsWith(Symbol.MINUS))) {
            args.add(Builder.ABOUT_BLANK);
        }
        args.addAll(firefoxOptions.getArgs());
        return args;
    }

    /**
     * Resolves the Firefox executable path.
     *
     * @param executablePath explicit executable path
     * @return Firefox executable path
     */
    public Path resolveExecutablePath(Path executablePath) {
        if (executablePath != null) {
            ensureExecutableExists(executablePath);
            return executablePath;
        }
        return defaultFirefoxCandidates().stream().filter(path -> FileKit.isFile(path.toFile())).findFirst()
                .orElseThrow(
                        () -> new InternalException(
                                BrowserFetcher.missingBrowserMessage("firefox", "configured", null)));
    }

    /**
     * Resolves the Firefox executable path.
     *
     * @param options launch options
     * @return executable path
     */
    @Override
    public Path executable(LaunchOptions options) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        return resolveExecutablePath(actualOptions.getExecutablePath());
    }

    /**
     * Resolves Firefox command line arguments.
     *
     * @param options launch options
     * @return command line arguments
     */
    @Override
    public List<String> args(LaunchOptions options) {
        return defaultArgs(options == null ? new LaunchOptions() : options);
    }

    /**
     * Converts generic launch options into Firefox options.
     *
     * @param options launch options
     * @return Firefox launch options
     */
    private FirefoxOptions toFirefoxOptions(LaunchOptions options) {
        if (options instanceof FirefoxOptions firefoxOptions) {
            return firefoxOptions;
        }
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        if (options != null) {
            firefoxOptions.setBrowser(options.getBrowser());
            firefoxOptions.setChannel(options.getChannel());
            firefoxOptions.setExecutablePath(options.getExecutablePath());
            firefoxOptions.setHeadless(options.isHeadless());
            firefoxOptions.setArgs(options.getArgs());
            firefoxOptions.setProtocolTimeoutMillis(options.getProtocolTimeoutMillis());
            firefoxOptions.setPipe(options.isPipe());
            firefoxOptions.setDebuggingPort(options.getDebuggingPort());
            firefoxOptions.setUserDataDir(options.getUserDataDir());
            firefoxOptions.setTemporaryUserDataDir(options.isTemporaryUserDataDir());
            firefoxOptions.setDefaultViewport(options.getDefaultViewport());
            firefoxOptions.setNetworkEnabled(options.isNetworkEnabled());
            firefoxOptions.setAcceptInsecureCerts(options.isAcceptInsecureCerts());
            firefoxOptions.setSlowMoMillis(options.getSlowMoMillis());
            firefoxOptions.setIdGenerator(options.getIdGenerator());
            firefoxOptions.setCapabilities(options.getCapabilities());
            firefoxOptions.setDownloadBehavior(options.getDownloadBehavior());
            firefoxOptions.setHandleDevToolsAsPage(options.isHandleDevToolsAsPage());
            firefoxOptions.setIssuesEnabled(options.isIssuesEnabled());
            firefoxOptions.setTargetFilter(options.getTargetFilter());
            firefoxOptions.setIsPageTarget(options.getIsPageTarget());
            firefoxOptions.setWaitForInitialPage(options.isWaitForInitialPage());
            firefoxOptions.setDevtools(options.isDevtools());
            firefoxOptions.setTimeoutMillis(options.getTimeoutMillis());
            firefoxOptions.setDumpio(options.isDumpio());
            firefoxOptions.setEnv(options.getEnv());
            firefoxOptions.setIgnoreDefaultArgs(options.isIgnoreDefaultArgs());
            firefoxOptions.setIgnoredDefaultArgs(options.getIgnoredDefaultArgs());
            firefoxOptions.setEnableExtensions(options.isEnableExtensions());
            firefoxOptions.setExtensionPaths(options.getExtensionPaths());
            firefoxOptions.setHandleSIGINT(options.isHandleSIGINT());
            firefoxOptions.setHandleSIGTERM(options.isHandleSIGTERM());
            firefoxOptions.setHandleSIGHUP(options.isHandleSIGHUP());
            if (options.isProtocolConfigured()) {
                firefoxOptions.setProtocol(options.getProtocol());
            }
        }
        return firefoxOptions;
    }

    /**
     * Creates a Firefox profile directory.
     *
     * @return temporary profile directory
     */
    private Path createProfileDirectory() {
        try {
            Path profile = Files.createTempDirectory("lancia-firefox-profile-");
            FileKit.mkdir(profile.toFile());
            return profile;
        } catch (IOException | RuntimeException ex) {
            throw new InternalException("Failed to create Firefox temporary profile directory.", ex);
        }
    }

    /**
     * Finds the Firefox profile directory from arguments.
     *
     * @param args Firefox arguments
     * @return profile directory or {@code null}
     */
    private Path findProfile(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (!"-profile".equals(arg) && !"--profile".equals(arg)) {
                continue;
            }
            if (i + 1 >= args.size() || StringKit.isBlank(args.get(i + 1))) {
                throw new IllegalArgumentException("Missing value for profile command line argument");
            }
            return Path.of(args.get(i + 1));
        }
        return null;
    }

    /**
     * Creates or updates a Firefox profile with required preferences.
     *
     * @param profile              profile directory
     * @param preferences          Firefox preferences
     * @param temporaryUserDataDir whether the profile is temporary
     */
    private void createProfile(Path profile, Map<String, Object> preferences, boolean temporaryUserDataDir) {
        try {
            FileKit.mkdir(profile.toFile());
            if (!temporaryUserDataDir) {
                backupProfileFiles(profile);
            }
            Path userPrefs = profile.resolve("user.js");
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, Object> entry : preferences.entrySet()) {
                lines.add(
                        "user_pref(\"" + escapePreference(entry.getKey()) + "\", " + preferenceValue(entry.getValue())
                                + ");");
            }
            Files.write(userPrefs, lines, Charset.UTF_8);
        } catch (IOException | RuntimeException ex) {
            throw new InternalException("Failed to create Firefox profile: " + profile, ex);
        }
    }

    /**
     * Backs up preference files before writing Puppeteer preferences.
     *
     * @param profile profile directory
     * @throws IOException when backup fails
     */
    private void backupProfileFiles(Path profile) throws IOException {
        for (String file : BACKUP_FILES) {
            Path source = profile.resolve(file);
            if (Files.exists(source)) {
                Files.copy(source, profile.resolve(file + BACKUP_SUFFIX), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Restores preference files backed up for a custom profile.
     *
     * @param profile profile directory
     */
    private void restoreProfileBackups(Path profile) {
        try {
            for (String file : BACKUP_FILES) {
                Path backup = profile.resolve(file + BACKUP_SUFFIX);
                if (!Files.exists(backup)) {
                    continue;
                }
                Path target = profile.resolve(file);
                Files.deleteIfExists(target);
                Files.move(backup, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new InternalException("Failed to restore Firefox profile backups: " + profile, ex);
        }
    }

    /**
     * Serializes a Firefox preference value.
     *
     * @param value preference value
     * @return serialized preference value
     */
    private String preferenceValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return Symbol.DOUBLE_QUOTES + escapePreference(String.valueOf(value)) + Symbol.DOUBLE_QUOTES;
    }

    /**
     * Escapes a Firefox preference string.
     *
     * @param value raw value
     * @return escaped value
     */
    private String escapePreference(String value) {
        return value.replace(Symbol.BACKSLASH, "\\\\").replace(Symbol.DOUBLE_QUOTES, "\\\"");
    }

    /**
     * Returns default Firefox executable candidates.
     *
     * @return default Firefox executable candidates
     */
    private List<Path> defaultFirefoxCandidates() {
        if (BrowserPlatform.isCurrentMac()) {
            return List.of(
                    Path.of("/Applications/Firefox.app/Contents/MacOS/firefox"),
                    Path.of("/Applications/Firefox Nightly.app/Contents/MacOS/firefox"));
        }
        if (BrowserPlatform.isCurrentWindows()) {
            List<Path> candidates = new ArrayList<>();
            addWindowsCandidate(candidates, System.getenv("PROGRAMFILES"), "Mozilla Firefox", "firefox.exe");
            addWindowsCandidate(candidates, System.getenv("PROGRAMFILES(X86)"), "Mozilla Firefox", "firefox.exe");
            addWindowsCandidate(candidates, System.getenv("LOCALAPPDATA"), "Mozilla Firefox", "firefox.exe");
            return List.copyOf(candidates);
        }
        return List.of(Path.of("/usr/bin/firefox"), Path.of("/usr/local/bin/firefox"), Path.of("/snap/bin/firefox"));
    }

    /**
     * Adds a Windows Firefox executable candidate.
     *
     * @param candidates executable candidates
     * @param root       root directory
     * @param segments   path segments
     */
    private void addWindowsCandidate(List<Path> candidates, String root, String... segments) {
        if (StringKit.isBlank(root)) {
            return;
        }
        candidates.add(Path.of(root, segments));
    }

}
