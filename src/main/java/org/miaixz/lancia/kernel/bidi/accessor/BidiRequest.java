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
package org.miaixz.lancia.kernel.bidi.accessor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.bidi.session.BidiProtocolSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi request.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiRequest implements AutoCloseable {

    /**
     * Shared constant for redirect.
     */
    public static final String REDIRECT = "redirect";

    /**
     * Shared constant for authenticate.
     */
    public static final String AUTHENTICATE = "authenticate";

    /**
     * Shared constant for success.
     */
    public static final String SUCCESS = "success";

    /**
     * Shared constant for response.
     */
    public static final String RESPONSE = "response";

    /**
     * Shared constant for error.
     */
    public static final String ERROR = "error";
    /**
     * Current context.
     */
    private final BidiBrowsingContext context;
    /**
     * Current event.
     */
    private final CdpPayload event;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Current response content future.
     */
    private CompletableFuture<byte[]> responseContentFuture;
    /**
     * Current request body future.
     */
    private CompletableFuture<Optional<String>> requestBodyFuture;
    /**
     * Current error.
     */
    private String error;
    /**
     * Current redirect.
     */
    private BidiRequest redirect;
    /**
     * Current response.
     */
    private CdpPayload response;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Returns the from.
     *
     * @param context browser context
     * @param event   event type
     * @return from value
     */
    public static BidiRequest from(BidiBrowsingContext context, CdpPayload event) {
        BidiRequest request = new BidiRequest(context, event);
        request.initialize();
        return request;
    }

    /**
     * Creates a BiDi request.
     *
     * @param context browser context
     * @param event   event name
     */
    private BidiRequest(BidiBrowsingContext context, CdpPayload event) {
        this.context = Assert.notNull(context, "context");
        this.event = Assert.notNull(event, "event");
    }

    /**
     * Handles initialize.
     */
    private void initialize() {
        binding = binding.combine(context.once(BidiBrowsingContext.CLOSED, value -> {
            error = String.valueOf(value);
            emitter.emit(ERROR, error);
            dispose();
        }));
        binding = binding.combine(session().connection().on("network.beforeRequestSent", this::onBeforeRequestSent));
        binding = binding.combine(session().connection().on("network.authRequired", this::onAuthRequired));
        binding = binding.combine(session().connection().on("network.fetchError", this::onFetchError));
        binding = binding.combine(session().connection().on("network.responseStarted", this::onResponseStarted));
        binding = binding.combine(session().connection().on("network.responseCompleted", this::onResponseCompleted));
    }

    /**
     * Returns the continue request.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<Void> continueRequest(Map<String, Object> options) {
        Map<String, Object> params = withRequest(options);
        return session().send("network.continueRequest", params).thenApply(result -> null);
    }

    /**
     * Returns the fail request.
     *
     * @return completion future
     */
    public CompletableFuture<Void> failRequest() {
        return session().send("network.failRequest", Map.of("request", id())).thenApply(result -> null);
    }

    /**
     * Returns the provide response.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<Void> provideResponse(Map<String, Object> options) {
        return session().send("network.provideResponse", withRequest(options)).thenApply(result -> null);
    }

    /**
     * Returns the fetch post data.
     *
     * @return completion future
     */
    public CompletableFuture<Optional<String>> fetchPostData() {
        if (!hasPostData()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (requestBodyFuture == null) {
            requestBodyFuture = session().send("network.getData", Map.of("dataType", "request", "request", id()))
                    .thenApply(result -> Optional.of(StringKit.toString(contentBytes(result), Charset.UTF_8)))
                    .exceptionally(throwable -> Optional.empty());
        }
        return requestBodyFuture;
    }

    /**
     * Returns the response content.
     *
     * @return completion future
     */
    public CompletableFuture<byte[]> getResponseContent() {
        if (responseContentFuture == null) {
            responseContentFuture = session().send("network.getData", Map.of("dataType", "response", "request", id()))
                    .thenApply(BidiRequest::contentBytes).exceptionally(throwable -> new byte[0]);
        }
        return responseContentFuture;
    }

    /**
     * Returns the continue with auth.
     *
     * @param parameters parameters value
     * @return completion future
     */
    public CompletableFuture<Void> continueWithAuth(Map<String, Object> parameters) {
        Map<String, Object> params = withRequest(parameters);
        return session().send("network.continueWithAuth", params).thenApply(result -> null);
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        return EventHooks.onNamed(emitter, event, listener);
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String event, Consumer<Object> listener) {
        return EventHooks.onceNamed(emitter, event, listener);
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        binding.unbind(disposed);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        dispose();
    }

    /**
     * Returns the error.
     *
     * @return optional value
     */
    public Optional<String> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Returns the headers.
     *
     * @return values
     */
    public List<CdpPayload> headers() {
        return PayloadReader.elements(request().get("headers"), CdpPayload.class);
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return PayloadReader.text(request().get("request"));
    }

    /**
     * Returns the initiator.
     *
     * @return initiator value
     */
    public CdpPayload initiator() {
        return event.get("initiator");
    }

    /**
     * Returns the method.
     *
     * @return method value
     */
    public String method() {
        return PayloadReader.text(request().get("method"));
    }

    /**
     * Returns the navigation.
     *
     * @return optional value
     */
    public Optional<String> navigation() {
        String navigation = PayloadReader.text(event.get("navigation"));
        return navigation.isEmpty() ? Optional.empty() : Optional.of(navigation);
    }

    /**
     * Returns the redirect.
     *
     * @return optional value
     */
    public Optional<BidiRequest> redirect() {
        return Optional.ofNullable(redirect);
    }

    /**
     * Returns the last redirect.
     *
     * @return optional value
     */
    public Optional<BidiRequest> lastRedirect() {
        BidiRequest current = redirect;
        while (current != null && current.redirect != null) {
            current = current.redirect;
        }
        return Optional.ofNullable(current);
    }

    /**
     * Returns the response.
     *
     * @return optional value
     */
    public Optional<CdpPayload> response() {
        return Optional.ofNullable(response);
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return PayloadReader.text(request().get("url"));
    }

    /**
     * Returns whether blocked is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isBlocked() {
        return PayloadReader.bool(event.get("isBlocked"));
    }

    /**
     * Returns the resource type.
     *
     * @return optional value
     */
    public Optional<String> resourceType() {
        String type = PayloadReader.text(request().get("goog:resourceType"));
        return type.isEmpty() ? Optional.empty() : Optional.of(type);
    }

    /**
     * Returns the post data.
     *
     * @return optional value
     */
    public Optional<String> postData() {
        String value = PayloadReader.text(request().get("goog:postData"));
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    /**
     * Returns whether post data is available.
     *
     * @return {@code true} when the condition matches
     */
    public boolean hasPostData() {
        return PayloadReader.number(request().get("bodySize")) > 0;
    }

    /**
     * Returns the timing.
     *
     * @return timing value
     */
    public CdpPayload timing() {
        return request().get("timings");
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return disposed.get();
    }

    /**
     * Handles on before request sent.
     *
     * @param nextEvent next event value
     */
    private void onBeforeRequestSent(CdpPayload nextEvent) {
        if (!matchesRequest(nextEvent)) {
            return;
        }
        boolean nextRedirect = PayloadReader
                .number(nextEvent.get("redirectCount")) == PayloadReader.number(event.get("redirectCount")) + 1;
        boolean afterAuth = hasAuthorization(nextEvent) && !hasAuthorization(event);
        if (!nextRedirect && !afterAuth) {
            return;
        }
        redirect = BidiRequest.from(context, nextEvent);
        emitter.emit(REDIRECT, redirect);
        dispose();
    }

    /**
     * Handles on auth required.
     *
     * @param authEvent auth event value
     */
    private void onAuthRequired(CdpPayload authEvent) {
        if (matchesRequest(authEvent) && PayloadReader.bool(authEvent.get("isBlocked"))) {
            emitter.emit(AUTHENTICATE, CdpPayload.NULL);
        }
    }

    /**
     * Handles on fetch error.
     *
     * @param errorEvent error event value
     */
    private void onFetchError(CdpPayload errorEvent) {
        if (!matchesRequest(errorEvent) || PayloadReader.number(errorEvent.get("redirectCount")) != PayloadReader
                .number(event.get("redirectCount"))) {
            return;
        }
        error = PayloadReader.text(errorEvent.get("errorText"));
        emitter.emit(ERROR, error);
        dispose();
    }

    /**
     * Handles on response started.
     *
     * @param responseEvent response event value
     */
    private void onResponseStarted(CdpPayload responseEvent) {
        if (!matchesRequest(responseEvent) || PayloadReader.number(responseEvent.get("redirectCount")) != PayloadReader
                .number(event.get("redirectCount"))) {
            return;
        }
        response = responseEvent.get("response");
        emitter.emit(RESPONSE, response);
    }

    /**
     * Handles on response completed.
     *
     * @param responseEvent response event value
     */
    private void onResponseCompleted(CdpPayload responseEvent) {
        if (!matchesRequest(responseEvent) || PayloadReader.number(responseEvent.get("redirectCount")) != PayloadReader
                .number(event.get("redirectCount"))) {
            return;
        }
        response = responseEvent.get("response");
        emitter.emit(SUCCESS, response);
        int status = PayloadReader.number(response.get("status"));
        if (status < 300 || status >= 400) {
            dispose();
        }
    }

    /**
     * Returns the matches request.
     *
     * @param candidate candidate value
     * @return {@code true} when the condition matches
     */
    private boolean matchesRequest(CdpPayload candidate) {
        return context.id().equals(PayloadReader.text(candidate.get("context")))
                && id().equals(PayloadReader.text(candidate.get("request").get("request")));
    }

    /**
     * Returns the with request.
     *
     * @param options operation options
     * @return mapped values
     */
    private Map<String, Object> withRequest(Map<String, Object> options) {
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("request", id());
        return params;
    }

    /**
     * Returns the request.
     *
     * @return request value
     */
    private CdpPayload request() {
        return event.get("request");
    }

    /**
     * Returns the session.
     *
     * @return session value
     */
    private BidiProtocolSession session() {
        return context.browser().session();
    }

    /**
     * Returns whether authorization is available.
     *
     * @param payload protocol payload
     * @return {@code true} when the condition matches
     */
    private static boolean hasAuthorization(CdpPayload payload) {
        for (CdpPayload header : PayloadReader.elements(payload.get("request").get("headers"), CdpPayload.class)) {
            if ("authorization".equals(PayloadReader.text(header.get("name")).toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the content bytes.
     *
     * @param payload protocol payload
     * @return content bytes value
     */
    private static byte[] contentBytes(CdpPayload payload) {
        CdpPayload bytes = PayloadReader.first(CdpPayload.NULL, payload.get("bytes"), payload.get("body"), payload);
        CdpPayload value = PayloadReader.first(CdpPayload.NULL, bytes.get("value"), payload.get("body"));
        if ("base64".equals(PayloadReader.text(bytes.get("type")))) {
            return Base64.decode(PayloadReader.text(value));
        }
        if (value.isArray()) {
            byte[] result = new byte[value.elements().size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) value.elements().get(i).asInt();
            }
            return result;
        }
        return ByteKit.toBytes(PayloadReader.text(value), Charset.UTF_8);
    }

}
