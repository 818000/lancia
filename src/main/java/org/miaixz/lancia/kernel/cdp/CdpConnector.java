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
package org.miaixz.lancia.kernel.cdp;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Transport;
import org.miaixz.lancia.browser.BrowserNetwork;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.bundle.BrowserManager;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ChromeReleaseChannel;
import org.miaixz.lancia.kernel.cdp.browser.CdpBrowser;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.kernel.cdp.targets.UrlRestrictionRule;
import org.miaixz.lancia.kernel.cdp.transport.SocketTransportFactory;
import org.miaixz.lancia.options.AttachOptions;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.shared.async.Awaitable;

/**
 * Defines the CdpConnector class.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpConnector {

    /**
     * Current transport factory.
     */
    private final TransportFactory transportFactory;

    /**
     * Creates a CDP browser connector.
     */
    public CdpConnector() {
        this(new TransportFactory() {

            /**
             * Returns the create.
             *
             * @param endpoint endpoint value
             * @return create value
             */
            @Override
            public Transport create(String endpoint) {
                return SocketTransportFactory.of(endpoint);
            }

            /**
             * Returns the create.
             *
             * @param endpoint endpoint value
             * @param headers  HTTP headers
             * @return create value
             */
            @Override
            public Transport create(String endpoint, Map<String, String> headers) {
                return SocketTransportFactory.of(endpoint, headers);
            }
        });
    }

    /**
     * Creates a CDP browser connector.
     *
     * @param transportFactory the transport factory value
     */
    public CdpConnector(TransportFactory transportFactory) {
        this.transportFactory = Assert.notNull(transportFactory, "transportFactory");
    }

    /**
     * Connects to a browser using CDP connection options.
     *
     * @param options connection options
     * @return browser instance
     */
    public Browser connect(ConnectOptions options) {
        ConnectOptions actualOptions = Assert.notNull(options, "options");
        Logger.debug(
                true,
                "Browser",
                "CDP connect requested: protocol={}, browserURL={}, wsEndpoint={}, channel={}",
                actualOptions.getProtocol(),
                actualOptions.getBrowserURL() != null,
                actualOptions.getBrowserWSEndpoint() != null,
                actualOptions.getChannel());
        assertSupportedUrlRestrictions(actualOptions);
        if (ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI.equals(actualOptions.getProtocol())) {
            throw new InternalException("Invalid connector protocol route: webDriverBiDi must be routed before CDP.");
        }
        try {
            TransportSelection selection = selectTransport(actualOptions);
            Browser result = connect(selection.transport(), selection.endpoint(), actualOptions);
            Logger.debug(
                    false,
                    "Browser",
                    "CDP connect completed: endpoint={}",
                    selection.endpoint() == null ? Normal.EMPTY
                            : selection.endpoint().replaceAll("[?#].*$", "?<redacted>"));
            return result;
        } catch (RuntimeException ex) {
            Logger.error(false, "Browser", ex, "CDP connect failed.");
            throw ex;
        }
    }

    /**
     * Asserts the supported url restrictions condition.
     *
     * @param options the options value
     */
    public static void assertSupportedUrlRestrictions(ConnectOptions options) {
        ConnectOptions actualOptions = Assert.notNull(options, "options");
        boolean hasBlocklist = !actualOptions.getBlocklist().isEmpty();
        boolean hasAllowlist = !actualOptions.getAllowlist().isEmpty();
        if (hasBlocklist && hasAllowlist) {
            throw new IllegalArgumentException("Cannot specify both blocklist and allowlist");
        }
        if (ConnectOptions.PROTOCOL_WEB_DRIVER_BIDI.equals(actualOptions.getProtocol())
                && (hasBlocklist || hasAllowlist)) {
            throw new IllegalArgumentException("blocklist and allowlist are only supported with the CDP protocol");
        }
        actualOptions.getBlocklist().forEach(CdpConnector::validateUrlRule);
        actualOptions.getAllowlist().forEach(CdpConnector::validateUrlRule);
    }

    /**
     * Attaches a CDP browser to an existing transport.
     *
     * @param transport transport to use
     * @param endpoint  endpoint label
     * @param options   connection options
     * @return browser instance
     */
    public Browser connect(Transport transport, String endpoint, ConnectOptions options) {
        ConnectOptions actualOptions = options == null ? new ConnectOptions() : options;
        Logger.debug(
                true,
                "Browser",
                "CDP attach requested: endpoint={}, network={}, issues={}",
                endpoint == null ? Normal.EMPTY : endpoint.replaceAll("[?#].*$", "?<redacted>"),
                actualOptions.isNetworkEnabled(),
                actualOptions.isIssuesEnabled());
        Connection connection = new Connection(Assert.notNull(transport, "transport"), endpoint,
                actualOptions.getSlowMoMillis(), actualOptions.getProtocolTimeoutMillis(),
                actualOptions.getIdGenerator());
        long timeoutMillis = timeoutMillis(actualOptions);
        List<String> browserContextIds = browserContextIds(connection, timeoutMillis);
        if (actualOptions.isAcceptInsecureCerts()) {
            Awaitable.await(
                    connection.send("Security.setIgnoreCertificateErrors", java.util.Map.of("ignore", true)),
                    "Failed to configure HTTPS certificate error ignoring.",
                    timeoutMillis);
        }
        AutoCloseable closeHook = () -> Awaitable
                .await(connection.send("Browser.close"), "Failed to close browser.", timeoutMillis);
        CdpBrowser browser = new CdpBrowser(connection, closeHook, endpoint, actualOptions.isNetworkEnabled(),
                actualOptions.isIssuesEnabled(), browserContextIds);
        browser.attach(AttachOptions.from(actualOptions));
        Logger.debug(
                false,
                "Browser",
                "CDP attach completed: endpoint={}, contexts={}",
                endpoint == null ? Normal.EMPTY : endpoint.replaceAll("[?#].*$", "?<redacted>"),
                browserContextIds.size());
        return browser;
    }

    /**
     * Returns the browser context ids.
     *
     * @param connection    protocol connection
     * @param timeoutMillis timeout in milliseconds
     * @return values
     */
    private List<String> browserContextIds(Connection connection, long timeoutMillis) {
        CdpPayload response = Awaitable
                .await(connection.send("Target.getBrowserContexts"), "Failed to read browser contexts.", timeoutMillis);
        List<String> ids = new ArrayList<>();
        CdpPayload contexts = response.get("browserContextIds");
        if (!contexts.isNull() && contexts.isArray()) {
            for (CdpPayload context : contexts.elements()) {
                if (!context.isNull() && StringKit.isNotBlank(context.asText())) {
                    ids.add(context.asText());
                }
            }
        }
        return List.copyOf(ids);
    }

    /**
     * Returns the select transport.
     *
     * @param options operation options
     * @return select transport value
     */
    private TransportSelection selectTransport(ConnectOptions options) {
        int configured = (options.getTransport() == null ? Normal._0 : Normal._1)
                + (options.getBrowserWSEndpoint() == null ? Normal._0 : Normal._1)
                + (options.getBrowserURL() == null ? Normal._0 : Normal._1)
                + (StringKit.isBlank(options.getChannel()) ? Normal._0 : Normal._1);
        if (configured != Normal._1) {
            throw new IllegalArgumentException(
                    "Exactly one of browserWSEndpoint, browserURL, transport or channel must be passed.");
        }
        if (options.getTransport() != null) {
            Logger.debug(false, "Browser", "Using provided CDP transport.");
            return new TransportSelection(options.getTransport(), Normal.EMPTY);
        }
        String endpoint;
        if (options.getBrowserWSEndpoint() != null) {
            endpoint = options.getBrowserWSEndpoint().toString();
        } else if (options.getBrowserURL() != null) {
            endpoint = webSocketEndpoint(options.getBrowserURL());
        } else {
            endpoint = webSocketEndpoint(options.getChannel());
        }
        Logger.debug(
                false,
                "Browser",
                "CDP transport selected: endpoint={}",
                endpoint == null ? Normal.EMPTY : endpoint.replaceAll("[?#].*$", "?<redacted>"));
        return new TransportSelection(transportFactory.create(endpoint, options.getHeaders()), endpoint);
    }

    /**
     * Returns the web socket endpoint.
     *
     * @param browserURL browser URL value
     * @return web socket endpoint value
     */
    private String webSocketEndpoint(URI browserURL) {
        URI endpointURL = Assert.notNull(browserURL, "browserURL").resolve("/json/version");
        Logger.debug(
                true,
                "Browser",
                "Resolving CDP endpoint from browser URL: {}",
                browserURL.toString().replaceAll("[?#].*$", "?<redacted>"));
        try {
            String endpoint = BrowserNetwork.getJSON(endpointURL).get("webSocketDebuggerUrl").asText();
            Logger.debug(
                    false,
                    "Browser",
                    "Resolved CDP endpoint from browser URL: {}",
                    endpoint == null ? Normal.EMPTY : endpoint.replaceAll("[?#].*$", "?<redacted>"));
            return endpoint;
        } catch (RuntimeException ex) {
            Logger.error(false, "Browser", ex, "Failed to resolve CDP endpoint from browser URL: {}", browserURL);
            throw new InternalException("Failed to read browser WebSocket endpoint: " + endpointURL, ex);
        }
    }

    /**
     * Returns the web socket endpoint.
     *
     * @param channel channel value
     * @return web socket endpoint value
     */
    private String webSocketEndpoint(String channel) {
        BrowserPlatform platform = BrowserPlatform.detect();
        Logger.debug(
                true,
                "Browser",
                "Resolving CDP endpoint from Chrome channel: channel={}, platform={}",
                channel,
                platform);
        Path userDataDir = BrowserManager.resolveDefaultUserDataDir(
                BrowserDataTypes.Browser.CHROME,
                platform,
                ChromeReleaseChannel.fromValue(channel));
        Path portPath = userDataDir.resolve("DevToolsActivePort");
        try {
            String endpoint = endpointFromDevToolsActivePort(portPath);
            Logger.debug(
                    false,
                    "Browser",
                    "Resolved CDP endpoint from Chrome channel: channel={}, endpoint={}",
                    channel,
                    endpoint == null ? Normal.EMPTY : endpoint.replaceAll("[?#].*$", "?<redacted>"));
            return endpoint;
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "Failed to resolve CDP endpoint from Chrome channel: channel={}",
                    channel);
            throw new InternalException("Could not find " + portPath + " in " + channel + " DevToolsActivePort.", ex);
        }
    }

    /**
     * Returns the endpoint from dev tools active port.
     *
     * @param portPath port path value
     * @return endpoint from dev tools active port value
     */
    static String endpointFromDevToolsActivePort(Path portPath) {
        try {
            List<String> lines = Files.readAllLines(Assert.notNull(portPath, "portPath"), Charset.US_ASCII).stream()
                    .map(String::trim).filter(StringKit::isNotBlank).toList();
            if (lines.size() < 2) {
                throw new InternalException("DevToolsActivePort content is incomplete.");
            }
            int port = Integer.parseInt(lines.get(Normal._0));
            if (port <= Normal._0 || port > 65_535) {
                throw new InternalException("Invalid DevToolsActivePort port: " + lines.get(Normal._0));
            }
            return "ws://localhost:" + port + lines.get(Normal._1);
        } catch (NumberFormatException ex) {
            throw new InternalException("DevToolsActivePort port is not numeric.", ex);
        } catch (java.io.IOException ex) {
            throw new InternalException("Failed to read DevToolsActivePort.", ex);
        }
    }

    /**
     * Validates URL rule.
     *
     * @param rule rule value
     */
    private static void validateUrlRule(String rule) {
        try {
            UrlRestrictionRule.compile(rule);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid URL restriction rule: " + rule, ex);
        }
    }

    /**
     * Returns the timeout millis.
     *
     * @param options operation options
     * @return timeout millis value
     */
    private long timeoutMillis(ConnectOptions options) {
        return options.getProtocolTimeoutMillis() <= Normal._0 ? Builder.DEFAULT_TIMEOUT_MILLIS
                : options.getProtocolTimeoutMillis();
    }

    /**
     * Defines the TransportFactory interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface TransportFactory {

        /**
         * Returns the create.
         *
         * @param endpoint endpoint value
         * @return create value
         */
        Transport create(String endpoint);

        /**
         * Returns the create.
         *
         * @param endpoint endpoint value
         * @param headers  HTTP headers
         * @return create value
         */
        default Transport create(String endpoint, Map<String, String> headers) {
            return create(endpoint);
        }
    }

    /**
     * Defines the TransportSelection class.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class TransportSelection {

        /**
         * Current transport.
         */
        private final Transport transport;
        /**
         * Current endpoint.
         */
        private final String endpoint;

        /**
         * Creates an instance.
         *
         * @param transport transport value
         * @param endpoint  endpoint value
         */
        private TransportSelection(Transport transport, String endpoint) {
            this.transport = Assert.notNull(transport, "transport");
            this.endpoint = endpoint == null ? Normal.EMPTY : endpoint;
        }

        /**
         * Returns the transport.
         *
         * @return transport value
         */
        private Transport transport() {
            return transport;
        }

        /**
         * Returns the endpoint.
         *
         * @return endpoint value
         */
        private String endpoint() {
            return endpoint;
        }
    }

}
