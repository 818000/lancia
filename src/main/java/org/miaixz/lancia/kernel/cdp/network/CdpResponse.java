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
package org.miaixz.lancia.kernel.cdp.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.kernel.cdp.page.CdpFrame;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.nimble.network.SecurityDetails;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.payload.PayloadSecurityDetails;

/**
 * CDP response implementation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpResponse extends Response {

    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Current request.
     */
    private final CdpRequest request;
    /**
     * Current URL.
     */
    private final String url;
    /**
     * Current status.
     */
    private int status;
    /**
     * Current status text.
     */
    private String statusText;
    /**
     * Mapped headers values.
     */
    private final Map<String, String> headers = new LinkedHashMap<>();
    /**
     * Current security details.
     */
    private final SecurityDetails securityDetails;
    /**
     * Current remote address.
     */
    private final Response.RemoteAddress remoteAddress;
    /**
     * Mapped timing values.
     */
    private final Map<String, Object> timing;
    /**
     * Whether from cache is enabled.
     */
    private final boolean fromCache;
    /**
     * Whether from service worker is enabled.
     */
    private final boolean fromServiceWorker;
    /**
     * Current content.
     */
    private byte[] content;
    /**
     * Resource limits.
     */
    private final ResourceLimits resourceLimits;

    /**
     * Creates a CDP response.
     *
     * @param session    protocol session
     * @param request    request object
     * @param url        target URL
     * @param status     status
     * @param statusText status text
     * @param headers    HTTP headers
     */
    public CdpResponse(CDPSession session, CdpRequest request, String url, int status, String statusText,
            Map<String, String> headers) {
        this(session, request, url, status, statusText, headers, null);
    }

    /**
     * Creates a CDP response.
     *
     * @param session         protocol session
     * @param request         request object
     * @param url             target URL
     * @param status          status
     * @param statusText      status text
     * @param headers         HTTP headers
     * @param securityDetails security details
     */
    public CdpResponse(CDPSession session, CdpRequest request, String url, int status, String statusText,
            Map<String, String> headers, SecurityDetails securityDetails) {
        this(session, request, url, status, statusText, headers, securityDetails,
                new Response.RemoteAddress(Normal.EMPTY, -1), Map.of(), false, false);
    }

    /**
     * Creates a CDP response.
     *
     * @param session           protocol session
     * @param request           request object
     * @param url               target URL
     * @param status            status
     * @param statusText        status text
     * @param headers           HTTP headers
     * @param securityDetails   security details
     * @param remoteAddress     remote address
     * @param timing            timing
     * @param fromCache         from cache
     * @param fromServiceWorker from service worker
     */
    public CdpResponse(CDPSession session, CdpRequest request, String url, int status, String statusText,
            Map<String, String> headers, SecurityDetails securityDetails, Response.RemoteAddress remoteAddress,
            Map<String, Object> timing, boolean fromCache, boolean fromServiceWorker) {
        this(session, request, url, status, statusText, headers, securityDetails, remoteAddress, timing, fromCache,
                fromServiceWorker, ResourceLimits.defaults());
    }

    /**
     * Creates a CDP response.
     *
     * @param session           protocol session
     * @param request           request object
     * @param url               target URL
     * @param status            status
     * @param statusText        status text
     * @param headers           HTTP headers
     * @param securityDetails   security details
     * @param remoteAddress     remote address
     * @param timing            timing
     * @param fromCache         from cache
     * @param fromServiceWorker from service worker
     * @param resourceLimits    resource limits
     */
    public CdpResponse(CDPSession session, CdpRequest request, String url, int status, String statusText,
            Map<String, String> headers, SecurityDetails securityDetails, Response.RemoteAddress remoteAddress,
            Map<String, Object> timing, boolean fromCache, boolean fromServiceWorker, ResourceLimits resourceLimits) {
        this.session = session;
        this.request = request;
        this.url = url == null ? Normal.EMPTY : url;
        this.status = status;
        this.statusText = statusText == null ? Normal.EMPTY : statusText;
        this.headers.putAll(CdpHeaders.normalize(headers));
        this.securityDetails = securityDetails;
        this.remoteAddress = remoteAddress == null ? new Response.RemoteAddress(Normal.EMPTY, -1) : remoteAddress;
        this.timing = Collections.unmodifiableMap(new LinkedHashMap<>(timing == null ? Map.of() : timing));
        this.fromCache = fromCache;
        this.fromServiceWorker = fromServiceWorker;
        this.resourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
    }

    /**
     * Returns the from.
     *
     * @param session protocol session
     * @param request request object
     * @param params  protocol parameters
     * @return from value
     */
    public static CdpResponse from(CDPSession session, CdpRequest request, CdpPayload params) {
        return from(session, request, params, null);
    }

    /**
     * Returns the from.
     *
     * @param session   protocol session
     * @param request   request object
     * @param params    protocol parameters
     * @param extraInfo extra info value
     * @return from value
     */
    public static CdpResponse from(CDPSession session, CdpRequest request, CdpPayload params, CdpPayload extraInfo) {
        CdpPayload response = params.get("response");
        CdpPayload headerSource = extraInfo == null || extraInfo.isNull() ? response.get("headers")
                : extraInfo.get("headers");
        int status = extraInfo == null || extraInfo.get("statusCode").isNull() ? response.get("status").asInt()
                : extraInfo.get("statusCode").asInt();
        String statusText = parseStatusTextFromExtraInfo(extraInfo);
        if (StringKit.isBlank(statusText)) {
            statusText = PayloadReader.text(response.get("statusText"));
        }
        return new CdpResponse(session, request, PayloadReader.text(response.get("url")), status, statusText,
                CdpRequest.headers(headerSource), PayloadSecurityDetails.from(response.get("securityDetails")),
                new Response.RemoteAddress(PayloadReader.text(response.get("remoteIPAddress")),
                        PayloadReader.number(response.get("remotePort"), -1)),
                PayloadReader.object(response.get("timing")),
                PayloadReader.bool(response.get("fromDiskCache"))
                        || PayloadReader.bool(response.get("fromPrefetchCache")),
                PayloadReader.bool(response.get("fromServiceWorker")));
    }

    /**
     * Returns the status.
     *
     * @return status value
     */
    public int status() {
        return status;
    }

    /**
     * Returns the ok.
     *
     * @return {@code true} when the condition matches
     */
    public boolean ok() {
        return status == 0 || (status >= 200 && status <= 299);
    }

    /**
     * Returns the headers.
     *
     * @return mapped values
     */
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Returns the buffer.
     *
     * @return buffer value
     */
    public byte[] buffer() {
        if (content != null) {
            return content.clone();
        }
        try {
            CDPSession actualSession = request == null || request.client() == null ? session : request.client();
            String requestId = request == null ? Normal.EMPTY : request.requestId();
            Logger.debug(
                    true,
                    "Network",
                    "Response body read requested: id={}, status={}, url={}",
                    requestId,
                    status,
                    StringKit.isBlank(url) ? Normal.EMPTY
                            : url.contains("?") ? url.substring(Normal._0, url.indexOf('?')) : url);
            CdpPayload result = actualSession.send("Network.getResponseBody", Map.of("requestId", requestId))
                    .get(5, TimeUnit.SECONDS);
            String body = PayloadReader.text(result.get("body"));
            boolean encoded = !result.get("base64Encoded").isNull() && result.get("base64Encoded").asBoolean();
            resourceLimits.validateResponseBodyBytes(ByteKit.toBytes(body, Charset.UTF_8).length);
            content = encoded ? Base64.decode(body) : ByteKit.toBytes(body, Charset.UTF_8);
            resourceLimits.validateResponseBodyBytes(content.length);
            Logger.debug(
                    false,
                    "Network",
                    "Response body read completed: id={}, bytes={}, encoded={}",
                    requestId,
                    content.length,
                    encoded);
            return content.clone();
        } catch (Exception ex) {
            if (containsMessage(ex, "No resource with given identifier found")) {
                Logger.warn(
                        false,
                        "Network",
                        ex,
                        "Response body is unavailable: id={}, url={}",
                        request == null ? Normal.EMPTY : request.requestId(),
                        StringKit.isBlank(url) ? Normal.EMPTY
                                : url.contains("?") ? url.substring(Normal._0, url.indexOf('?')) : url);
                throw new InternalException(
                        "Could not load response body for this request. This might happen if the request is a preflight request.",
                        ex);
            }
            Logger.error(
                    false,
                    "Network",
                    ex,
                    "Response body read failed: id={}, url={}",
                    request == null ? Normal.EMPTY : request.requestId(),
                    StringKit.isBlank(url) ? Normal.EMPTY
                            : url.contains("?") ? url.substring(Normal._0, url.indexOf('?')) : url);
            throw new InternalException(
                    "Failed to read response body: " + (request == null ? Normal.EMPTY : request.requestId()), ex);
        }
    }

    /**
     * Returns the current HTML content.
     *
     * @return current HTML content
     */
    public byte[] content() {
        return buffer();
    }

    /**
     * Returns the text.
     *
     * @return text value
     */
    public String text() {
        return StringKit.toString(buffer(), Charset.UTF_8);
    }

    /**
     * Returns the json.
     *
     * @return JSON value
     */
    public CdpPayload json() {
        return CdpPayload.parse(text());
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return url;
    }

    /**
     * Returns the status text.
     *
     * @return status text value
     */
    public String statusText() {
        return statusText;
    }

    /**
     * Returns the request.
     *
     * @return request value
     */
    public CdpRequest request() {
        return request;
    }

    /**
     * Returns the frame.
     *
     * @return optional value
     */
    public Optional<CdpFrame> frame() {
        return request == null ? Optional.empty() : request.frame();
    }

    /**
     * Handles merge extra info.
     *
     * @param extraInfo extra info value
     */
    void mergeExtraInfo(CdpPayload extraInfo) {
        if (extraInfo == null || extraInfo.isNull()) {
            return;
        }
        if (!extraInfo.get("statusCode").isNull()) {
            this.status = extraInfo.get("statusCode").asInt();
        }
        String parsedStatusText = parseStatusTextFromExtraInfo(extraInfo);
        if (StringKit.isNotBlank(parsedStatusText)) {
            this.statusText = parsedStatusText;
        }
        headers.putAll(CdpRequest.headers(extraInfo.get("headers")));
    }

    /**
     * Returns the header.
     *
     * @param name name to use
     * @return optional value
     */
    public Optional<String> header(String name) {
        return Optional.ofNullable(StringKit.isBlank(name) ? null : headers.get(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns the security details.
     *
     * @return optional value
     */
    public Optional<SecurityDetails> securityDetails() {
        return Optional.ofNullable(securityDetails);
    }

    /**
     * Returns the remote address.
     *
     * @return remote address value
     */
    public Response.RemoteAddress remoteAddress() {
        return remoteAddress;
    }

    /**
     * Returns the timing.
     *
     * @return mapped values
     */
    public Map<String, Object> timing() {
        return timing;
    }

    /**
     * Creates this value from cache.
     *
     * @return {@code true} when the condition matches
     */
    public boolean fromCache() {
        return fromCache || (request != null && request.fromMemoryCache());
    }

    /**
     * Creates this value from service worker.
     *
     * @return {@code true} when the condition matches
     */
    public boolean fromServiceWorker() {
        return fromServiceWorker;
    }

    /**
     * Parses status text from extra info.
     *
     * @param extraInfo extra info value
     * @return parse status text from extra info value
     */
    private static String parseStatusTextFromExtraInfo(CdpPayload extraInfo) {
        if (extraInfo == null || extraInfo.isNull()) {
            return Normal.EMPTY;
        }
        String headersText = PayloadReader.nullableText(extraInfo.get("headersText"));
        if (StringKit.isBlank(headersText)) {
            return Normal.EMPTY;
        }
        int lineEnd = headersText.indexOf(Symbol.C_CR);
        String firstLine = lineEnd < 0 ? headersText : headersText.substring(0, lineEnd);
        if (StringKit.isBlank(firstLine) || firstLine.length() > 1000) {
            return Normal.EMPTY;
        }
        String[] parts = firstLine.split(Symbol.SPACE, 3);
        return parts.length == 3 ? parts[2] : Normal.EMPTY;
    }

    /**
     * Returns the contains message.
     *
     * @param throwable throwable value
     * @param message   message text
     * @return {@code true} when the condition matches
     */
    private static boolean containsMessage(Throwable throwable, String message) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
