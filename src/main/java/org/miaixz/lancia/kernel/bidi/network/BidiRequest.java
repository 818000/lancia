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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Request;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.page.BidiFrame;
import org.miaixz.lancia.kernel.bidi.session.BidiCDPSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a bidi request.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiRequest extends Request {

    /**
     * Shared constant for status texts.
     */
    private static final Map<Integer, String> STATUS_TEXTS = Map.ofEntries(
            Map.entry(200, "OK"),
            Map.entry(201, "Created"),
            Map.entry(204, "No Content"),
            Map.entry(301, "Moved Permanently"),
            Map.entry(302, "Found"),
            Map.entry(304, "Not Modified"),
            Map.entry(400, "Bad Request"),
            Map.entry(401, "Unauthorized"),
            Map.entry(403, "Forbidden"),
            Map.entry(404, "Not Found"),
            Map.entry(500, "Internal Server Error"));
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current frame.
     */
    private final BidiFrame frame;
    /**
     * Current URL.
     */
    private final String url;
    /**
     * Current method.
     */
    private final String method;
    /**
     * Mapped headers values.
     */
    private final Map<String, String> headers;
    /**
     * Current post data.
     */
    private final String postData;
    /**
     * Current resource type.
     */
    private final String resourceType;
    /**
     * Whether navigation request is enabled.
     */
    private final boolean navigationRequest;
    /**
     * Mapped initiator values.
     */
    private final Map<String, Object> initiator;
    /**
     * Registered redirect chain values.
     */
    private final List<BidiRequest> redirectChain;
    /**
     * Whether blocked is enabled.
     */
    private final boolean blocked;
    /**
     * Thread-safe interception handled state.
     */
    private final AtomicBoolean interceptionHandled = new AtomicBoolean(false);
    /**
     * Current failure text.
     */
    private volatile String failureText;
    /**
     * Current response payload.
     */
    private volatile CdpPayload responsePayload = CdpPayload.NULL;
    /**
     * Current response.
     */
    private volatile BidiResponse response;

    /**
     * Returns the from.
     *
     * @param payload             protocol payload
     * @param frame               frame instance
     * @param interceptionEnabled interception enabled value
     * @param redirect            redirect value
     * @return from value
     */
    public static BidiRequest from(
            CdpPayload payload,
            BidiFrame frame,
            boolean interceptionEnabled,
            BidiRequest redirect) {
        CdpPayload request = payload.get("request").isNull() ? payload : payload.get("request");
        List<BidiRequest> redirects = new ArrayList<>();
        if (redirect != null) {
            redirects.addAll(redirect.redirectChain());
            redirects.add(redirect);
        }
        return new BidiRequest(PayloadReader.text(
                PayloadReader
                        .first(CdpPayload.NULL, request.get("request"), payload.get("requestId"), payload.get("id"))),
                frame, PayloadReader.text(PayloadReader.first(CdpPayload.NULL, request.get("url"), payload.get("url"))),
                PayloadReader.nonBlankText(
                        PayloadReader.first(CdpPayload.NULL, request.get("method"), payload.get("method")),
                        "GET"),
                headers(PayloadReader.first(CdpPayload.NULL, request.get("headers"), payload.get("headers"))),
                postData(
                        PayloadReader.first(
                                CdpPayload.NULL,
                                request.get("body"),
                                request.get("postData"),
                                payload.get("postData"))),
                PayloadReader.nonBlankText(
                        PayloadReader.first(CdpPayload.NULL, payload.get("resourceType"), request.get("destination")),
                        "other").toLowerCase(),
                !payload.get("navigation").isNull(), PayloadReader.object(payload.get("initiator")), redirects,
                interceptionEnabled || PayloadReader.bool(payload.get("isBlocked"))
                        || PayloadReader.bool(payload.get("blocked")));
    }

    /**
     * Creates a bidi request.
     *
     * @param id                identifier
     * @param frame             frame instance
     * @param url               target URL
     * @param method            protocol method
     * @param headers           HTTP headers
     * @param postData          post data
     * @param resourceType      resource type
     * @param navigationRequest navigation request
     * @param initiator         initiator
     * @param redirectChain     redirect chain
     * @param blocked           blocked
     */
    public BidiRequest(String id, BidiFrame frame, String url, String method, Map<String, String> headers,
            String postData, String resourceType, boolean navigationRequest, Map<String, Object> initiator,
            List<BidiRequest> redirectChain, boolean blocked) {
        this.id = Assert.notBlank(id, "id");
        this.frame = Assert.notNull(frame, "frame");
        this.url = url == null ? Normal.EMPTY : url;
        this.method = StringKit.isBlank(method) ? "GET" : method;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers == null ? Map.of() : headers));
        this.postData = postData;
        this.resourceType = StringKit.isBlank(resourceType) ? "other" : resourceType;
        this.navigationRequest = navigationRequest;
        this.initiator = Collections.unmodifiableMap(new LinkedHashMap<>(initiator == null ? Map.of() : initiator));
        this.redirectChain = Collections
                .unmodifiableList(new ArrayList<>(redirectChain == null ? List.of() : redirectChain));
        this.blocked = blocked;
        Logger.debug(
                false,
                "Network",
                "BiDi request created: id={}, method={}, resource={}, blocked={}, url={}",
                id,
                this.method,
                this.resourceType,
                blocked,
                this.url.replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Returns the continue request.
     *
     * @param overrides overrides value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> continueRequest(Map<String, Object> overrides) {
        verifyInterception();
        Logger.debug(
                true,
                "Network",
                "BiDi request continue requested: id={}, overrides={}",
                id,
                overrides == null ? Normal._0 : overrides.size());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("request", id);
        Map<String, Object> actualOverrides = overrides == null ? Map.of() : overrides;
        copyIfPresent(actualOverrides, params, "url");
        copyIfPresent(actualOverrides, params, "method");
        if (actualOverrides.containsKey("postData")) {
            params.put("body", body(String.valueOf(actualOverrides.get("postData"))));
        }
        Object headerOverrides = actualOverrides.get("headers");
        if (headerOverrides instanceof Map<?, ?> headerMap) {
            params.put("headers", headerEntries(headerMap));
        }
        return session().send("network.continueRequest", params);
    }

    /**
     * Returns the respond.
     *
     * @param status  status value
     * @param headers HTTP headers
     * @param body    body value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> respond(int status, Map<String, String> headers, byte[] body) {
        verifyInterception();
        byte[] actualBody = body == null ? new byte[0] : body;
        Logger.debug(
                true,
                "Network",
                "BiDi request respond requested: id={}, status={}, bytes={}",
                id,
                status,
                actualBody.length);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("request", id);
        params.put("statusCode", status <= 0 ? 200 : status);
        params.put("reasonPhrase", STATUS_TEXTS.getOrDefault(status <= 0 ? 200 : status, Normal.EMPTY));
        params.put("headers", headerEntries(headers));
        params.put("body", Map.of("type", "base64", "value", Base64.encode(actualBody)));
        return session().send("network.provideResponse", params);
    }

    /**
     * Responds to an intercepted request.
     *
     * @param response response data
     * @return protocol result
     */
    public CompletableFuture<CdpPayload> respond(Map<String, Object> response) {
        Map<String, Object> data = response == null ? Map.of() : response;
        Object body = data.get("body");
        byte[] bytes = body instanceof byte[] value ? value : ByteKit.toBytes(String.valueOf(body), Charset.UTF_8);
        return respond(data.get("status") instanceof Number status ? status.intValue() : 200, Map.of(), bytes);
    }

    /**
     * Returns the abort.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> abort() {
        verifyInterception();
        Logger.debug(true, "Network", "BiDi request abort requested: id={}", id);
        return session().send("network.failRequest", Map.of("request", id));
    }

    /**
     * Aborts an intercepted request.
     *
     * @param errorCode error code
     * @return protocol result
     */
    public CompletableFuture<CdpPayload> abort(String errorCode) {
        return abort();
    }

    /**
     * Returns the continue with auth.
     *
     * @param username username value
     * @param password password value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> continueWithAuth(String username, String password) {
        verifyInterception();
        Logger.debug(
                true,
                "Network",
                "BiDi request auth continue requested: id={}, hasUsername={}",
                id,
                StringKit.isNotBlank(username));
        return session().send(
                "network.continueWithAuth",
                Map.of(
                        "request",
                        id,
                        "action",
                        "provideCredentials",
                        "credentials",
                        Map.of("type", "password", "username", username, "password", password)));
    }

    /**
     * Returns whether cel auth is available.
     *
     * @return {@code true} when the condition matches
     */
    public CompletableFuture<CdpPayload> cancelAuth() {
        verifyInterception();
        Logger.debug(true, "Network", "BiDi request auth cancel requested: id={}", id);
        return session().send("network.continueWithAuth", Map.of("request", id, "action", "cancel"));
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
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
     * Returns the method.
     *
     * @return method value
     */
    public String method() {
        return method;
    }

    /**
     * Returns the headers.
     *
     * @return mapped values
     */
    public Map<String, String> headers() {
        return headers;
    }

    /**
     * Returns the resource type.
     *
     * @return resource type value
     */
    public String resourceType() {
        return resourceType;
    }

    /**
     * Returns the post data.
     *
     * @return optional value
     */
    public Optional<String> postData() {
        return Optional.ofNullable(postData);
    }

    /**
     * Returns whether post data is available.
     *
     * @return {@code true} when the condition matches
     */
    public boolean hasPostData() {
        return postData != null;
    }

    /**
     * Returns the fetch post data.
     *
     * @return completion future
     */
    public Optional<String> fetchPostData() {
        return postData();
    }

    /**
     * Returns the response.
     *
     * @return optional value
     */
    public Optional<? extends Response> response() {
        return Optional.ofNullable(response);
    }

    /**
     * Returns response payload.
     *
     * @return optional value
     */
    public Optional<CdpPayload> responsePayload() {
        return responsePayload.isNull() ? Optional.empty() : Optional.of(responsePayload);
    }

    /**
     * Returns the BiDi response.
     *
     * @return optional value
     */
    public Optional<BidiResponse> bidiResponse() {
        return Optional.ofNullable(response);
    }

    /**
     * Updates response.
     *
     * @param response response object
     */
    void setResponse(BidiResponse response) {
        this.response = response;
        this.responsePayload = response == null ? CdpPayload.NULL : response.data();
        Logger.debug(false, "Network", "BiDi request response linked: id={}, hasResponse={}", id, response != null);
    }

    /**
     * Updates response payload.
     *
     * @param responsePayload response payload value
     */
    public void setResponsePayload(CdpPayload responsePayload) {
        this.responsePayload = responsePayload == null ? CdpPayload.NULL : responsePayload;
        Logger.debug(
                false,
                "Network",
                "BiDi request response payload updated: id={}, present={}",
                id,
                !this.responsePayload.isNull());
    }

    /**
     * Returns the response content.
     *
     * @return completion future
     */
    public CompletableFuture<byte[]> getResponseContent() {
        Logger.debug(true, "Network", "BiDi response content requested: id={}", id);
        return session().send("network.getResponseContent", Map.of("request", id)).thenApply(payload -> {
            byte[] bytes = contentBytes(payload);
            Logger.debug(false, "Network", "BiDi response content received: id={}, bytes={}", id, bytes.length);
            return bytes;
        });
    }

    /**
     * Returns the failure.
     *
     * @return optional value
     */
    public Optional<Map<String, String>> failure() {
        return StringKit.isBlank(failureText) ? Optional.empty() : Optional.of(Map.of("errorText", failureText));
    }

    /**
     * Updates failure text.
     *
     * @param failureText failure text value
     */
    public void setFailureText(String failureText) {
        this.failureText = failureText;
        Logger.warn(false, "Network", "BiDi request failure recorded: id={}, reason={}", id, failureText);
    }

    /**
     * Returns whether this request is a navigation request.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isNavigationRequest() {
        return navigationRequest;
    }

    /**
     * Returns the initiator.
     *
     * @return mapped values
     */
    public Map<String, Object> initiator() {
        return initiator;
    }

    /**
     * Returns the redirect chain.
     *
     * @return values
     */
    public List<BidiRequest> redirectChain() {
        return redirectChain;
    }

    /**
     * Returns the frame.
     *
     * @return frame value
     */
    public Optional<? extends Frame> frame() {
        return Optional.of(frame);
    }

    /**
     * Returns the BiDi frame.
     *
     * @return frame value
     */
    public BidiFrame bidiFrame() {
        return frame;
    }

    /**
     * Finalizes queued interceptions.
     *
     * @return protocol result
     */
    public CompletableFuture<CdpPayload> finalizeInterceptions() {
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns queued continue request overrides.
     *
     * @return continue request overrides
     */
    public Map<String, Object> continueRequestOverrides() {
        return Map.of();
    }

    /**
     * Returns queued response data for this request.
     *
     * @return response data
     */
    public Optional<Map<String, Object>> responseForRequest() {
        return Optional.empty();
    }

    /**
     * Returns queued abort error reason.
     *
     * @return abort error reason
     */
    public Optional<String> abortErrorReason() {
        return Optional.ofNullable(failureText);
    }

    /**
     * Returns interception resolution state.
     *
     * @return interception state
     */
    public Map<String, Object> interceptResolutionState() {
        return Map.of("action", interceptionHandled.get() ? "already-handled" : "none");
    }

    /**
     * Enqueues an interception action.
     *
     * @param action interception action
     */
    public void enqueueInterceptAction(java.util.function.Supplier<CompletableFuture<?>> action) {
        if (action != null) {
            action.get();
        }
    }

    /**
     * Returns the CDP session.
     *
     * @return CDP session
     */
    public Session client() {
        return Awaitable.await(
                BidiCDPSession.fromContext(frame.page().browser().session(), frame.id()),
                "BiDi request CDP session failed.",
                5_000L);
    }

    /**
     * Returns the blocked.
     *
     * @return {@code true} when the condition matches
     */
    public boolean blocked() {
        return blocked;
    }

    /**
     * Returns whether request interception has already been resolved.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isInterceptResolutionHandled() {
        return interceptionHandled.get();
    }

    /**
     * Returns the session.
     *
     * @return session value
     */
    private BidiSession session() {
        return frame.page().browserContext().browser().session();
    }

    /**
     * Handles verify interception.
     */
    private void verifyInterception() {
        if (!blocked) {
            Logger.warn(false, "Network", "BiDi interception rejected for non-blocked request: id={}", id);
            throw new InternalException(
                    "BiDi request is not intercepted; interception control operations cannot be performed.");
        }
        if (!interceptionHandled.compareAndSet(false, true)) {
            Logger.warn(false, "Network", "BiDi interception already handled: id={}", id);
            throw new InternalException("BiDi request interception has already been handled.");
        }
    }

    /**
     * Handles copy if present.
     *
     * @param source source value
     * @param target target object
     * @param key    key value
     */
    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    /**
     * Returns the body.
     *
     * @param text text to use
     * @return mapped values
     */
    private Map<String, Object> body(String text) {
        return Map.of("type", "base64", "value", Base64.encode(ByteKit.toBytes(text, Charset.UTF_8)));
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
                if (StringKit.isNotBlank(name)) {
                    result.put(
                            name,
                            PayloadReader.text(
                                    PayloadReader.first(
                                            CdpPayload.NULL,
                                            header.get("value").get("value"),
                                            header.get("value"))));
                }
            }
        }
        return Map.copyOf(result);
    }

    /**
     * Returns the header entries.
     *
     * @param rawHeaders raw headers value
     * @return values
     */
    private List<Map<String, Object>> headerEntries(Map<?, ?> rawHeaders) {
        if (rawHeaders == null || rawHeaders.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : rawHeaders.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            result.add(
                    Map.of(
                            "name",
                            String.valueOf(entry.getKey()).toLowerCase(),
                            "value",
                            Map.of("type", "string", "value", String.valueOf(entry.getValue()))));
        }
        return List.copyOf(result);
    }

    /**
     * Returns the post data.
     *
     * @param payload protocol payload
     * @return post data value
     */
    private static String postData(CdpPayload payload) {
        if (!PayloadReader.present(payload)) {
            return null;
        }
        String type = PayloadReader.text(payload.get("type"));
        String value = PayloadReader.text(PayloadReader.first(CdpPayload.NULL, payload.get("value"), payload));
        if ("base64".equals(type)) {
            return StringKit.toString(Base64.decode(value), Charset.UTF_8);
        }
        return StringKit.isBlank(value) ? null : value;
    }

    /**
     * Returns the content bytes.
     *
     * @param payload protocol payload
     * @return content bytes value
     */
    private static byte[] contentBytes(CdpPayload payload) {
        CdpPayload body = PayloadReader.first(CdpPayload.NULL, payload.get("body"), payload.get("bytes"));
        String type = PayloadReader.text(body.get("type"));
        String value = PayloadReader.text(PayloadReader.first(CdpPayload.NULL, body.get("value"), payload.get("body")));
        if ("base64".equals(type) || PayloadReader.bool(payload.get("base64Encoded"))) {
            return Base64.decode(value);
        }
        return ByteKit.toBytes(value, Charset.UTF_8);
    }

}
