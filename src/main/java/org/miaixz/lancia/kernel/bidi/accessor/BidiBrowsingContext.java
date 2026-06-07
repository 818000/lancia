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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.bidi.session.BidiProtocolSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi browsing context.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiBrowsingContext implements AutoCloseable {

    /**
     * Shared constant for closed.
     */
    public static final String CLOSED = "closed";

    /**
     * Shared constant for browsing context.
     */
    public static final String BROWSING_CONTEXT = "browsingcontext";

    /**
     * Shared constant for navigation.
     */
    public static final String NAVIGATION = "navigation";

    /**
     * Shared constant for file dialog opened.
     */
    public static final String FILE_DIALOG_OPENED = "filedialogopened";

    /**
     * Shared constant for request.
     */
    public static final String REQUEST = "request";

    /**
     * Shared constant for log.
     */
    public static final String LOG = "log";

    /**
     * Shared constant for user prompt.
     */
    public static final String USER_PROMPT = "userprompt";

    /**
     * Shared constant for history updated.
     */
    public static final String HISTORY_UPDATED = "historyUpdated";

    /**
     * Shared constant for DOM content loaded.
     */
    public static final String DOM_CONTENT_LOADED = "DOMContentLoaded";

    /**
     * Shared constant for load.
     */
    public static final String LOAD = "load";

    /**
     * Shared constant for worker.
     */
    public static final String WORKER = "worker";
    /**
     * Current browser.
     */
    private final BidiBrowser browser;
    /**
     * Current user context.
     */
    private final String userContext;
    /**
     * Current parent.
     */
    private final BidiBrowsingContext parent;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current original opener.
     */
    private final String originalOpener;
    /**
     * Current window ID.
     */
    private final String windowId;
    /**
     * Mapped children values.
     */
    private final Map<String, BidiBrowsingContext> children = new ConcurrentHashMap<>();
    /**
     * Registered realms values.
     */
    private final List<String> realms = new ArrayList<>();
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Current bluetooth.
     */
    private final CdpPayload bluetooth = CdpPayload.NULL;
    /**
     * Current URL.
     */
    private volatile String url;
    /**
     * Whether JavaScript execution is enabled.
     */
    private volatile boolean javaScriptEnabled = true;
    /**
     * Whether client hints are set is enabled.
     */
    private volatile boolean clientHintsAreSet;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    /**
     * Current reason.
     */
    private volatile String reason;

    /**
     * Returns the from.
     *
     * @param browser        browser instance
     * @param userContext    user context value
     * @param parent         parent value
     * @param id             identifier
     * @param url            target URL
     * @param originalOpener original opener value
     * @param clientWindow   client window value
     * @return from value
     */
    public static BidiBrowsingContext from(
            BidiBrowser browser,
            String userContext,
            BidiBrowsingContext parent,
            String id,
            String url,
            String originalOpener,
            String clientWindow) {
        BidiBrowsingContext context = new BidiBrowsingContext(browser, userContext, parent, id, url, originalOpener,
                clientWindow);
        context.initialize();
        return context;
    }

    /**
     * Creates a BiDi browsing context.
     *
     * @param browser        browser instance
     * @param userContext    user context
     * @param parent         parent
     * @param id             identifier
     * @param url            target URL
     * @param originalOpener original opener
     * @param clientWindow   client window
     */
    private BidiBrowsingContext(BidiBrowser browser, String userContext, BidiBrowsingContext parent, String id,
            String url, String originalOpener, String clientWindow) {
        this.browser = Assert.notNull(browser, "browser");
        this.userContext = StringKit.isBlank(userContext) ? BidiBrowser.DEFAULT_USER_CONTEXT : userContext;
        this.parent = parent;
        this.id = Assert.notBlank(id, "id");
        this.url = url == null ? Normal.EMPTY : url;
        this.originalOpener = originalOpener;
        this.windowId = clientWindow == null ? Normal.EMPTY : clientWindow;
    }

    /**
     * Handles initialize.
     */
    private void initialize() {
        binding = binding.combine(session().connection().on("input.fileDialogOpened", this::onFileDialogOpened));
        binding = binding.combine(session().connection().on("browsingContext.contextCreated", this::onContextCreated));
        binding = binding
                .combine(session().connection().on("browsingContext.contextDestroyed", this::onContextDestroyed));
        binding = binding.combine(session().connection().on("browsingContext.historyUpdated", this::onHistoryUpdated));
        binding = binding
                .combine(session().connection().on("browsingContext.domContentLoaded", this::onDomContentLoaded));
        binding = binding.combine(session().connection().on("browsingContext.load", this::onLoad));
        binding = binding
                .combine(session().connection().on("browsingContext.navigationStarted", this::onNavigationStarted));
        binding = binding.combine(session().connection().on("network.beforeRequestSent", this::onBeforeRequestSent));
        binding = binding.combine(session().connection().on("log.entryAdded", this::onLogEntryAdded));
        binding = binding
                .combine(session().connection().on("browsingContext.userPromptOpened", this::onUserPromptOpened));
    }

    /**
     * Returns the activate.
     *
     * @return completion future
     */
    public CompletableFuture<Void> activate() {
        return sendVoid("browsingContext.activate", Map.of("context", id));
    }

    /**
     * Returns the capture screenshot.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<String> captureScreenshot(Map<String, Object> options) {
        Map<String, Object> params = withContext(options);
        return send("browsingContext.captureScreenshot", params)
                .thenApply(result -> PayloadReader.text(result.get("data")));
    }

    /**
     * Returns the close context.
     *
     * @param promptUnload prompt unload value
     * @return completion future
     */
    public CompletableFuture<Void> closeContext(Boolean promptUnload) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("context", id);
        if (promptUnload != null) {
            params.put("promptUnload", promptUnload);
        }
        return sendVoid("browsingContext.close", params);
    }

    /**
     * Returns the traverse history.
     *
     * @param delta delta value
     * @return completion future
     */
    public CompletableFuture<Void> traverseHistory(int delta) {
        return sendVoid("browsingContext.traverseHistory", Map.of("context", id, "delta", delta));
    }

    /**
     * Sends a navigation request.
     *
     * @param url  target URL
     * @param wait wait
     * @return navigation result
     */
    public CompletableFuture<Void> navigate(String url, String wait) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("context", id);
        params.put("url", Assert.notBlank(url, "url"));
        if (StringKit.isNotBlank(wait)) {
            params.put("wait", wait);
        }
        return sendVoid("browsingContext.navigate", params);
    }

    /**
     * Returns the reload.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<Void> reload(Map<String, Object> options) {
        return sendVoid("browsingContext.reload", withContext(options));
    }

    /**
     * Updates cache behavior.
     *
     * @param cacheBehavior cache behavior
     * @return set cache behavior value
     */
    public CompletableFuture<Void> setCacheBehavior(String cacheBehavior) {
        return sendVoid("network.setCacheBehavior", Map.of("contexts", List.of(id), "cacheBehavior", cacheBehavior));
    }

    /**
     * Returns the print.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<String> print(Map<String, Object> options) {
        return send("browsingContext.print", withContext(options))
                .thenApply(result -> PayloadReader.text(result.get("data")));
    }

    /**
     * Handles user prompt.
     *
     * @param options operation options
     * @return handle user prompt value
     */
    public CompletableFuture<Void> handleUserPrompt(Map<String, Object> options) {
        return sendVoid("browsingContext.handleUserPrompt", withContext(options));
    }

    /**
     * Updates viewport.
     *
     * @param options operation options
     * @return set viewport value
     */
    public CompletableFuture<Void> setViewport(Map<String, Object> options) {
        return sendVoid("browsingContext.setViewport", withContext(options));
    }

    /**
     * Updates touch override.
     *
     * @param maxTouchPoints max touch points
     * @return set touch override value
     */
    public CompletableFuture<Void> setTouchOverride(Integer maxTouchPoints) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contexts", List.of(id));
        params.put("maxTouchPoints", maxTouchPoints == null ? CdpPayload.NULL : maxTouchPoints);
        return sendVoid("emulation.setTouchOverride", params);
    }

    /**
     * Performs actions.
     *
     * @param actions actions
     * @return perform actions value
     */
    public CompletableFuture<Void> performActions(List<Map<String, Object>> actions) {
        return sendVoid(
                "input.performActions",
                Map.of("context", id, "actions", actions == null ? List.of() : actions));
    }

    /**
     * Releases actions.
     *
     * @return release actions value
     */
    public CompletableFuture<Void> releaseActions() {
        return sendVoid("input.releaseActions", Map.of("context", id));
    }

    /**
     * Creates window realm.
     *
     * @param sandbox sandbox
     * @return created window realm
     */
    public String createWindowRealm(String sandbox) {
        assertActive();
        String actualSandbox = Assert.notBlank(sandbox, "sandbox");
        realms.add(actualSandbox);
        return actualSandbox;
    }

    /**
     * Adds preload script.
     *
     * @param functionDeclaration function declaration
     * @param options             operation options
     * @return add preload script value
     */
    public CompletableFuture<String> addPreloadScript(String functionDeclaration, Map<String, Object> options) {
        assertActive();
        return browser.addPreloadScript(functionDeclaration, List.of(id), options);
    }

    /**
     * Adds intercept.
     *
     * @param options operation options
     * @return add intercept value
     */
    public CompletableFuture<String> addIntercept(Map<String, Object> options) {
        return send("network.addIntercept", withContexts(options))
                .thenApply(result -> PayloadReader.text(result.get("intercept")));
    }

    /**
     * Removes preload script.
     *
     * @param script script source
     * @return remove preload script value
     */
    public CompletableFuture<Void> removePreloadScript(String script) {
        assertActive();
        return browser.removePreloadScript(script);
    }

    /**
     * Updates geolocation override.
     *
     * @param options operation options
     * @return set geolocation override value
     */
    public CompletableFuture<Void> setGeolocationOverride(Map<String, Object> options) {
        Map<String, Object> actualOptions = options == null ? Map.of() : options;
        if (!actualOptions.containsKey("coordinates")) {
            return Awaitable.failed("Missing coordinates parameter.");
        }
        return sendVoid("emulation.setGeolocationOverride", withContexts(actualOptions));
    }

    /**
     * Updates timezone override.
     *
     * @param timezoneId timezone id
     * @return set timezone override value
     */
    public CompletableFuture<Void> setTimezoneOverride(String timezoneId) {
        String timezone = timezoneId != null && timezoneId.startsWith("GMT")
                ? timezoneId.replaceFirst("GMT", Normal.EMPTY)
                : timezoneId;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contexts", List.of(id));
        params.put("timezone", timezone == null ? CdpPayload.NULL : timezone);
        return sendVoid("emulation.setTimezoneOverride", params);
    }

    /**
     * Updates screen orientation override.
     *
     * @param screenOrientation screen orientation
     * @return set screen orientation override value
     */
    public CompletableFuture<Void> setScreenOrientationOverride(Map<String, Object> screenOrientation) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contexts", List.of(id));
        params.put("screenOrientation", screenOrientation == null ? CdpPayload.NULL : screenOrientation);
        return sendVoid("emulation.setScreenOrientationOverride", params);
    }

    /**
     * Returns the cookies.
     *
     * @param options operation options
     * @return cookies
     */
    public CompletableFuture<List<CdpPayload>> getCookies(Map<String, Object> options) {
        return send("storage.getCookies", withPartition(options))
                .thenApply(result -> PayloadReader.elements(result.get("cookies"), CdpPayload.class));
    }

    /**
     * Updates cookie.
     *
     * @param cookie cookie value
     * @return set cookie value
     */
    public CompletableFuture<Void> setCookie(Map<String, Object> cookie) {
        return sendVoid(
                "storage.setCookie",
                Map.of("cookie", cookie == null ? Map.of() : cookie, "partition", partition()));
    }

    /**
     * Updates files.
     *
     * @param element element handle
     * @param files   files
     * @return set files value
     */
    public CompletableFuture<Void> setFiles(Map<String, Object> element, List<String> files) {
        return sendVoid(
                "input.setFiles",
                Map.of(
                        "context",
                        id,
                        "element",
                        element == null ? Map.of() : element,
                        "files",
                        files == null ? List.of() : files));
    }

    /**
     * Returns the subscribe.
     *
     * @param events events value
     * @return completion future
     */
    public CompletableFuture<Void> subscribe(List<String> events) {
        assertActive();
        return session().subscribe(events, List.of(id)).thenApply(result -> null);
    }

    /**
     * Adds interception.
     *
     * @param events events
     * @return add interception value
     */
    public CompletableFuture<Void> addInterception(List<String> events) {
        return subscribe(events);
    }

    /**
     * Returns the delete cookie.
     *
     * @param cookieFilters cookie filters value
     * @return completion future
     */
    public CompletableFuture<Void> deleteCookie(List<Map<String, Object>> cookieFilters) {
        assertActive();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        List<Map<String, Object>> filters = cookieFilters == null ? List.of() : cookieFilters;
        for (Map<String, Object> filter : filters) {
            chain = chain.thenCompose(
                    value -> session()
                            .send(
                                    "storage.deleteCookies",
                                    Map.of("filter", filter == null ? Map.of() : filter, "partition", partition()))
                            .thenApply(result -> null));
        }
        return chain;
    }

    /**
     * Returns the locate nodes.
     *
     * @param locator    locator value
     * @param startNodes start nodes value
     * @return completion future
     */
    public CompletableFuture<List<CdpPayload>> locateNodes(
            Map<String, Object> locator,
            List<Map<String, Object>> startNodes) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("context", id);
        params.put("locator", locator == null ? Map.of() : locator);
        if (startNodes != null && !startNodes.isEmpty()) {
            params.put("startNodes", startNodes);
        }
        return send("browsingContext.locateNodes", params)
                .thenApply(result -> PayloadReader.elements(result.get("nodes"), CdpPayload.class));
    }

    /**
     * Updates java script enabled.
     *
     * @param enabled enabled
     * @return set java script enabled value
     */
    public CompletableFuture<Void> setJavaScriptEnabled(boolean enabled) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("enabled", enabled ? CdpPayload.NULL : Boolean.FALSE);
        params.put("contexts", List.of(id));
        return sendVoid("emulation.setScriptingEnabled", params).thenRun(() -> javaScriptEnabled = enabled);
    }

    /**
     * Returns whether java script is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isJavaScriptEnabled() {
        return javaScriptEnabled;
    }

    /**
     * Updates user agent.
     *
     * @param userAgent user agent
     * @return set user agent value
     */
    public CompletableFuture<Void> setUserAgent(String userAgent) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userAgent", userAgent == null ? CdpPayload.NULL : userAgent);
        params.put("contexts", List.of(id));
        return sendVoid("emulation.setUserAgentOverride", params);
    }

    /**
     * Updates client hints override.
     *
     * @param clientHints client hints
     * @return set client hints override value
     */
    public CompletableFuture<Void> setClientHintsOverride(Map<String, Object> clientHints) {
        if (clientHints == null && !clientHintsAreSet) {
            return CompletableFuture.completedFuture(null);
        }
        clientHintsAreSet = true;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("clientHints", clientHints == null ? CdpPayload.NULL : clientHints);
        params.put("contexts", List.of(id));
        return sendVoid("userAgentClientHints.setClientHintsOverride", params);
    }

    /**
     * Updates offline mode.
     *
     * @param enabled enabled
     * @return set offline mode value
     */
    public CompletableFuture<Void> setOfflineMode(boolean enabled) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("networkConditions", enabled ? Map.of("type", "offline") : CdpPayload.NULL);
        params.put("contexts", List.of(id));
        return sendVoid("emulation.setNetworkConditions", params);
    }

    /**
     * Updates extra HTTP headers.
     *
     * @param headers HTTP headers
     * @return set extra HTTP headers value
     */
    public CompletableFuture<Void> setExtraHTTPHeaders(Map<String, String> headers) {
        List<Map<String, Object>> values = new ArrayList<>();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getValue() == null) {
                    return Awaitable.failed("HTTP header value must be a string: " + entry.getKey());
                }
                values.add(
                        Map.of(
                                "name",
                                entry.getKey().toLowerCase(),
                                "value",
                                Map.of("type", "string", "value", entry.getValue())));
            }
        }
        return sendVoid("network.setExtraHeaders", Map.of("headers", values, "contexts", List.of(id)));
    }

    /**
     * Returns the bluetooth.
     *
     * @return bluetooth value
     */
    public CdpPayload bluetooth() {
        return bluetooth;
    }

    /**
     * Returns the children.
     *
     * @return values
     */
    public List<BidiBrowsingContext> children() {
        return List.copyOf(children.values());
    }

    /**
     * Converts this value to p.
     *
     * @return p
     */
    public BidiBrowsingContext top() {
        BidiBrowsingContext context = this;
        while (context.parent != null) {
            context = context.parent;
        }
        return context;
    }

    /**
     * Returns the realms.
     *
     * @return values
     */
    public List<String> realms() {
        return List.copyOf(realms);
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
     *
     * @param reason reason
     */
    public void dispose(String reason) {
        if (disposed.compareAndSet(false, true)) {
            this.reason = StringKit.isBlank(reason) ? "Browsing context already closed." : reason;
            for (BidiBrowsingContext child : List.copyOf(children.values())) {
                child.dispose("Parent browsing context was disposed");
            }
            children.clear();
            binding.unbind();
            binding = new EventBinding();
            emitter.emit(CLOSED, this.reason);
        }
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        closeContext(null).join();
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
     * Returns the user context.
     *
     * @return user context value
     */
    public String userContext() {
        return userContext;
    }

    /**
     * Returns the parent.
     *
     * @return optional value
     */
    public Optional<BidiBrowsingContext> parent() {
        return Optional.ofNullable(parent);
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
     * Returns the original opener.
     *
     * @return optional value
     */
    public Optional<String> originalOpener() {
        return Optional.ofNullable(originalOpener);
    }

    /**
     * Returns the window ID.
     *
     * @return window ID value
     */
    public String windowId() {
        return windowId;
    }

    /**
     * Returns whether this object is closed.
     *
     * @return whether this object is closed
     */
    public boolean closed() {
        return disposed.get();
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
     * Returns the reason.
     *
     * @return reason value
     */
    public String reason() {
        return reason == null ? Normal.EMPTY : reason;
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
     * Returns the session.
     *
     * @return session value
     */
    private BidiProtocolSession session() {
        return browser.session();
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return completion future
     */
    private CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi BrowsingContext has been closed: " + id);
        }
        return session().send(method, params);
    }

    /**
     * Returns the send void.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return completion future
     */
    private CompletableFuture<Void> sendVoid(String method, Map<String, Object> params) {
        return send(method, params).thenApply(result -> null);
    }

    /**
     * Asserts the active condition.
     */
    private void assertActive() {
        if (disposed.get()) {
            throw new InternalException("BiDi BrowsingContext has been closed: " + id);
        }
    }

    /**
     * Returns the with context.
     *
     * @param options operation options
     * @return mapped values
     */
    private Map<String, Object> withContext(Map<String, Object> options) {
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("context", id);
        return params;
    }

    /**
     * Returns the with contexts.
     *
     * @param options operation options
     * @return mapped values
     */
    private Map<String, Object> withContexts(Map<String, Object> options) {
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("contexts", List.of(id));
        return params;
    }

    /**
     * Returns the with partition.
     *
     * @param options operation options
     * @return mapped values
     */
    private Map<String, Object> withPartition(Map<String, Object> options) {
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("partition", partition());
        return params;
    }

    /**
     * Returns the partition.
     *
     * @return mapped values
     */
    private Map<String, Object> partition() {
        return Map.of("type", "context", "context", id);
    }

    /**
     * Handles on file dialog opened.
     *
     * @param info info value
     */
    private void onFileDialogOpened(CdpPayload info) {
        emitIfContext(FILE_DIALOG_OPENED, info);
    }

    /**
     * Handles on context created.
     *
     * @param info info value
     */
    private void onContextCreated(CdpPayload info) {
        if (!id.equals(PayloadReader.text(info.get("parent")))) {
            return;
        }
        BidiBrowsingContext child = BidiBrowsingContext.from(
                browser,
                userContext,
                this,
                PayloadReader.text(info.get("context")),
                PayloadReader.text(info.get("url")),
                PayloadReader.nullableText(info.get("originalOpener")),
                PayloadReader.text(info.get("clientWindow")));
        children.put(child.id(), child);
        emitter.emit(BROWSING_CONTEXT, child);
    }

    /**
     * Handles on context destroyed.
     *
     * @param info info value
     */
    private void onContextDestroyed(CdpPayload info) {
        if (id.equals(PayloadReader.text(info.get("context")))) {
            dispose("Browsing context already closed.");
        }
    }

    /**
     * Handles on history updated.
     *
     * @param info info value
     */
    private void onHistoryUpdated(CdpPayload info) {
        if (!id.equals(PayloadReader.text(info.get("context")))) {
            return;
        }
        url = PayloadReader.text(info.get("url"));
        emitter.emit(HISTORY_UPDATED, info);
    }

    /**
     * Handles on DOM content loaded.
     *
     * @param info info value
     */
    private void onDomContentLoaded(CdpPayload info) {
        if (!id.equals(PayloadReader.text(info.get("context")))) {
            return;
        }
        url = PayloadReader.text(info.get("url"));
        emitter.emit(DOM_CONTENT_LOADED, info);
    }

    /**
     * Handles on load.
     *
     * @param info info value
     */
    private void onLoad(CdpPayload info) {
        if (!id.equals(PayloadReader.text(info.get("context")))) {
            return;
        }
        url = PayloadReader.text(info.get("url"));
        emitter.emit(LOAD, info);
    }

    /**
     * Handles on navigation started.
     *
     * @param info info value
     */
    private void onNavigationStarted(CdpPayload info) {
        emitIfContext(NAVIGATION, info);
    }

    /**
     * Handles on before request sent.
     *
     * @param info info value
     */
    private void onBeforeRequestSent(CdpPayload info) {
        if (!id.equals(PayloadReader.text(info.get("context")))
                || PayloadReader.number(info.get("redirectCount")) > 0) {
            return;
        }
        emitter.emit(REQUEST, info);
    }

    /**
     * Handles on log entry added.
     *
     * @param info info value
     */
    private void onLogEntryAdded(CdpPayload info) {
        CdpPayload source = info.get("source");
        if (source != null && id.equals(PayloadReader.text(source.get("context")))) {
            emitter.emit(LOG, info);
        }
    }

    /**
     * Handles on user prompt opened.
     *
     * @param info info value
     */
    private void onUserPromptOpened(CdpPayload info) {
        emitIfContext(USER_PROMPT, info);
    }

    /**
     * Handles emit if context.
     *
     * @param event event type
     * @param info  info value
     */
    private void emitIfContext(String event, CdpPayload info) {
        if (id.equals(PayloadReader.text(info.get("context")))) {
            emitter.emit(event, info);
        }
    }

}
