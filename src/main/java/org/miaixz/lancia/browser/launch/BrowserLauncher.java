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

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.IoKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Launcher;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.browser.supervisor.BrowserProcess;
import org.miaixz.lancia.browser.supervisor.PipeEnvelope;
import org.miaixz.lancia.browser.supervisor.Runner;
import org.miaixz.lancia.kernel.bidi.BidiConnector;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowser;
import org.miaixz.lancia.kernel.cdp.CdpConnector;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.kernel.cdp.transport.PipeTransport;
import org.miaixz.lancia.kernel.cdp.transport.SocketTransportFactory;
import org.miaixz.lancia.nimble.browser.BrowserVariant;
import org.miaixz.lancia.nimble.browser.TargetType;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.options.LaunchOptions;

/**
 * Shared browser launcher base.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public abstract class BrowserLauncher implements Launcher {

    /**
     * Graceful browser close timeout.
     */
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(5L);

    /**
     * Browser family handled by this launcher.
     */
    @Getter
    private final BrowserVariant browser;

    /**
     * Browser process runner.
     */
    @Getter
    protected final Runner runner;

    /**
     * Creates a Chrome launcher base with the default process runner.
     */
    protected BrowserLauncher() {
        this(BrowserVariant.CHROME, new Runner());
    }

    /**
     * Creates a Chrome launcher base with a custom process runner.
     *
     * @param runner browser process runner
     */
    protected BrowserLauncher(Runner runner) {
        this(BrowserVariant.CHROME, runner);
    }

    /**
     * Creates a launcher base with a custom browser variant and process runner.
     *
     * @param browser browser variant
     * @param runner  browser process runner
     */
    protected BrowserLauncher(BrowserVariant browser, Runner runner) {
        this.browser = Assert.notNull(browser, "browser");
        this.runner = Assert.notNull(runner, "runner");
    }

    /**
     * Resolves the browser executable path.
     *
     * @param options launch options
     * @return executable path
     */
    @Override
    public Path executable(LaunchOptions options) {
        throw new InternalException("Executable resolution must be implemented by a concrete browser launcher.");
    }

    /**
     * Resolves browser command line arguments.
     *
     * @param options launch options
     * @return command line arguments
     */
    @Override
    public List<String> args(LaunchOptions options) {
        throw new InternalException("Argument resolution must be implemented by a concrete browser launcher.");
    }

    /**
     * Resolves the browser executable path.
     *
     * @param executablePath executable path
     * @return executable path
     */
    protected Path resolveExecutablePath(Path executablePath) {
        throw new InternalException("Executable path resolution must be implemented by a concrete browser launcher.");
    }

    /**
     * Resolves the browser executable path.
     *
     * @param executablePath explicit executable path text
     * @return executable path
     */
    public Path resolveExecutablePath(String executablePath) {
        return StringKit.isBlank(executablePath) ? resolveExecutablePath((Path) null)
                : resolveExecutablePath(Path.of(executablePath));
    }

    /**
     * Returns the browser variant handled by this launcher.
     *
     * @return browser variant
     */
    public BrowserVariant browser() {
        return browser;
    }

    /**
     * Connects to an existing browser using the protocol configured in connect options.
     *
     * @param options connect options
     * @return browser instance
     */
    @Override
    public Browser connect(ConnectOptions options) {
        ConnectOptions actualOptions = options == null ? new ConnectOptions() : options;
        Logger.debug(
                true,
                "Launcher",
                "Browser connect requested: browser={}, protocol={}",
                browser,
                actualOptions.getProtocol());
        try {
            Browser result;
            if (ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI.equals(actualOptions.getProtocol())) {
                result = adapt(new BidiConnector().connect(actualOptions));
            } else {
                result = new CdpConnector().connect(actualOptions);
            }
            Logger.debug(
                    false,
                    "Launcher",
                    "Browser connect completed: browser={}, protocol={}",
                    browser,
                    actualOptions.getProtocol());
            return result;
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Launcher",
                    ex,
                    "Browser connect failed: browser={}, protocol={}",
                    browser,
                    actualOptions.getProtocol());
            throw ex;
        }
    }

    /**
     * Adapts a BiDi browser to the public Browser contract.
     *
     * @param browser BiDi browser
     * @return public browser
     */
    protected Browser adapt(BidiBrowser browser) {
        return browser;
    }

    /**
     * Builds process launch options from resolved executable, arguments, and launch options.
     *
     * @param executablePath resolved browser executable
     * @param args           resolved browser arguments
     * @param options        launch options
     * @param onExit         optional exit hook
     * @return browser process launch options
     */
    protected BrowserProcess.ProcessLaunchOptions processLaunchOptions(
            Path executablePath,
            List<String> args,
            LaunchOptions options,
            Runnable onExit) {
        LaunchOptions actualOptions = options == null ? new LaunchOptions() : options;
        BrowserProcess.ProcessLaunchOptions processOptions = new BrowserProcess.ProcessLaunchOptions();
        processOptions.setExecutablePath(Assert.notNull(executablePath, "executablePath"));
        processOptions.setArgs(args == null ? List.of() : args);
        processOptions.setPipe(actualOptions.isPipe());
        processOptions.setOnExit(onExit);
        if (actualOptions.isTemporaryUserDataDir()) {
            processOptions.setTemporaryUserDataDir(actualOptions.getUserDataDir());
        }
        Logger.debug(
                false,
                "Launcher",
                "Process launch options resolved: executable={}, args={}, pipe={}, tempProfile={}",
                executablePath,
                processOptions.getArgs().size(),
                processOptions.isPipe(),
                actualOptions.isTemporaryUserDataDir());
        return processOptions;
    }

    /**
     * Validates that the resolved executable exists.
     *
     * @param executablePath resolved executable path
     */
    protected void ensureExecutableExists(Path executablePath) {
        Path actualPath = Assert.notNull(executablePath, "executablePath");
        if (!FileKit.isFile(actualPath.toFile())) {
            Logger.warn(false, "Launcher", "Browser executable missing: {}", actualPath);
            throw new InternalException(
                    "Browser was not found at the configured executablePath (" + actualPath + Symbol.PARENTHESE_RIGHT);
        }
        Logger.debug(false, "Launcher", "Browser executable verified: {}", actualPath);
    }

    /**
     * Creates a CDP WebSocket connection from browser process output.
     *
     * @param browserProcess browser process
     * @param timeoutMillis  endpoint wait timeout in milliseconds
     * @return CDP connection
     */
    protected Connection createCdpSocketConnection(BrowserProcess browserProcess, long timeoutMillis) {
        Logger.debug(true, "Launcher", "Waiting for CDP WebSocket endpoint: timeoutMillis={}", timeoutMillis);
        try {
            String endpoint = Assert.notNull(browserProcess, "browserProcess")
                    .waitForLineOutput(BrowserProcess.CDP_WEBSOCKET_ENDPOINT_REGEX, timeoutMillis)
                    .get(Math.max(1L, timeoutMillis), TimeUnit.MILLISECONDS);
            Logger.debug(false, "Launcher", "CDP WebSocket endpoint resolved: {}", endpoint);
            return new Connection(SocketTransportFactory.of(endpoint), endpoint);
        } catch (java.util.concurrent.TimeoutException ex) {
            Logger.error(
                    false,
                    "Launcher",
                    ex,
                    "Timed out waiting for CDP WebSocket endpoint: timeoutMillis={}",
                    timeoutMillis);
            throw new TimeoutException("Timed out while waiting for the browser WebSocket endpoint.", ex);
        } catch (Exception ex) {
            Logger.error(false, "Launcher", ex, "Failed to create CDP WebSocket connection.");
            throw new InternalException("Failed to create CDP WebSocket connection.", ex);
        }
    }

    /**
     * Creates a CDP pipe connection from a pipe envelope.
     *
     * @param pipeEnvelope pipe envelope
     * @return CDP connection
     */
    protected Connection createCdpPipeConnection(PipeEnvelope pipeEnvelope) {
        PipeEnvelope actualProcess = Assert.notNull(pipeEnvelope, "pipeEnvelope");
        Logger.debug(false, "Launcher", "CDP pipe connection resolved.");
        return new Connection(new PipeTransport(actualProcess.getCdpReader(), actualProcess.getCdpWriter()));
    }

    /**
     * Waits for the first page target and closes the browser when the wait fails.
     *
     * @param browser       browser instance
     * @param timeoutMillis wait timeout in milliseconds
     */
    protected void waitForPageTarget(Browser browser, long timeoutMillis) {
        Browser actualBrowser = Assert.notNull(browser, "browser");
        Logger.debug(true, "Browser", "Waiting for first page target: timeoutMillis={}", timeoutMillis);
        try {
            actualBrowser.waitForTarget(
                    target -> ((Target) target).type() == TargetType.PAGE,
                    Duration.ofMillis(timeoutMillis));
            Logger.debug(false, "Browser", "First page target detected.");
        } catch (RuntimeException ex) {
            Logger.error(false, "Browser", ex, "Failed to detect first page target: timeoutMillis={}", timeoutMillis);
            actualBrowser.close();
            throw ex;
        }
    }

    /**
     * Closes a browser process, preferring a graceful CDP Browser.close command when a connection exists.
     *
     * @param browserProcess browser process handle
     * @param cdpConnection  optional CDP connection
     */
    protected void closeBrowser(AutoCloseable browserProcess, Connection cdpConnection) {
        Logger.debug(true, "Browser", "Browser close requested: hasConnection={}", cdpConnection != null);
        if (cdpConnection != null && cdpConnection.hasConfiguredTransport() && !cdpConnection.isClosed()) {
            try {
                cdpConnection.closeBrowser().get(CLOSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                Logger.debug(false, "Browser", "Browser close command completed.");
            } catch (Exception ignored) {
                Logger.warn(false, "Browser", ignored, "Browser close command failed; disposing connection.");
                cdpConnection.dispose();
            }
        }
        IoKit.closeQuietly(browserProcess);
        Logger.debug(false, "Browser", "Browser process close completed.");
    }

    /**
     * Resolved browser launch arguments.
     */
    @Getter
    @Setter
    /**
     * Represents resolved launch args.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class ResolvedLaunchArgs {

        /**
         * Creates resolved launch arguments.
         */
        public ResolvedLaunchArgs() {
            // No initialization required.
        }

        /**
         * Whether the user data directory was created by the launcher.
         */
        private boolean tempUserDataDir;

        /**
         * Resolved user data directory.
         */
        private Path userDataDir;

        /**
         * Resolved browser executable.
         */
        private Path executablePath;

        /**
         * Resolved browser launch arguments.
         */
        private List<String> args = List.of();

        /**
         * Updates args.
         *
         * @param args resolved launch arguments
         */
        public void setArgs(List<String> args) {
            this.args = args == null ? List.of() : List.copyOf(args);
        }
    }

}
