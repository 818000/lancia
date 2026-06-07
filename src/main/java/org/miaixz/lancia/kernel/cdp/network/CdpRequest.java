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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
import org.miaixz.lancia.Verdict;
import org.miaixz.lancia.kernel.cdp.page.CdpFrame;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * CDP request implementation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpRequest extends Request {

    /**
     * Default intercept resolution priority.
     */
    public static final int DEFAULT_INTERCEPT_RESOLUTION_PRIORITY = 0;
    /**
     * Shared constant for status texts.
     */
    private static final Map<Integer, String> STATUS_TEXTS = statusTexts();
    /**
     * Shared constant for error reasons.
     */
    private static final Map<String, String> ERROR_REASONS = errorReasons();
    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Current request ID.
     */
    private final String requestId;
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
     * Whether has post data is enabled.
     */
    private final boolean hasPostData;
    /**
     * Current resource type.
     */
    private final String resourceType;
    /**
     * Current frame.
     */
    private final CdpFrame frame;
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
    private final List<CdpRequest> redirectChain;
    /**
     * Current interception ID.
     */
    private String interceptionId;
    /**
     * Current response.
     */
    private CdpResponse response;
    /**
     * Current failure text.
     */
    private String failureText;
    /**
     * Thread-safe intercept handled state.
     */
    private final AtomicBoolean interceptHandled = new AtomicBoolean();
    /**
     * Whether interception is enabled.
     */
    private boolean interceptionEnabled;
    /**
     * Current intercept verdict.
     */
    private Verdict interceptVerdict = Verdict.NONE;
    /**
     * Current intercept resolution priority.
     */
    private Integer interceptResolutionPriority;
    /**
     * Mapped continue request overrides values.
     */
    private Map<String, Object> continueRequestOverrides = Map.of();
    /**
     * Mapped response for request values.
     */
    private Map<String, Object> responseForRequest;
    /**
     * Current abort error reason.
     */
    private String abortErrorReason;
    /**
     * Whether from memory cache is enabled.
     */
    private boolean fromMemoryCache;
    /**
     * Registered intercept actions values.
     */
    private final List<Supplier<CompletableFuture<?>>> interceptActions = new ArrayList<>();

    /**
     * Creates a CDP request.
     *
     * @param session       protocol session
     * @param requestId     request id
     * @param url           target URL
     * @param method        protocol method
     * @param headers       HTTP headers
     * @param postData      post data
     * @param redirectChain redirect chain
     */
    public CdpRequest(CDPSession session, String requestId, String url, String method, Map<String, String> headers,
            String postData, List<CdpRequest> redirectChain) {
        this(session, requestId, url, method, headers, postData, postData != null, "document", null, false, Map.of(),
                redirectChain);
    }

    /**
     * Creates a CDP request.
     *
     * @param session           protocol session
     * @param requestId         request id
     * @param url               target URL
     * @param method            protocol method
     * @param headers           HTTP headers
     * @param postData          post data
     * @param resourceType      resource type
     * @param frame             frame instance
     * @param navigationRequest navigation request
     * @param initiator         initiator
     * @param redirectChain     redirect chain
     */
    public CdpRequest(CDPSession session, String requestId, String url, String method, Map<String, String> headers,
            String postData, String resourceType, CdpFrame frame, boolean navigationRequest,
            Map<String, Object> initiator, List<CdpRequest> redirectChain) {
        this(session, requestId, url, method, headers, postData, postData != null, resourceType, frame,
                navigationRequest, initiator, redirectChain);
    }

    /**
     * Creates a CDP request.
     *
     * @param session           protocol session
     * @param requestId         request id
     * @param url               target URL
     * @param method            protocol method
     * @param headers           HTTP headers
     * @param postData          post data
     * @param hasPostData       has post data
     * @param resourceType      resource type
     * @param frame             frame instance
     * @param navigationRequest navigation request
     * @param initiator         initiator
     * @param redirectChain     redirect chain
     */
    public CdpRequest(CDPSession session, String requestId, String url, String method, Map<String, String> headers,
            String postData, boolean hasPostData, String resourceType, CdpFrame frame, boolean navigationRequest,
            Map<String, Object> initiator, List<CdpRequest> redirectChain) {
        this.session = session;
        this.requestId = Assert.notNull(requestId, "requestId");
        this.url = url == null ? Normal.EMPTY : url;
        this.method = method == null ? "GET" : method;
        this.headers = new LinkedHashMap<>(CdpHeaders.normalize(headers));
        this.postData = postData;
        this.hasPostData = hasPostData;
        this.resourceType = StringKit.isBlank(resourceType) ? "other" : resourceType.toLowerCase(Locale.ROOT);
        this.frame = frame;
        this.navigationRequest = navigationRequest;
        this.initiator = Collections.unmodifiableMap(new LinkedHashMap<>(initiator == null ? Map.of() : initiator));
        this.redirectChain = Collections
                .unmodifiableList(new ArrayList<>(redirectChain == null ? List.of() : redirectChain));
        Logger.trace(
                false,
                "Network",
                "Request created: id={}, method={}, resource={}, url={}",
                this.requestId,
                this.method,
                this.resourceType,
                StringKit.isBlank(this.url) ? Normal.EMPTY
                        : this.url.contains("?") ? this.url.substring(Normal._0, this.url.indexOf('?')) : this.url);
    }

    /**
     * Returns the from.
     *
     * @param session       protocol session
     * @param params        protocol parameters
     * @param redirectChain redirect chain value
     * @return from value
     */
    public static CdpRequest from(CDPSession session, CdpPayload params, List<CdpRequest> redirectChain) {
        return from(session, null, null, false, params, redirectChain);
    }

    /**
     * Returns the from.
     *
     * @param session           protocol session
     * @param frame             frame instance
     * @param interceptionId    interception ID value
     * @param allowInterception allow interception value
     * @param params            protocol parameters
     * @param redirectChain     redirect chain value
     * @return from value
     */
    public static CdpRequest from(
            CDPSession session,
            CdpFrame frame,
            String interceptionId,
            boolean allowInterception,
            CdpPayload params,
            List<CdpRequest> redirectChain) {
        CdpPayload request = params.get("request");
        String requestId = PayloadReader.text(params.get("requestId"));
        String type = PayloadReader.text(params.get("type"));
        String requestPostData = postData(request);
        CdpRequest result = new CdpRequest(session, requestId,
                PayloadReader.text(request.get("url")) + PayloadReader.text(request.get("urlFragment")),
                PayloadReader.text(request.get("method")), headers(request.get("headers")), requestPostData,
                request.get("hasPostData").isNull() ? requestPostData != null : request.get("hasPostData").asBoolean(),
                type, frame, requestId.equals(PayloadReader.text(params.get("loaderId"))) && "Document".equals(type),
                PayloadReader.object(params.get("initiator")), redirectChain);
        result.interceptionId = interceptionId;
        result.interceptionEnabled = allowInterception || StringKit.isNotBlank(interceptionId);
        return result;
    }

    /**
     * Returns the continue request.
     *
     * @param overrides overrides value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> continueRequest(Map<String, Object> overrides) {
        verifyInterception();
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return continueImmediately(overrides);
    }

    /**
     * Returns the continue request.
     *
     * @param overrides overrides value
     * @param priority  priority value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> continueRequest(Map<String, Object> overrides, int priority) {
        verifyInterception();
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        continueRequestOverrides = immutableCopy(overrides);
        if (interceptResolutionPriority == null || priority > interceptResolutionPriority) {
            interceptVerdict = Verdict.CONTINUE;
            interceptResolutionPriority = priority;
        } else if (priority == interceptResolutionPriority && interceptVerdict != Verdict.ABORT
                && interceptVerdict != Verdict.RESPOND) {
            interceptVerdict = Verdict.CONTINUE;
        }
        return CompletableFuture.completedFuture(CdpPayload.NULL);
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
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return respondImmediately(status, headers, null, body);
    }

    /**
     * Returns the respond.
     *
     * @param status   status value
     * @param headers  HTTP headers
     * @param body     body value
     * @param priority priority value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> respond(int status, Map<String, String> headers, byte[] body, int priority) {
        verifyInterception();
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        responseForRequest = responseSnapshot(status, headers, null, body);
        if (interceptResolutionPriority == null || priority > interceptResolutionPriority) {
            interceptVerdict = Verdict.RESPOND;
            interceptResolutionPriority = priority;
        } else if (priority == interceptResolutionPriority && interceptVerdict != Verdict.ABORT) {
            interceptVerdict = Verdict.RESPOND;
        }
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns the respond.
     *
     * @param status      status value
     * @param headers     HTTP headers
     * @param contentType content type value
     * @param body        body value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> respond(
            int status,
            Map<String, String> headers,
            String contentType,
            byte[] body) {
        verifyInterception();
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return respondImmediately(status, headers, contentType, body);
    }

    /**
     * Returns the respond.
     *
     * @param response response object
     * @return completion future
     */
    public CompletableFuture<CdpPayload> respond(Map<String, Object> response) {
        verifyInterception();
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return respondFromMap(response);
    }

    /**
     * Returns the respond.
     *
     * @param response response object
     * @param priority priority value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> respond(Map<String, Object> response, int priority) {
        verifyInterception();
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        responseForRequest = responseSnapshot(response);
        if (interceptResolutionPriority == null || priority > interceptResolutionPriority) {
            interceptVerdict = Verdict.RESPOND;
            interceptResolutionPriority = priority;
        } else if (priority == interceptResolutionPriority && interceptVerdict != Verdict.ABORT) {
            interceptVerdict = Verdict.RESPOND;
        }
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns the respond immediately.
     *
     * @param status      status value
     * @param headers     HTTP headers
     * @param contentType content type value
     * @param body        body value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> respondImmediately(
            int status,
            Map<String, String> headers,
            String contentType,
            byte[] body) {
        markInterceptionHandled();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("requestId", requireInterceptionId());
        params.put("responseCode", status);
        params.put("responsePhrase", STATUS_TEXTS.get(status));
        Map<String, Object> responseHeaders = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : (headers == null ? Map.<String, String>of() : headers).entrySet()) {
            responseHeaders.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        if (StringKit.isNotBlank(contentType)) {
            responseHeaders.put("content-type", contentType);
        }
        byte[] actualBody = body == null ? null : body;
        if (actualBody != null && actualBody.length > 0 && !responseHeaders.containsKey("content-length")) {
            responseHeaders.put("content-length", String.valueOf(actualBody.length));
        }
        params.put("responseHeaders", headerEntries(responseHeaders));
        if (actualBody != null) {
            params.put("body", Base64.encode(actualBody));
        }
        responseForRequest = responseSnapshot(status, headers, contentType, body);
        Logger.debug(
                true,
                "Network",
                "Request respond requested: id={}, status={}, bytes={}",
                requestId,
                status,
                actualBody == null ? Normal._0 : actualBody.length);
        return resetHandledOnFailure(session.send("Fetch.fulfillRequest", params));
    }

    /**
     * Returns the abort.
     *
     * @param errorReason error reason value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> abort(String errorReason) {
        verifyInterception();
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return abortImmediately(errorReason);
    }

    /**
     * Returns the abort.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> abort() {
        return abort("failed");
    }

    /**
     * Returns the abort.
     *
     * @param errorReason error reason value
     * @param priority    priority value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> abort(String errorReason, int priority) {
        verifyInterception();
        if (!canBeIntercepted()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        abortErrorReason = normalizeErrorReason(errorReason);
        if (interceptResolutionPriority == null || priority >= interceptResolutionPriority) {
            interceptVerdict = Verdict.ABORT;
            interceptResolutionPriority = priority;
        }
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns the abort immediately.
     *
     * @param errorReason error reason value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> abortImmediately(String errorReason) {
        markInterceptionHandled();
        abortErrorReason = normalizeErrorReason(errorReason);
        Logger.debug(true, "Network", "Request abort requested: id={}, reason={}", requestId, abortErrorReason);
        return resetHandledOnFailure(
                session.send(
                        "Fetch.failRequest",
                        Map.of("requestId", requireInterceptionId(), "errorReason", abortErrorReason)));
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
        return hasPostData;
    }

    /**
     * Returns the fetch post data.
     *
     * @return optional value
     */
    public Optional<String> fetchPostData() {
        if (postData != null) {
            return Optional.of(postData);
        }
        if (!hasPostData || session == null) {
            return Optional.empty();
        }
        try {
            CdpPayload result = session.send("Network.getRequestPostData", Map.of("requestId", requestId))
                    .get(5, TimeUnit.SECONDS);
            return Optional.ofNullable(PayloadReader.nullableText(result.get("postData")));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * Returns the request ID.
     *
     * @return request ID value
     */
    public String requestId() {
        return requestId;
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return requestId;
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    public CDPSession client() {
        return session;
    }

    /**
     * Updates client.
     *
     * @param session protocol session
     */
    public void setClient(CDPSession session) {
        this.session = session;
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
     * Returns the resource type.
     *
     * @return resource type value
     */
    public String resourceType() {
        return resourceType;
    }

    /**
     * Returns the frame.
     *
     * @return optional value
     */
    public Optional<CdpFrame> frame() {
        return Optional.ofNullable(frame);
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
    public List<CdpRequest> redirectChain() {
        return redirectChain;
    }

    /**
     * Returns the response.
     *
     * @return optional value
     */
    public Optional<CdpResponse> response() {
        return Optional.ofNullable(response);
    }

    /**
     * Updates response.
     *
     * @param response response object
     */
    void setResponse(CdpResponse response) {
        this.response = response;
    }

    /**
     * Updates interception ID.
     *
     * @param interceptionId interception ID value
     */
    void setInterceptionId(String interceptionId) {
        this.interceptionId = interceptionId;
        this.interceptionEnabled = StringKit.isNotBlank(interceptionId);
    }

    /**
     * Returns the interception ID.
     *
     * @return optional value
     */
    Optional<String> interceptionId() {
        return Optional.ofNullable(interceptionId);
    }

    /**
     * Handles update headers.
     *
     * @param headers HTTP headers
     */
    void updateHeaders(Map<String, String> headers) {
        this.headers.putAll(CdpHeaders.normalize(headers));
    }

    /**
     * Returns the failure text.
     *
     * @return optional value
     */
    public Optional<String> failureText() {
        return Optional.ofNullable(failureText);
    }

    /**
     * Returns the failure.
     *
     * @return optional value
     */
    public Optional<Map<String, String>> failure() {
        return failureText == null ? Optional.empty() : Optional.of(Map.of("errorText", failureText));
    }

    /**
     * Handles mark failed.
     *
     * @param failureText failure text value
     */
    void markFailed(String failureText) {
        this.failureText = failureText;
    }

    /**
     * Returns the continue request overrides.
     *
     * @return mapped values
     */
    public Map<String, Object> continueRequestOverrides() {
        return continueRequestOverrides;
    }

    /**
     * Returns the response for request.
     *
     * @return optional value
     */
    public Optional<Map<String, Object>> responseForRequest() {
        return Optional.ofNullable(responseForRequest);
    }

    /**
     * Returns the abort error reason.
     *
     * @return optional value
     */
    public Optional<String> abortErrorReason() {
        return Optional.ofNullable(abortErrorReason);
    }

    /**
     * Returns whether request interception has already been resolved.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isInterceptResolutionHandled() {
        return interceptHandled.get();
    }

    /**
     * Returns the intercept resolution state.
     *
     * @return mapped values
     */
    public Map<String, Object> interceptResolutionState() {
        if (!interceptionEnabled) {
            return Map.of("action", Verdict.DISABLED.value());
        }
        if (interceptHandled.get()) {
            return Map.of("action", Verdict.ALREADY_HANDLED.value());
        }
        if (interceptResolutionPriority == null) {
            return Map.of("action", interceptVerdict.value());
        }
        return Map.of("action", interceptVerdict.value(), "priority", interceptResolutionPriority);
    }

    /**
     * Returns whether be intercepted is available.
     *
     * @return {@code true} when the condition matches
     */
    public boolean canBeIntercepted() {
        return !url().startsWith("data:") && !fromMemoryCache;
    }

    /**
     * Handles mark from memory cache.
     */
    public void markFromMemoryCache() {
        this.fromMemoryCache = true;
    }

    /**
     * Creates this value from memory cache.
     *
     * @return {@code true} when the condition matches
     */
    public boolean fromMemoryCache() {
        return fromMemoryCache;
    }

    /**
     * Enqueues an interception action.
     *
     * @param action action
     */
    public void enqueueInterceptAction(Supplier<CompletableFuture<?>> action) {
        interceptActions.add(Assert.notNull(action, "action"));
    }

    /**
     * Returns the finalize interceptions.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> finalizeInterceptions() {
        if (!interceptActions.isEmpty()) {
            Logger.debug(
                    true,
                    "Network",
                    "Request interception actions requested: id={}, count={}",
                    requestId,
                    interceptActions.size());
            CompletableFuture<Void> actions = CompletableFuture.completedFuture(null);
            for (Supplier<CompletableFuture<?>> action : List.copyOf(interceptActions)) {
                actions = actions.thenCompose(ignored -> {
                    CompletableFuture<?> future = action.get();
                    return future == null ? CompletableFuture.completedFuture(null) : future.thenApply(value -> null);
                });
            }
            interceptActions.clear();
            return actions.thenCompose(ignored -> dispatchInterceptionResolution());
        }
        return dispatchInterceptionResolution();
    }

    /**
     * Dispatches the selected interception resolution.
     *
     * @return dispatch interception resolution value
     */
    private CompletableFuture<CdpPayload> dispatchInterceptionResolution() {
        if (interceptHandled.get()) {
            Logger.trace(false, "Network", "Request interception already handled: id={}", requestId);
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        Logger.debug(
                false,
                "Network",
                "Request interception dispatch: id={}, action={}, priority={}",
                requestId,
                interceptVerdict.value(),
                interceptResolutionPriority);
        return switch (interceptVerdict) {
            case ABORT -> abortImmediately(abortErrorReason);
            case RESPOND -> respondFromSnapshot();
            case CONTINUE -> continueImmediately(continueRequestOverrides);
            default -> CompletableFuture.completedFuture(CdpPayload.NULL);
        };
    }

    /**
     * Returns the require interception ID.
     *
     * @return require interception ID value
     */
    private String requireInterceptionId() {
        if (StringKit.isBlank(interceptionId)) {
            throw new InternalException(
                    "Request is not intercepted by Fetch; interception control operations cannot be performed.");
        }
        return interceptionId;
    }

    /**
     * Handles verify interception.
     */
    private void verifyInterception() {
        if (!interceptionEnabled && StringKit.isBlank(interceptionId)) {
            throw new InternalException("Request interception is not enabled.");
        }
        if (interceptHandled.get()) {
            throw new InternalException("Request interception has already been handled.");
        }
    }

    /**
     * Returns the continue immediately.
     *
     * @param overrides overrides value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> continueImmediately(Map<String, Object> overrides) {
        markInterceptionHandled();
        Map<String, Object> source = overrides == null ? Map.of() : overrides;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("requestId", requireInterceptionId());
        copyIfPresent(source, params, "url");
        copyIfPresent(source, params, "method");
        Object postDataOverride = source.get("postData");
        if (postDataOverride != null) {
            byte[] postDataBytes = bodyBytes(postDataOverride);
            if (postDataBytes.length > 0) {
                params.put("postData", Base64.encode(postDataBytes));
            }
        }
        Object overrideHeaders = source.get("headers");
        if (overrideHeaders instanceof Map<?, ?> headerMap) {
            params.put("headers", headerEntries(headerMap));
        }
        continueRequestOverrides = immutableCopy(overrides);
        Logger.debug(true, "Network", "Request continue requested: id={}, overrides={}", requestId, source.size());
        return resetHandledOnFailure(session.send("Fetch.continueRequest", params));
    }

    /**
     * Returns the respond from snapshot.
     *
     * @return completion future
     */
    private CompletableFuture<CdpPayload> respondFromSnapshot() {
        if (responseForRequest == null) {
            throw new InternalException("Request interception is missing response data.");
        }
        Logger.debug(true, "Network", "Request response snapshot dispatch requested: id={}", requestId);
        Object statusValue = responseForRequest.get("status");
        int status = statusValue instanceof Number number ? number.intValue() : 200;
        Object headersValue = responseForRequest.get("headers");
        Map<String, String> headers = headersValue instanceof Map<?, ?> map ? stringMap(map) : Map.of();
        Object bodyValue = responseForRequest.get("body");
        byte[] body = bodyValue instanceof byte[] bytes ? bytes
                : bodyValue == null ? null : ByteKit.toBytes(String.valueOf(bodyValue), Charset.UTF_8);
        Object contentTypeValue = responseForRequest.get("contentType");
        return respondImmediately(
                status,
                headers,
                contentTypeValue == null ? null : String.valueOf(contentTypeValue),
                body);
    }

    /**
     * Responds from a response map.
     *
     * @param response response
     * @return respond from map value
     */
    private CompletableFuture<CdpPayload> respondFromMap(Map<String, Object> response) {
        Map<String, Object> snapshot = responseSnapshot(response);
        Object statusValue = snapshot.get("status");
        int status = statusValue instanceof Number number ? number.intValue() : 200;
        Object headersValue = snapshot.get("headers");
        Map<String, String> headers = headersValue instanceof Map<?, ?> map ? stringMap(map) : Map.of();
        Object contentTypeValue = snapshot.get("contentType");
        Object bodyValue = snapshot.get("body");
        byte[] body = bodyValue instanceof byte[] bytes ? bytes
                : bodyValue == null ? null : ByteKit.toBytes(String.valueOf(bodyValue), Charset.UTF_8);
        return respondImmediately(
                status,
                headers,
                contentTypeValue == null ? null : String.valueOf(contentTypeValue),
                body);
    }

    /**
     * Handles mark interception handled.
     */
    private void markInterceptionHandled() {
        if (!interceptHandled.compareAndSet(false, true)) {
            throw new InternalException("Request interception has already been handled.");
        }
    }

    /**
     * Returns the reset handled on failure.
     *
     * @param future future value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> resetHandledOnFailure(CompletableFuture<CdpPayload> future) {
        return future.whenComplete((payload, error) -> {
            if (error != null) {
                interceptHandled.set(false);
                Logger.warn(
                        false,
                        "Network",
                        error,
                        "Request interception command failed: id={}, action={}",
                        requestId,
                        interceptVerdict.value());
            } else {
                Logger.debug(
                        false,
                        "Network",
                        "Request interception command completed: id={}, action={}",
                        requestId,
                        interceptVerdict.value());
            }
        });
    }

    /**
     * Handles copy if present.
     *
     * @param source source value
     * @param target target object
     * @param key    key value
     */
    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key) && source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }

    /**
     * Returns the immutable copy.
     *
     * @param values values value
     * @return mapped values
     */
    private Map<String, Object> immutableCopy(Map<String, Object> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values == null ? Map.of() : values));
    }

    /**
     * Returns the response snapshot.
     *
     * @param status      status value
     * @param headers     HTTP headers
     * @param contentType content type value
     * @param body        body value
     * @return mapped values
     */
    private Map<String, Object> responseSnapshot(
            int status,
            Map<String, String> headers,
            String contentType,
            byte[] body) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", status);
        snapshot.put("headers", CdpHeaders.normalize(headers));
        if (StringKit.isNotBlank(contentType)) {
            snapshot.put("contentType", contentType);
        }
        if (body != null) {
            snapshot.put("body", body);
        }
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Returns the response snapshot.
     *
     * @param response response object
     * @return mapped values
     */
    private Map<String, Object> responseSnapshot(Map<String, Object> response) {
        Map<String, Object> source = response == null ? Map.of() : response;
        Object statusValue = source.get("status");
        int status = statusValue instanceof Number number ? number.intValue() : 200;
        Object headersValue = source.get("headers");
        Map<String, String> headers = headersValue instanceof Map<?, ?> map ? stringMap(map) : Map.of();
        Object contentTypeValue = source.get("contentType");
        Object bodyValue = source.get("body");
        byte[] body = bodyValue == null ? null : bodyBytes(bodyValue);
        return responseSnapshot(
                status,
                headers,
                contentTypeValue == null ? null : String.valueOf(contentTypeValue),
                body);
    }

    /**
     * Converts a body value to bytes.
     *
     * @param value to convert
     * @return body bytes value
     */
    private byte[] bodyBytes(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        if (value instanceof ByteArrayOutputStream output) {
            return output.toByteArray();
        }
        return ByteKit.toBytes(String.valueOf(value), Charset.UTF_8);
    }

    /**
     * Returns the normalize error reason.
     *
     * @param errorReason error reason value
     * @return normalize error reason value
     */
    private String normalizeErrorReason(String errorReason) {
        if (StringKit.isBlank(errorReason)) {
            return "Failed";
        }
        String mapped = ERROR_REASONS.get(errorReason.toLowerCase(Locale.ROOT));
        return mapped == null ? errorReason : mapped;
    }

    /**
     * Returns the header entries.
     *
     * @param headers HTTP headers
     * @return values
     */
    private List<Map<String, String>> headerEntries(Map<?, ?> headers) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : (headers == null ? Map.of() : headers).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String name = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item != null) {
                        result.add(Map.of("name", name, "value", String.valueOf(item)));
                    }
                }
            } else if (value.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(value);
                for (int index = 0; index < length; index++) {
                    Object item = java.lang.reflect.Array.get(value, index);
                    if (item != null) {
                        result.add(Map.of("name", name, "value", String.valueOf(item)));
                    }
                }
            } else {
                result.add(Map.of("name", name, "value", String.valueOf(value)));
            }
        }
        return result;
    }

    /**
     * Returns the string map.
     *
     * @param values values value
     * @return mapped values
     */
    private static Map<String, String> stringMap(Map<?, ?> values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * Returns the post data.
     *
     * @param request request object
     * @return post data value
     */
    private static String postData(CdpPayload request) {
        CdpPayload entries = request.get("postDataEntries");
        if (entries != null && entries.isArray() && !entries.elements().isEmpty()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (CdpPayload entry : entries.elements()) {
                String bytes = PayloadReader.nullableText(entry.get("bytes"));
                if (StringKit.isNotBlank(bytes)) {
                    output.writeBytes(Base64.decode(bytes));
                }
            }
            return output.toString(Charset.UTF_8);
        }
        return PayloadReader.nullableText(request.get("postData"));
    }

    /**
     * Returns the headers.
     *
     * @param payload protocol payload
     * @return mapped values
     */
    static Map<String, String> headers(CdpPayload payload) {
        if (payload == null || payload.isNull() || !payload.isObject()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, CdpPayload> entry : payload.fields().entrySet()) {
            result.put(entry.getKey().toLowerCase(Locale.ROOT), PayloadReader.text(entry.getValue()));
        }
        return result;
    }

    /**
     * Returns the status texts.
     *
     * @return mapped values
     */
    private static Map<Integer, String> statusTexts() {
        return Map.ofEntries(
                Map.entry(100, "Continue"),
                Map.entry(101, "Switching Protocols"),
                Map.entry(102, "Processing"),
                Map.entry(103, "Early Hints"),
                Map.entry(200, "OK"),
                Map.entry(201, "Created"),
                Map.entry(202, "Accepted"),
                Map.entry(203, "Non-Authoritative Information"),
                Map.entry(204, "No Content"),
                Map.entry(205, "Reset Content"),
                Map.entry(206, "Partial Content"),
                Map.entry(207, "Multi-Status"),
                Map.entry(208, "Already Reported"),
                Map.entry(226, "IM Used"),
                Map.entry(300, "Multiple Choices"),
                Map.entry(301, "Moved Permanently"),
                Map.entry(302, "Found"),
                Map.entry(303, "See Other"),
                Map.entry(304, "Not Modified"),
                Map.entry(305, "Use Proxy"),
                Map.entry(306, "Switch Proxy"),
                Map.entry(307, "Temporary Redirect"),
                Map.entry(308, "Permanent Redirect"),
                Map.entry(400, "Bad Request"),
                Map.entry(401, "Unauthorized"),
                Map.entry(402, "Payment Required"),
                Map.entry(403, "Forbidden"),
                Map.entry(404, "Not Found"),
                Map.entry(405, "Method Not Allowed"),
                Map.entry(406, "Not Acceptable"),
                Map.entry(407, "Proxy Authentication Required"),
                Map.entry(408, "Request Timeout"),
                Map.entry(409, "Conflict"),
                Map.entry(410, "Gone"),
                Map.entry(411, "Length Required"),
                Map.entry(412, "Precondition Failed"),
                Map.entry(413, "Payload Too Large"),
                Map.entry(414, "URI Too Long"),
                Map.entry(415, "Un" + "supported Media Type"),
                Map.entry(416, "Range Not Satisfiable"),
                Map.entry(417, "Expectation Failed"),
                Map.entry(418, "I'm a teapot"),
                Map.entry(421, "Misdirected Request"),
                Map.entry(422, "Unprocessable Entity"),
                Map.entry(423, "Locked"),
                Map.entry(424, "Failed Dependency"),
                Map.entry(425, "Too Early"),
                Map.entry(426, "Upgrade Required"),
                Map.entry(428, "Precondition Required"),
                Map.entry(429, "Too Many Requests"),
                Map.entry(431, "Request Header Fields Too Large"),
                Map.entry(451, "Unavailable For Legal Reasons"),
                Map.entry(500, "Internal Server Error"),
                Map.entry(501, "Not Implemented"),
                Map.entry(502, "Bad Gateway"),
                Map.entry(503, "Service Unavailable"),
                Map.entry(504, "Gateway Timeout"),
                Map.entry(505, "HTTP Version Not Supported"),
                Map.entry(506, "Variant Also Negotiates"),
                Map.entry(507, "Insufficient Storage"),
                Map.entry(508, "Loop Detected"),
                Map.entry(510, "Not Extended"),
                Map.entry(511, "Network Authentication Required"));
    }

    /**
     * Returns the error reasons.
     *
     * @return mapped values
     */
    private static Map<String, String> errorReasons() {
        return Map.ofEntries(
                Map.entry("aborted", "Aborted"),
                Map.entry("accessdenied", "AccessDenied"),
                Map.entry("addressunreachable", "AddressUnreachable"),
                Map.entry("blockedbyclient", "BlockedByClient"),
                Map.entry("blockedbyresponse", "BlockedByResponse"),
                Map.entry("connectionaborted", "ConnectionAborted"),
                Map.entry("connectionclosed", "ConnectionClosed"),
                Map.entry("connectionfailed", "ConnectionFailed"),
                Map.entry("connectionrefused", "ConnectionRefused"),
                Map.entry("connectionreset", "ConnectionReset"),
                Map.entry("internetdisconnected", "InternetDisconnected"),
                Map.entry("namenotresolved", "NameNotResolved"),
                Map.entry("timedout", "TimedOut"),
                Map.entry("failed", "Failed"));
    }

}
