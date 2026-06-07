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
package org.miaixz.lancia.runtime;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

import org.miaixz.bus.core.basic.normal.ErrorCode;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.ForbiddenException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.UrlKit;

/**
 * Validates external URLs and redacts sensitive values before logging.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class SecurityPolicy {

    /**
     * Allowed HTTP schemes.
     */
    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");
    /**
     * Allowed WebSocket schemes.
     */
    private static final Set<String> WEBSOCKET_SCHEMES = Set.of("ws", "wss");
    /**
     * Allowed navigation schemes.
     */
    private static final Set<String> NAVIGATION_SCHEMES = Set.of("http", "https", "about", "data");
    /**
     * Allowed render request schemes.
     */
    private static final Set<String> RENDER_REQUEST_SCHEMES = Set
            .of("http", "https", "ws", "wss", "about", "data", "blob");
    /**
     * Shared constant for redacted.
     */
    private static final String REDACTED = Symbol.BRACKET_LEFT + "REDACTED" + Symbol.BRACKET_RIGHT;
    /**
     * Registered allowed schemes values.
     */
    private final Set<String> allowedSchemes;
    /**
     * Whether allow private network is enabled.
     */
    private boolean allowPrivateNetwork;

    /**
     * Creates a security policy.
     *
     * @param allowedSchemes allowed schemes
     */
    public SecurityPolicy(Set<String> allowedSchemes) {
        this.allowedSchemes = Set.copyOf(allowedSchemes);
    }

    /**
     * Returns the default policy.
     *
     * @return default policy value
     */
    public static SecurityPolicy defaultPolicy() {
        return new SecurityPolicy(Set.of("http", "https", "ws", "wss", "about", "data"));
    }

    /**
     * Returns the allow private network.
     *
     * @param allowPrivateNetwork allow private network value
     * @return allow private network value
     */
    public SecurityPolicy allowPrivateNetwork(boolean allowPrivateNetwork) {
        this.allowPrivateNetwork = allowPrivateNetwork;
        return this;
    }

    /**
     * Validates URL.
     *
     * @param url target URL
     */
    public void validateUrl(String url) {
        validateUrl(UrlKit.toURI(url));
    }

    /**
     * Validates URL.
     *
     * @param uri target URL
     */
    public void validateUrl(URI uri) {
        validateUri(uri, allowedSchemes);
    }

    /**
     * Validates HTTP URL.
     *
     * @param uri target URL
     */
    public void validateHttpUrl(URI uri) {
        validateUri(uri, HTTP_SCHEMES);
    }

    /**
     * Validates WebSocket URL.
     *
     * @param uri target URL
     */
    public void validateWebSocketUrl(URI uri) {
        validateUri(uri, WEBSOCKET_SCHEMES);
    }

    /**
     * Validates a navigation URL.
     *
     * @param uri target URL
     */
    public void validateNavigationUrl(URI uri) {
        validateUri(uri, NAVIGATION_SCHEMES);
    }

    /**
     * Validates a render-time request URL.
     *
     * @param uri target URL
     */
    public void validateRenderRequestUrl(URI uri) {
        validateUri(uri, RENDER_REQUEST_SCHEMES);
    }

    /**
     * Returns whether the URL is allowed by the configured policy.
     *
     * @param url target URL
     * @return {@code true} when the URL is accepted
     */
    public boolean isAllowed(String url) {
        try {
            validateUrl(url);
            return true;
        } catch (ForbiddenException ex) {
            return false;
        }
    }

    /**
     * Sanitizes URL.
     *
     * @param url target URL
     * @return sanitized URL
     */
    public String sanitizeUrl(String url) {
        try {
            return sanitizeUrl(UrlKit.toURI(url));
        } catch (Exception ex) {
            return REDACTED;
        }
    }

    /**
     * Sanitizes URL.
     *
     * @param uri target URL
     * @return sanitized URL
     */
    public String sanitizeUrl(URI uri) {
        try {
            URI actualUri = uri == null ? URI.create(Normal.EMPTY) : uri;
            String authority = actualUri.getRawAuthority();
            if (authority != null && authority.contains(Symbol.AT)) {
                authority = REDACTED + Symbol.AT + authority.substring(authority.indexOf(Symbol.C_AT) + 1);
            }
            return new URI(actualUri.getScheme(), authority, actualUri.getPath(),
                    actualUri.getRawQuery() == null ? null : REDACTED, actualUri.getFragment()).toString();
        } catch (Exception ex) {
            return REDACTED;
        }
    }

    /**
     * Sanitizes log value.
     *
     * @param name  name to use
     * @param value to use
     * @return sanitized log value
     */
    public String sanitizeLogValue(String name, String value) {
        String key = normalize(name);
        if (key.contains("authorization") || key.contains("cookie") || key.contains("token")
                || key.contains("password")) {
            return REDACTED;
        }
        if (key.contains("url") || key.contains("endpoint")) {
            return sanitizeUrl(value);
        }
        return value;
    }

    /**
     * Returns whether allow private network is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isAllowPrivateNetwork() {
        return allowPrivateNetwork;
    }

    /**
     * Returns whether private host is enabled.
     *
     * @param host host value
     * @return {@code true} when the condition matches
     */
    private boolean isPrivateHost(String host) {
        String value = normalize(host);
        if (StringKit.isBlank(value)) {
            return false;
        }
        if ("localhost".equals(value) || value.endsWith(".localhost") || "::1".equals(value)
                || "0:0:0:0:0:0:0:1".equals(value)) {
            return true;
        }
        if (value.startsWith("10.") || value.startsWith("127.") || value.startsWith("169.254.")) {
            return true;
        }
        if (value.startsWith("192.168.")) {
            return true;
        }
        if (value.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            return true;
        }
        return value.startsWith("fc") || value.startsWith("fd") || value.startsWith("fe80:");
    }

    /**
     * Validates URL with allowed schemes.
     *
     * @param uri     target URL
     * @param schemes allowed schemes
     */
    private void validateUri(URI uri, Set<String> schemes) {
        if (uri == null) {
            throw new ForbiddenException(ErrorCode._BLOCKED, "URL rejected by security policy: " + REDACTED);
        }
        String scheme = normalize(uri.getScheme());
        if (!schemes.contains(scheme)) {
            throw new ForbiddenException(ErrorCode._BLOCKED, "URL scheme rejected by security policy: " + scheme);
        }
        if (!allowPrivateNetwork && isPrivateHost(uri.getHost())) {
            throw new ForbiddenException(ErrorCode._100903,
                    "URL host rejected by SSRF security policy: " + sanitizeUrl(uri));
        }
    }

    /**
     * Returns the normalize.
     *
     * @param value to use
     * @return normalize value
     */
    private String normalize(String value) {
        return value == null ? Normal.EMPTY : value.toLowerCase(Locale.ROOT);
    }

}
