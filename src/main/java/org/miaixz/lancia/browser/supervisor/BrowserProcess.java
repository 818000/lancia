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
package org.miaixz.lancia.browser.supervisor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.bundle.BrowserCache;
import org.miaixz.lancia.browser.bundle.ExecutableResolver;
import org.miaixz.lancia.browser.bundle.FetcherOptions;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.Browser;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ChromeReleaseChannel;

/**
 * Launches, tracks, and closes browser child processes.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BrowserProcess implements AutoCloseable {

    /**
     * Matches the CDP WebSocket endpoint emitted by Chromium.
     */
    public static final Pattern CDP_WEBSOCKET_ENDPOINT_REGEX = Pattern.compile("^DevTools listening on (ws://.*)$");

    /**
     * Matches the WebDriver BiDi WebSocket endpoint emitted by Firefox.
     */
    public static final Pattern WEBDRIVER_BIDI_WEBSOCKET_ENDPOINT_REGEX = Pattern
            .compile("^WebDriver BiDi listening on (ws://.*)$");
    /**
     * Shared constant for process error explanation.
     */
    private static final String PROCESS_ERROR_EXPLANATION = "Puppeteer was unable to kill the process which ran the browser binary.";
    /**
     * Shared constant for close timeout.
     */
    private static final Duration CLOSE_TIMEOUT = Duration.ofMillis(500L);
    /**
     * Default max log lines size.
     */
    private static final int DEFAULT_MAX_LOG_LINES_SIZE = 1000;
    /**
     * Timer used by process waiters.
     */
    private static final ScheduledExecutorService WAITER_TIMER = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-browser-process-waiter-", true));
    /**
     * Managed browser process.
     */
    private final Process process;

    /**
     * Temporary user data directory.
     */
    private final Path temporaryUserDataDir;
    /**
     * Recent browser output lines.
     */
    private final Deque<String> logs = new ArrayDeque<>();
    /**
     * Pending output line waiters.
     */
    private final List<LineWaiter> lineWaiters = new CopyOnWriteArrayList<>();
    /**
     * Completion signal for process exit.
     */
    private final CompletableFuture<Void> exiting = new CompletableFuture<>();
    /**
     * Hook invoked once after process exit.
     */
    private final Runnable onExitHook;
    /**
     * Thread-safe hooks ran state.
     */
    private final AtomicBoolean hooksRan = new AtomicBoolean();
    /**
     * Thread-safe exited state.
     */
    private final AtomicBoolean exited = new AtomicBoolean();
    /**
     * Maximum number of output lines retained for diagnostics.
     */
    private int maxLogLinesSize = DEFAULT_MAX_LOG_LINES_SIZE;

    /**
     * Creates a browser process.
     *
     * @param process              process
     * @param temporaryUserDataDir temporary user data dir
     */
    public BrowserProcess(Process process, Path temporaryUserDataDir) {
        this(process, temporaryUserDataDir, null, false, false);
    }

    /**
     * Creates a browser process.
     *
     * @param process              process
     * @param temporaryUserDataDir temporary user data dir
     * @param onExitHook           on exit hook
     * @param recordStreams        record streams
     * @param dumpio               dumpio
     */
    private BrowserProcess(Process process, Path temporaryUserDataDir, Runnable onExitHook, boolean recordStreams,
            boolean dumpio) {
        this.process = Assert.notNull(process, "process");
        this.temporaryUserDataDir = temporaryUserDataDir;
        this.onExitHook = onExitHook == null ? () -> {
        } : onExitHook;
        if (recordStreams) {
            recordStream(this.process.getInputStream(), dumpio ? System.out : null);
            recordStream(this.process.getErrorStream(), dumpio ? System.err : null);
        }
        this.process.onExit().whenComplete((ignored, error) -> {
            exited.set(true);
            completeWaitersOnExit();
            try {
                runHooks();
                exiting.complete(null);
            } catch (RuntimeException ex) {
                exiting.completeExceptionally(ex);
            }
        });
    }

    /**
     * Returns the launch.
     *
     * @param options operation options
     * @return launch value
     */
    public static BrowserProcess launch(ProcessLaunchOptions options) {
        ProcessLaunchOptions actualOptions = new ProcessLaunchOptions(Assert.notNull(options, "options"));
        if (actualOptions.isAborted()) {
            throw new InternalException(StringKit.isBlank(actualOptions.getAbortReason()) ? "Launch aborted"
                    : actualOptions.getAbortReason());
        }
        List<String> command = new ArrayList<>();
        command.add(Assert.notNull(actualOptions.getExecutablePath(), "executablePath").toString());
        command.addAll(actualOptions.getArgs());
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            if (!actualOptions.getEnv().isEmpty()) {
                builder.environment().clear();
                builder.environment().putAll(actualOptions.getEnv());
            }
            Logger.debug(
                    false,
                    "Launcher",
                    "Launching {} {}",
                    actualOptions.getExecutablePath(),
                    actualOptions.getArgs());
            Process process = builder.start();
            Logger.debug(false, "Launcher", "Launched {}", process.pid());
            return new BrowserProcess(process, actualOptions.getTemporaryUserDataDir(), actualOptions.getOnExit(), true,
                    actualOptions.isDumpio());
        } catch (IOException ex) {
            throw new InternalException("Failed to start browser process: " + command, ex);
        }
    }

    /**
     * Returns the compute executable path.
     *
     * @param options operation options
     * @return compute executable path value
     */
    public static Path computeExecutablePath(ComputeExecutablePathOptions options) {
        ComputeExecutablePathOptions actualOptions = new ComputeExecutablePathOptions(
                Assert.notNull(options, "options"));
        if (actualOptions.getPlatform() == null) {
            actualOptions.setPlatform(
                    BrowserPlatform.detectBrowserPlatform().orElseThrow(
                            () -> new InternalException(
                                    "No platform specified. Couldn't auto-detect browser platform.")));
        }
        if (actualOptions.getCacheDir() == null) {
            return BrowserData.relativeExecutablePath(
                    Browser.fromValue(actualOptions.getBrowser()),
                    actualOptions.getPlatform(),
                    actualOptions.getBuildId());
        }
        FetcherOptions fetcherOptions = new FetcherOptions();
        fetcherOptions.setCacheDir(actualOptions.getCacheDir());
        fetcherOptions.setBrowser(actualOptions.getBrowser());
        fetcherOptions.setBuildId(actualOptions.getBuildId());
        fetcherOptions.setPlatform(actualOptions.getPlatform());
        return new BrowserCache(actualOptions.getCacheDir()).computeExecutablePath(fetcherOptions);
    }

    /**
     * Returns the compute system executable path.
     *
     * @param options operation options
     * @return compute system executable path value
     */
    public static Path computeSystemExecutablePath(SystemOptions options) {
        SystemOptions actualOptions = new SystemOptions(Assert.notNull(options, "options"));
        if (actualOptions.getPlatform() == null) {
            actualOptions.setPlatform(
                    BrowserPlatform.detectBrowserPlatform().orElseThrow(
                            () -> new InternalException("Could not detect the current browser platform.")));
        }
        List<Path> paths = BrowserData.resolveSystemExecutablePaths(
                Browser.fromValue(actualOptions.getBrowser()),
                actualOptions.getPlatform(),
                actualOptions.getChannel());
        for (Path path : paths) {
            if (Files.exists(path)) {
                return path;
            }
        }
        throw new InternalException(
                "Could not find " + actualOptions.getChannel().id() + " channel Chrome executable: " + paths);
    }

    /**
     * Returns the node process.
     *
     * @return node process value
     */
    public Process nodeProcess() {
        return process;
    }

    /**
     * Returns the process.
     *
     * @return process
     */
    public Process getProcess() {
        return process;
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        runHooks();
        if (!exited.get()) {
            kill();
        }
        try {
            exiting.get(CLOSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // Implementation note.
        }
        cleanupTemporaryDirectory(temporaryUserDataDir);
    }

    /**
     * Cleans up a temporary directory.
     *
     * @param directory directory
     */
    static void cleanupTemporaryDirectory(Path directory) {
        if (directory == null || !FileKit.exists(directory.toFile())) {
            return;
        }
        try {
            FileKit.remove(directory.toFile());
        } catch (Exception ignored) {
            // Keep cleanup best-effort so shutdown errors are not hidden.
        }
    }

    /**
     * Returns whether closed is available.
     *
     * @return {@code true} when the condition matches
     */
    public CompletableFuture<Void> hasClosed() {
        return exiting;
    }

    /**
     * Drains stdout so browser startup cannot block on an unread pipe.
     *
     * @param process process
     */
    static void drainStdout(Process process) {
        if (process == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.UTF_8))) {
                while (reader.readLine() != null) {
                    // Drain only.
                }
            } catch (Exception ignored) {
                // Stdout drain is best-effort and must not affect launch.
            }
        });
    }

    /**
     * Handles kill.
     */
    public void kill() {
        Logger.debug(false, "Launcher", "Trying to kill {}", process.pid());
        try {
            if (pidExists(process.pid())) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        } catch (RuntimeException ex) {
            throw new InternalException(PROCESS_ERROR_EXPLANATION + Symbol.LF + "Error cause: " + ex, ex);
        } finally {
            completeWaitersOnExit();
        }
    }

    /**
     * Returns the recent logs.
     *
     * @return values
     */
    public List<String> getRecentLogs() {
        synchronized (logs) {
            return List.copyOf(logs);
        }
    }

    /**
     * Waits for line output.
     *
     * @param regex         regex
     * @param timeoutMillis timeout in milliseconds
     * @return wait for line output value
     */
    public CompletableFuture<String> waitForLineOutput(Pattern regex, long timeoutMillis) {
        Assert.notNull(regex, "regex");
        CompletableFuture<String> future = new CompletableFuture<>();
        LineWaiter waiter = new LineWaiter(regex, future);
        lineWaiters.add(waiter);
        for (String line : getRecentLogs()) {
            if (acceptLine(waiter, line)) {
                return future;
            }
        }
        if (timeoutMillis > Normal._0) {
            WAITER_TIMER.schedule(() -> {
                if (future.completeExceptionally(
                        new TimeoutException("Timed out after " + timeoutMillis
                                + " ms while waiting for the WS endpoint URL to appear in stdout!"))) {
                    lineWaiters.remove(waiter);
                }
            }, timeoutMillis, TimeUnit.MILLISECONDS);
        }
        future.whenComplete((value, error) -> lineWaiters.remove(waiter));
        return future;
    }

    /**
     * Updates max log lines size.
     *
     * @param maxLogLinesSize max log lines size value
     */
    public void setMaxLogLinesSize(int maxLogLinesSize) {
        this.maxLogLinesSize = Math.max(Normal._1, maxLogLinesSize);
        trimLogs();
    }

    /**
     * Returns the pid exists.
     *
     * @param pid pid value
     * @return {@code true} when the condition matches
     */
    public static boolean pidExists(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    /**
     * Returns whether the object looks like an error.
     *
     * @param value to use
     * @return {@code true} when the condition matches
     */
    public static boolean isErrorLike(Object value) {
        return value instanceof Throwable;
    }

    /**
     * Returns whether the object carries an errno-style exception.
     *
     * @param value to use
     * @return {@code true} when the condition matches
     */
    public static boolean isErrnoException(Object value) {
        return value instanceof IOException || value instanceof InternalException;
    }

    /**
     * Handles record stream.
     *
     * @param stream stream value
     * @param dumpTo dump to value
     */
    private void recordStream(InputStream stream, OutputStream dumpTo) {
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (StringKit.isBlank(line)) {
                        continue;
                    }
                    recordLine(line);
                    if (dumpTo != null) {
                        dumpTo.write(ByteKit.toBytes(line + System.lineSeparator(), Charset.UTF_8));
                        dumpTo.flush();
                    }
                }
            } catch (IOException ignored) {
                // Implementation note.
            }
        });
    }

    /**
     * Handles record line.
     *
     * @param line line value
     */
    private void recordLine(String line) {
        synchronized (logs) {
            logs.addLast(line);
            trimLogs();
        }
        for (LineWaiter waiter : lineWaiters) {
            acceptLine(waiter, line);
        }
    }

    /**
     * Handles trim logs.
     */
    private void trimLogs() {
        synchronized (logs) {
            while (logs.size() > maxLogLinesSize) {
                logs.removeFirst();
            }
        }
    }

    /**
     * Returns the accept line.
     *
     * @param waiter waiter value
     * @param line   line value
     * @return {@code true} when the condition matches
     */
    private boolean acceptLine(LineWaiter waiter, String line) {
        Matcher matcher = waiter.regex.matcher(line);
        if (!matcher.matches()) {
            return false;
        }
        String value = matcher.groupCount() >= Normal._1 ? matcher.group(Normal._1) : matcher.group();
        boolean completed = waiter.future.complete(value);
        if (completed) {
            lineWaiters.remove(waiter);
        }
        return completed;
    }

    /**
     * Handles complete waiters on exit.
     */
    private void completeWaitersOnExit() {
        InternalException error = new InternalException("Failed to launch the browser process:" + Symbol.LF + Symbol.LF
                + "stderr:" + Symbol.LF + String.join(Symbol.LF, getRecentLogs()) + Symbol.LF + Symbol.LF
                + "TROUBLESHOOTING: https://pptr.dev/troubleshooting");
        for (LineWaiter waiter : lineWaiters) {
            waiter.future.completeExceptionally(error);
        }
        lineWaiters.clear();
    }

    /**
     * Handles run hooks.
     */
    private void runHooks() {
        if (hooksRan.compareAndSet(false, true)) {
            onExitHook.run();
        }
    }

    /**
     * Waits for line completion.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class LineWaiter {

        /**
         * Output line pattern to wait for.
         */
        private final Pattern regex;
        /**
         * Future completed with the first matching line.
         */
        private final CompletableFuture<String> future;

        /**
         * Creates an instance.
         *
         * @param regex  regex value
         * @param future future value
         */
        private LineWaiter(Pattern regex, CompletableFuture<String> future) {
            this.regex = regex;
            this.future = future;
        }
    }

    /**
     * Defines options for compute executable path operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class ComputeExecutablePathOptions {

        /**
         * Current cache dir.
         */
        private Path cacheDir;
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
         * Creates an instance.
         */
        public ComputeExecutablePathOptions() {
            // No initialization required.
        }

        /**
         * Creates an instance.
         *
         * @param source source value
         */
        public ComputeExecutablePathOptions(ComputeExecutablePathOptions source) {
            ComputeExecutablePathOptions actualSource = Assert.notNull(source, "source");
            this.cacheDir = actualSource.cacheDir;
            this.platform = actualSource.platform;
            this.browser = actualSource.browser;
            this.buildId = actualSource.buildId;
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
    }

    /**
     * Defines options for system operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class SystemOptions {

        /**
         * Current platform.
         */
        private BrowserPlatform platform;
        /**
         * Current browser.
         */
        private String browser = ExecutableResolver.CHROME;
        /**
         * Current channel.
         */
        private ChromeReleaseChannel channel = ChromeReleaseChannel.STABLE;

        /**
         * Creates an instance.
         */
        public SystemOptions() {
            // No initialization required.
        }

        /**
         * Creates an instance.
         *
         * @param source source value
         */
        public SystemOptions(SystemOptions source) {
            SystemOptions actualSource = Assert.notNull(source, "source");
            this.platform = actualSource.platform;
            this.browser = actualSource.browser;
            this.channel = actualSource.channel;
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
         * Returns the channel.
         *
         * @return channel
         */
        public ChromeReleaseChannel getChannel() {
            return channel;
        }

        /**
         * Updates channel.
         *
         * @param channel channel value
         */
        public void setChannel(ChromeReleaseChannel channel) {
            this.channel = channel;
        }
    }

    /**
     * Defines options for process launch operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class ProcessLaunchOptions {

        /**
         * Current executable path.
         */
        private Path executablePath;
        /**
         * Whether pipe is enabled.
         */
        private boolean pipe;
        /**
         * Whether dumpio is enabled.
         */
        private boolean dumpio;
        /**
         * Registered args values.
         */
        private final List<String> args = new ArrayList<>();
        /**
         * Mapped env values.
         */
        private final Map<String, String> env = new LinkedHashMap<>();
        /**
         * Whether handle sigint is enabled.
         */
        private boolean handleSIGINT = true;
        /**
         * Whether handle sigterm is enabled.
         */
        private boolean handleSIGTERM = true;
        /**
         * Whether handle sighup is enabled.
         */
        private boolean handleSIGHUP = true;
        /**
         * Whether detached is enabled.
         */
        private Boolean detached;
        /**
         * Hook invoked when the launched process exits.
         */
        private Runnable onExit;
        /**
         * Whether launch has already been aborted.
         */
        private boolean aborted;
        /**
         * Current abort reason.
         */
        private String abortReason;

        /**
         * Temporary user data directory.
         */
        private Path temporaryUserDataDir;

        /**
         * Creates an instance.
         */
        public ProcessLaunchOptions() {
            // No initialization required.
        }

        /**
         * Creates an instance.
         *
         * @param source source value
         */
        public ProcessLaunchOptions(ProcessLaunchOptions source) {
            ProcessLaunchOptions actualSource = Assert.notNull(source, "source");
            this.executablePath = actualSource.executablePath;
            this.pipe = actualSource.pipe;
            this.dumpio = actualSource.dumpio;
            this.args.addAll(actualSource.args);
            this.env.putAll(actualSource.env);
            this.handleSIGINT = actualSource.handleSIGINT;
            this.handleSIGTERM = actualSource.handleSIGTERM;
            this.handleSIGHUP = actualSource.handleSIGHUP;
            this.detached = actualSource.detached;
            this.onExit = actualSource.onExit;
            this.aborted = actualSource.aborted;
            this.abortReason = actualSource.abortReason;
            this.temporaryUserDataDir = actualSource.temporaryUserDataDir;
        }

        /**
         * Returns the executable path.
         *
         * @return executable path
         */
        public Path getExecutablePath() {
            return executablePath;
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
         * Returns whether pipe is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isPipe() {
            return pipe;
        }

        /**
         * Updates pipe.
         *
         * @param pipe pipe value
         */
        public void setPipe(boolean pipe) {
            this.pipe = pipe;
        }

        /**
         * Returns whether dumpio is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isDumpio() {
            return dumpio;
        }

        /**
         * Updates dumpio.
         *
         * @param dumpio dumpio value
         */
        public void setDumpio(boolean dumpio) {
            this.dumpio = dumpio;
        }

        /**
         * Returns the args.
         *
         * @return values
         */
        public List<String> getArgs() {
            return Collections.unmodifiableList(args);
        }

        /**
         * Updates args.
         *
         * @param args arguments to pass
         */
        public void setArgs(List<String> args) {
            this.args.clear();
            if (args != null) {
                this.args.addAll(args);
            }
        }

        /**
         * Adds arg.
         *
         * @param arg arg
         */
        public void addArg(String arg) {
            this.args.add(arg);
        }

        /**
         * Returns the env.
         *
         * @return mapped values
         */
        public Map<String, String> getEnv() {
            return Map.copyOf(env);
        }

        /**
         * Updates env.
         *
         * @param env env value
         */
        public void setEnv(Map<String, String> env) {
            this.env.clear();
            if (env != null) {
                this.env.putAll(env);
            }
        }

        /**
         * Handles put env.
         *
         * @param key   key value
         * @param value to use
         */
        public void putEnv(String key, String value) {
            if (StringKit.isNotBlank(key) && value != null) {
                this.env.put(key, value);
            }
        }

        /**
         * Returns whether handle sigint is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isHandleSIGINT() {
            return handleSIGINT;
        }

        /**
         * Updates handle sigint.
         *
         * @param handleSIGINT handle sigint value
         */
        public void setHandleSIGINT(boolean handleSIGINT) {
            this.handleSIGINT = handleSIGINT;
        }

        /**
         * Returns whether handle sigterm is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isHandleSIGTERM() {
            return handleSIGTERM;
        }

        /**
         * Updates handle sigterm.
         *
         * @param handleSIGTERM handle sigterm value
         */
        public void setHandleSIGTERM(boolean handleSIGTERM) {
            this.handleSIGTERM = handleSIGTERM;
        }

        /**
         * Returns whether handle sighup is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isHandleSIGHUP() {
            return handleSIGHUP;
        }

        /**
         * Updates handle sighup.
         *
         * @param handleSIGHUP handle sighup value
         */
        public void setHandleSIGHUP(boolean handleSIGHUP) {
            this.handleSIGHUP = handleSIGHUP;
        }

        /**
         * Returns the detached.
         *
         * @return {@code true} when the condition matches
         */
        public Boolean getDetached() {
            return detached;
        }

        /**
         * Updates detached.
         *
         * @param detached detached value
         */
        public void setDetached(Boolean detached) {
            this.detached = detached;
        }

        /**
         * Returns the on exit.
         *
         * @return on exit
         */
        public Runnable getOnExit() {
            return onExit;
        }

        /**
         * Updates on exit.
         *
         * @param onExit on exit value
         */
        public void setOnExit(Runnable onExit) {
            this.onExit = onExit;
        }

        /**
         * Returns whether aborted is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isAborted() {
            return aborted;
        }

        /**
         * Updates aborted.
         *
         * @param aborted aborted value
         */
        public void setAborted(boolean aborted) {
            this.aborted = aborted;
        }

        /**
         * Returns the abort reason.
         *
         * @return abort reason
         */
        public String getAbortReason() {
            return abortReason;
        }

        /**
         * Updates abort reason.
         *
         * @param abortReason abort reason value
         */
        public void setAbortReason(String abortReason) {
            this.abortReason = abortReason;
        }

        /**
         * Returns the temporary user data dir.
         *
         * @return temporary user data dir
         */
        public Path getTemporaryUserDataDir() {
            return temporaryUserDataDir;
        }

        /**
         * Updates temporary user data dir.
         *
         * @param temporaryUserDataDir temporary user data dir value
         */
        public void setTemporaryUserDataDir(Path temporaryUserDataDir) {
            this.temporaryUserDataDir = temporaryUserDataDir;
        }
    }

}
