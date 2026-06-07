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

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.Transport;
import org.miaixz.lancia.nimble.browser.TargetType;
import org.miaixz.lancia.nimble.emulation.Viewport;
import org.miaixz.lancia.nimble.network.DownloadBehavior;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Defines options for connect operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ConnectOptions {

    /**
     * Shared constant for protocol CDP.
     */
    public static final String PROTOCOL_CDP = "cdp";

    /**
     * Shared constant for protocol web driver BiDi.
     */
    public static final String PROTOCOL_WEB_DRIVER_BIDI = "webDriverBiDi";

    /**
     * Creates connect options.
     */
    public ConnectOptions() {
        // No initialization required.
    }

    /**
     * Current browser ws endpoint.
     */
    private URI browserWSEndpoint;
    /**
     * Current browser URL.
     */
    private URI browserURL;
    /**
     * Current transport.
     */
    private Transport transport;
    /**
     * Current protocol.
     */
    private String protocol = PROTOCOL_CDP;

    /**
     * Whether the protocol was explicitly configured by the caller.
     */
    private boolean protocolConfigured;
    /**
     * Current channel.
     */
    private String channel;
    /**
     * Registered allowlist values.
     */
    private List<String> allowlist = List.of();
    /**
     * Registered blocklist values.
     */
    private List<String> blocklist = List.of();
    /**
     * Mapped headers values.
     */
    private Map<String, String> headers = new LinkedHashMap<>();
    /**
     * Current default viewport.
     */
    private Viewport defaultViewport = new Viewport();
    /**
     * Current download behavior.
     */
    private DownloadBehavior downloadBehavior;
    /**
     * Current slow mo millis.
     */
    private long slowMoMillis;
    /**
     * Current target filter.
     */
    private Predicate<Target> targetFilter;
    /**
     * Current is page target.
     */
    private Predicate<Target> isPageTarget;
    /**
     * Whether handle dev tools as page is enabled.
     */
    private boolean handleDevToolsAsPage;
    /**
     * Whether accept insecure certs is enabled.
     */
    private boolean acceptInsecureCerts;
    /**
     * Whether network is enabled.
     */
    private boolean networkEnabled = true;
    /**
     * Whether issues is enabled.
     */
    private boolean issuesEnabled = true;
    /**
     * Runtime security policy.
     */
    private SecurityPolicy securityPolicy;
    /**
     * Runtime resource limits.
     */
    private ResourceLimits resourceLimits;
    /**
     * Whether render security boundary is enabled.
     */
    private boolean renderSecurityBoundaryEnabled = true;
    /**
     * Current protocol timeout millis.
     */
    private long protocolTimeoutMillis = Builder.DEFAULT_TIMEOUT_MILLIS;
    /**
     * Current ID generator.
     */
    private IntSupplier idGenerator;
    /**
     * Current capabilities.
     */
    private SupportedWebDriverCapabilities capabilities = new SupportedWebDriverCapabilities();

    /**
     * Returns the browser ws endpoint.
     *
     * @return browser ws endpoint
     */
    public URI getBrowserWSEndpoint() {
        return browserWSEndpoint;
    }

    /**
     * Updates browser ws endpoint.
     *
     * @param browserWSEndpoint browser ws endpoint value
     */
    public void setBrowserWSEndpoint(URI browserWSEndpoint) {
        this.browserWSEndpoint = browserWSEndpoint;
    }

    /**
     * Updates browser ws endpoint.
     *
     * @param browserWSEndpoint browser ws endpoint value
     */
    public void setBrowserWSEndpoint(String browserWSEndpoint) {
        this.browserWSEndpoint = browserWSEndpoint == null ? null : URI.create(browserWSEndpoint);
    }

    /**
     * Returns the browser URL.
     *
     * @return browser URL
     */
    public URI getBrowserURL() {
        return browserURL;
    }

    /**
     * Updates browser URL.
     *
     * @param browserURL browser url
     */
    public void setBrowserURL(URI browserURL) {
        this.browserURL = browserURL;
    }

    /**
     * Updates browser URL.
     *
     * @param browserURL browser url
     */
    public void setBrowserURL(String browserURL) {
        this.browserURL = browserURL == null ? null : URI.create(browserURL);
    }

    /**
     * Returns the transport.
     *
     * @return transport
     */
    public Transport getTransport() {
        return transport;
    }

    /**
     * Updates transport.
     *
     * @param transport transport value
     */
    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    /**
     * Returns the protocol.
     *
     * @return protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Returns whether a protocol endpoint has been configured.
     *
     * @return {@code true} when the caller set the protocol
     */
    public boolean isProtocolConfigured() {
        return protocolConfigured;
    }

    /**
     * Updates protocol.
     *
     * @param protocol protocol value
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
        this.protocolConfigured = true;
    }

    /**
     * Returns the channel.
     *
     * @return channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Updates channel.
     *
     * @param channel channel value
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the allowlist.
     *
     * @return values
     */
    public List<String> getAllowlist() {
        return allowlist;
    }

    /**
     * Updates allowlist.
     *
     * @param allowlist allowlist value
     */
    public void setAllowlist(List<String> allowlist) {
        this.allowlist = allowlist == null ? List.of() : List.copyOf(allowlist);
    }

    /**
     * Returns the blocklist.
     *
     * @return values
     */
    public List<String> getBlocklist() {
        return blocklist;
    }

    /**
     * Updates blocklist.
     *
     * @param blocklist blocklist value
     */
    public void setBlocklist(List<String> blocklist) {
        this.blocklist = blocklist == null ? List.of() : List.copyOf(blocklist);
    }

    /**
     * Returns the headers.
     *
     * @return mapped values
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Updates headers.
     *
     * @param headers HTTP headers
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }

    /**
     * Returns the default viewport.
     *
     * @return default viewport
     */
    public Viewport getDefaultViewport() {
        return defaultViewport;
    }

    /**
     * Updates default viewport.
     *
     * @param defaultViewport default viewport value
     */
    public void setDefaultViewport(Viewport defaultViewport) {
        this.defaultViewport = defaultViewport;
    }

    /**
     * Returns the download behavior.
     *
     * @return download behavior
     */
    public DownloadBehavior getDownloadBehavior() {
        return downloadBehavior;
    }

    /**
     * Updates download behavior.
     *
     * @param downloadBehavior download behavior value
     */
    public void setDownloadBehavior(DownloadBehavior downloadBehavior) {
        this.downloadBehavior = downloadBehavior;
    }

    /**
     * Returns the slow mo millis.
     *
     * @return slow mo millis
     */
    public long getSlowMoMillis() {
        return slowMoMillis;
    }

    /**
     * Updates slow mo millis.
     *
     * @param slowMoMillis slow mo millis value
     */
    public void setSlowMoMillis(long slowMoMillis) {
        this.slowMoMillis = Math.max(0L, slowMoMillis);
    }

    /**
     * Returns the target filter.
     *
     * @return target filter
     */
    public Predicate<Target> getTargetFilter() {
        return targetFilter;
    }

    /**
     * Updates target filter.
     *
     * @param targetFilter target filter value
     */
    public void setTargetFilter(Predicate<Target> targetFilter) {
        this.targetFilter = targetFilter;
    }

    /**
     * Returns the allows target.
     *
     * @param target target object
     * @return {@code true} when the condition matches
     */
    public boolean allowsTarget(Target target) {
        return targetFilter == null || targetFilter.test(target);
    }

    /**
     * Returns the is page target.
     *
     * @return is page target
     */
    public Predicate<Target> getIsPageTarget() {
        return isPageTarget;
    }

    /**
     * Updates is page target.
     *
     * @param isPageTarget is page target value
     */
    public void setIsPageTarget(Predicate<Target> isPageTarget) {
        this.isPageTarget = isPageTarget;
    }

    /**
     * Returns whether this target represents a page.
     *
     * @param target target object
     * @return {@code true} when the condition matches
     */
    public boolean isPageTarget(Target target) {
        if (target == null) {
            return false;
        }
        if (isPageTarget != null) {
            return isPageTarget.test(target);
        }
        TargetType type = target.type();
        return type == TargetType.PAGE || type == TargetType.BACKGROUND_PAGE || type == TargetType.WEBVIEW;
    }

    /**
     * Returns whether handle dev tools as page is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isHandleDevToolsAsPage() {
        return handleDevToolsAsPage;
    }

    /**
     * Updates handle dev tools as page.
     *
     * @param handleDevToolsAsPage handle dev tools as page value
     */
    public void setHandleDevToolsAsPage(boolean handleDevToolsAsPage) {
        this.handleDevToolsAsPage = handleDevToolsAsPage;
    }

    /**
     * Returns whether accept insecure certs is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isAcceptInsecureCerts() {
        return acceptInsecureCerts;
    }

    /**
     * Updates accept insecure certs.
     *
     * @param acceptInsecureCerts accept insecure certs value
     */
    public void setAcceptInsecureCerts(boolean acceptInsecureCerts) {
        this.acceptInsecureCerts = acceptInsecureCerts;
    }

    /**
     * Returns whether network tracking is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isNetworkEnabled() {
        return networkEnabled;
    }

    /**
     * Updates network enabled.
     *
     * @param networkEnabled network enabled value
     */
    public void setNetworkEnabled(boolean networkEnabled) {
        this.networkEnabled = networkEnabled;
    }

    /**
     * Returns whether issue tracking is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isIssuesEnabled() {
        return issuesEnabled;
    }

    /**
     * Updates issues enabled.
     *
     * @param issuesEnabled issues enabled value
     */
    public void setIssuesEnabled(boolean issuesEnabled) {
        this.issuesEnabled = issuesEnabled;
    }

    /**
     * Returns security policy.
     *
     * @return security policy
     */
    public SecurityPolicy getSecurityPolicy() {
        return securityPolicy == null ? SecurityPolicy.defaultPolicy() : securityPolicy;
    }

    /**
     * Updates security policy.
     *
     * @param securityPolicy security policy value
     */
    public void setSecurityPolicy(SecurityPolicy securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    /**
     * Returns resource limits.
     *
     * @return resource limits
     */
    public ResourceLimits getResourceLimits() {
        return resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
    }

    /**
     * Updates resource limits.
     *
     * @param resourceLimits resource limits value
     */
    public void setResourceLimits(ResourceLimits resourceLimits) {
        this.resourceLimits = resourceLimits;
    }

    /**
     * Returns whether render security boundary is enabled.
     *
     * @return {@code true} when enabled
     */
    public boolean isRenderSecurityBoundaryEnabled() {
        return renderSecurityBoundaryEnabled;
    }

    /**
     * Updates render security boundary.
     *
     * @param renderSecurityBoundaryEnabled render security boundary enabled value
     */
    public void setRenderSecurityBoundaryEnabled(boolean renderSecurityBoundaryEnabled) {
        this.renderSecurityBoundaryEnabled = renderSecurityBoundaryEnabled;
    }

    /**
     * Returns the protocol timeout millis.
     *
     * @return protocol timeout millis
     */
    public long getProtocolTimeoutMillis() {
        return protocolTimeoutMillis;
    }

    /**
     * Returns the protocol timeout.
     *
     * @return protocol timeout
     */
    public long getProtocolTimeout() {
        return protocolTimeoutMillis;
    }

    /**
     * Updates protocol timeout millis.
     *
     * @param protocolTimeoutMillis protocol timeout millis value
     */
    public void setProtocolTimeoutMillis(long protocolTimeoutMillis) {
        this.protocolTimeoutMillis = protocolTimeoutMillis;
    }

    /**
     * Updates protocol timeout.
     *
     * @param protocolTimeout protocol timeout value
     */
    public void setProtocolTimeout(long protocolTimeout) {
        this.protocolTimeoutMillis = protocolTimeout;
    }

    /**
     * Returns the ID generator.
     *
     * @return ID generator
     */
    public IntSupplier getIdGenerator() {
        return idGenerator;
    }

    /**
     * Updates ID generator.
     *
     * @param idGenerator id generator
     */
    public void setIdGenerator(IntSupplier idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * Returns the capabilities.
     *
     * @return capabilities
     */
    public SupportedWebDriverCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Updates capabilities.
     *
     * @param capabilities capabilities value
     */
    public void setCapabilities(SupportedWebDriverCapabilities capabilities) {
        this.capabilities = capabilities == null ? new SupportedWebDriverCapabilities() : capabilities;
    }

    /**
     * Represents supported web driver capabilities.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static class SupportedWebDriverCapabilities {

        /**
         * Creates an instance.
         */
        public SupportedWebDriverCapabilities() {
            // No initialization required.
        }

        /**
         * Mapped first match values.
         */
        private List<Map<String, Object>> firstMatch = List.of();
        /**
         * Mapped always match values.
         */
        private Map<String, Object> alwaysMatch = new LinkedHashMap<>();

        /**
         * Returns the first match.
         *
         * @return values
         */
        public List<Map<String, Object>> getFirstMatch() {
            return firstMatch;
        }

        /**
         * Updates first match.
         *
         * @param firstMatch first match value
         */
        public void setFirstMatch(List<Map<String, Object>> firstMatch) {
            if (firstMatch == null || firstMatch.isEmpty()) {
                this.firstMatch = List.of();
                return;
            }
            this.firstMatch = firstMatch.stream()
                    .map(
                            capability -> capability == null ? Map.<String, Object>of()
                                    : Collections.unmodifiableMap(new LinkedHashMap<>(capability)))
                    .toList();
        }

        /**
         * Returns the always match.
         *
         * @return mapped values
         */
        public Map<String, Object> getAlwaysMatch() {
            return Collections.unmodifiableMap(alwaysMatch);
        }

        /**
         * Updates always match.
         *
         * @param alwaysMatch always match value
         */
        public void setAlwaysMatch(Map<String, Object> alwaysMatch) {
            this.alwaysMatch = alwaysMatch == null ? new LinkedHashMap<>() : new LinkedHashMap<>(alwaysMatch);
        }

        /**
         * Converts this value to protocol parameters.
         *
         * @return protocol parameters
         */
        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            if (!firstMatch.isEmpty()) {
                result.put("firstMatch", firstMatch);
            }
            if (!alwaysMatch.isEmpty()) {
                result.put("alwaysMatch", alwaysMatch);
            }
            return result;
        }
    }

}
