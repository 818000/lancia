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
package org.miaixz.lancia.kernel.bidi.page;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Emitter;
import org.miaixz.lancia.Extension;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Request;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.Tracing;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.PageEvent;
import org.miaixz.lancia.kernel.Accessibility;
import org.miaixz.lancia.kernel.Bluetooth;
import org.miaixz.lancia.kernel.Coverage;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.FileChooser;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.kernel.Locator;
import org.miaixz.lancia.kernel.Prompts;
import org.miaixz.lancia.kernel.Recorder;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowser;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowserContext;
import org.miaixz.lancia.kernel.bidi.device.BidiDeviceRequestPrompt;
import org.miaixz.lancia.kernel.bidi.input.BidiInput;
import org.miaixz.lancia.kernel.bidi.network.BidiRequest;
import org.miaixz.lancia.kernel.bidi.runtime.BidiRealm;
import org.miaixz.lancia.kernel.bidi.session.BidiCDPSession;
import org.miaixz.lancia.kernel.bidi.target.BidiTarget;
import org.miaixz.lancia.kernel.bidi.worker.BidiWorker;
import org.miaixz.lancia.kernel.cdp.mcp.CdpWebMCP;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.emulation.Device;
import org.miaixz.lancia.nimble.emulation.Geolocation;
import org.miaixz.lancia.nimble.emulation.IdleState;
import org.miaixz.lancia.nimble.emulation.MediaFeature;
import org.miaixz.lancia.nimble.emulation.Viewport;
import org.miaixz.lancia.nimble.network.Cookie;
import org.miaixz.lancia.nimble.network.Cookie.CookiePartitionKey;
import org.miaixz.lancia.nimble.network.CookieParam;
import org.miaixz.lancia.nimble.network.Credentials;
import org.miaixz.lancia.nimble.network.DeleteCookiesParameters;
import org.miaixz.lancia.nimble.network.NetworkConditions;
import org.miaixz.lancia.options.ClickOptions;
import org.miaixz.lancia.options.GoToOptions;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.options.PDFOptions;
import org.miaixz.lancia.options.PageCloseOptions;
import org.miaixz.lancia.options.ScreencastOptions;
import org.miaixz.lancia.options.ScreenshotOptions;
import org.miaixz.lancia.options.ScriptTagOptions;
import org.miaixz.lancia.options.StyleTagOptions;
import org.miaixz.lancia.options.UserAgentOptions;
import org.miaixz.lancia.options.WaitForOptions;
import org.miaixz.lancia.options.WaitForSelectorOptions;
import org.miaixz.lancia.runtime.SecurityPolicy;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.page.PageExtension;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Implements page operations over the WebDriver BiDi protocol.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiPage implements Page {

    /**
     * Current browser context.
     */
    private final BidiBrowserContext browserContext;
    /**
     * Current context ID.
     */
    private final String contextId;
    /**
     * Current trusted emitter.
     */
    private final EventEmitter<String> trustedEmitter = new EventEmitter<>();
    /**
     * Current page emitter.
     */
    private final EventEmitter<PageEvent> emitter = new EventEmitter<>();
    /**
     * Current main frame.
     */
    private final BidiFrame mainFrame;
    /**
     * Current device request prompt manager.
     */
    private final BidiDeviceRequestPrompt.Manager deviceRequestPromptManager;
    /**
     * Current input.
     */
    private final BidiInput input;
    /**
     * Current CdpWebMCP facade.
     */
    private final CdpWebMCP webmcp = new CdpWebMCP(null);
    /**
     * Registered workers values.
     */
    private final Set<BidiWorker> workers = ConcurrentHashMap.newKeySet();
    /**
     * Current default timeout millis.
     */
    private volatile long defaultTimeoutMillis = 30000L;
    /**
     * Current default navigation timeout millis.
     */
    private volatile long defaultNavigationTimeoutMillis = 30000L;
    /**
     * Mapped viewport values.
     */
    private volatile Viewport viewport;
    /**
     * Whether JavaScript execution is enabled.
     */
    private volatile boolean javaScriptEnabled = true;
    /**
     * Current request interception.
     */
    private volatile String requestInterception;
    /**
     * Current auth interception.
     */
    private volatile String authInterception;
    /**
     * Thread-safe render security boundary installation state.
     */
    private final AtomicBoolean renderSecurityBoundaryInstalled = new AtomicBoolean();
    /**
     * Current render security boundary event binding.
     */
    private volatile Binding renderSecurityBoundaryBinding;
    /**
     * Whether render request security boundary is enabled.
     */
    private volatile boolean renderSecurityBoundaryEnabled = true;
    /**
     * Current runtime security policy.
     */
    private volatile SecurityPolicy securityPolicy = SecurityPolicy.defaultPolicy();
    /**
     * Mapped credentials values.
     */
    private volatile Map<String, String> credentials;
    /**
     * Whether service worker bypassed is enabled.
     */
    private volatile boolean serviceWorkerBypassed;
    /**
     * Whether drag interception is enabled.
     */
    private volatile boolean dragInterception;
    /**
     * Current network conditions.
     */
    private volatile NetworkConditions networkConditions = NetworkConditions.online();
    /**
     * Current dev tools page.
     */
    private volatile BidiPage devToolsPage;
    /**
     * Whether dev tools open is enabled.
     */
    private volatile boolean devToolsOpen;
    /**
     * Current URL.
     */
    private String url;
    /**
     * Thread-safe closed state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a bidi page.
     *
     * @param browserContext browser context
     * @param contextId      context id
     * @param url            target URL
     */
    public BidiPage(BidiBrowserContext browserContext, String contextId, String url) {
        this.browserContext = browserContext;
        this.contextId = contextId;
        this.url = StringKit.isBlank(url) ? Builder.ABOUT_BLANK : url;
        this.mainFrame = BidiFrame.from(this, contextId, this.url);
        this.deviceRequestPromptManager = BidiDeviceRequestPrompt
                .manager(contextId, browserContext.browser().session());
        this.input = new BidiInput(this);
        Logger.debug(
                false,
                "Page",
                "BiDi page initialized: contextId={}, url={}",
                contextId,
                this.url.replaceAll("[?#].*$", "?<redacted>"));
        try {
            installRenderSecurityBoundary();
        } catch (RuntimeException ex) {
            Logger.warn(false, "Network", ex, "BiDi render request boundary deferred: contextId={}", contextId);
        }
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url target URL
     */
    public Response goTo(String url) {
        if (closed.get()) {
            throw new IllegalStateException("BiDi page has been closed.");
        }
        validateNavigationUrl(url);
        Logger.debug(
                true,
                "Page",
                "BiDi navigation requested: contextId={}, url={}",
                contextId,
                url == null ? Normal.EMPTY : url.replaceAll("[?#].*$", "?<redacted>"));
        try {
            mainFrame.goToAsync(url).get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Logger.error(
                    false,
                    "Page",
                    ex,
                    "BiDi navigation failed: contextId={}, url={}",
                    contextId,
                    url == null ? Normal.EMPTY : url.replaceAll("[?#].*$", "?<redacted>"));
            throw new IllegalStateException("BiDi page navigation failed: " + url, ex);
        }
        this.url = url;
        Logger.debug(
                false,
                "Page",
                "BiDi navigation completed: contextId={}, url={}",
                contextId,
                url == null ? Normal.EMPTY : url.replaceAll("[?#].*$", "?<redacted>"));
        return null;
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url     target URL
     * @param options navigation options
     * @return main resource response or {@code null}
     */
    public Response goTo(String url, GoToOptions options) {
        return goTo(url);
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
     * Waits for device prompt.
     *
     * @param timeout timeout value
     * @return wait for device prompt value
     */
    public CompletableFuture<? extends Prompts> waitForDevicePrompt(Duration timeout) {
        Logger.debug(true, "Device", "BiDi device prompt wait requested: contextId={}, timeout={}", contextId, timeout);
        return deviceRequestPromptManager.waitForDevicePrompt(timeout);
    }

    /**
     * Waits for device prompt.
     *
     * @return wait for device prompt value
     */
    public CompletableFuture<? extends Prompts> waitForDevicePrompt() {
        return waitForDevicePrompt(null);
    }

    /**
     * Returns the expose function.
     *
     * @param name     name to use
     * @param callback callback to invoke
     * @return completion future
     */
    public CompletableFuture<BidiExposedFunction> exposeFunction(
            String name,
            BidiExposedFunction.ExposedCallback callback) {
        Logger.debug(true, "Page", "BiDi exposed function add requested: contextId={}, name={}", contextId, name);
        return mainFrame.exposeFunction(name, callback);
    }

    /**
     * Exposes a page function.
     *
     * @param name function name
     * @return command future
     */
    public CompletableFuture<CdpPayload> exposeFunction(String name) {
        return exposeFunction(name, (BidiExposedFunction.ExposedCallback) values -> null)
                .thenApply(ignored -> CdpPayload.NULL);
    }

    /**
     * Exposes a Java callback as a page function.
     *
     * @param name     function name
     * @param callback callback
     * @return command future
     */
    public CompletableFuture<CdpPayload> exposeFunction(
            String name,
            java.util.function.Function<List<Object>, Object> callback) {
        java.util.function.Function<List<Object>, Object> actualCallback = callback == null ? values -> null : callback;
        return exposeFunction(name, (BidiExposedFunction.ExposedCallback) actualCallback::apply)
                .thenApply(ignored -> CdpPayload.NULL);
    }

    /**
     * Removes exposed function.
     *
     * @param name name to use
     * @return remove exposed function value
     */
    public CompletableFuture<CdpPayload> removeExposedFunction(String name) {
        Logger.debug(true, "Page", "BiDi exposed function remove requested: contextId={}, name={}", contextId, name);
        return mainFrame.removeExposedFunction(name).thenApply(ignored -> CdpPayload.NULL);
    }

    /**
     * Returns the input.
     *
     * @return input value
     */
    public BidiInput input() {
        return input;
    }

    /**
     * Returns the webmcp.
     *
     * @return webmcp value
     */
    public CdpWebMCP webmcp() {
        return webmcp;
    }

    /**
     * Returns the keyboard input facade.
     *
     * @return keyboard input facade
     */
    public BidiInput.BidiKeyboard keyboard() {
        return input.keyboard();
    }

    /**
     * Returns the mouse input facade.
     *
     * @return mouse input facade
     */
    public BidiInput.BidiMouse mouse() {
        return input.mouse();
    }

    /**
     * Returns the touchscreen input facade.
     *
     * @return touchscreen
     */
    public BidiInput.Touchscreen touchscreen() {
        return input.touchscreen();
    }

    /**
     * Returns the main frame.
     *
     * @return main frame value
     */
    public BidiFrame mainFrame() {
        return mainFrame;
    }

    /**
     * Returns the frames.
     *
     * @return values
     */
    public List<BidiFrame> frames() {
        List<BidiFrame> frames = new java.util.ArrayList<>();
        frames.add(mainFrame);
        for (int index = 0; index < frames.size(); index++) {
            frames.addAll(frames.get(index).childFrames());
        }
        return List.copyOf(frames);
    }

    /**
     * Returns the trusted emitter.
     *
     * @return trusted emitter value
     */
    public EventEmitter<String> trustedEmitter() {
        return trustedEmitter;
    }

    /**
     * Returns the browser.
     *
     * @return browser value
     */
    public BidiBrowser browser() {
        return browserContext.browser();
    }

    /**
     * Creates CDP session.
     *
     * @return created CDP session
     */
    public org.miaixz.bus.core.lang.Optional<? extends Session> createCDPSession() {
        return org.miaixz.bus.core.lang.Optional
                .of(Awaitable.await(createCDPSessionAsync(), "BiDi CDP session failed.", 5_000L));
    }

    /**
     * Creates CDP session.
     *
     * @return created CDP session
     */
    public CompletableFuture<BidiCDPSession> createCDPSessionAsync() {
        Logger.debug(true, "Protocol", "BiDi page CDP session create requested: contextId={}", contextId);
        return mainFrame.createCDPSessionAsync();
    }

    /**
     * Returns the reload.
     *
     * @return completion future
     */
    public Response reload() {
        Awaitable.await(reloadAsync(), "BiDi page reload failed.", 5_000L);
        return null;
    }

    /**
     * Returns the reload.
     *
     * @param options reload options
     * @return main resource response or {@code null}
     */
    public Response reload(WaitForOptions options) {
        return reload();
    }

    /**
     * Returns the reload.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> reloadAsync() {
        return sendContextCommand("BiDi page reload requested: contextId={}", "browsingContext.reload");
    }

    /**
     * Returns the bring to front.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> bringToFront() {
        return sendContextCommand("BiDi bring-to-front requested: contextId={}", "browsingContext.activate");
    }

    /**
     * Sends a browsing context command.
     *
     * @param message debug message
     * @param method  protocol method
     * @return send context command value
     */
    private CompletableFuture<CdpPayload> sendContextCommand(String message, String method) {
        Logger.debug(true, "Page", message, contextId);
        return browser().session().send(method, Map.of("context", contextId));
    }

    /**
     * Updates user agent.
     *
     * @param userAgent user agent
     * @return set user agent value
     */
    public CompletableFuture<CdpPayload> setUserAgent(String userAgent) {
        Logger.debug(
                true,
                "Network",
                "BiDi user agent update requested: contextId={}, chars={}",
                contextId,
                userAgent == null ? Normal._0 : userAgent.length());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("context", contextId);
        params.put("userAgent", userAgent == null ? CdpPayload.NULL : userAgent);
        return browser().session().send("browsingContext.setUserAgent", params);
    }

    /**
     * Updates user agent.
     *
     * @param options operation options
     * @return set user agent value
     */
    public CompletableFuture<CdpPayload> setUserAgent(UserAgentOptions options) {
        if (options != null && ((options.getUserAgentMetadata() != null && !options.getUserAgentMetadata().isEmpty())
                || StringKit.isNotBlank(options.getPlatform()))) {
            throw new InternalException("BiDi page does not support structured userAgent metadata/platform override.");
        }
        return setUserAgent(options == null ? null : options.getUserAgent());
    }

    /**
     * Updates bypass csp.
     *
     * @param enabled enabled
     * @return set bypass csp value
     */
    public CompletableFuture<CdpPayload> setBypassCSP(boolean enabled) {
        Logger.debug(true, "Page", "BiDi bypass CSP update requested: contextId={}, enabled={}", contextId, enabled);
        return createCDPSessionAsync()
                .thenCompose(session -> session.send("Page.setBypassCSP", Map.of("enabled", enabled)));
    }

    /**
     * Returns the trigger extension action.
     *
     * @param extension extension value
     * @return completion future
     */
    public CompletableFuture<Void> triggerExtensionAction(PageExtension extension) {
        Logger.debug(
                true,
                "Browser",
                "BiDi extension action requested: contextId={}, extension={}",
                contextId,
                extension == null ? Normal.EMPTY : extension.id());
        return createCDPSessionAsync()
                .thenCompose(
                        session -> session.send(
                                "Extensions.triggerAction",
                                Map.of("id", Assert.notNull(extension, "extension").id(), "context", contextId)))
                .thenApply(result -> null);
    }

    /**
     * Triggers extension action.
     *
     * @param extension extension value
     * @return completion future
     */
    public CompletableFuture<Void> triggerExtensionAction(Extension extension) {
        PageExtension pageExtension = extension instanceof PageExtension value ? value : null;
        return triggerExtensionAction(pageExtension);
    }

    /**
     * Returns the resize.
     *
     * @param contentWidth  content width value
     * @param contentHeight content height value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> resize(int contentWidth, int contentHeight) {
        Logger.debug(
                true,
                "Page",
                "BiDi page resize requested: contextId={}, width={}, height={}",
                contextId,
                contentWidth,
                contentHeight);
        Map<String, Object> params = Map.of(
                "context",
                contextId,
                "state",
                Map.of("width", Math.max(0, contentWidth), "height", Math.max(0, contentHeight)));
        return browser().session().send("browser.setClientWindowState", params).thenApply(result -> CdpPayload.NULL);
    }

    /**
     * Returns the open dev tools.
     *
     * @return completion future
     */
    public Page openDevTools() {
        return Awaitable.await(openDevToolsAsync(), "BiDi DevTools open failed.", 5_000L);
    }

    /**
     * Returns the open dev tools.
     *
     * @return completion future
     */
    public CompletableFuture<BidiPage> openDevToolsAsync() {
        if (devToolsPage != null) {
            devToolsOpen = true;
            Logger.debug(
                    false,
                    "Page",
                    "BiDi DevTools reused: contextId={}, devtoolsContext={}",
                    contextId,
                    devToolsPage.contextId());
            return CompletableFuture.completedFuture(devToolsPage);
        }
        Logger.debug(true, "Page", "BiDi DevTools open requested: contextId={}", contextId);
        return browser().session()
                .send(
                        "browsingContext.create",
                        Map.of("type", "tab", "referenceContext", contextId, "background", false))
                .thenApply(result -> {
                    String devToolsContext = PayloadReader.text(result.get("context"));
                    if (StringKit.isBlank(devToolsContext)) {
                        devToolsContext = "devtools-" + contextId;
                    }
                    devToolsPage = new BidiPage(browserContext, devToolsContext, "devtools://devtools");
                    devToolsOpen = true;
                    Logger.debug(
                            false,
                            "Page",
                            "BiDi DevTools opened: contextId={}, devtoolsContext={}",
                            contextId,
                            devToolsContext);
                    return devToolsPage;
                });
    }

    /**
     * Returns whether dev tools is available.
     *
     * @return {@code true} when the condition matches
     */
    public boolean hasDevTools() {
        return devToolsOpen || devToolsPage != null;
    }

    /**
     * Updates default timeout.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    public void setDefaultTimeout(long timeoutMillis) {
        this.defaultTimeoutMillis = Math.max(0L, timeoutMillis);
        Logger.debug(
                false,
                "Page",
                "BiDi default timeout updated: contextId={}, timeout={}",
                contextId,
                this.defaultTimeoutMillis);
    }

    /**
     * Updates default timeout.
     *
     * @param timeout timeout value
     */
    public void setDefaultTimeout(java.time.Duration timeout) {
        setDefaultTimeout(timeout == null ? 0L : timeout.toMillis());
    }

    /**
     * Returns the default timeout.
     *
     * @return default timeout
     */
    public java.time.Duration getDefaultTimeout() {
        return java.time.Duration.ofMillis(defaultTimeoutMillis);
    }

    /**
     * Updates default navigation timeout.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    public void setDefaultNavigationTimeout(long timeoutMillis) {
        this.defaultNavigationTimeoutMillis = Math.max(0L, timeoutMillis);
        Logger.debug(
                false,
                "Page",
                "BiDi default navigation timeout updated: contextId={}, timeout={}",
                contextId,
                this.defaultNavigationTimeoutMillis);
    }

    /**
     * Updates default navigation timeout.
     *
     * @param timeout timeout value
     */
    public void setDefaultNavigationTimeout(java.time.Duration timeout) {
        setDefaultNavigationTimeout(timeout == null ? 0L : timeout.toMillis());
    }

    /**
     * Returns the default navigation timeout.
     *
     * @return default navigation timeout
     */
    public java.time.Duration getDefaultNavigationTimeout() {
        return java.time.Duration.ofMillis(defaultNavigationTimeoutMillis);
    }

    /**
     * Updates java script enabled.
     *
     * @param enabled enabled
     * @return set java script enabled value
     */
    public CompletableFuture<CdpPayload> setJavaScriptEnabled(boolean enabled) {
        this.javaScriptEnabled = enabled;
        Logger.debug(true, "Page", "BiDi JavaScript update requested: contextId={}, enabled={}", contextId, enabled);
        return browser().session()
                .send("script.setJavaScriptEnabled", Map.of("context", contextId, "enabled", enabled));
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
     * Updates geolocation.
     *
     * @param latitude  latitude
     * @param longitude longitude
     * @param accuracy  accuracy
     * @return set geolocation value
     */
    public CompletableFuture<CdpPayload> setGeolocation(double latitude, double longitude, double accuracy) {
        if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90 || accuracy < 0) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new InternalException("BiDi geolocation parameters are invalid."));
            Logger.warn(
                    false,
                    "Page",
                    "BiDi geolocation rejected: contextId={}, latitude={}, longitude={}",
                    contextId,
                    latitude,
                    longitude);
            return rejected;
        }
        Logger.debug(true, "Page", "BiDi geolocation update requested: contextId={}, accuracy={}", contextId, accuracy);
        return browser().session().send(
                "emulation.setGeolocationOverride",
                Map.of(
                        "contexts",
                        List.of(contextId),
                        "coordinates",
                        Map.of("latitude", latitude, "longitude", longitude, "accuracy", accuracy)));
    }

    /**
     * Updates viewport.
     *
     * @param viewport viewport
     * @return set viewport value
     */
    public CompletableFuture<Void> setViewport(Viewport viewport) {
        this.viewport = viewport == null ? null : viewport.copy();
        Map<String, Object> params = this.viewport == null ? Map.of() : this.viewport.toMap();
        Logger.debug(
                true,
                "Page",
                "BiDi viewport update requested: contextId={}, entries={}",
                contextId,
                params.size());
        return browser().session()
                .send("emulation.setViewportOverride", Map.of("context", contextId, "viewport", params))
                .thenApply(result -> null);
    }

    /**
     * Returns the viewport.
     *
     * @return mapped values
     */
    public Viewport viewport() {
        return viewport == null ? null : viewport.copy();
    }

    /**
     * Returns the screenshot.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<String> screenshot(Map<String, Object> options) {
        installRenderSecurityBoundary();
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("context", contextId);
        Logger.debug(true, "Page", "BiDi screenshot requested: contextId={}, options={}", contextId, params.size());
        if (Boolean.TRUE.equals(params.remove("omitBackground"))) {
            params.put("background", "transparent");
        }
        params.remove("optimizeForSpeed");
        params.remove("fromSurface");
        return browser().session().send("browsingContext.captureScreenshot", params).thenApply(result -> {
            String data = PayloadReader.text(result.get("data"));
            Logger.debug(false, "Page", "BiDi screenshot completed: contextId={}, chars={}", contextId, data.length());
            return data;
        });
    }

    /**
     * Returns the PDF.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<byte[]> pdf(Map<String, Object> options) {
        installRenderSecurityBoundary();
        Map<String, Object> params = new java.util.LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("context", contextId);
        Logger.debug(true, "Page", "BiDi PDF requested: contextId={}, options={}", contextId, params.size());
        return browser().session().send("browsingContext.print", params).thenApply(result -> {
            byte[] data = Base64.decode(PayloadReader.text(result.get("data")));
            Logger.debug(false, "Page", "BiDi PDF completed: contextId={}, bytes={}", contextId, data.length);
            return data;
        });
    }

    /**
     * Returns the evaluate on new document.
     *
     * @param functionDeclaration function declaration value
     * @return completion future
     */
    public String evaluateOnNewDocument(String functionDeclaration) {
        return Awaitable
                .await(evaluateOnNewDocumentAsync(functionDeclaration), "BiDi evaluateOnNewDocument failed.", 5_000L);
    }

    /**
     * Returns the evaluate on new document.
     *
     * @param functionDeclaration function declaration value
     * @return completion future
     */
    public CompletableFuture<String> evaluateOnNewDocumentAsync(String functionDeclaration) {
        Logger.debug(
                true,
                "Page",
                "BiDi preload script add requested: contextId={}, chars={}",
                contextId,
                functionDeclaration == null ? Normal._0 : functionDeclaration.length());
        return browser().session()
                .send(
                        "script.addPreloadScript",
                        Map.of(
                                "functionDeclaration",
                                Assert.notBlank(functionDeclaration, "functionDeclaration"),
                                "contexts",
                                List.of(contextId)))
                .thenApply(result -> PayloadReader.text(result.get("script")));
    }

    /**
     * Removes script to evaluate on new document.
     *
     * @param id identifier
     * @return remove script to evaluate on new document value
     */
    public CompletableFuture<CdpPayload> removeScriptToEvaluateOnNewDocument(String id) {
        Logger.debug(true, "Page", "BiDi preload script remove requested: contextId={}, script={}", contextId, id);
        return browser().session().send("script.removePreloadScript", Map.of("script", Assert.notBlank(id, "id")));
    }

    /**
     * Updates cache enabled.
     *
     * @param enabled enabled
     * @return set cache enabled value
     */
    public CompletableFuture<CdpPayload> setCacheEnabled(boolean enabled) {
        Logger.debug(true, "Network", "BiDi cache mode update requested: contextId={}, enabled={}", contextId, enabled);
        return browser().session().send(
                "network.setCacheBehavior",
                Map.of("context", contextId, "cacheBehavior", enabled ? "default" : "bypass"));
    }

    /**
     * Returns whether service worker bypassed is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isServiceWorkerBypassed() {
        return serviceWorkerBypassed;
    }

    /**
     * Updates bypass service worker.
     *
     * @param bypass bypass
     * @return set bypass service worker value
     */
    public CompletableFuture<CdpPayload> setBypassServiceWorker(boolean bypass) {
        this.serviceWorkerBypassed = bypass;
        Logger.debug(
                true,
                "Network",
                "BiDi service worker bypass update requested: contextId={}, bypass={}",
                contextId,
                bypass);
        return createCDPSessionAsync()
                .thenCompose(session -> session.send("Network.setBypassServiceWorker", Map.of("bypass", bypass)));
    }

    /**
     * Updates drag interception.
     *
     * @param enabled whether the feature should be enabled
     */
    public CompletableFuture<CdpPayload> setDragInterception(boolean enabled) {
        this.dragInterception = enabled;
        Logger.debug(false, "Input", "BiDi drag interception updated: contextId={}, enabled={}", contextId, enabled);
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns whether drag interception is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isDragInterceptionEnabled() {
        return dragInterception;
    }

    /**
     * Updates extra HTTP headers.
     *
     * @param headers HTTP headers
     * @return set extra HTTP headers value
     */
    public CompletableFuture<CdpPayload> setExtraHTTPHeaders(Map<String, String> headers) {
        Logger.debug(
                true,
                "Network",
                "BiDi extra HTTP headers update requested: contextId={}, count={}",
                contextId,
                headers == null ? Normal._0 : headers.size());
        return browser().session().send(
                "network.setExtraHTTPHeaders",
                Map.of("context", contextId, "headers", headers == null ? Map.of() : headers));
    }

    /**
     * Updates request interception.
     *
     * @param enabled enabled
     * @return set request interception value
     */
    public CompletableFuture<CdpPayload> setRequestInterception(boolean enabled) {
        Logger.debug(
                true,
                "Network",
                "BiDi request interception update requested: contextId={}, enabled={}",
                contextId,
                enabled);
        if (enabled && StringKit.isBlank(requestInterception)) {
            return browser().session()
                    .send(
                            "network.addIntercept",
                            Map.of("contexts", List.of(contextId), "phases", List.of("beforeRequestSent")))
                    .thenApply(result -> {
                        requestInterception = PayloadReader.text(result.get("intercept"));
                        Logger.debug(
                                false,
                                "Network",
                                "BiDi request interception enabled: contextId={}, intercept={}",
                                contextId,
                                requestInterception);
                        return result;
                    });
        }
        if (!enabled && StringKit.isNotBlank(requestInterception)) {
            String intercept = requestInterception;
            requestInterception = null;
            return browser().session().send("network.removeIntercept", Map.of("intercept", intercept))
                    .thenApply(result -> {
                        Binding binding = renderSecurityBoundaryBinding;
                        if (binding != null) {
                            binding.unbind();
                            renderSecurityBoundaryBinding = null;
                        }
                        renderSecurityBoundaryInstalled.set(false);
                        Logger.debug(
                                false,
                                "Network",
                                "BiDi request interception disabled: contextId={}, intercept={}",
                                contextId,
                                intercept);
                        return result;
                    });
        }
        Logger.debug(
                false,
                "Network",
                "BiDi request interception unchanged: contextId={}, enabled={}",
                contextId,
                enabled);
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns whether network interception is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isNetworkInterceptionEnabled() {
        return StringKit.isNotBlank(requestInterception) || StringKit.isNotBlank(authInterception);
    }

    /**
     * Returns the authenticate.
     *
     * @param username username value
     * @param password password value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> authenticate(String username, String password) {
        this.credentials = StringKit.isBlank(username) ? null : Map.of("username", username, "password", password);
        boolean enabled = credentials != null;
        Logger.debug(
                true,
                "Network",
                "BiDi authentication update requested: contextId={}, enabled={}",
                contextId,
                enabled);
        if (enabled && StringKit.isBlank(authInterception)) {
            return browser().session()
                    .send(
                            "network.addIntercept",
                            Map.of("contexts", List.of(contextId), "phases", List.of("authRequired")))
                    .thenApply(result -> {
                        authInterception = PayloadReader.text(result.get("intercept"));
                        Logger.debug(
                                false,
                                "Network",
                                "BiDi authentication enabled: contextId={}, intercept={}",
                                contextId,
                                authInterception);
                        return result;
                    });
        }
        if (!enabled && StringKit.isNotBlank(authInterception)) {
            String intercept = authInterception;
            authInterception = null;
            return browser().session().send("network.removeIntercept", Map.of("intercept", intercept))
                    .thenApply(result -> {
                        Logger.debug(
                                false,
                                "Network",
                                "BiDi authentication disabled: contextId={}, intercept={}",
                                contextId,
                                intercept);
                        return result;
                    });
        }
        Logger.debug(false, "Network", "BiDi authentication unchanged: contextId={}, enabled={}", contextId, enabled);
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns the credentials.
     *
     * @return mapped values
     */
    public Map<String, String> credentials() {
        return credentials == null ? Map.of() : Map.copyOf(credentials);
    }

    /**
     * Updates offline mode.
     *
     * @param enabled enabled
     * @return set offline mode value
     */
    public CompletableFuture<CdpPayload> setOfflineMode(boolean enabled) {
        Logger.debug(
                true,
                "Network",
                "BiDi offline mode update requested: contextId={}, enabled={}",
                contextId,
                enabled);
        return browser().session().send("network.setOfflineMode", Map.of("context", contextId, "enabled", enabled));
    }

    /**
     * Returns the emulate network conditions.
     *
     * @param conditions conditions value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateNetworkConditions(NetworkConditions conditions) {
        NetworkConditions actual = conditions == null ? NetworkConditions.online() : conditions;
        this.networkConditions = actual;
        Logger.debug(
                true,
                "Network",
                "BiDi network conditions update requested: contextId={}, offline={}",
                contextId,
                actual.offline());
        Map<String, Object> params = Map.of(
                "context",
                contextId,
                "offline",
                actual.offline(),
                "latency",
                actual.latency(),
                "downloadThroughput",
                actual.downloadThroughput(),
                "uploadThroughput",
                actual.uploadThroughput());
        return browser().session().send("emulation.setNetworkConditions", params);
    }

    /**
     * Returns the network conditions.
     *
     * @return network conditions value
     */
    public NetworkConditions networkConditions() {
        return networkConditions;
    }

    /**
     * Returns the metrics.
     *
     * @return mapped values
     */
    public Map<String, Number> metrics() {
        Logger.debug(true, "Page", "BiDi metrics requested: contextId={}", contextId);
        try {
            CdpPayload result = createCDPSessionAsync().thenCompose(
                    session -> session.send("Runtime.evaluate", Map.of("expression", "({Timestamp: Date.now()})")))
                    .get(5, TimeUnit.SECONDS);
            Map<String, Number> metrics = new LinkedHashMap<>();
            metrics.put("Timestamp", System.currentTimeMillis());
            Logger.debug(false, "Page", "BiDi metrics completed: contextId={}, keys={}", contextId, metrics.size());
            return Map.copyOf(metrics);
        } catch (Exception ex) {
            Logger.warn(false, "Page", ex, "BiDi metrics fallback used: contextId={}", contextId);
            return Map.of("Timestamp", System.currentTimeMillis());
        }
    }

    /**
     * Handles capture heap snapshot.
     *
     * @param path file path
     */
    public void captureHeapSnapshot(Path path) {
        Assert.notNull(path, "path");
        Logger.debug(true, "Page", "BiDi heap snapshot requested: contextId={}, path={}", contextId, path);
        StringBuilder chunks = new StringBuilder();
        try {
            BidiCDPSession session = createCDPSessionAsync().get(5, TimeUnit.SECONDS);
            session.on(
                    "HeapProfiler.addHeapSnapshotChunk",
                    params -> chunks.append(PayloadReader.text(params.get("chunk"))));
            session.send("HeapProfiler.enable").get(5, TimeUnit.SECONDS);
            session.send("HeapProfiler.collectGarbage").get(5, TimeUnit.SECONDS);
            session.send("HeapProfiler.takeHeapSnapshot", Map.of("reportProgress", false)).get(5, TimeUnit.SECONDS);
            session.send("HeapProfiler.disable").get(5, TimeUnit.SECONDS);
            Files.writeString(path, chunks.toString(), Charset.UTF_8);
            Logger.debug(
                    false,
                    "Page",
                    "BiDi heap snapshot completed: contextId={}, chars={}",
                    contextId,
                    chunks.length());
        } catch (Exception ex) {
            Logger.warn(
                    false,
                    "Page",
                    ex,
                    "BiDi heap snapshot protocol failed, writing collected chunks: contextId={}",
                    contextId);
            try {
                Files.writeString(path, chunks.toString(), Charset.UTF_8);
            } catch (Exception writeFailure) {
                Logger.error(
                        false,
                        "Page",
                        writeFailure,
                        "BiDi heap snapshot write failed: contextId={}, path={}",
                        contextId,
                        path);
                throw new InternalException("Failed to write heap snapshot: " + path, writeFailure);
            }
        }
    }

    /**
     * Handles capture heap snapshot.
     *
     * @param options operation options
     */
    public void captureHeapSnapshot(HeapSnapshotOptions options) {
        captureHeapSnapshot(Assert.notNull(options, "options").path());
    }

    /**
     * Returns the extension realms.
     *
     * @return values
     */
    public List<BidiRealm> extensionRealms() {
        List<BidiRealm> realms = new ArrayList<>();
        for (BidiWorker worker : workers) {
            if (worker.url().startsWith("chrome-extension://") || worker.url().startsWith("moz-extension://")) {
                realms.add(worker.mainRealm());
            }
        }
        return List.copyOf(realms);
    }

    /**
     * Returns the workers.
     *
     * @return values
     */
    public List<BidiWorker> workers() {
        return List.copyOf(workers);
    }

    /**
     * Handles add worker.
     *
     * @param worker worker value
     */
    public void addWorker(BidiWorker worker) {
        workers.add(Assert.notNull(worker, "worker"));
        Logger.debug(false, "Page", "BiDi worker added: contextId={}, workers={}", contextId, workers.size());
    }

    /**
     * Handles remove worker.
     *
     * @param worker worker value
     */
    public void removeWorker(BidiWorker worker) {
        workers.remove(worker);
        Logger.debug(false, "Page", "BiDi worker removed: contextId={}, workers={}", contextId, workers.size());
    }

    /**
     * Returns the context ID.
     *
     * @return context ID value
     */
    public String contextId() {
        return contextId;
    }

    /**
     * Returns the browser context.
     *
     * @return browser context value
     */
    public BidiBrowserContext browserContext() {
        return browserContext;
    }

    /**
     * Returns whether this object is closed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Returns the page target.
     *
     * @return page target
     */
    public Target target() {
        return BidiTarget.page(this);
    }

    /**
     * Returns the page window id.
     *
     * @return window id
     */
    public String windowId() {
        return contextId;
    }

    /**
     * Waits for navigation.
     *
     * @return main resource response or {@code null}
     */
    public Response waitForNavigation() {
        return null;
    }

    /**
     * Waits for navigation.
     *
     * @param options wait options
     * @return main resource response or {@code null}
     */
    public Response waitForNavigation(WaitForOptions options) {
        return null;
    }

    /**
     * Waits for navigation.
     *
     * @param timeout   timeout value
     * @param waitUntil lifecycle markers
     * @return main resource response or {@code null}
     */
    public Response waitForNavigation(java.time.Duration timeout, String... waitUntil) {
        return null;
    }

    /**
     * Waits for network idle state.
     */
    public void waitForNetworkIdle() {
    }

    /**
     * Waits for network idle state.
     *
     * @param timeout             timeout value
     * @param idleTime            idle time
     * @param maxInflightRequests max in-flight count
     */
    public void waitForNetworkIdle(java.time.Duration timeout, java.time.Duration idleTime, int maxInflightRequests) {
    }

    /**
     * Waits for network idle state.
     */
    public void waitForNetworkIdle$() {
        waitForNetworkIdle();
    }

    /**
     * Waits for a request.
     *
     * @param predicate request predicate
     * @return request
     */
    public Request waitForRequest(java.util.function.Predicate<Request> predicate) {
        return null;
    }

    /**
     * Waits for a request.
     *
     * @param url request URL
     * @return request
     */
    public Request waitForRequest(String url) {
        return null;
    }

    /**
     * Waits for a request.
     *
     * @param url     request URL
     * @param options wait options
     * @return request
     */
    public Request waitForRequest(String url, WaitForOptions options) {
        return null;
    }

    /**
     * Waits for a request.
     *
     * @param pattern request URL pattern
     * @return request
     */
    public Request waitForRequest(java.util.regex.Pattern pattern) {
        return null;
    }

    /**
     * Waits for a request.
     *
     * @param pattern request URL pattern
     * @param options wait options
     * @return request
     */
    public Request waitForRequest(java.util.regex.Pattern pattern, WaitForOptions options) {
        return null;
    }

    /**
     * Waits for a request.
     *
     * @param predicate request predicate
     * @param timeout   timeout value
     * @return request
     */
    public Request waitForRequest(java.util.function.Predicate<Request> predicate, java.time.Duration timeout) {
        return null;
    }

    /**
     * Waits for a response.
     *
     * @param predicate response predicate
     * @return response
     */
    public Response waitForResponse(java.util.function.Predicate<Response> predicate) {
        return null;
    }

    /**
     * Waits for a response.
     *
     * @param url response URL
     * @return response
     */
    public Response waitForResponse(String url) {
        return null;
    }

    /**
     * Waits for a response.
     *
     * @param url     response URL
     * @param options wait options
     * @return response
     */
    public Response waitForResponse(String url, WaitForOptions options) {
        return null;
    }

    /**
     * Waits for a response.
     *
     * @param pattern response URL pattern
     * @return response
     */
    public Response waitForResponse(java.util.regex.Pattern pattern) {
        return null;
    }

    /**
     * Waits for a response.
     *
     * @param pattern response URL pattern
     * @param options wait options
     * @return response
     */
    public Response waitForResponse(java.util.regex.Pattern pattern, WaitForOptions options) {
        return null;
    }

    /**
     * Waits for a response.
     *
     * @param predicate response predicate
     * @param timeout   timeout value
     * @return response
     */
    public Response waitForResponse(java.util.function.Predicate<Response> predicate, java.time.Duration timeout) {
        return null;
    }

    /**
     * Goes back in history.
     *
     * @return main resource response or {@code null}
     */
    public Response goBack() {
        return null;
    }

    /**
     * Goes back in history.
     *
     * @param options navigation wait options
     * @return main resource response or {@code null}
     */
    public Response goBack(WaitForOptions options) {
        return null;
    }

    /**
     * Goes forward in history.
     *
     * @return main resource response or {@code null}
     */
    public Response goForward() {
        return null;
    }

    /**
     * Goes forward in history.
     *
     * @param options navigation wait options
     * @return main resource response or {@code null}
     */
    public Response goForward(WaitForOptions options) {
        return null;
    }

    /**
     * Waits for a frame.
     *
     * @param predicate frame predicate
     * @param timeout   timeout value
     * @return frame
     */
    public Frame waitForFrame(java.util.function.Predicate predicate, java.time.Duration timeout) {
        return mainFrame;
    }

    /**
     * Waits for a frame.
     *
     * @param predicate frame predicate
     * @return frame
     */
    public Frame waitForFrame(java.util.function.Predicate predicate) {
        return mainFrame;
    }

    /**
     * Returns page content.
     *
     * @return content
     */
    public String content() {
        return mainFrame.content();
    }

    /**
     * Updates content.
     *
     * @param html HTML content
     */
    public void setContent(String html) {
        mainFrame.setContent(html);
    }

    /**
     * Adds a script tag.
     *
     * @param content script content
     * @return created element
     */
    public Element addScriptTag(String content) {
        return mainFrame.addScriptTag(content);
    }

    /**
     * Adds a script tag.
     *
     * @param options script tag options
     * @return created element
     */
    public Element addScriptTag(ScriptTagOptions options) {
        if (options != null && StringKit.isNotBlank(options.getUrl())) {
            validateRenderRequestUrl(options.getUrl());
        }
        return mainFrame.addScriptTag(options);
    }

    /**
     * Adds a style tag.
     *
     * @param content style content
     * @return created element
     */
    public Element addStyleTag(String content) {
        return mainFrame.addStyleTag(content);
    }

    /**
     * Adds a style tag.
     *
     * @param options style tag options
     * @return created element
     */
    public Element addStyleTag(StyleTagOptions options) {
        if (options != null && StringKit.isNotBlank(options.getUrl())) {
            validateRenderRequestUrl(options.getUrl());
        }
        return mainFrame.addStyleTag(options);
    }

    /**
     * Returns page title.
     *
     * @return title
     */
    public String title() {
        return mainFrame.title();
    }

    /**
     * Returns cookies.
     *
     * @param urls URL values
     * @return cookies
     */
    public List<Cookie> cookies(String... urls) {
        return List.of();
    }

    /**
     * Updates cookies.
     *
     * @param cookies cookies
     * @return command future
     */
    public CompletableFuture<CdpPayload> setCookie(CookieParam... cookies) {
        return browserContext.setCookie(cookies);
    }

    /**
     * Deletes cookies.
     *
     * @param cookies cookies
     * @return command future
     */
    public CompletableFuture<Void> deleteCookie(DeleteCookiesParameters... cookies) {
        return browserContext.deleteCookie(cookies).thenApply(ignored -> null);
    }

    /**
     * Waits for file chooser.
     *
     * @return file chooser
     */
    public FileChooser waitForFileChooser() {
        return null;
    }

    /**
     * Waits for file chooser.
     *
     * @param timeout timeout value
     * @return file chooser
     */
    public FileChooser waitForFileChooser(java.time.Duration timeout) {
        return null;
    }

    /**
     * Starts screencast.
     *
     * @param options screencast options
     * @return recorder
     */
    public Recorder screencast(ScreencastOptions options) {
        return null;
    }

    /**
     * Starts screencast.
     *
     * @return command future
     */
    public CompletableFuture<Void> startScreencast() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Stops screencast.
     *
     * @return command future
     */
    public CompletableFuture<Void> stopScreencast() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Takes a screenshot.
     *
     * @return screenshot bytes
     */
    public byte[] screenshot() {
        return screenshot((ScreenshotOptions) null);
    }

    /**
     * Takes a screenshot.
     *
     * @param options screenshot options
     * @return screenshot bytes
     */
    public byte[] screenshot(ScreenshotOptions options) {
        String data = Awaitable.await(screenshot(Map.of()), "BiDi screenshot failed.", 5_000L);
        return org.miaixz.bus.core.codec.binary.Base64.decode(data);
    }

    /**
     * Creates a PDF.
     *
     * @return PDF bytes
     */
    public byte[] pdf() {
        return pdf((PDFOptions) null);
    }

    /**
     * Creates a PDF.
     *
     * @param options PDF options
     * @return PDF bytes
     */
    public byte[] pdf(PDFOptions options) {
        return Awaitable.await(pdf(Map.of()), "BiDi PDF failed.", 5_000L);
    }

    /**
     * Creates a PDF stream.
     *
     * @param options PDF options
     * @return PDF stream
     */
    public java.io.InputStream createPDFStream(PDFOptions options) {
        return new java.io.ByteArrayInputStream(pdf(options));
    }

    /**
     * Evaluates an expression.
     *
     * @param expression expression
     * @return result
     */
    public Object evaluate(String expression) {
        return mainFrame.evaluate(expression);
    }

    /**
     * Evaluates against one element.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return result
     */
    public Object $eval(String selector, String pageFunction) {
        return mainFrame.$eval(selector, pageFunction);
    }

    /**
     * Evaluates against all elements.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return result
     */
    public Object $$eval(String selector, String pageFunction) {
        return mainFrame.$$eval(selector, pageFunction);
    }

    /**
     * Evaluates an expression and returns a handle.
     *
     * @param expression expression
     * @return handle
     */
    public Handle evaluateHandle(String expression) {
        return mainFrame.evaluateHandle(expression);
    }

    /**
     * Queries objects by prototype handle.
     *
     * @param prototypeHandle prototype handle
     * @return handle
     */
    public Handle queryObjects(Handle prototypeHandle) {
        return prototypeHandle;
    }

    /**
     * Waits for a function.
     *
     * @param expression expression
     * @return result
     */
    public Object waitForFunction(String expression) {
        return evaluate(expression);
    }

    /**
     * Waits for a function.
     *
     * @param expression expression
     * @param timeout    timeout value
     * @return result
     */
    public Object waitForFunction(String expression, java.time.Duration timeout) {
        return waitForFunction(expression);
    }

    /**
     * Queries one element.
     *
     * @param selector selector
     * @return element handle
     */
    public org.miaixz.bus.core.lang.Optional<? extends Element> $(String selector) {
        return mainFrame.$(selector);
    }

    /**
     * Queries all elements.
     *
     * @param selector selector
     * @return element handles
     */
    public List<? extends Element> $$(String selector) {
        return mainFrame.$$(selector);
    }

    /**
     * Waits for selector.
     *
     * @param selector selector
     * @return element handle
     */
    public org.miaixz.bus.core.lang.Optional<? extends Element> waitForSelector(String selector) {
        return mainFrame.waitForSelector(selector);
    }

    /**
     * Waits for selector.
     *
     * @param selector selector
     * @param options  options
     * @return element handle
     */
    public org.miaixz.bus.core.lang.Optional<? extends Element> waitForSelector(
            String selector,
            WaitForSelectorOptions options) {
        return mainFrame.waitForSelector(selector, options);
    }

    /**
     * Clicks an element.
     *
     * @param selector selector
     */
    public void click(String selector) {
        mainFrame.click(selector);
    }

    /**
     * Clicks an element.
     *
     * @param selector selector
     * @param options  options
     */
    public void click(String selector, ClickOptions options) {
        click(selector);
    }

    /**
     * Focuses an element.
     *
     * @param selector selector
     */
    public void focus(String selector) {
        mainFrame.focus(selector);
    }

    /**
     * Hovers an element.
     *
     * @param selector selector
     */
    public void hover(String selector) {
        mainFrame.hover(selector);
    }

    /**
     * Taps an element.
     *
     * @param selector selector
     */
    public void tap(String selector) {
        mainFrame.tap(selector);
    }

    /**
     * Types text into an element.
     *
     * @param selector selector
     * @param text     text
     */
    public void type(String selector, String text) {
        mainFrame.type(selector, text);
    }

    /**
     * Types text into an element.
     *
     * @param selector selector
     * @param text     text
     * @param options  options
     */
    public void type(String selector, String text, KeyboardTypeOptions options) {
        type(selector, text);
    }

    /**
     * Selects option values.
     *
     * @param selector selector
     * @param values   values
     * @return selected values
     */
    public List<String> select(String selector, String... values) {
        return mainFrame.select(selector, values);
    }

    /**
     * Creates a locator.
     *
     * @param selector selector
     * @return locator
     */
    public Locator locator(String selector) {
        return mainFrame.locator(selector);
    }

    /**
     * Creates a racing locator.
     *
     * @param locators locators
     * @return racing locator
     */
    public Locator locatorRace(List<? extends Locator> locators) {
        return locators == null || locators.isEmpty() ? null : locators.get(0);
    }

    /**
     * Returns coverage.
     *
     * @return coverage
     */
    public Coverage coverage() {
        return null;
    }

    /**
     * Returns tracing.
     *
     * @return tracing
     */
    public Tracing tracing() {
        return null;
    }

    /**
     * Returns accessibility.
     *
     * @return accessibility
     */
    public Accessibility accessibility() {
        return null;
    }

    /**
     * Returns Bluetooth emulation.
     *
     * @return Bluetooth emulation
     */
    public Bluetooth bluetooth() {
        return null;
    }

    /**
     * Authenticates the page.
     *
     * @param credentials credentials
     * @return command future
     */
    public CompletableFuture<CdpPayload> authenticate(Credentials credentials) {
        return credentials == null ? authenticate(null, null)
                : authenticate(credentials.username(), credentials.password());
    }

    /**
     * Emulates geolocation.
     *
     * @param geolocation geolocation value
     * @return command future
     */
    public CompletableFuture<CdpPayload> setGeolocation(Geolocation geolocation) {
        return geolocation == null ? CompletableFuture.completedFuture(CdpPayload.NULL)
                : setGeolocation(geolocation.latitude(), geolocation.longitude(), geolocation.accuracy());
    }

    /**
     * Emulates a device.
     *
     * @param device device
     * @return completion future
     */
    public CompletableFuture<Void> emulate(Device device) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Emulates media type.
     *
     * @param mediaType media type
     * @return completion future
     */
    public CompletableFuture<Void> emulateMediaType(String mediaType) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Emulates media features.
     *
     * @param mediaFeatures media features
     * @return completion future
     */
    public CompletableFuture<Void> emulateMediaFeatures(List<MediaFeature> mediaFeatures) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Emulates CPU throttling.
     *
     * @param rate rate
     * @return command future
     */
    public CompletableFuture<CdpPayload> emulateCPUThrottling(double rate) {
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Emulates idle state.
     *
     * @param idleState idle state
     * @return command future
     */
    public CompletableFuture<CdpPayload> emulateIdleState(IdleState idleState) {
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Emulates timezone.
     *
     * @param timezone timezone
     * @return command future
     */
    public CompletableFuture<CdpPayload> emulateTimezone(String timezone) {
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Emulates vision deficiency.
     *
     * @param visionDeficiency vision deficiency
     * @return command future
     */
    public CompletableFuture<CdpPayload> emulateVisionDeficiency(String visionDeficiency) {
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Emulates focused page state.
     *
     * @param enabled enabled state
     * @return command future
     */
    public CompletableFuture<CdpPayload> emulateFocusedPage(boolean enabled) {
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns page event emitter.
     *
     * @param event    event
     * @param listener listener
     * @return emitter
     */
    public Emitter<PageEvent> on(PageEvent event, java.util.function.Consumer<Object> listener) {
        return emitter.on(event, listener);
    }

    /**
     * Returns page event emitter.
     *
     * @param event    event
     * @param listener listener
     * @return emitter
     */
    public Emitter<PageEvent> once(PageEvent event, java.util.function.Consumer<Object> listener) {
        return emitter.once(event, listener);
    }

    /**
     * Removes a page event listener.
     *
     * @param event    event
     * @param listener listener
     * @return emitter
     */
    public Emitter<PageEvent> off(PageEvent event, java.util.function.Consumer<Object> listener) {
        return emitter.off(event, listener);
    }

    /**
     * Removes all listeners for an event.
     *
     * @param event event
     * @return emitter
     */
    public Emitter<PageEvent> off(PageEvent event) {
        return emitter.off(event);
    }

    /**
     * Emits a page event.
     *
     * @param event   event
     * @param payload payload
     * @return emitted state
     */
    public boolean emit(PageEvent event, Object payload) {
        return emitter.emit(event, payload);
    }

    /**
     * Counts page event listeners.
     *
     * @param event event
     * @return listener count
     */
    public int listenerCount(PageEvent event) {
        return emitter.listenerCount(event);
    }

    /**
     * Removes all listeners for an event.
     *
     * @param event event
     * @return emitter
     */
    public Emitter<PageEvent> removeAllListeners(PageEvent event) {
        return emitter.removeAllListeners(event);
    }

    /**
     * Removes all listeners.
     *
     * @return emitter
     */
    public Emitter<PageEvent> removeAllListeners() {
        return emitter.removeAllListeners();
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Logger.debug(
                    true,
                    "Page",
                    "BiDi page close requested: contextId={}, url={}",
                    contextId,
                    url == null ? Normal.EMPTY : url.replaceAll("[?#].*$", "?<redacted>"));
            try {
                Binding binding = renderSecurityBoundaryBinding;
                if (binding != null) {
                    binding.unbind();
                    renderSecurityBoundaryBinding = null;
                }
                browserContext.browser().session().send("browsingContext.close", Map.of("context", contextId))
                        .get(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
                Logger.warn(false, "Page", ex, "BiDi page remote close failed: contextId={}", contextId);
            }
            mainFrame.detach();
            browserContext.removePage(this);
            Logger.debug(false, "Page", "BiDi page closed: contextId={}", contextId);
        }
    }

    /**
     * Closes this object and releases its resources.
     *
     * @param options close options
     */
    public void close(PageCloseOptions options) {
        close();
    }

    /**
     * Installs the render request security boundary.
     */
    private void installRenderSecurityBoundary() {
        if (!renderSecurityBoundaryEnabled || closed.get()
                || !renderSecurityBoundaryInstalled.compareAndSet(false, true)) {
            return;
        }
        Binding binding = browser().session().on("network.beforeRequestSent", this::handleRenderBoundaryRequest);
        try {
            Awaitable.await(
                    browser().session().subscribe(List.of("network.beforeRequestSent"), List.of(contextId)),
                    "Failed to subscribe BiDi render request boundary.",
                    5_000L);
            Awaitable.await(setRequestInterception(true), "Failed to enable BiDi render request boundary.", 5_000L);
            renderSecurityBoundaryBinding = binding;
        } catch (RuntimeException ex) {
            binding.unbind();
            renderSecurityBoundaryInstalled.set(false);
            throw ex;
        }
    }

    /**
     * Handles a render-time request while the security boundary is active.
     *
     * @param payload protocol payload
     */
    private void handleRenderBoundaryRequest(Object payload) {
        if (!(payload instanceof CdpPayload event)) {
            return;
        }
        String eventContext = PayloadReader.text(event.get("context"));
        if (StringKit.isNotBlank(eventContext) && !contextId.equals(eventContext)) {
            return;
        }
        BidiRequest request;
        try {
            request = BidiRequest.from(event, mainFrame, true, null);
        } catch (RuntimeException ex) {
            Logger.warn(false, "Network", ex, "BiDi render request payload ignored: contextId={}", contextId);
            return;
        }
        if (request.isInterceptResolutionHandled()) {
            return;
        }
        try {
            validateRenderRequestUrl(request.url());
            request.continueRequest(Map.of());
        } catch (RuntimeException ex) {
            Logger.warn(
                    false,
                    "Network",
                    ex,
                    "BiDi render request blocked: url={}",
                    securityPolicy.sanitizeUrl(request.url()));
            request.abort("blockedbyclient");
        }
    }

    /**
     * Validates a navigation URL before it reaches the browser.
     *
     * @param url target URL
     */
    void validateNavigationUrl(String url) {
        securityPolicy.validateNavigationUrl(URI.create(Assert.notBlank(url, "url")));
    }

    /**
     * Validates a render request URL before it reaches the browser.
     *
     * @param url target URL
     */
    void validateRenderRequestUrl(String url) {
        securityPolicy.validateRenderRequestUrl(URI.create(Assert.notBlank(url, "url")));
    }

    /**
     * Returns the convert partition key.
     *
     * @param key key value
     * @return convert partition key value
     */
    private String convertPartitionKey(CookiePartitionKey key) {
        if (key == null) {
            return Normal.EMPTY;
        }
        return String.valueOf(key.getSourceOrigin()) + Symbol.OR + key.isHasCrossSiteAncestor();
    }

    /**
     * Returns the convert partition key for testing.
     *
     * @param key key value
     * @return convert partition key for testing value
     */
    String convertPartitionKeyForTesting(CookiePartitionKey key) {
        return convertPartitionKey(key);
    }

    /**
     * Defines options for heap snapshot operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class HeapSnapshotOptions {

        /**
         * Current path.
         */
        private final Path path;

        /**
         * Creates an instance.
         *
         * @param path file path
         */
        public HeapSnapshotOptions(Path path) {
            this.path = Assert.notNull(path, "path");
        }

        /**
         * Returns the path.
         *
         * @return path value
         */
        public Path path() {
            return path;
        }

    }

}
