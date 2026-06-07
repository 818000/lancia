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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.lang.Charset;
import org.miaixz.lancia.kernel.bidi.BidiConnector;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowser;
import org.miaixz.lancia.kernel.bidi.session.BidiConnection;
import org.miaixz.lancia.kernel.bidi.transport.BidiWebSocketTransport;
import org.miaixz.lancia.options.FirefoxOptions;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Launches and connects Firefox browser processes.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class FirefoxRunner {

    /**
     * Creates a firefox runner.
     */
    public FirefoxRunner() {
        // No initialization required.
    }

    /**
     * Shared constant for endpoint timeout.
     */
    private static final Duration ENDPOINT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Returns the launch.
     *
     * @param executable executable value
     * @param args       arguments to pass
     * @param options    operation options
     * @return launch value
     */
    public BidiBrowser launch(Path executable, List<String> args, FirefoxOptions options) {
        try {
            Process process = new ProcessBuilder(buildCommand(executable, args)).start();
            BrowserProcess.drainStdout(process);
            String endpoint = waitForEndpoint(process);
            Path temporaryDir = options.isTemporaryUserDataDir() ? options.getUserDataDir() : null;
            return connect(
                    endpoint,
                    options.getProtocolTimeoutMillis(),
                    new BrowserProcess(process, temporaryDir),
                    process,
                    options.isNetworkEnabled(),
                    options.isIssuesEnabled(),
                    options.getSecurityPolicy(),
                    options.getResourceLimits());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to launch Firefox BiDi.", ex);
        }
    }

    /**
     * Connects to a Firefox BiDi endpoint.
     *
     * @param endpoint              endpoint value
     * @param protocolTimeoutMillis protocol timeout millis value
     * @return BiDi browser instance
     * @throws Exception if the operation fails
     */
    protected BidiBrowser connect(String endpoint, long protocolTimeoutMillis) throws Exception {
        return connect(endpoint, protocolTimeoutMillis, null, null, true, true, null, null);
    }

    /**
     * Connects to a Firefox BiDi endpoint.
     *
     * @param endpoint              endpoint value
     * @param protocolTimeoutMillis protocol timeout millis value
     * @param closeHook             close hook value
     * @param process               process value
     * @param networkEnabled        network enabled value
     * @param issuesEnabled         issues enabled value
     * @return BiDi browser instance
     * @throws Exception if the operation fails
     */
    protected BidiBrowser connect(
            String endpoint,
            long protocolTimeoutMillis,
            AutoCloseable closeHook,
            Process process,
            boolean networkEnabled,
            boolean issuesEnabled) throws Exception {
        return connect(endpoint, protocolTimeoutMillis, closeHook, process, networkEnabled, issuesEnabled, null, null);
    }

    /**
     * Connects to a Firefox BiDi endpoint.
     *
     * @param endpoint              endpoint value
     * @param protocolTimeoutMillis protocol timeout millis value
     * @param closeHook             close hook value
     * @param process               process value
     * @param networkEnabled        network enabled value
     * @param issuesEnabled         issues enabled value
     * @param securityPolicy        security policy
     * @param resourceLimits        resource limits
     * @return BiDi browser instance
     * @throws Exception if the operation fails
     */
    protected BidiBrowser connect(
            String endpoint,
            long protocolTimeoutMillis,
            AutoCloseable closeHook,
            Process process,
            boolean networkEnabled,
            boolean issuesEnabled,
            SecurityPolicy securityPolicy,
            ResourceLimits resourceLimits) throws Exception {
        long timeoutMillis = protocolTimeoutMillis <= 0 ? ENDPOINT_TIMEOUT.toMillis() : protocolTimeoutMillis;
        BidiWebSocketTransport transport = BidiWebSocketTransport
                .connect(endpoint, timeoutMillis, securityPolicy, resourceLimits);
        BidiConnection connection = new BidiConnection(endpoint, transport, timeoutMillis);
        transport.bind(connection);
        return new BidiConnector()
                .connect(connection, timeoutMillis, closeHook, process, null, false, networkEnabled, issuesEnabled);
    }

    /**
     * Waits for endpoint.
     *
     * @param process process value
     * @return wait for endpoint value
     * @throws Exception if the operation fails
     */
    private String waitForEndpoint(Process process) throws Exception {
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
                throw new IllegalStateException("Failed to read Firefox stderr.", ex);
            }
            throw new IllegalStateException("Firefox did not output a BiDi WebSocket endpoint.");
        });
        return endpoint.get(ENDPOINT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Builds command.
     *
     * @param executable executable value
     * @param args       arguments to pass
     * @return values
     */
    private List<String> buildCommand(Path executable, List<String> args) {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(args);
        return command;
    }

}
