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
package org.miaixz.lancia.options;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import lombok.Getter;
import lombok.Setter;

import org.miaixz.lancia.nimble.browser.BrowserVariant;

/**
 * Generic browser launch options.
 */
@Getter
@Setter
/**
 * Defines options for launch operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class LaunchOptions extends ConnectOptions {

    /**
     * Chrome release channel used to resolve a system Chrome executable.
     */
    private String channel;

    /**
     * Browser executable path.
     */
    private Path executablePath;

    /**
     * Whether all default arguments should be ignored.
     */
    private boolean ignoreDefaultArgs;

    /**
     * Default arguments that should be filtered out.
     */
    private List<String> ignoredDefaultArgs = List.of();

    /**
     * Whether browser extensions should be enabled.
     */
    private boolean enableExtensions;

    /**
     * Unpacked extension paths that should be enabled.
     */
    private List<Path> extensionPaths = List.of();

    /**
     * Whether the browser process should be closed on SIGINT.
     */
    private boolean handleSIGINT = true;

    /**
     * Whether the browser process should be closed on SIGTERM.
     */
    private boolean handleSIGTERM = true;

    /**
     * Whether the browser process should be closed on SIGHUP.
     */
    private boolean handleSIGHUP = true;

    /**
     * Browser startup timeout in milliseconds.
     */
    private long timeoutMillis = 30_000L;

    /**
     * Whether browser stdout and stderr should be piped to the current process.
     */
    private boolean dumpio;

    /**
     * Environment variables visible to the browser process.
     */
    private Map<String, String> env = new LinkedHashMap<>();

    /**
     * Whether Chromium should use remote-debugging-pipe instead of a WebSocket endpoint.
     */
    private boolean pipe;

    /**
     * Browser family to launch.
     */
    private BrowserVariant browser;

    /**
     * Whether the launcher should wait for the initial page target.
     */
    private boolean waitForInitialPage = true;

    /**
     * Browser headless launch mode.
     */
    private HeadlessMode headlessMode = HeadlessMode.TRUE;

    /**
     * User data directory.
     */
    private Path userDataDir;

    /**
     * Whether DevTools should be opened for each tab.
     */
    private boolean devtools;

    /**
     * DevTools TCP debugging port.
     */
    private Integer debuggingPort;

    /**
     * Additional browser command line arguments.
     */
    private List<String> args = List.of();

    /**
     * Whether the user data directory was created by Lancia.
     */
    private boolean temporaryUserDataDir;

    /**
     * Launch cancellation signal. A completed or cancelled future aborts launch.
     */
    private CompletableFuture<?> signal;

    /**
     * Creates launch options with Puppeteer-compatible defaults.
     */
    public LaunchOptions() {
        // No initialization required.
    }

    /**
     * Updates executable path.
     *
     * @param executablePath executable path value
     */
    public void setExecutablePath(Path executablePath) {
        this.executablePath = executablePath;
    }

    /**
     * Updates executable path.
     *
     * @param executablePath executable path text
     */
    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath == null ? null : Path.of(executablePath);
    }

    /**
     * Updates user data dir.
     *
     * @param userDataDir user data directory
     */
    public void setUserDataDir(Path userDataDir) {
        this.userDataDir = userDataDir;
    }

    /**
     * Updates user data dir.
     *
     * @param userDataDir user data directory text
     */
    public void setUserDataDir(String userDataDir) {
        this.userDataDir = userDataDir == null ? null : Path.of(userDataDir);
    }

    /**
     * Updates devtools.
     *
     * @param devtools whether DevTools should open
     */
    public void setDevtools(boolean devtools) {
        this.devtools = devtools;
        if (devtools) {
            this.headlessMode = HeadlessMode.FALSE;
        }
    }

    /**
     * Returns whether headless mode is enabled.
     *
     * @return whether headless mode is enabled
     */
    public boolean isHeadless() {
        return headlessMode != HeadlessMode.FALSE;
    }

    /**
     * Updates headless.
     *
     * @param headless whether the browser should run headlessly
     */
    public void setHeadless(boolean headless) {
        this.headlessMode = headless ? HeadlessMode.TRUE : HeadlessMode.FALSE;
    }

    /**
     * Updates headless mode.
     *
     * @param headlessMode headless mode value
     */
    public void setHeadlessMode(HeadlessMode headlessMode) {
        this.headlessMode = headlessMode == null ? HeadlessMode.TRUE : headlessMode;
    }

    /**
     * Updates headless.
     *
     * @param headless headless value
     */
    public void setHeadless(String headless) {
        if (headless == null) {
            this.headlessMode = HeadlessMode.TRUE;
            return;
        }
        if ("shell".equalsIgnoreCase(headless)) {
            this.headlessMode = HeadlessMode.SHELL;
            return;
        }
        this.headlessMode = Boolean.parseBoolean(headless) ? HeadlessMode.TRUE : HeadlessMode.FALSE;
    }

    /**
     * Enables or disables chrome-headless-shell mode.
     *
     * @param shell whether shell mode should be used
     */
    public void setHeadlessShell(boolean shell) {
        this.headlessMode = shell ? HeadlessMode.SHELL : HeadlessMode.TRUE;
    }

    /**
     * Returns whether headless shell mode is enabled.
     *
     * @return whether shell mode is enabled
     */
    public boolean isHeadlessShell() {
        return headlessMode == HeadlessMode.SHELL;
    }

    /**
     * Returns whether launch cancellation was requested.
     *
     * @return whether the launch signal is done or cancelled
     */
    public boolean isLaunchCancelled() {
        return signal != null && signal.isDone();
    }

    /**
     * Throws when launch has been cancelled.
     */
    public void throwIfLaunchCancelled() {
        if (isLaunchCancelled()) {
            throw new CancellationException("Browser launch has been cancelled.");
        }
    }

    /**
     * Returns default arguments that should be filtered out.
     *
     * @return immutable ignored default argument list
     */
    public List<String> getIgnoredDefaultArgs() {
        return ignoredDefaultArgs;
    }

    /**
     * Updates ignored default args.
     *
     * @param ignoredDefaultArgs ignored default arguments
     */
    public void setIgnoredDefaultArgs(List<String> ignoredDefaultArgs) {
        this.ignoredDefaultArgs = ignoredDefaultArgs == null ? List.of() : List.copyOf(ignoredDefaultArgs);
    }

    /**
     * Returns extension paths.
     *
     * @return immutable extension path list
     */
    public List<Path> getExtensionPaths() {
        return extensionPaths;
    }

    /**
     * Updates extension paths.
     *
     * @param extensionPaths extension paths value
     */
    public void setExtensionPaths(List<Path> extensionPaths) {
        this.extensionPaths = extensionPaths == null ? List.of() : List.copyOf(extensionPaths);
    }

    /**
     * Returns browser environment variables.
     *
     * @return immutable environment map
     */
    public Map<String, String> getEnv() {
        return Map.copyOf(env);
    }

    /**
     * Updates env.
     *
     * @param env environment variables
     */
    public void setEnv(Map<String, String> env) {
        this.env = env == null ? new LinkedHashMap<>() : new LinkedHashMap<>(env);
    }

    /**
     * Adds a browser environment variable.
     *
     * @param key   environment key
     * @param value environment value
     */
    public void putEnv(String key, String value) {
        if (key != null && value != null) {
            env.put(key, value);
        }
    }

    /**
     * Returns browser command line arguments.
     *
     * @return immutable browser argument list
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Updates args.
     *
     * @param args browser command line arguments
     */
    public void setArgs(List<String> args) {
        this.args = args == null ? List.of() : List.copyOf(args);
    }

    /**
     * Adds one browser command line argument.
     *
     * @param arg browser command line argument
     */
    public void addArg(String arg) {
        if (arg != null) {
            List<String> updated = new ArrayList<>(args);
            updated.add(arg);
            args = List.copyOf(updated);
        }
    }

}
