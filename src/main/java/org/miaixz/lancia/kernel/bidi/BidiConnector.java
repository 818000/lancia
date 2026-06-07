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
package org.miaixz.lancia.kernel.bidi;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Transport;
import org.miaixz.lancia.kernel.Connector;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowser;
import org.miaixz.lancia.kernel.bidi.session.BidiConnection;
import org.miaixz.lancia.kernel.bidi.transport.BidiTransport;
import org.miaixz.lancia.kernel.bidi.transport.BidiWebSocketTransport;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.kernel.cdp.transport.SocketTransportFactory;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Connects BiDi browser protocol clients.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiConnector {

    /**
     * Default BiDi events used during session bootstrap.
     */
    private static final List<String> DEFAULT_EVENTS = List.of("browsingContext", "network", "log", "script", "input");

    /**
     * CDP event mirrors used when BiDi is tunneled over CDP.
     */
    private static final List<String> DEFAULT_CDP_EVENTS = List.of(
            "goog:cdp.Debugger.scriptParsed",
            "goog:cdp.CSS.styleSheetAdded",
            "goog:cdp.Runtime.executionContextsCleared",
            "goog:cdp.Tracing.tracingComplete",
            "goog:cdp.Network.requestWillBeSent",
            "goog:cdp.Page.screencastFrame");
    /**
     * Current BiDi transport factory.
     */
    private final BidiTransportFactory bidiTransportFactory;
    /**
     * Current CDP transport factory.
     */
    private final CdpTransportFactory cdpTransportFactory;

    /**
     * Creates a bidi browser connector.
     */
    public BidiConnector() {
        this(new BidiTransportFactory() {

            @Override
            public BidiTransport create(String endpoint) {
                return BidiWebSocketTransport.connect(endpoint);
            }

            @Override
            public BidiTransport create(
                    String endpoint,
                    long timeoutMillis,
                    SecurityPolicy securityPolicy,
                    ResourceLimits resourceLimits) {
                return BidiWebSocketTransport.connect(endpoint, timeoutMillis, securityPolicy, resourceLimits);
            }

        }, new CdpTransportFactory() {

            @Override
            public Transport create(String endpoint) {
                return SocketTransportFactory.of(endpoint);
            }

            @Override
            public Transport create(
                    String endpoint,
                    long timeoutMillis,
                    SecurityPolicy securityPolicy,
                    ResourceLimits resourceLimits) {
                return SocketTransportFactory.of(endpoint, Map.of(), securityPolicy, resourceLimits);
            }

        });
    }

    /**
     * Creates a bidi browser connector.
     *
     * @param bidiTransportFactory bidi transport factory
     * @param cdpTransportFactory  cdp transport factory
     */
    public BidiConnector(BidiTransportFactory bidiTransportFactory, CdpTransportFactory cdpTransportFactory) {
        this.bidiTransportFactory = Assert.notNull(bidiTransportFactory, "bidiTransportFactory");
        this.cdpTransportFactory = Assert.notNull(cdpTransportFactory, "cdpTransportFactory");
    }

    /**
     * Connects to a browser using WebDriver BiDi connection options.
     *
     * @param options connection options
     * @return BiDi browser instance
     */
    public BidiBrowser connect(ConnectOptions options) {
        ConnectOptions actualOptions = Assert.notNull(options, "options");
        Logger.debug(
                true,
                "Browser",
                "BiDi connect requested: hasTransport={}, hasEndpoint={}, timeout={}",
                actualOptions.getTransport() != null,
                actualOptions.getBrowserWSEndpoint() != null,
                actualOptions.getProtocolTimeoutMillis());
        if (actualOptions.getTransport() != null) {
            Logger.debug(false, "Browser", "BiDi connect using provided CDP transport.");
            return connectBidiOverCdp(
                    new Connection(actualOptions.getTransport(), Normal.EMPTY, actualOptions.getSlowMoMillis(),
                            actualOptions.getProtocolTimeoutMillis(), actualOptions.getIdGenerator()),
                    actualOptions.getProtocolTimeoutMillis(),
                    () -> {
                    },
                    actualOptions.isNetworkEnabled(),
                    actualOptions.isIssuesEnabled());
        }
        URI endpoint = actualOptions.getBrowserWSEndpoint();
        if (endpoint == null) {
            Logger.error(false, "Browser", "BiDi connect failed: missing browserWSEndpoint or transport.");
            throw new InternalException("BiDi connect requires browserWSEndpoint or transport.");
        }
        return connect(
                endpoint.toString(),
                actualOptions.getProtocolTimeoutMillis(),
                actualOptions.isNetworkEnabled(),
                actualOptions.isIssuesEnabled(),
                actualOptions.getSecurityPolicy(),
                actualOptions.getResourceLimits());
    }

    /**
     * Connects to a BiDi WebSocket endpoint.
     *
     * @param endpoint endpoint URL
     * @return BiDi browser instance
     */
    public BidiBrowser connect(String endpoint) {
        return connect(endpoint, Builder.DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Connects to a BiDi WebSocket endpoint.
     *
     * @param endpoint              endpoint URL
     * @param protocolTimeoutMillis protocol timeout in milliseconds
     * @return BiDi browser instance
     */
    public BidiBrowser connect(String endpoint, long protocolTimeoutMillis) {
        return connect(endpoint, protocolTimeoutMillis, true, true);
    }

    /**
     * Connects to a BiDi WebSocket endpoint with feature flags.
     *
     * @param endpoint              endpoint URL
     * @param protocolTimeoutMillis protocol timeout in milliseconds
     * @param networkEnabled        whether network tracking is enabled
     * @param issuesEnabled         whether issue tracking is enabled
     * @return BiDi browser instance
     */
    public BidiBrowser connect(
            String endpoint,
            long protocolTimeoutMillis,
            boolean networkEnabled,
            boolean issuesEnabled) {
        return connect(
                endpoint,
                protocolTimeoutMillis,
                networkEnabled,
                issuesEnabled,
                SecurityPolicy.defaultPolicy(),
                ResourceLimits.defaults());
    }

    /**
     * Connects to a BiDi WebSocket endpoint with runtime policies.
     *
     * @param endpoint              endpoint URL
     * @param protocolTimeoutMillis protocol timeout in milliseconds
     * @param networkEnabled        whether network tracking is enabled
     * @param issuesEnabled         whether issue tracking is enabled
     * @param securityPolicy        security policy
     * @param resourceLimits        resource limits
     * @return BiDi browser instance
     */
    public BidiBrowser connect(
            String endpoint,
            long protocolTimeoutMillis,
            boolean networkEnabled,
            boolean issuesEnabled,
            SecurityPolicy securityPolicy,
            ResourceLimits resourceLimits) {
        String actualEndpoint = Assert.notBlank(endpoint, "endpoint");
        long timeoutMillis = protocolTimeoutMillis <= 0 ? Builder.DEFAULT_TIMEOUT_MILLIS : protocolTimeoutMillis;
        SecurityPolicy actualSecurityPolicy = securityPolicy == null ? SecurityPolicy.defaultPolicy() : securityPolicy;
        ResourceLimits actualResourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
        Logger.debug(
                true,
                "Browser",
                "BiDi endpoint connect requested: endpoint={}, timeout={}",
                actualEndpoint.replaceAll("[?#].*$", "?<redacted>"),
                timeoutMillis);
        try {
            BidiBrowser browser = connectPureBidi(
                    actualEndpoint,
                    timeoutMillis,
                    networkEnabled,
                    issuesEnabled,
                    actualSecurityPolicy,
                    actualResourceLimits);
            Logger.debug(
                    false,
                    "Browser",
                    "BiDi endpoint connect completed: endpoint={}, mode=pure",
                    actualEndpoint.replaceAll("[?#].*$", "?<redacted>"));
            return browser;
        } catch (Exception pureFailure) {
            Logger.warn(
                    false,
                    "Browser",
                    pureFailure,
                    "Pure BiDi connect failed, trying CDP bridge: endpoint={}",
                    actualEndpoint.replaceAll("[?#].*$", "?<redacted>"));
            BidiBrowser browser = connectBidiOverCdp(
                    actualEndpoint,
                    timeoutMillis,
                    networkEnabled,
                    issuesEnabled,
                    actualSecurityPolicy,
                    actualResourceLimits);
            Logger.debug(
                    false,
                    "Browser",
                    "BiDi endpoint connect completed: endpoint={}, mode=cdpBridge",
                    actualEndpoint.replaceAll("[?#].*$", "?<redacted>"));
            return browser;
        }
    }

    /**
     * Returns the connect pure BiDi.
     *
     * @param endpoint       endpoint value
     * @param timeoutMillis  timeout in milliseconds
     * @param networkEnabled network enabled value
     * @param issuesEnabled  issues enabled value
     * @return connect pure BiDi value
     * @throws Exception if the operation fails
     */
    private BidiBrowser connectPureBidi(
            String endpoint,
            long timeoutMillis,
            boolean networkEnabled,
            boolean issuesEnabled,
            SecurityPolicy securityPolicy,
            ResourceLimits resourceLimits) throws Exception {
        Logger.debug(
                true,
                "Browser",
                "Pure BiDi connect requested: endpoint={}, timeout={}",
                endpoint.replaceAll("[?#].*$", "?<redacted>"),
                timeoutMillis);
        BidiTransport transport = bidiTransportFactory.create(endpoint, timeoutMillis, securityPolicy, resourceLimits);
        BidiConnection connection = new BidiConnection(endpoint, transport, timeoutMillis);
        bindTransport(transport, connection);
        try {
            connection.send("session.status").get(timeoutMillis, TimeUnit.MILLISECONDS);
            BidiBrowser browser = connect(
                    connection,
                    timeoutMillis,
                    () -> connection.send("browser.close", Map.of()),
                    null,
                    null,
                    false,
                    networkEnabled,
                    issuesEnabled);
            Logger.debug(
                    false,
                    "Browser",
                    "Pure BiDi connect completed: endpoint={}",
                    endpoint.replaceAll("[?#].*$", "?<redacted>"));
            return browser;
        } catch (Exception ex) {
            Logger.warn(
                    false,
                    "Browser",
                    ex,
                    "Pure BiDi connect failed: endpoint={}",
                    endpoint.replaceAll("[?#].*$", "?<redacted>"));
            connection.close();
            throw ex;
        }
    }

    /**
     * Returns the connect BiDi over CDP.
     *
     * @param endpoint       endpoint value
     * @param timeoutMillis  timeout in milliseconds
     * @param networkEnabled network enabled value
     * @param issuesEnabled  issues enabled value
     * @return connect BiDi over CDP value
     */
    private BidiBrowser connectBidiOverCdp(
            String endpoint,
            long timeoutMillis,
            boolean networkEnabled,
            boolean issuesEnabled,
            SecurityPolicy securityPolicy,
            ResourceLimits resourceLimits) {
        Logger.debug(
                true,
                "Browser",
                "BiDi-over-CDP connect requested: endpoint={}, timeout={}",
                endpoint.replaceAll("[?#].*$", "?<redacted>"),
                timeoutMillis);
        try {
            Connection cdp = new Connection(
                    cdpTransportFactory.create(endpoint, timeoutMillis, securityPolicy, resourceLimits), endpoint, 0L,
                    timeoutMillis, null);
            cdp.send("Browser.getVersion").get(timeoutMillis, TimeUnit.MILLISECONDS);
            BidiBrowser browser = connectBidiOverCdp(
                    cdp,
                    timeoutMillis,
                    () -> cdp.send("Browser.close", Map.of()),
                    networkEnabled,
                    issuesEnabled);
            Logger.debug(
                    false,
                    "Browser",
                    "BiDi-over-CDP connect completed: endpoint={}",
                    endpoint.replaceAll("[?#].*$", "?<redacted>"));
            return browser;
        } catch (Exception ex) {
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "BiDi-over-CDP connect failed: endpoint={}",
                    endpoint.replaceAll("[?#].*$", "?<redacted>"));
            throw new InternalException("Failed to connect BiDi browser: " + endpoint, ex);
        }
    }

    /**
     * Returns the connect BiDi over CDP.
     *
     * @param cdp            CDP value
     * @param timeoutMillis  timeout in milliseconds
     * @param closeHook      close hook value
     * @param networkEnabled network enabled value
     * @param issuesEnabled  issues enabled value
     * @return connect BiDi over CDP value
     */
    public BidiBrowser connectBidiOverCdp(
            Connection cdp,
            long timeoutMillis,
            AutoCloseable closeHook,
            boolean networkEnabled,
            boolean issuesEnabled) {
        long actualTimeout = timeoutMillis <= 0 ? Builder.DEFAULT_TIMEOUT_MILLIS : timeoutMillis;
        Logger.debug(true, "Browser", "BiDi-over-CDP browser attach requested: timeout={}", actualTimeout);
        try {
            BidiConnection bidi = Connector.connect(cdp, actualTimeout);
            BidiBrowser browser = connect(
                    bidi,
                    actualTimeout,
                    closeHook,
                    null,
                    cdp,
                    true,
                    networkEnabled,
                    issuesEnabled);
            Logger.debug(false, "Browser", "BiDi-over-CDP browser attached.");
            return browser;
        } catch (Exception ex) {
            Logger.error(false, "Browser", ex, "BiDi-over-CDP browser attach failed.");
            throw new InternalException("BiDi-over-CDP connect failed.", ex);
        }
    }

    /**
     * Connects an existing BiDi transport and returns a fully assembled browser instance.
     *
     * @param connection     connection
     * @param timeoutMillis  timeout millis
     * @param closeHook      close hook
     * @param process        process
     * @param cdpConnection  CDP connection
     * @param cdpEvents      CDP events
     * @param networkEnabled network enabled
     * @param issuesEnabled  issues enabled
     * @return connect value
     */
    public BidiBrowser connect(
            BidiConnection connection,
            long timeoutMillis,
            AutoCloseable closeHook,
            Process process,
            Connection cdpConnection,
            boolean cdpEvents,
            boolean networkEnabled,
            boolean issuesEnabled) {
        BidiConnection actualConnection = Assert.notNull(connection, "connection");
        long actualTimeout = timeoutMillis <= 0 ? Builder.DEFAULT_TIMEOUT_MILLIS : timeoutMillis;
        Logger.debug(
                true,
                "Browser",
                "BiDi browser attach requested: cdpEvents={}, timeout={}",
                cdpEvents,
                actualTimeout);
        try {
            BidiSession session = BidiSession
                    .from(
                            org.miaixz.lancia.kernel.bidi.accessor.BidiConnection.from(actualConnection),
                            Map.of("alwaysMatch", Map.of("webSocketUrl", true)))
                    .get(actualTimeout, TimeUnit.MILLISECONDS);
            List<String> events = events(cdpEvents);
            session.subscribe(events).get(actualTimeout, TimeUnit.MILLISECONDS);
            BidiBrowser browser = new BidiBrowser(session, process, cdpConnection, closeHook, networkEnabled,
                    issuesEnabled);
            Logger.debug(false, "Browser", "BiDi browser attached: events={}", events.size());
            return browser;
        } catch (Exception ex) {
            Logger.error(false, "Browser", ex, "BiDi browser attach failed: cdpEvents={}", cdpEvents);
            throw new InternalException("BiDi browser connect failed.", ex);
        }
    }

    /**
     * Returns bootstrap events.
     *
     * @param cdpEvents CDP event mirror state
     * @return event list
     */
    private List<String> events(boolean cdpEvents) {
        if (!cdpEvents) {
            return DEFAULT_EVENTS;
        }
        List<String> events = new ArrayList<>(DEFAULT_EVENTS);
        events.addAll(DEFAULT_CDP_EVENTS);
        return events;
    }

    /**
     * Handles bind transport.
     *
     * @param transport  transport value
     * @param connection protocol connection
     */
    private void bindTransport(BidiTransport transport, BidiConnection connection) {
        if (transport instanceof BidiWebSocketTransport webSocketTransport) {
            webSocketTransport.bind(connection);
            Logger.debug(false, "Protocol", "BiDi WebSocket transport binding applied.");
        }
        if (transport instanceof BindableBidiTransport bindableTransport) {
            bindableTransport.bind(connection);
            Logger.debug(false, "Protocol", "BiDi bindable transport binding applied.");
        }
    }

    /**
     * Defines the bidi transport factory contract.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface BidiTransportFactory {

        /**
         * Returns the create.
         *
         * @param endpoint endpoint value
         * @return create value
         */
        BidiTransport create(String endpoint);

        /**
         * Returns the create.
         *
         * @param endpoint       endpoint value
         * @param timeoutMillis  timeout millis
         * @param securityPolicy security policy
         * @param resourceLimits resource limits
         * @return create value
         */
        default BidiTransport create(
                String endpoint,
                long timeoutMillis,
                SecurityPolicy securityPolicy,
                ResourceLimits resourceLimits) {
            return create(endpoint);
        }
    }

    /**
     * Defines the CDP transport factory contract.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface CdpTransportFactory {

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
         * @param endpoint       endpoint value
         * @param timeoutMillis  timeout millis
         * @param securityPolicy security policy
         * @param resourceLimits resource limits
         * @return create value
         */
        default Transport create(
                String endpoint,
                long timeoutMillis,
                SecurityPolicy securityPolicy,
                ResourceLimits resourceLimits) {
            return create(endpoint);
        }
    }

    /**
     * Defines the bindable bidi transport contract.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface BindableBidiTransport {

        /**
         * Binds this transport to the protocol connection that owns it.
         *
         * @param connection protocol connection
         */
        void bind(BidiConnection connection);
    }

}
