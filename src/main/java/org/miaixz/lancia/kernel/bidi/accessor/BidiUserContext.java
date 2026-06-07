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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi user context.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiUserContext implements AutoCloseable {

    /**
     * Shared constant for default.
     */
    public static final String DEFAULT = BidiBrowser.DEFAULT_USER_CONTEXT;

    /**
     * Shared constant for browsing context.
     */
    public static final String BROWSING_CONTEXT = "browsingcontext";

    /**
     * Shared constant for closed.
     */
    public static final String CLOSED = "closed";
    /**
     * Current browser.
     */
    private final BidiBrowser browser;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Mapped browsing contexts values.
     */
    private final Map<String, BidiBrowsingContext> browsingContexts = new ConcurrentHashMap<>();
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Current reason.
     */
    private volatile String reason;

    /**
     * Returns the create.
     *
     * @param browser browser instance
     * @param id      identifier
     * @return create value
     */
    public static BidiUserContext create(BidiBrowser browser, String id) {
        BidiUserContext context = new BidiUserContext(browser, id);
        context.initialize();
        return context;
    }

    /**
     * Creates a BiDi user context.
     *
     * @param browser browser instance
     * @param id      identifier
     */
    private BidiUserContext(BidiBrowser browser, String id) {
        this.browser = Assert.notNull(browser, "browser");
        this.id = StringKit.isBlank(id) ? DEFAULT : id;
    }

    /**
     * Handles initialize.
     */
    private void initialize() {
        binding = binding
                .combine(browser.once(BidiBrowser.CLOSED, value -> dispose("User context was closed: " + value)));
        binding = binding
                .combine(browser.once(BidiBrowser.DISCONNECTED, value -> dispose("User context was closed: " + value)));
        binding = binding
                .combine(browser.session().connection().on("browsingContext.contextCreated", this::onContextCreated));
    }

    /**
     * Creates browsing context.
     *
     * @param type    type name
     * @param options operation options
     * @return created browsing context
     */
    public CompletableFuture<BidiBrowsingContext> createBrowsingContext(String type, Map<String, Object> options) {
        if (closed()) {
            return Awaitable.failed(reason());
        }
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("type", StringKit.isBlank(type) ? "tab" : type);
        params.put("userContext", id);
        Object reference = params.get("referenceContext");
        if (reference instanceof BidiBrowsingContext browsingContext) {
            params.put("referenceContext", browsingContext.id());
        }
        return browser.session().send("browsingContext.create", params).thenApply(result -> {
            String contextId = PayloadReader.text(result.get("context"));
            BidiBrowsingContext context = browsingContexts.get(contextId);
            if (context == null) {
                context = BidiBrowsingContext.from(
                        browser,
                        id,
                        null,
                        contextId,
                        PayloadReader.text(result.get("url")),
                        null,
                        PayloadReader.text(result.get("clientWindow")));
                browsingContexts.put(context.id(), context);
            }
            return context;
        });
    }

    /**
     * Returns the remove.
     *
     * @return completion future
     */
    public CompletableFuture<Void> remove() {
        if (closed()) {
            return Awaitable.failed(reason());
        }
        CompletableFuture<Void> result = new CompletableFuture<>();
        browser.session().send("browser.removeUserContext", Map.of("userContext", id))
                .whenComplete((value, throwable) -> {
                    dispose("User context already closed.");
                    if (throwable != null) {
                        result.completeExceptionally(throwable);
                        return;
                    }
                    result.complete(null);
                });
        return result;
    }

    /**
     * Returns the cookies.
     *
     * @param options      operation options
     * @param sourceOrigin source origin
     * @return cookies
     */
    public CompletableFuture<List<CdpPayload>> getCookies(Map<String, Object> options, String sourceOrigin) {
        if (closed()) {
            return Awaitable.failed(reason());
        }
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("partition", partition(sourceOrigin));
        return browser.session().send("storage.getCookies", params)
                .thenApply(result -> PayloadReader.elements(result.get("cookies"), CdpPayload.class));
    }

    /**
     * Updates cookie.
     *
     * @param cookie       cookie value
     * @param sourceOrigin source origin
     * @return set cookie value
     */
    public CompletableFuture<Void> setCookie(Map<String, Object> cookie, String sourceOrigin) {
        if (closed()) {
            return Awaitable.failed(reason());
        }
        return browser.session()
                .send(
                        "storage.setCookie",
                        Map.of("cookie", cookie == null ? Map.of() : cookie, "partition", partition(sourceOrigin)))
                .thenApply(result -> null);
    }

    /**
     * Updates permissions.
     *
     * @param origin     origin
     * @param descriptor descriptor
     * @param state      state
     * @return set permissions value
     */
    public CompletableFuture<Void> setPermissions(String origin, Map<String, Object> descriptor, String state) {
        if (closed()) {
            return Awaitable.failed(reason());
        }
        return browser.session()
                .send(
                        "permissions.setPermission",
                        Map.of(
                                "origin",
                                Assert.notBlank(origin, "origin"),
                                "descriptor",
                                descriptor == null ? Map.of() : descriptor,
                                "state",
                                Assert.notBlank(state, "state"),
                                "userContext",
                                id))
                .thenApply(result -> null);
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
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        remove().join();
    }

    /**
     * Releases resources held by this object.
     *
     * @param reason reason
     */
    public void dispose(String reason) {
        if (this.reason != null) {
            return;
        }
        this.reason = StringKit.isBlank(reason)
                ? "User context already closed, probably because the browser disconnected/closed."
                : reason;
        binding.unbind();
        binding = new EventBinding();
        browsingContexts.clear();
        emitter.emit(CLOSED, this.reason);
    }

    /**
     * Returns the browsing contexts.
     *
     * @return values
     */
    public List<BidiBrowsingContext> browsingContexts() {
        return List.copyOf(browsingContexts.values());
    }

    /**
     * Returns the browser.
     *
     * @return browser value
     */
    public BidiBrowser browser() {
        return browser;
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
     * Returns whether this object is closed.
     *
     * @return whether this object is closed
     */
    public boolean closed() {
        return reason != null;
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return closed();
    }

    /**
     * Returns the reason.
     *
     * @return reason value
     */
    public String reason() {
        return reason == null ? Normal.EMPTY : reason;
    }

    /**
     * Handles on context created.
     *
     * @param info info value
     */
    private void onContextCreated(CdpPayload info) {
        if (StringKit.isNotBlank(PayloadReader.text(info.get("parent")))
                || !id.equals(PayloadReader.text(info.get("userContext")))) {
            return;
        }
        BidiBrowsingContext context = BidiBrowsingContext.from(
                browser,
                id,
                null,
                PayloadReader.text(info.get("context")),
                PayloadReader.text(info.get("url")),
                PayloadReader.nullableText(info.get("originalOpener")),
                PayloadReader.text(info.get("clientWindow")));
        browsingContexts.put(context.id(), context);
        context.once(BidiBrowsingContext.CLOSED, value -> browsingContexts.remove(context.id()));
        emitter.emit(BROWSING_CONTEXT, context);
    }

    /**
     * Returns the partition.
     *
     * @param sourceOrigin source origin value
     * @return mapped values
     */
    private Map<String, Object> partition(String sourceOrigin) {
        Map<String, Object> partition = new LinkedHashMap<>();
        partition.put("type", "storageKey");
        partition.put("userContext", id);
        if (StringKit.isNotBlank(sourceOrigin)) {
            partition.put("sourceOrigin", sourceOrigin);
        }
        return partition;
    }

}
