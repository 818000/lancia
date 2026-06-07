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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.Network;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.options.UserAgentOptions;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Manages CDP network state and protocol events.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpNetworkManager implements Network {

    /**
     * Returns the event name.
     */
    public static final String REQUEST = CdpNetworkManagerEvent.REQUEST.eventName();

    /**
     * Returns the event name.
     */
    public static final String RESPONSE = CdpNetworkManagerEvent.RESPONSE.eventName();

    /**
     * Returns the event name.
     */
    public static final String REQUEST_FINISHED = CdpNetworkManagerEvent.REQUEST_FINISHED.eventName();

    /**
     * Returns the event name.
     */
    public static final String REQUEST_FAILED = CdpNetworkManagerEvent.REQUEST_FAILED.eventName();

    /**
     * Returns the event name.
     */
    public static final String REQUEST_SERVED_FROM_CACHE = CdpNetworkManagerEvent.REQUEST_SERVED_FROM_CACHE.eventName();
    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Current event manager.
     */
    private final CdpNetworkEventManager eventManager = new CdpNetworkEventManager();
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Thread-safe initialized state.
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    /**
     * Whether user request interception is enabled.
     */
    private volatile boolean userRequestInterceptionEnabled;
    /**
     * Whether protocol request interception is enabled.
     */
    private volatile Boolean protocolRequestInterceptionEnabled;
    /**
     * Whether user cache disabled is enabled.
     */
    private volatile Boolean userCacheDisabled;
    /**
     * Current auth username.
     */
    private volatile String authUsername;
    /**
     * Current auth password.
     */
    private volatile String authPassword;
    /**
     * Registered attempted authentications values.
     */
    private final Set<String> attemptedAuthentications = ConcurrentHashMap.newKeySet();
    /**
     * Mapped extra HTTP headers values.
     */
    private volatile Map<String, String> extraHTTPHeaders;

    /**
     * User-Agent.
     */
    private volatile String userAgent;
    /**
     * Current user agent options.
     */
    private volatile UserAgentOptions userAgentOptions;

    /**
     * Creates a CDP network manager.
     *
     * @param session protocol session
     */
    public CdpNetworkManager(CDPSession session) {
        this.session = session;
    }

    /**
     * Initializes protocol state for this object.
     */
    public void initialize() {
        if (session == null || !initialized.compareAndSet(false, true)) {
            Logger.debug(
                    false,
                    "Network",
                    "Network manager initialization skipped: session={}, initialized={}",
                    session != null,
                    initialized.get());
            return;
        }
        Logger.debug(true, "Network", "Network manager initialization requested: session={}", session.id());
        binding = binding.combine(session.on("Network.requestWillBeSent", this::onRequestWillBeSent));
        binding = binding.combine(session.on("Fetch.requestPaused", this::onRequestPaused));
        binding = binding.combine(session.on("Fetch.authRequired", this::onAuthRequired));
        binding = binding.combine(session.on("Network.responseReceived", this::onResponseReceived));
        binding = binding.combine(session.on("Network.requestWillBeSentExtraInfo", this::onRequestWillBeSentExtraInfo));
        binding = binding.combine(session.on("Network.requestServedFromCache", this::onRequestServedFromCache));
        binding = binding.combine(session.on("Network.responseReceivedExtraInfo", this::onResponseReceivedExtraInfo));
        binding = binding.combine(session.on("Network.loadingFinished", this::onLoadingFinished));
        binding = binding.combine(session.on("Network.loadingFailed", this::onLoadingFailed));
        session.send("Network.enable");
        applyCurrentConfiguration();
        Logger.debug(false, "Network", "Network manager initialized: session={}", session.id());
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        return EventHooks.on(emitter, event, listener);
    }

    /**
     * Updates extra HTTP headers.
     *
     * @param headers HTTP headers
     * @return set extra HTTP headers value
     */
    public CompletableFuture<CdpPayload> setExtraHTTPHeaders(Map<String, String> headers) {
        Map<String, String> normalized = CdpHeaders.normalize(headers);
        this.extraHTTPHeaders = normalized;
        Logger.debug(true, "Network", "Extra HTTP headers update requested: count={}", normalized.size());
        return send("Network.setExtraHTTPHeaders", Map.of("headers", normalized));
    }

    /**
     * Returns the extra HTTP headers.
     *
     * @return mapped values
     */
    public Map<String, String> extraHTTPHeaders() {
        return extraHTTPHeaders == null ? Map.of() : Map.copyOf(extraHTTPHeaders);
    }

    /**
     * Updates user agent.
     *
     * @param userAgent user agent
     * @return set user agent value
     */
    public CompletableFuture<CdpPayload> setUserAgent(String userAgent) {
        UserAgentOptions options = new UserAgentOptions();
        options.setUserAgent(userAgent == null ? Normal.EMPTY : userAgent);
        return setUserAgent(options);
    }

    /**
     * Updates user agent.
     *
     * @param options operation options
     * @return set user agent value
     */
    public CompletableFuture<CdpPayload> setUserAgent(UserAgentOptions options) {
        UserAgentOptions actual = normalizeUserAgentOptions(options);
        this.userAgent = actual.getUserAgent();
        this.userAgentOptions = actual;
        Logger.debug(
                true,
                "Network",
                "User agent update requested: chars={}, hasMetadata={}, platform={}",
                this.userAgent.length(),
                actual.getUserAgentMetadata() != null && !actual.getUserAgentMetadata().isEmpty(),
                StringKit.isBlank(actual.getPlatform()) ? Normal.EMPTY : actual.getPlatform());
        return send("Network.setUserAgentOverride", userAgentParams(actual));
    }

    /**
     * Updates cache enabled.
     *
     * @param enabled enabled
     * @return set cache enabled value
     */
    public CompletableFuture<CdpPayload> setCacheEnabled(boolean enabled) {
        this.userCacheDisabled = !enabled;
        Logger.debug(true, "Network", "Cache mode update requested: enabled={}", enabled);
        return applyProtocolCacheDisabled();
    }

    /**
     * Updates request interception.
     *
     * @param enabled enabled
     * @return set request interception value
     */
    public CompletableFuture<CdpPayload> setRequestInterception(boolean enabled) {
        this.userRequestInterceptionEnabled = enabled;
        Logger.debug(true, "Network", "Request interception update requested: enabled={}", enabled);
        return applyProtocolRequestInterception();
    }

    /**
     * Returns the authenticate.
     *
     * @param username username value
     * @param password password value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> authenticate(String username, String password) {
        this.authUsername = StringKit.isBlank(username) ? null : username;
        this.authPassword = password == null ? Normal.EMPTY : password;
        Logger.debug(true, "Network", "Authentication update requested: enabled={}", authUsername != null);
        return applyProtocolRequestInterception();
    }

    /**
     * Updates offline mode.
     *
     * @param offline offline
     * @return set offline mode value
     */
    public CompletableFuture<CdpPayload> setOfflineMode(boolean offline) {
        return emulateNetworkConditions(offline, 0, 0, 0);
    }

    /**
     * Returns the emulate network conditions.
     *
     * @param offline            offline value
     * @param latency            latency value
     * @param downloadThroughput download throughput value
     * @param uploadThroughput   upload throughput value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateNetworkConditions(
            boolean offline,
            double latency,
            double downloadThroughput,
            double uploadThroughput) {
        Logger.debug(
                true,
                "Network",
                "Network condition emulation requested: offline={}, latency={}, download={}, upload={}",
                offline,
                latency,
                downloadThroughput,
                uploadThroughput);
        return send(
                "Network.emulateNetworkConditions",
                Map.of(
                        "offline",
                        offline,
                        "latency",
                        latency,
                        "downloadThroughput",
                        downloadThroughput,
                        "uploadThroughput",
                        uploadThroughput));
    }

    /**
     * Returns the in flight request count.
     *
     * @return in flight request count value
     */
    public int inFlightRequestCount() {
        return eventManager.inFlightRequestCount();
    }

    /**
     * Returns the event manager.
     *
     * @return event manager value
     */
    public CdpNetworkEventManager eventManager() {
        return eventManager;
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        Logger.debug(
                true,
                "Network",
                "Network manager dispose requested: bindingUnbound={}, inFlight={}",
                binding.isUnbound(),
                inFlightRequestCount());
        binding.unbind();
        binding = new EventBinding();
        initialized.set(false);
        Logger.debug(false, "Network", "Network manager disposed.");
    }

    /**
     * Handles on request will be sent.
     *
     * @param params protocol parameters
     */
    private void onRequestWillBeSent(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        String url = PayloadReader.text(params.get("request").get("url"));
        if (userRequestInterceptionEnabled && !url.startsWith("data:")) {
            eventManager.storeRequestWillBeSent(requestId, params);
            Optional<CdpPayload> paused = eventManager.getRequestPaused(requestId);
            if (paused.isPresent()) {
                onRequest(params, PayloadReader.text(paused.getOrThrow().get("requestId")), false, paused.getOrThrow());
                eventManager.forgetRequestPaused(requestId);
            }
            return;
        }
        onRequest(params, null, false);
    }

    /**
     * Handles on request paused.
     *
     * @param params protocol parameters
     */
    private void onRequestPaused(CdpPayload params) {
        if (!params.get("authChallenge").isNull()) {
            onAuthRequired(params);
            return;
        }
        if (!userRequestInterceptionEnabled && Boolean.TRUE.equals(protocolRequestInterceptionEnabled)) {
            send("Fetch.continueRequest", Map.of("requestId", PayloadReader.text(params.get("requestId"))));
        }
        String networkId = PayloadReader.text(params.get("networkId"));
        if (StringKit.isBlank(networkId)) {
            onRequestWithoutNetworkInstrumentation(params);
            return;
        }
        Optional<CdpPayload> willBeSent = eventManager.getRequestWillBeSent(networkId);
        if (willBeSent.isPresent() && !sameRequest(willBeSent.getOrThrow(), params)) {
            eventManager.forgetRequestWillBeSent(networkId);
            willBeSent = Optional.empty();
        }
        if (willBeSent.isPresent()) {
            onRequest(willBeSent.getOrThrow(), PayloadReader.text(params.get("requestId")), false, params);
            eventManager.forgetRequestPaused(networkId);
            return;
        }
        Optional<CdpRequest> request = eventManager.request(networkId);
        if (request.isPresent()) {
            request.getOrThrow().setInterceptionId(PayloadReader.text(params.get("requestId")));
            request.getOrThrow().updateHeaders(CdpRequest.headers(params.get("request").get("headers")));
            if (!userRequestInterceptionEnabled) {
                request.getOrThrow().continueRequest(Map.of());
            }
            return;
        }
        eventManager.storeRequestPaused(networkId, params);
    }

    /**
     * Handles on auth required.
     *
     * @param params protocol parameters
     */
    private void onAuthRequired(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        String response = "Default";
        if (attemptedAuthentications.contains(requestId)) {
            response = "CancelAuth";
        } else if (authUsername != null) {
            response = "ProvideCredentials";
            attemptedAuthentications.add(requestId);
        }
        Map<String, Object> authChallengeResponse = new LinkedHashMap<>();
        authChallengeResponse.put("response", response);
        if (authUsername != null) {
            authChallengeResponse.put("username", authUsername);
            authChallengeResponse.put("password", authPassword);
        }
        send("Fetch.continueWithAuth", Map.of("requestId", requestId, "authChallengeResponse", authChallengeResponse));
    }

    /**
     * Handles on request without network instrumentation.
     *
     * @param params protocol parameters
     */
    private void onRequestWithoutNetworkInstrumentation(CdpPayload params) {
        CdpPayload requestPayload = params.get("request");
        CdpRequest request = new CdpRequest(session, PayloadReader.text(params.get("requestId")),
                PayloadReader.text(requestPayload.get("url")), PayloadReader.text(requestPayload.get("method")),
                CdpRequest.headers(requestPayload.get("headers")),
                PayloadReader.nullableText(requestPayload.get("postData")),
                !requestPayload.get("hasPostData").isNull() && requestPayload.get("hasPostData").asBoolean(), "other",
                null, false, Map.of(), List.of());
        request.setInterceptionId(PayloadReader.text(params.get("requestId")));
        emit(REQUEST, request);
        request.finalizeInterceptions();
    }

    /**
     * Handles on request.
     *
     * @param params          protocol parameters
     * @param fetchRequestId  fetch request ID value
     * @param fromMemoryCache from memory cache value
     */
    private void onRequest(CdpPayload params, String fetchRequestId, boolean fromMemoryCache) {
        onRequest(params, fetchRequestId, fromMemoryCache, null);
    }

    /**
     * Handles on request.
     *
     * @param params          protocol parameters
     * @param fetchRequestId  fetch request ID value
     * @param fromMemoryCache from memory cache value
     * @param pausedParams    paused params value
     */
    private void onRequest(CdpPayload params, String fetchRequestId, boolean fromMemoryCache, CdpPayload pausedParams) {
        String requestId = PayloadReader.text(params.get("requestId"));
        List<CdpRequest> redirectChain = new ArrayList<>();
        if (!params.get("redirectResponse").isNull()) {
            CdpPayload redirectExtraInfo = null;
            if (PayloadReader.bool(params.get("redirectHasExtraInfo"))) {
                Optional<CdpPayload> extraInfo = eventManager.takeResponseExtraInfo(requestId);
                if (extraInfo.isEmpty()) {
                    eventManager.queueRedirectInfo(
                            requestId,
                            new CdpNetworkEventManager.RedirectInfo(params, fetchRequestId));
                    return;
                }
                redirectExtraInfo = extraInfo.getOrThrow();
            }
            Optional<CdpRequest> previous = eventManager.request(requestId);
            if (previous.isPresent()) {
                CdpRequest previousRequest = previous.getOrThrow();
                handleRequestRedirect(previousRequest, params.get("redirectResponse"), redirectExtraInfo);
                redirectChain.addAll(previousRequest.redirectChain());
                redirectChain.add(previousRequest);
                eventManager.takeRequestExtraInfo(requestId).ifPresent(
                        extraInfo -> previousRequest.updateHeaders(CdpRequest.headers(extraInfo.get("headers"))));
            }
        }
        CdpRequest request = CdpRequest
                .from(session, null, fetchRequestId, userRequestInterceptionEnabled, params, redirectChain);
        if (pausedParams != null) {
            request.updateHeaders(CdpRequest.headers(pausedParams.get("request").get("headers")));
        }
        eventManager.takeRequestExtraInfo(requestId)
                .ifPresent(extraInfo -> request.updateHeaders(CdpRequest.headers(extraInfo.get("headers"))));
        if (fromMemoryCache) {
            request.markFromMemoryCache();
        }
        eventManager.storeRequest(requestId, request);
        Logger.debug(
                false,
                "Network",
                "Request observed: id={}, method={}, resource={}, url={}",
                requestId,
                request.method(),
                request.resourceType(),
                StringKit.isBlank(request.url()) ? Normal.EMPTY
                        : request.url().contains("?") ? request.url().substring(Normal._0, request.url().indexOf('?'))
                                : request.url());
        eventManager.forgetRequestWillBeSent(requestId);
        emit(REQUEST, request);
        request.finalizeInterceptions();
        replayQueuedEventGroup(requestId);
    }

    /**
     * Handles handle request redirect.
     *
     * @param request         request object
     * @param responsePayload response payload value
     * @param extraInfo       extra info value
     */
    private void handleRequestRedirect(CdpRequest request, CdpPayload responsePayload, CdpPayload extraInfo) {
        CdpResponse response = CdpResponse
                .from(session, request, CdpPayload.of(Map.of("response", responsePayload)), extraInfo);
        request.setResponse(response);
        eventManager.recordResponse(request.requestId(), response);
        Logger.debug(
                false,
                "Network",
                "Request redirected: id={}, status={}, url={}",
                request.requestId(),
                response.status(),
                StringKit.isBlank(request.url()) ? Normal.EMPTY
                        : request.url().contains("?") ? request.url().substring(Normal._0, request.url().indexOf('?'))
                                : request.url());
        emit(RESPONSE, response);
        forgetRequest(request, false);
        emit(REQUEST_FINISHED, request);
    }

    /**
     * Handles on response received.
     *
     * @param params protocol parameters
     */
    private void onResponseReceived(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        Optional<CdpRequest> request = eventManager.request(requestId);
        CdpPayload extraInfo = null;
        boolean hasExtraInfo = PayloadReader.bool(params.get("hasExtraInfo"));
        boolean hasQueuedExtraInfo = !eventManager.responseExtraInfo(requestId).isEmpty();
        if (request.isPresent() && !request.getOrThrow().fromMemoryCache() && (hasExtraInfo || hasQueuedExtraInfo)) {
            Optional<CdpPayload> queuedExtraInfo = eventManager.takeResponseExtraInfo(requestId);
            if (queuedExtraInfo.isEmpty()) {
                eventManager.queueEventGroup(requestId, new CdpNetworkEventManager.QueuedEventGroup(params));
                return;
            }
            extraInfo = queuedExtraInfo.getOrThrow();
        }
        if (request.isEmpty()) {
            return;
        }
        emitResponseEvent(params, extraInfo);
    }

    /**
     * Handles emit response event.
     *
     * @param params    protocol parameters
     * @param extraInfo extra info value
     */
    private void emitResponseEvent(CdpPayload params, CdpPayload extraInfo) {
        String requestId = PayloadReader.text(params.get("requestId"));
        Optional<CdpRequest> request = eventManager.request(requestId);
        if (request.isEmpty()) {
            return;
        }
        if (PayloadReader.bool(params.get("response").get("fromDiskCache"))) {
            extraInfo = null;
        }
        CdpResponse response = CdpResponse.from(session, request.getOrThrow(), params, extraInfo);
        request.getOrThrow().setResponse(response);
        eventManager.recordResponse(requestId, response);
        Logger.debug(
                false,
                "Network",
                "Response observed: id={}, status={}, url={}",
                requestId,
                response.status(),
                StringKit.isBlank(response.url()) ? Normal.EMPTY
                        : response.url().contains("?")
                                ? response.url().substring(Normal._0, response.url().indexOf('?'))
                                : response.url());
        emit(RESPONSE, response);
    }

    /**
     * Handles on request will be sent extra info.
     *
     * @param params protocol parameters
     */
    private void onRequestWillBeSentExtraInfo(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        Optional<CdpRequest> request = eventManager.request(requestId);
        if (request.isPresent()) {
            request.getOrThrow().updateHeaders(CdpRequest.headers(params.get("headers")));
        } else {
            eventManager.recordRequestExtraInfo(requestId, params);
        }
    }

    /**
     * Handles on request served from cache.
     *
     * @param params protocol parameters
     */
    private void onRequestServedFromCache(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        Optional<CdpRequest> request = eventManager.request(requestId);
        if (request.isPresent()) {
            request.getOrThrow().markFromMemoryCache();
            emit(REQUEST_SERVED_FROM_CACHE, request.getOrThrow());
            return;
        }
        Optional<CdpPayload> requestWillBeSent = eventManager.getRequestWillBeSent(requestId);
        if (requestWillBeSent.isPresent()) {
            onRequest(requestWillBeSent.getOrThrow(), null, true);
            eventManager.request(requestId).ifPresent(value -> emit(REQUEST_SERVED_FROM_CACHE, value));
        }
    }

    /**
     * Handles on response received extra info.
     *
     * @param params protocol parameters
     */
    private void onResponseReceivedExtraInfo(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        Optional<CdpNetworkEventManager.RedirectInfo> redirectInfo = eventManager.takeQueuedRedirectInfo(requestId);
        if (redirectInfo.isPresent()) {
            eventManager.recordResponseExtraInfo(requestId, params);
            onRequest(
                    redirectInfo.getOrThrow().event(),
                    redirectInfo.getOrThrow().fetchRequestId().orElse(null),
                    false);
            return;
        }
        Optional<CdpNetworkEventManager.QueuedEventGroup> queued = eventManager.getQueuedEventGroup(requestId);
        if (queued.isPresent()) {
            eventManager.forgetQueuedEventGroup(requestId);
            emitResponseEvent(queued.getOrThrow().responseReceivedEvent(), params);
            queued.getOrThrow().loadingFinishedEvent().ifPresent(this::emitLoadingFinished);
            queued.getOrThrow().loadingFailedEvent().ifPresent(this::emitLoadingFailed);
            return;
        }
        eventManager.recordResponseExtraInfo(requestId, params);
        eventManager.response(requestId).ifPresent(response -> response.mergeExtraInfo(params));
    }

    /**
     * Handles on loading finished.
     *
     * @param params protocol parameters
     */
    private void onLoadingFinished(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        Optional<CdpNetworkEventManager.QueuedEventGroup> queued = eventManager.getQueuedEventGroup(requestId);
        if (queued.isPresent()) {
            queued.getOrThrow().setLoadingFinishedEvent(params);
            return;
        }
        emitLoadingFinished(params);
    }

    /**
     * Handles emit loading finished.
     *
     * @param params protocol parameters
     */
    private void emitLoadingFinished(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        eventManager.request(requestId).ifPresent(request -> {
            request.setClient(session);
            forgetRequest(request, true);
            Logger.debug(
                    false,
                    "Network",
                    "Request finished: id={}, url={}",
                    requestId,
                    StringKit.isBlank(request.url()) ? Normal.EMPTY
                            : request.url().contains("?")
                                    ? request.url().substring(Normal._0, request.url().indexOf('?'))
                                    : request.url());
            emit(REQUEST_FINISHED, request);
        });
    }

    /**
     * Handles on loading failed.
     *
     * @param params protocol parameters
     */
    private void onLoadingFailed(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        Optional<CdpNetworkEventManager.QueuedEventGroup> queued = eventManager.getQueuedEventGroup(requestId);
        if (queued.isPresent()) {
            queued.getOrThrow().setLoadingFailedEvent(params);
            return;
        }
        emitLoadingFailed(params);
    }

    /**
     * Handles emit loading failed.
     *
     * @param params protocol parameters
     */
    private void emitLoadingFailed(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        eventManager.request(requestId).ifPresent(request -> {
            request.setClient(session);
            request.markFailed(PayloadReader.text(params.get("errorText")));
            forgetRequest(request, true);
            Logger.warn(
                    false,
                    "Network",
                    "Request failed: id={}, reason={}, url={}",
                    requestId,
                    PayloadReader.text(params.get("errorText")),
                    StringKit.isBlank(request.url()) ? Normal.EMPTY
                            : request.url().contains("?")
                                    ? request.url().substring(Normal._0, request.url().indexOf('?'))
                                    : request.url());
            emit(REQUEST_FAILED, request);
        });
    }

    /**
     * Handles replay queued event group.
     *
     * @param requestId request ID value
     */
    private void replayQueuedEventGroup(String requestId) {
        eventManager.getQueuedEventGroup(requestId).ifPresent(group -> {
            eventManager.forgetQueuedEventGroup(requestId);
            onResponseReceived(group.responseReceivedEvent());
            group.loadingFinishedEvent().ifPresent(this::onLoadingFinished);
            group.loadingFailedEvent().ifPresent(this::onLoadingFailed);
        });
    }

    /**
     * Handles forget request.
     *
     * @param request request object
     * @param events  events value
     */
    private void forgetRequest(CdpRequest request, boolean events) {
        eventManager.forgetRequest(request.requestId());
        request.interceptionId().ifPresent(attemptedAuthentications::remove);
        if (events) {
            eventManager.forget(request.requestId());
        }
    }

    /**
     * Handles apply current configuration.
     */
    private void applyCurrentConfiguration() {
        if (extraHTTPHeaders != null) {
            send("Network.setExtraHTTPHeaders", Map.of("headers", extraHTTPHeaders));
        }
        if (userCacheDisabled != null) {
            applyProtocolCacheDisabled();
        }
        if (protocolRequestInterceptionEnabled != null) {
            if (protocolRequestInterceptionEnabled) {
                send(
                        "Fetch.enable",
                        Map.of("patterns", List.of(Map.of("urlPattern", Symbol.STAR)), "handleAuthRequests", true));
            } else {
                send("Fetch.disable", Map.of());
            }
        }
        if (userAgentOptions != null) {
            send("Network.setUserAgentOverride", userAgentParams(userAgentOptions));
        }
    }

    /**
     * Normalizes user agent options.
     *
     * @param options options
     * @return normalized options
     */
    private UserAgentOptions normalizeUserAgentOptions(UserAgentOptions options) {
        UserAgentOptions actual = new UserAgentOptions();
        actual.setUserAgent(options == null || options.getUserAgent() == null ? Normal.EMPTY : options.getUserAgent());
        actual.setPlatform(options == null ? null : options.getPlatform());
        if (options != null && options.getUserAgentMetadata() != null) {
            actual.setUserAgentMetadata(new LinkedHashMap<>(options.getUserAgentMetadata()));
        }
        return actual;
    }

    /**
     * Creates CDP user agent override parameters.
     *
     * @param options options
     * @return params
     */
    private Map<String, Object> userAgentParams(UserAgentOptions options) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userAgent", options.getUserAgent() == null ? Normal.EMPTY : options.getUserAgent());
        if (StringKit.isNotBlank(options.getPlatform())) {
            params.put("platform", options.getPlatform());
        }
        if (options.getUserAgentMetadata() != null && !options.getUserAgentMetadata().isEmpty()) {
            params.put("userAgentMetadata", options.getUserAgentMetadata());
        }
        return params;
    }

    /**
     * Returns the apply protocol request interception.
     *
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyProtocolRequestInterception() {
        boolean enabled = userRequestInterceptionEnabled || authUsername != null;
        if (protocolRequestInterceptionEnabled != null && protocolRequestInterceptionEnabled == enabled) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        protocolRequestInterceptionEnabled = enabled;
        Logger.debug(false, "Network", "Protocol request interception changed: enabled={}", enabled);
        if (enabled) {
            if (userCacheDisabled == null) {
                userCacheDisabled = false;
            }
            applyProtocolCacheDisabled();
            return send(
                    "Fetch.enable",
                    Map.of("patterns", List.of(Map.of("urlPattern", Symbol.STAR)), "handleAuthRequests", true));
        }
        return send("Fetch.disable", Map.of());
    }

    /**
     * Returns the apply protocol cache disabled.
     *
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyProtocolCacheDisabled() {
        if (userCacheDisabled == null) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        Logger.debug(false, "Network", "Protocol cache disabled changed: disabled={}", userCacheDisabled);
        return send("Network.setCacheDisabled", Map.of("cacheDisabled", userCacheDisabled));
    }

    /**
     * Returns the same request.
     *
     * @param requestWillBeSent request will be sent value
     * @param requestPaused     request paused value
     * @return {@code true} when the condition matches
     */
    private boolean sameRequest(CdpPayload requestWillBeSent, CdpPayload requestPaused) {
        CdpPayload networkRequest = requestWillBeSent.get("request");
        CdpPayload fetchRequest = requestPaused.get("request");
        return PayloadReader.text(networkRequest.get("url")).equals(PayloadReader.text(fetchRequest.get("url")))
                && PayloadReader.text(networkRequest.get("method"))
                        .equals(PayloadReader.text(fetchRequest.get("method")));
    }

    /**
     * Emits an event to registered listeners.
     *
     * @param event   event type
     * @param payload protocol payload
     */
    private void emit(String event, Object payload) {
        emitter.emit(event, payload);
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return completion future
     */
    private CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        if (session == null) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        CompletableFuture<CdpPayload> result = session.send(method, params);
        result.whenComplete((payload, error) -> {
            if (error == null) {
                Logger.trace(false, "Network", "Network protocol command completed: method={}", method);
            } else {
                Logger.warn(false, "Network", error, "Network protocol command failed: method={}", method);
            }
        });
        return result;
    }

}
