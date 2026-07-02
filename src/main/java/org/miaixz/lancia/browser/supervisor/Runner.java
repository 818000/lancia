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
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.IoKit;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.Transport;
import org.miaixz.lancia.kernel.Connector;
import org.miaixz.lancia.kernel.bidi.BidiConnector;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowser;
import org.miaixz.lancia.kernel.cdp.CdpConnector;
import org.miaixz.lancia.kernel.cdp.browser.CdpBrowser;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.kernel.cdp.transport.PipeTransport;
import org.miaixz.lancia.kernel.cdp.transport.SocketTransportFactory;
import org.miaixz.lancia.nimble.browser.TargetType;
import org.miaixz.lancia.options.AttachOptions;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.options.ExtensionInstallOptions;
import org.miaixz.lancia.options.LaunchOptions;

/**
 * Launches and connects browser processes.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Runner {

    /**
     * Current pipe envelope factory.
     */
    private final PipeEnvelopeFactory pipeEnvelopeFactory;
    /**
     * Shared constant for endpoint timeout.
     */
    private static final Duration ENDPOINT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Creates a runner.
     */
    public Runner() {
        this(defaultPipeEnvelopeFactory());
    }

    /**
     * Creates a runner.
     *
     * @param factory pipe envelope factory
     */
    public Runner(PipeEnvelopeFactory factory) {
        this.pipeEnvelopeFactory = factory;
    }

    /**
     * Returns the launch.
     *
     * @param executable executable value
     * @param args       arguments to pass
     * @param options    operation options
     * @return launch value
     */
    public Browser launch(Path executable, List<String> args, LaunchOptions options) {
        Assert.notNull(executable, "executable");
        Assert.notNull(args, "args");
        Assert.notNull(options, "options");
        ensureLaunchNotCancelled(options);
        if (options.isPipe()) {
            return launchPipe(executable, args, options);
        }
        return launchWebSocket(executable, args, options);
    }

    /**
     * Launches a browser and exposes it as a BiDi browser over the CDP transport.
     *
     * @param executable executable
     * @param args       args
     * @param options    options
     * @return BiDi browser instance
     */
    public BidiBrowser launchBidiOverCdp(Path executable, List<String> args, LaunchOptions options) {
        Assert.notNull(executable, "executable");
        Assert.notNull(args, "args");
        Assert.notNull(options, "options");
        ensureLaunchNotCancelled(options);
        if (options.isPipe()) {
            return launchPipeBidiOverCdp(executable, args, options);
        }
        return launchWebSocketBidiOverCdp(executable, args, options);
    }

    /**
     * Connects to an existing browser through CDP options.
     *
     * @param options connection options
     * @return browser instance
     */
    public Browser connect(ConnectOptions options) {
        Assert.notNull(options, "options");
        return new CdpConnector(SocketTransportFactory::of).connect(options);
    }

    /**
     * Launches a browser using CDP pipe transport.
     *
     * @param executable executable value
     * @param args       arguments to pass
     * @param options    operation options
     * @return launch pipe value
     */
    private Browser launchPipe(Path executable, List<String> args, LaunchOptions options) {
        PipeEnvelope process = null;
        PipeTransport transport = null;
        AutoCloseable cancellationHook;
        try {
            ensureLaunchNotCancelled(options);
            process = this.pipeEnvelopeFactory.create(buildCommand(executable, args));
            PipeEnvelope launchedProcess = process;
            cancellationHook = onLaunchCancelled(options, launchedProcess::close);
            ensureLaunchNotCancelled(options);
            transport = new PipeTransport(process.getCdpReader(), process.getCdpWriter(), options.getResourceLimits());
            CdpBrowser browser = new CdpBrowser(newConnection(transport, Normal.EMPTY, options), process);
            browser.attach(AttachOptions.from(options));
            afterLaunch(browser, options);
            IoKit.closeQuietly(cancellationHook);
            return browser;
        } catch (CancellationException ex) {
            IoKit.closeQuietly(transport, process);
            cleanupTemporaryUserDataDir(options);
            throw ex;
        } catch (Exception ex) {
            IoKit.closeQuietly(transport, process);
            cleanupTemporaryUserDataDir(options);
            if (options.isLaunchCancelled()) {
                throw new CancellationException("Browser launch has been cancelled.");
            }
            throw new IllegalStateException("Failed to launch browser in pipe mode.", ex);
        }
    }

    /**
     * Launches a pipe browser and exposes BiDi over the CDP pipe transport.
     *
     * @param executable executable
     * @param args       args
     * @param options    options
     * @return BiDi browser instance
     */
    private BidiBrowser launchPipeBidiOverCdp(Path executable, List<String> args, LaunchOptions options) {
        PipeEnvelope process = null;
        PipeTransport transport = null;
        AutoCloseable cancellationHook;
        try {
            ensureLaunchNotCancelled(options);
            process = this.pipeEnvelopeFactory.create(buildCommand(executable, args));
            PipeEnvelope launchedProcess = process;
            cancellationHook = onLaunchCancelled(options, launchedProcess::close);
            ensureLaunchNotCancelled(options);
            transport = new PipeTransport(process.getCdpReader(), process.getCdpWriter(), options.getResourceLimits());
            Connection cdp = newConnection(transport, Normal.EMPTY, options);
            BidiBrowser browser = connectBidiOverCdp(cdp, options, process, process.getProcess());
            IoKit.closeQuietly(cancellationHook);
            return browser;
        } catch (CancellationException ex) {
            IoKit.closeQuietly(transport, process);
            cleanupTemporaryUserDataDir(options);
            throw ex;
        } catch (Exception ex) {
            IoKit.closeQuietly(transport, process);
            cleanupTemporaryUserDataDir(options);
            if (options.isLaunchCancelled()) {
                throw new CancellationException("Browser launch has been cancelled.");
            }
            throw new IllegalStateException("pipe mode BiDi browser launch failed.", ex);
        }
    }

    /**
     * Returns the launch web socket.
     *
     * @param executable executable value
     * @param args       arguments to pass
     * @param options    operation options
     * @return launch web socket value
     */
    private Browser launchWebSocket(Path executable, List<String> args, LaunchOptions options) {
        Process process = null;
        AutoCloseable cancellationHook;
        try {
            ensureLaunchNotCancelled(options);
            process = new ProcessBuilder(buildCommand(executable, args)).start();
            Process launchedProcess = process;
            cancellationHook = onLaunchCancelled(options, () -> destroyProcess(launchedProcess));
            BrowserProcess.drainStdout(process);
            String endpoint = waitForWebSocketEndpoint(process, options);
            ensureLaunchNotCancelled(options);
            Transport transport = SocketTransportFactory
                    .of(endpoint, Map.of(), options.getSecurityPolicy(), options.getResourceLimits());
            Path temporaryDir = options.isTemporaryUserDataDir() ? options.getUserDataDir() : null;
            CdpBrowser browser = new CdpBrowser(newConnection(transport, endpoint, options),
                    new BrowserProcess(process, temporaryDir));
            browser.attach(AttachOptions.from(options));
            afterLaunch(browser, options);
            IoKit.closeQuietly(cancellationHook);
            return browser;
        } catch (CancellationException ex) {
            destroyProcess(process);
            cleanupTemporaryUserDataDir(options);
            throw ex;
        } catch (Exception ex) {
            destroyProcess(process);
            cleanupTemporaryUserDataDir(options);
            if (options.isLaunchCancelled()) {
                throw new CancellationException("Browser launch has been cancelled.");
            }
            throw new IllegalStateException("Failed to launch browser in WebSocket mode.", ex);
        }
    }

    /**
     * Launches a WebSocket browser and exposes BiDi over the CDP WebSocket transport.
     *
     * @param executable executable
     * @param args       args
     * @param options    options
     * @return BiDi browser instance
     */
    private BidiBrowser launchWebSocketBidiOverCdp(Path executable, List<String> args, LaunchOptions options) {
        Process process = null;
        AutoCloseable cancellationHook;
        try {
            ensureLaunchNotCancelled(options);
            process = new ProcessBuilder(buildCommand(executable, args)).start();
            Process launchedProcess = process;
            cancellationHook = onLaunchCancelled(options, () -> destroyProcess(launchedProcess));
            BrowserProcess.drainStdout(process);
            String endpoint = waitForWebSocketEndpoint(process, options);
            ensureLaunchNotCancelled(options);
            Transport transport = SocketTransportFactory
                    .of(endpoint, Map.of(), options.getSecurityPolicy(), options.getResourceLimits());
            Path temporaryDir = options.isTemporaryUserDataDir() ? options.getUserDataDir() : null;
            BidiBrowser browser = connectBidiOverCdp(
                    newConnection(transport, endpoint, options),
                    options,
                    new BrowserProcess(process, temporaryDir),
                    process);
            IoKit.closeQuietly(cancellationHook);
            return browser;
        } catch (CancellationException ex) {
            destroyProcess(process);
            cleanupTemporaryUserDataDir(options);
            throw ex;
        } catch (Exception ex) {
            destroyProcess(process);
            cleanupTemporaryUserDataDir(options);
            if (options.isLaunchCancelled()) {
                throw new CancellationException("Browser launch has been cancelled.");
            }
            throw new IllegalStateException("WebSocket mode BiDi browser launch failed.", ex);
        }
    }

    /**
     * Connects a CDP connection as a BiDi-over-CDP browser.
     *
     * @param cdp       CDP connection
     * @param options   launch options
     * @param closeHook browser close hook
     * @param process   browser process
     * @return BiDi browser instance
     */
    private BidiBrowser connectBidiOverCdp(
            Connection cdp,
            LaunchOptions options,
            AutoCloseable closeHook,
            Process process) {
        long timeoutMillis = options.getProtocolTimeoutMillis() <= 0 ? ENDPOINT_TIMEOUT.toMillis()
                : options.getProtocolTimeoutMillis();
        return new BidiConnector().connect(Connector.connect(cdp, timeoutMillis), timeoutMillis, () -> {
            try {
                cdp.closeBrowser().get(Math.min(timeoutMillis, ENDPOINT_TIMEOUT.toMillis()), TimeUnit.MILLISECONDS);
            } finally {
                closeHook.close();
            }
        }, process, cdp, true, options.isNetworkEnabled(), options.isIssuesEnabled());
    }

    /**
     * Creates CDP connection from launch options.
     *
     * @param transport transport
     * @param endpoint  endpoint
     * @param options   launch options
     * @return CDP connection
     */
    private Connection newConnection(Transport transport, String endpoint, LaunchOptions options) {
        return new Connection(transport, endpoint, options.getSlowMoMillis(), options.getProtocolTimeoutMillis(),
                options.getIdGenerator());
    }

    /**
     * Applies post-launch options to the browser.
     *
     * @param browser launched browser
     * @param options launch options
     */
    private void afterLaunch(Browser browser, LaunchOptions options) {
        ensureLaunchNotCancelled(options);
        if (options.isEnableExtensions()) {
            for (Path extensionPath : options.getExtensionPaths()) {
                ensureLaunchNotCancelled(options);
                ExtensionInstallOptions installOptions = new ExtensionInstallOptions();
                installOptions.setEnabledInIncognito(options.getExtensionsEnabledInIncognito().contains(extensionPath));
                browser.installExtension(extensionPath, installOptions);
            }
        }
        if (options.isWaitForInitialPage()) {
            waitForInitialPage(browser, options);
        }
    }

    /**
     * Waits for web socket endpoint.
     *
     * @param process process value
     * @param options operation options
     * @return wait for web socket endpoint value
     * @throws Exception if the operation fails
     */
    private String waitForWebSocketEndpoint(Process process, LaunchOptions options) throws Exception {
        CompletableFuture<String> endpoint = CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), Charset.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int index = line.indexOf("ws://");
                    if (index >= 0) {
                        return line.substring(index).trim();
                    }
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to read Chrome stderr.", ex);
            }
            throw new IllegalStateException("Chrome did not output a DevTools WebSocket endpoint.");
        });
        return waitForEndpoint(endpoint, process, options);
    }

    /**
     * Waits for the WebSocket endpoint while honoring cancellation and browser process exit.
     *
     * @param endpoint endpoint future
     * @param process  process
     * @param options  launch options
     * @return endpoint
     * @throws Exception if waiting fails
     */
    private String waitForEndpoint(CompletableFuture<String> endpoint, Process process, LaunchOptions options)
            throws Exception {
        long timeoutMillis = options.getTimeoutMillis() <= 0 ? ENDPOINT_TIMEOUT.toMillis() : options.getTimeoutMillis();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (true) {
            ensureLaunchNotCancelled(options);
            try {
                return endpoint.get(50, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException ignored) {
                if (!process.isAlive()) {
                    throw new IllegalStateException(
                            "Browser process exited before outputting the DevTools endpoint, exit code: "
                                    + process.exitValue());
                }
                if (System.nanoTime() >= deadline) {
                    throw new java.util.concurrent.TimeoutException("Timed out after " + timeoutMillis
                            + " ms while waiting for the WS endpoint URL to appear in stdout!");
                }
            }
        }
    }

    /**
     * Waits for the initial page target while honoring cancellation.
     *
     * @param browser browser
     * @param options launch options
     */
    private void waitForInitialPage(Browser browser, LaunchOptions options) {
        long timeoutMillis = Math.max(1L, options.getTimeoutMillis());
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (true) {
            ensureLaunchNotCancelled(options);
            for (Target target : browser.targets()) {
                if (target.type() == TargetType.PAGE) {
                    return;
                }
            }
            if (System.nanoTime() >= deadline) {
                throw new TimeoutException("Timed out waiting for target.");
            }
            if (!ThreadKit.sleep(25L)) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the initial page.");
            }
        }
    }

    /**
     * Builds command.
     *
     * @param executable executable value
     * @param args       arguments to pass
     * @return values
     */
    private List<String> buildCommand(Path executable, List<String> args) {
        java.util.ArrayList<String> command = new java.util.ArrayList<>();
        command.add(executable.toString());
        command.addAll(args);
        return command;
    }

    /**
     * Throws if launch has been cancelled.
     *
     * @param options launch options
     */
    private void ensureLaunchNotCancelled(LaunchOptions options) {
        options.throwIfLaunchCancelled();
    }

    /**
     * Registers a temporary launch cancellation hook.
     *
     * @param options  launch options
     * @param onCancel cancel action
     * @return deactivation hook
     */
    private AutoCloseable onLaunchCancelled(LaunchOptions options, Runnable onCancel) {
        CompletableFuture<?> signal = options.getSignal();
        if (signal == null) {
            return () -> {
            };
        }
        AtomicBoolean active = new AtomicBoolean(true);
        signal.whenComplete((value, error) -> {
            if (active.get()) {
                onCancel.run();
            }
        });
        return () -> active.set(false);
    }

    /**
     * Destroys a process quietly.
     *
     * @param process process
     */
    private void destroyProcess(Process process) {
        if (process == null) {
            return;
        }
        try {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        } catch (RuntimeException ignored) {
            // Implementation note.
        }
    }

    /**
     * Cleans up a temporary user data directory if launch fails before browser ownership is established.
     *
     * @param options launch options
     */
    private void cleanupTemporaryUserDataDir(LaunchOptions options) {
        BrowserProcess.cleanupTemporaryDirectory(options.isTemporaryUserDataDir() ? options.getUserDataDir() : null);
    }

    /**
     * Returns the platform specific pipe envelope factory.
     *
     * @return pipe envelope factory
     */
    private static PipeEnvelopeFactory defaultPipeEnvelopeFactory() {
        String os = System.getProperty("os.name", Normal.EMPTY).toLowerCase(java.util.Locale.ROOT);
        if (os.contains("win")) {
            return new WindowsPipeEnvelopeFactory();
        }
        return new UnixPipeEnvelopeFactory();
    }

}
