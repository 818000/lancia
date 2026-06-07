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
package org.miaixz.lancia.kernel.bidi.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.network.SecurityDetails;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.payload.PayloadSecurityDetails;

/**
 * Represents a bidi response.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiResponse extends Response {

    /**
     * Current data.
     */
    private CdpPayload data;
    /**
     * Current request.
     */
    private final BidiRequest request;
    /**
     * Whether CDP supported is enabled.
     */
    private final boolean cdpSupported;
    /**
     * Current security details.
     */
    private SecurityDetails securityDetails;

    /**
     * Returns the from.
     *
     * @param data         data to use
     * @param request      request object
     * @param cdpSupported CDP supported value
     * @return from value
     */
    public static BidiResponse from(CdpPayload data, BidiRequest request, boolean cdpSupported) {
        Optional<BidiResponse> existing = request.bidiResponse();
        if (existing.isPresent()) {
            existing.getOrThrow().update(data);
            return existing.getOrThrow();
        }
        BidiResponse response = new BidiResponse(data, request, cdpSupported);
        request.setResponse(response);
        Logger.debug(
                false,
                "Network",
                "BiDi response created: request={}, status={}, url={}",
                request.id(),
                response.status(),
                response.url().replaceAll("[?#].*$", "?<redacted>"));
        return response;
    }

    /**
     * Creates a bidi response.
     *
     * @param data         data to use
     * @param request      request object
     * @param cdpSupported cdp supported
     */
    public BidiResponse(CdpPayload data, BidiRequest request, boolean cdpSupported) {
        this.request = request;
        this.cdpSupported = cdpSupported;
        update(data);
    }

    /**
     * Handles update.
     *
     * @param data data to use
     */
    public void update(CdpPayload data) {
        this.data = normalize(data);
        this.securityDetails = securityDetails(this.data);
        Logger.debug(false, "Network", "BiDi response updated: request={}, status={}", request.id(), status());
    }

    /**
     * Returns the data.
     *
     * @return data value
     */
    public CdpPayload data() {
        return data;
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return PayloadReader.text(data.get("url"));
    }

    /**
     * Returns the status.
     *
     * @return status value
     */
    public int status() {
        return data.get("status").isNull() ? 0 : data.get("status").asInt();
    }

    /**
     * Returns the ok.
     *
     * @return {@code true} when the condition matches
     */
    public boolean ok() {
        int status = status();
        return status == 0 || (status >= 200 && status <= 299);
    }

    /**
     * Returns the status text.
     *
     * @return status text value
     */
    public String statusText() {
        return PayloadReader.text(data.get("statusText"));
    }

    /**
     * Returns the headers.
     *
     * @return mapped values
     */
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers(data.get("headers")));
    }

    /**
     * Returns the request.
     *
     * @return request value
     */
    public BidiRequest request() {
        return request;
    }

    /**
     * Creates this value from cache.
     *
     * @return {@code true} when the condition matches
     */
    public boolean fromCache() {
        return PayloadReader.bool(data.get("fromCache"));
    }

    /**
     * Returns the timing.
     *
     * @return mapped values
     */
    public Map<String, Object> timing() {
        return PayloadReader.object(data.get("timing"));
    }

    /**
     * Returns the frame.
     *
     * @return frame value
     */
    public Optional<? extends Frame> frame() {
        return Optional.of(request.bidiFrame());
    }

    /**
     * Creates this value from service worker.
     *
     * @return {@code true} when the condition matches
     */
    public boolean fromServiceWorker() {
        return false;
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
        return new Response.RemoteAddress(PayloadReader.text(data.get("remoteIPAddress")),
                PayloadReader.number(data.get("remotePort"), -1));
    }

    /**
     * Returns the current HTML content.
     *
     * @return current HTML content
     */
    public byte[] content() {
        return Awaitable.await(contentAsync(), "BiDi response body failed.", 5_000L);
    }

    /**
     * Returns the current content asynchronously.
     *
     * @return current content
     */
    public CompletableFuture<byte[]> contentAsync() {
        Logger.debug(true, "Network", "BiDi response body requested: request={}, status={}", request.id(), status());
        return request.getResponseContent();
    }

    /**
     * Returns the buffer.
     *
     * @return completion future
     */
    public byte[] buffer() {
        return content();
    }

    /**
     * Returns the text.
     *
     * @return completion future
     */
    public String text() {
        return Awaitable.await(textAsync(), "BiDi response text failed.", 5_000L);
    }

    /**
     * Returns the text asynchronously.
     *
     * @return completion future
     */
    public CompletableFuture<String> textAsync() {
        return contentAsync().thenApply(bytes -> {
            String text = StringKit.toString(bytes, Charset.UTF_8);
            Logger.debug(
                    false,
                    "Network",
                    "BiDi response text decoded: request={}, chars={}",
                    request.id(),
                    text.length());
            return text;
        });
    }

    /**
     * Returns the json.
     *
     * @return completion future
     */
    public CdpPayload json() {
        return Awaitable.await(jsonAsync(), "BiDi response JSON failed.", 5_000L);
    }

    /**
     * Returns the json asynchronously.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> jsonAsync() {
        Logger.debug(true, "Network", "BiDi response JSON parse requested: request={}", request.id());
        return textAsync().thenApply(CdpPayload::parse);
    }

    /**
     * Returns the normalize.
     *
     * @param data data to use
     * @return normalize value
     */
    private static CdpPayload normalize(CdpPayload data) {
        CdpPayload payload = data == null ? CdpPayload.NULL : data;
        return payload.get("response").isNull() ? payload : payload.get("response");
    }

    /**
     * Returns the security details.
     *
     * @param payload protocol payload
     * @return security details value
     */
    private static SecurityDetails securityDetails(CdpPayload payload) {
        CdpPayload details = PayloadReader.first(
                CdpPayload.NULL,
                payload.get("goog:securityDetails"),
                payload.get("securityDetails"),
                payload.get("response").get("goog:securityDetails"),
                payload.get("response").get("securityDetails"));
        return PayloadSecurityDetails.from(details);
    }

    /**
     * Returns the headers.
     *
     * @param payload protocol payload
     * @return mapped values
     */
    private static Map<String, String> headers(CdpPayload payload) {
        Map<String, String> result = new LinkedHashMap<>();
        if (payload.isObject()) {
            for (Map.Entry<String, CdpPayload> entry : payload.fields().entrySet()) {
                result.put(entry.getKey().toLowerCase(), PayloadReader.text(entry.getValue()));
            }
        } else if (payload.isArray()) {
            for (CdpPayload header : payload.elements()) {
                String name = PayloadReader.text(header.get("name")).toLowerCase();
                String value = PayloadReader.text(
                        PayloadReader.first(CdpPayload.NULL, header.get("value").get("value"), header.get("value")));
                if (StringKit.isNotBlank(name)) {
                    result.put(name, value);
                }
            }
        }
        return result;
    }

    /**
     * Represents remote address.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class RemoteAddress {

        /**
         * Current ip.
         */
        private final String ip;
        /**
         * Current port.
         */
        private final int port;

        /**
         * Creates an instance.
         *
         * @param ip   ip value
         * @param port port value
         */
        public RemoteAddress(String ip, int port) {
            this.ip = ip == null ? Normal.EMPTY : ip;
            this.port = port;
        }

        /**
         * Returns the ip.
         *
         * @return ip value
         */
        public String ip() {
            return ip;
        }

        /**
         * Returns the port.
         *
         * @return port value
         */
        public int port() {
            return port;
        }
    }

}
