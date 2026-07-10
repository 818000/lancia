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
package org.miaixz.lancia.kernel.cdp.page;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.convert.Convert;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.bus.core.xyz.ExceptionKit;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Context;
import org.miaixz.lancia.Extension;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Request;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.Tracing;
import org.miaixz.lancia.Worker;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.PageEvent;
import org.miaixz.lancia.kernel.FileChooser;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.kernel.Locator;
import org.miaixz.lancia.kernel.Network;
import org.miaixz.lancia.kernel.cdp.accessibility.CdpAccessibility;
import org.miaixz.lancia.kernel.cdp.auth.CdpWebAuthn;
import org.miaixz.lancia.kernel.cdp.browser.CdpBrowserContext;
import org.miaixz.lancia.kernel.cdp.coverage.CdpCoverage;
import org.miaixz.lancia.kernel.cdp.device.CdpDevicePrompt;
import org.miaixz.lancia.kernel.cdp.device.CdpDevicePromptManager;
import org.miaixz.lancia.kernel.cdp.emulation.CdpBluetooth;
import org.miaixz.lancia.kernel.cdp.emulation.CdpEmulation;
import org.miaixz.lancia.kernel.cdp.input.CdpInput;
import org.miaixz.lancia.kernel.cdp.input.CdpKeyboard;
import org.miaixz.lancia.kernel.cdp.input.CdpMouse;
import org.miaixz.lancia.kernel.cdp.input.CdpTouchscreen;
import org.miaixz.lancia.kernel.cdp.mcp.CdpWebMCP;
import org.miaixz.lancia.kernel.cdp.network.CdpCookie;
import org.miaixz.lancia.kernel.cdp.network.CdpNetworkManager;
import org.miaixz.lancia.kernel.cdp.network.CdpRequest;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.protocol.CdpRuntime;
import org.miaixz.lancia.kernel.cdp.runtime.CdpElementHandle;
import org.miaixz.lancia.kernel.cdp.runtime.CdpJSHandle;
import org.miaixz.lancia.kernel.cdp.runtime.CdpRealm;
import org.miaixz.lancia.kernel.cdp.screen.CdpScreenRecorder;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.kernel.cdp.session.TargetInfo;
import org.miaixz.lancia.kernel.cdp.targets.CdpPageTarget;
import org.miaixz.lancia.kernel.cdp.targets.CdpTarget;
import org.miaixz.lancia.kernel.cdp.tracing.CdpTracing;
import org.miaixz.lancia.kernel.cdp.worker.CdpWorker;
import org.miaixz.lancia.nimble.emulation.Device;
import org.miaixz.lancia.nimble.emulation.Geolocation;
import org.miaixz.lancia.nimble.emulation.IdleState;
import org.miaixz.lancia.nimble.emulation.MediaFeature;
import org.miaixz.lancia.nimble.emulation.Viewport;
import org.miaixz.lancia.nimble.network.Cookie;
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
import org.miaixz.lancia.runtime.Scripts;
import org.miaixz.lancia.runtime.SecurityPolicy;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.locator.ElementLocator;
import org.miaixz.lancia.shared.page.FileChooserRequest;
import org.miaixz.lancia.shared.page.PageDefaults;
import org.miaixz.lancia.shared.page.TagInjection;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Implements page operations over the Chrome DevTools Protocol.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpPage implements Page {

    /**
     * Protocol event name for worker created.
     */
    public static final String WORKER_CREATED = "workercreated";

    /**
     * Protocol event name for worker destroyed.
     */
    public static final String WORKER_DESTROYED = "workerdestroyed";
    /**
     * Default navigation timeout.
     */
    private static final Duration DEFAULT_NAVIGATION_TIMEOUT = Duration.ofSeconds(30);
    /**
     * Default network idle time.
     */
    private static final Duration DEFAULT_NETWORK_IDLE_TIME = Duration.ofMillis(500);
    /**
     * Shared constant for network idle zero.
     */
    private static final int NETWORK_IDLE_ZERO = 0;
    /**
     * Shared constant for wait poll interval.
     */
    private static final Duration WAIT_POLL_INTERVAL = Duration.ofMillis(50);
    /**
     * Timer used for page-level delayed tasks.
     */
    private static final ScheduledExecutorService PAGE_TIMER = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-page-timer-", true));

    /**
     * Current emitter.
     */
    private final EventEmitter<PageEvent> emitter = new EventEmitter<>();
    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Current frame manager.
     */
    private final CdpFrameManager frameManager;
    /**
     * Current network manager.
     */
    private final CdpNetworkManager networkManager;
    /**
     * Thread-safe render security boundary installation state.
     */
    private final AtomicBoolean renderSecurityBoundaryInstalled = new AtomicBoolean();
    /**
     * Current render security boundary event binding.
     */
    private volatile Binding renderSecurityBoundaryBinding;
    /**
     * Current input.
     */
    private final CdpInput input;
    /**
     * Current emulation manager.
     */
    private final CdpEmulation emulationManager;
    /**
     * Current coverage.
     */
    private final CdpCoverage coverage;
    /**
     * Current tracing.
     */
    private final CdpTracing tracing;
    /**
     * Current accessibility.
     */
    private final CdpAccessibility accessibility;
    /**
     * Current device request prompt manager.
     */
    private final CdpDevicePromptManager deviceRequestPromptManager;
    /**
     * Current web authn.
     */
    private final CdpWebAuthn webAuthn;
    /**
     * Current bluetooth emulation.
     */
    private final CdpBluetooth bluetoothEmulation;
    /**
     * Current WebMCP controller.
     */
    private final CdpWebMCP webmcp;
    /**
     * Current worker emitter.
     */
    private final EventEmitter<String> workerEmitter = new EventEmitter<>();
    /**
     * Mapped workers values.
     */
    private final Map<String, Worker> workers = new ConcurrentHashMap<>();
    /**
     * Current keyboard.
     */
    private final CdpKeyboard keyboard;
    /**
     * Current mouse.
     */
    private final CdpMouse mouse;
    /**
     * Current touchscreen.
     */
    private final CdpTouchscreen touchscreen;
    /**
     * Current browser context.
     */
    private CdpBrowserContext browserContext;
    /**
     * Current browser.
     */
    private Browser browser;
    /**
     * Current target.
     */
    private CdpTarget target;
    /**
     * Thread-safe closed state.
     */
    private final AtomicReference<Boolean> closed = new AtomicReference<>(false);
    /**
     * Current current viewport.
     */
    private Viewport currentViewport;
    /**
     * Current default timeout.
     */
    private Duration defaultTimeout = DEFAULT_NAVIGATION_TIMEOUT;
    /**
     * Current default navigation timeout.
     */
    private Duration defaultNavigationTimeout = DEFAULT_NAVIGATION_TIMEOUT;
    /**
     * Whether JavaScript execution is enabled.
     */
    private boolean javaScriptEnabled = true;
    /**
     * Whether bypass CSP is enabled.
     */
    private boolean bypassCsp;
    /**
     * Whether bypass service worker is enabled.
     */
    private boolean bypassServiceWorker;
    /**
     * Whether drag interception is enabled.
     */
    private boolean dragInterceptionEnabled;
    /**
     * Whether cache is enabled.
     */
    private boolean cacheEnabled = true;
    /**
     * Current user agent.
     */
    private String userAgent = Normal.EMPTY;
    /**
     * Mapped extra HTTP headers values.
     */
    private Map<String, String> extraHTTPHeaders = Map.of();
    /**
     * Mapped scripts to evaluate on new document values.
     */
    private final Map<String, String> scriptsToEvaluateOnNewDocument = new ConcurrentHashMap<>();
    /**
     * Registered exposed function names values.
     */
    private final Set<String> exposedFunctionNames = ConcurrentHashMap.newKeySet();

    /**
     * Mapped exposed functions values.
     */
    private final Map<String, ExposedFunction> exposedFunctions = new ConcurrentHashMap<>();

    /**
     * Mapped exposed function preload ids values.
     */
    private final Map<String, String> exposedFunctionPreloadIds = new ConcurrentHashMap<>();
    /**
     * Current local URL.
     */
    private String localUrl = Builder.ABOUT_BLANK;
    /**
     * Current local content.
     */
    private String localContent = "<html><head></head><body></body></html>";
    /**
     * Current local dev tools page.
     */
    private Page localDevToolsPage;
    /**
     * Whether local dev tools open is enabled.
     */
    private boolean localDevToolsOpen;
    /**
     * Mapped local cookies values.
     */
    private final List<Map<String, Object>> localCookies = new java.util.ArrayList<>();
    /**
     * Registered file chooser waiters values.
     */
    private final List<CompletableFuture<FileChooser>> fileChooserWaiters = new CopyOnWriteArrayList<>();
    /**
     * Registered screen recorders values.
     */
    private final List<CdpScreenRecorder> screenRecorders = new CopyOnWriteArrayList<>();
    /**
     * Thread-safe screencast session count state.
     */
    private final AtomicInteger screencastSessionCount = new AtomicInteger();
    /**
     * Thread-safe local tag handle count state.
     */
    private final AtomicInteger localTagHandleCount = new AtomicInteger();
    /**
     * Thread-safe first screencast frame state.
     */
    private final AtomicReference<CompletableFuture<Void>> firstScreencastFrame = new AtomicReference<>();
    /**
     * Whether render request security boundary is enabled.
     */
    private volatile boolean renderSecurityBoundaryEnabled = true;
    /**
     * Current runtime security policy.
     */
    private volatile SecurityPolicy securityPolicy = SecurityPolicy.defaultPolicy();

    /**
     * Creates a CDP page.
     */
    public CdpPage() {
        this.session = null;
        this.frameManager = new CdpFrameManager(this);
        this.networkManager = new CdpNetworkManager(null);
        this.input = new CdpInput(null);
        this.emulationManager = new CdpEmulation(null);
        this.coverage = new CdpCoverage(null);
        this.tracing = new CdpTracing(null);
        this.accessibility = new CdpAccessibility(null);
        this.deviceRequestPromptManager = new CdpDevicePromptManager(null);
        this.webAuthn = new CdpWebAuthn(null);
        this.bluetoothEmulation = new CdpBluetooth(null);
        this.webmcp = new CdpWebMCP(null, frameManager);
        this.keyboard = new CdpKeyboard(input);
        this.mouse = new CdpMouse(input);
        this.touchscreen = new CdpTouchscreen(input);
    }

    /**
     * Creates a CDP page.
     *
     * @param session protocol session
     */
    public CdpPage(CDPSession session) {
        this.session = session;
        this.frameManager = new CdpFrameManager(this, session);
        this.networkManager = new CdpNetworkManager(session);
        this.input = new CdpInput(session);
        this.emulationManager = new CdpEmulation(session);
        this.coverage = new CdpCoverage(session);
        this.tracing = new CdpTracing(session);
        this.accessibility = new CdpAccessibility(session);
        this.deviceRequestPromptManager = new CdpDevicePromptManager(session);
        this.webAuthn = new CdpWebAuthn(session);
        this.bluetoothEmulation = new CdpBluetooth(session);
        this.webmcp = new CdpWebMCP(session, frameManager);
        this.keyboard = new CdpKeyboard(input);
        this.mouse = new CdpMouse(input);
        this.touchscreen = new CdpTouchscreen(input);
        this.frameManager.initialize();
        this.networkManager.initialize();
        this.webmcp.initialize();
        initializeWorkerEvents();
        initializePageEvents();
        try {
            installRenderSecurityBoundary();
        } catch (RuntimeException ex) {
            Logger.warn(false, "Network", ex, "CDP render request boundary deferred.");
        }
    }

    /**
     * Subscribes a persistent page event listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return page
     */
    @Override
    public Page on(PageEvent event, Consumer<Object> listener) {
        emitter.on(event, listener);
        return this;
    }

    /**
     * Subscribes a one-shot page event listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return page
     */
    @Override
    public Page once(PageEvent event, Consumer<Object> listener) {
        emitter.once(event, listener);
        return this;
    }

    /**
     * Removes a page event listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return page
     */
    @Override
    public Page off(PageEvent event, Consumer<Object> listener) {
        emitter.off(event, listener);
        return this;
    }

    /**
     * Removes all listeners for the page event.
     *
     * @param event event type
     * @return page
     */
    @Override
    public Page off(PageEvent event) {
        emitter.off(event);
        return this;
    }

    /**
     * Emits a page event.
     *
     * @param event   event value
     * @param payload event payload
     * @return emitted state
     */
    @Override
    public boolean emit(PageEvent event, Object payload) {
        return emitter.emit(event, payload);
    }

    /**
     * Counts page event listeners.
     *
     * @param event event type
     * @return listener count
     */
    @Override
    public int listenerCount(PageEvent event) {
        return emitter.listenerCount(event);
    }

    /**
     * Removes all listeners for the page event.
     *
     * @param event event type
     * @return page
     */
    @Override
    public Page removeAllListeners(PageEvent event) {
        emitter.removeAllListeners(event);
        return this;
    }

    /**
     * Removes all page event listeners.
     *
     * @return page
     */
    @Override
    public Page removeAllListeners() {
        emitter.removeAllListeners();
        return this;
    }

    /**
     * Handles bind browser context.
     *
     * @param context browser context
     */
    public void bindBrowserContext(Context context) {
        this.browserContext = (CdpBrowserContext) context;
        this.browser = context == null ? null : context.browser();
    }

    /**
     * Handles bind target.
     *
     * @param target target object
     */
    public void bindTarget(Target target) {
        this.target = (CdpTarget) target;
    }

    /**
     * Returns the target.
     *
     * @return target value
     */
    public CdpTarget target() {
        if (target == null) {
            TargetInfo targetInfo = new TargetInfo(targetId(), "page", url(), Normal.EMPTY);
            CdpTarget localTarget = new CdpPageTarget(targetInfo, null, this);
            CdpTarget.Internal.setSession(localTarget, session);
            this.target = localTarget;
        }
        return target;
    }

    /**
     * Returns the browser.
     *
     * @return browser value
     */
    public Browser browser() {
        return browser;
    }

    /**
     * Returns the browser context.
     *
     * @return browser context value
     */
    public CdpBrowserContext browserContext() {
        return browserContext;
    }

    /**
     * Creates CDP session.
     *
     * @return created CDP session
     */
    public Optional<CDPSession> createCDPSession() {
        return session();
    }

    /**
     * Returns the window ID.
     *
     * @return window ID value
     */
    public String windowId() {
        if (session == null) {
            return "local-window";
        }
        CdpPayload result = Awaitable
                .await(session.send("Browser.getWindowForTarget"), "Failed to read page window id.");
        return PayloadReader.text(result.get("windowId"));
    }

    /**
     * Returns the resize.
     *
     * @param contentWidth  content width value
     * @param contentHeight content height value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> resize(int contentWidth, int contentHeight) {
        if (session == null) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        String id = windowId();
        int numericWindowId = StringKit.isBlank(id) ? 0 : Integer.parseInt(id);
        return session.send(
                "Browser.setContentsSize",
                Map.of("windowId", numericWindowId, "width", contentWidth, "height", contentHeight));
    }

    /**
     * Updates default navigation timeout.
     *
     * @param timeout timeout value
     */
    public void setDefaultNavigationTimeout(Duration timeout) {
        this.defaultNavigationTimeout = timeout == null ? DEFAULT_NAVIGATION_TIMEOUT : timeout;
    }

    /**
     * Updates default timeout.
     *
     * @param timeout timeout value
     */
    public void setDefaultTimeout(Duration timeout) {
        this.defaultTimeout = timeout == null ? DEFAULT_NAVIGATION_TIMEOUT : timeout;
    }

    /**
     * Returns the default navigation timeout.
     *
     * @return default navigation timeout value
     */
    public Duration defaultNavigationTimeout() {
        return defaultNavigationTimeout;
    }

    /**
     * Returns the default timeout.
     *
     * @return default timeout value
     */
    public Duration defaultTimeout() {
        return defaultTimeout;
    }

    /**
     * Returns the default timeout.
     *
     * @return default timeout
     */
    public Duration getDefaultTimeout() {
        return defaultTimeout();
    }

    /**
     * Returns the default navigation timeout.
     *
     * @return default navigation timeout
     */
    public Duration getDefaultNavigationTimeout() {
        return defaultNavigationTimeout();
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
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return mainFrame().url();
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url target URL
     */
    public Response goTo(String url) {
        return goTo(url, null);
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url     target URL
     * @param options operation options
     * @return main resource response
     */
    public Response goTo(String url, GoToOptions options) {
        return goToFrame(mainFrame(), url, options);
    }

    /**
     * Returns the go to frame.
     *
     * @param frame   frame instance
     * @param url     target URL
     * @param options operation options
     * @return go to frame value
     */
    private Response goToFrame(CdpFrame frame, String url, GoToOptions options) {
        validateNavigationUrl(url);
        Logger.debug(true, "Page", "Navigation requested: url={}", url);
        if (session == null) {
            CdpFrame.Internal.navigate(
                    frame,
                    url,
                    options == null ? null : options.getReferer(),
                    options == null ? null : options.getReferrerPolicy());
            this.localUrl = url;
            Logger.debug(false, "Page", "Local navigation completed: url={}", url);
            return null;
        }
        CdpLifecycleWatcher watcher = new CdpLifecycleWatcher(frameManager, frame, lifecycleList(options),
                navigationTimeout(options));
        try {
            CdpFrame.Internal.navigate(
                    frame,
                    url,
                    options == null ? null : options.getReferer(),
                    options == null ? null : options.getReferrerPolicy());
            Response response = Awaitable.await(waitForNavigationResult(watcher), "Page navigation failed: " + url);
            Logger.debug(false, "Page", "Navigation completed: url={}", url);
            this.localUrl = url;
            return response;
        } catch (RuntimeException ex) {
            Logger.error(false, "Page", ex, "Navigation failed: url={}", url);
            throw ex;
        } finally {
            watcher.dispose();
        }
    }

    /**
     * Waits for navigation.
     *
     * @return wait for navigation value
     */
    public Response waitForNavigation() {
        return waitForNavigation(defaultNavigationTimeout, "load");
    }

    /**
     * Waits for navigation.
     *
     * @param options operation options
     * @return wait for navigation value
     */
    public Response waitForNavigation(WaitForOptions options) {
        return Awaitable.await(
                waitForNavigationAsync(navigationTimeout(options), lifecycleArray(options)),
                "Failed to wait for page navigation.");
    }

    /**
     * Waits for navigation.
     *
     * @param timeout   timeout value
     * @param waitUntil wait until
     * @return wait for navigation value
     */
    public Response waitForNavigation(Duration timeout, String... waitUntil) {
        return Awaitable.await(waitForNavigationAsync(timeout, waitUntil), "Failed to wait for page navigation.");
    }

    /**
     * Waits for navigation async.
     *
     * @param timeout   timeout value
     * @param waitUntil wait until
     * @return wait for navigation async value
     */
    public CompletableFuture<Response> waitForNavigationAsync(Duration timeout, String... waitUntil) {
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        CdpLifecycleWatcher watcher = new CdpLifecycleWatcher(frameManager, mainFrame(), lifecycleList(waitUntil),
                timeout == null ? defaultNavigationTimeout : timeout);
        CompletableFuture<Response> result = waitForNavigationResult(watcher);
        result.whenComplete((value, throwable) -> watcher.dispose());
        return result;
    }

    /**
     * Waits for network idle.
     */
    public void waitForNetworkIdle() {
        waitForNetworkIdle(defaultTimeout, DEFAULT_NETWORK_IDLE_TIME, NETWORK_IDLE_ZERO);
    }

    /**
     * Waits for network idle.
     *
     * @param timeout             timeout value
     * @param idleTime            idle time
     * @param maxInflightRequests max inflight requests
     */
    public void waitForNetworkIdle(Duration timeout, Duration idleTime, int maxInflightRequests) {
        Awaitable.await(
                waitForNetworkIdleAsync(timeout, idleTime, maxInflightRequests),
                "Failed to wait for page network idle.");
    }

    /**
     * Waits for network idle async.
     *
     * @param timeout             timeout value
     * @param idleTime            idle time
     * @param maxInflightRequests max inflight requests
     * @return wait for network idle async value
     */
    public CompletableFuture<Void> waitForNetworkIdleAsync(
            Duration timeout,
            Duration idleTime,
            int maxInflightRequests) {
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (maxInflightRequests < 0) {
            throw new IllegalArgumentException("Maximum allowed in-flight request count must not be less than 0.");
        }
        Duration safeTimeout = Assert.notNull(timeout, "timeout");
        Duration safeIdleTime = Assert.notNull(idleTime, "idleTime");
        session.send("Network.enable");
        CompletableFuture<Void> result = new CompletableFuture<>();
        Set<String> activeRequests = ConcurrentHashMap.newKeySet();
        AtomicInteger generation = new AtomicInteger();
        AtomicReference<ScheduledFuture<?>> idleTask = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> timeoutTask = new AtomicReference<>();
        AtomicReference<Runnable> reschedule = new AtomicReference<>();
        Runnable cancelIdleTask = () -> {
            ScheduledFuture<?> current = idleTask.getAndSet(null);
            if (current != null) {
                current.cancel(false);
            }
        };
        reschedule.set(() -> {
            int currentGeneration = generation.incrementAndGet();
            cancelIdleTask.run();
            if (activeRequests.size() <= maxInflightRequests) {
                idleTask.set(PAGE_TIMER.schedule(() -> {
                    if (generation.get() == currentGeneration && activeRequests.size() <= maxInflightRequests) {
                        result.complete(null);
                    }
                }, safeIdleTime.toMillis(), TimeUnit.MILLISECONDS));
            }
        });
        Binding binding = new EventBinding().combine(session.on("Network.requestWillBeSent", params -> {
            String requestId = PayloadReader.text(params.get("requestId"));
            if (StringKit.isNotBlank(requestId)) {
                activeRequests.add(requestId);
                reschedule.get().run();
            }
        })).combine(session.on("Network.loadingFinished", params -> {
            String requestId = PayloadReader.text(params.get("requestId"));
            if (StringKit.isNotBlank(requestId)) {
                activeRequests.remove(requestId);
                reschedule.get().run();
            }
        })).combine(session.on("Network.loadingFailed", params -> {
            String requestId = PayloadReader.text(params.get("requestId"));
            if (StringKit.isNotBlank(requestId)) {
                activeRequests.remove(requestId);
                reschedule.get().run();
            }
        }));
        timeoutTask.set(
                PAGE_TIMER.schedule(
                        () -> result.completeExceptionally(
                                new TimeoutException(
                                        "Timed out waiting for page network idle: " + safeTimeout.toMillis() + "ms")),
                        safeTimeout.toMillis(),
                        TimeUnit.MILLISECONDS));
        result.whenComplete((value, throwable) -> {
            binding.unbind();
            cancelIdleTask.run();
            ScheduledFuture<?> currentTimeout = timeoutTask.get();
            if (currentTimeout != null) {
                currentTimeout.cancel(false);
            }
        });
        reschedule.get().run();
        return result;
    }

    /**
     * Waits for network idle$.
     */
    public void waitForNetworkIdle$() {
        waitForNetworkIdle();
    }

    /**
     * Waits for request.
     *
     * @param predicate predicate
     * @return wait for request value
     */
    public Request waitForRequest(Predicate<Request> predicate) {
        return waitForRequest(predicate, defaultTimeout);
    }

    /**
     * Waits for request.
     *
     * @param url target URL
     * @return wait for request value
     */
    public Request waitForRequest(String url) {
        return waitForRequest(url, null);
    }

    /**
     * Waits for request.
     *
     * @param url     target URL
     * @param options operation options
     * @return wait for request value
     */
    public Request waitForRequest(String url, WaitForOptions options) {
        String actualUrl = Assert.notNull(url, "url");
        return waitForRequest(request -> actualUrl.equals(request.url()), waitTimeout(options));
    }

    /**
     * Waits for request.
     *
     * @param pattern pattern
     * @return wait for request value
     */
    public Request waitForRequest(Pattern pattern) {
        return waitForRequest(pattern, null);
    }

    /**
     * Waits for request.
     *
     * @param pattern pattern
     * @param options operation options
     * @return wait for request value
     */
    public Request waitForRequest(Pattern pattern, WaitForOptions options) {
        Pattern actualPattern = Assert.notNull(pattern, "pattern");
        return waitForRequest(request -> actualPattern.matcher(request.url()).find(), waitTimeout(options));
    }

    /**
     * Waits for request.
     *
     * @param predicate predicate
     * @param timeout   timeout value
     * @return wait for request value
     */
    public Request waitForRequest(Predicate<Request> predicate, Duration timeout) {
        return Awaitable.await(waitForRequestAsync(predicate, timeout), "Failed to wait for page request.");
    }

    /**
     * Waits for request async.
     *
     * @param predicate predicate
     * @param timeout   timeout value
     * @return wait for request async value
     */
    public CompletableFuture<Request> waitForRequestAsync(Predicate<Request> predicate, Duration timeout) {
        return waitForNetworkEvent(CdpNetworkManager.REQUEST, Request.class, predicate, timeout);
    }

    /**
     * Waits for response.
     *
     * @param predicate predicate
     * @return wait for response value
     */
    public Response waitForResponse(Predicate<Response> predicate) {
        return waitForResponse(predicate, defaultTimeout);
    }

    /**
     * Waits for response.
     *
     * @param url target URL
     * @return wait for response value
     */
    public Response waitForResponse(String url) {
        return waitForResponse(url, null);
    }

    /**
     * Waits for response.
     *
     * @param url     target URL
     * @param options operation options
     * @return wait for response value
     */
    public Response waitForResponse(String url, WaitForOptions options) {
        String actualUrl = Assert.notNull(url, "url");
        return waitForResponse(response -> actualUrl.equals(response.url()), waitTimeout(options));
    }

    /**
     * Waits for response.
     *
     * @param pattern pattern
     * @return wait for response value
     */
    public Response waitForResponse(Pattern pattern) {
        return waitForResponse(pattern, null);
    }

    /**
     * Waits for response.
     *
     * @param pattern pattern
     * @param options operation options
     * @return wait for response value
     */
    public Response waitForResponse(Pattern pattern, WaitForOptions options) {
        Pattern actualPattern = Assert.notNull(pattern, "pattern");
        return waitForResponse(response -> actualPattern.matcher(response.url()).find(), waitTimeout(options));
    }

    /**
     * Waits for response.
     *
     * @param predicate predicate
     * @param timeout   timeout value
     * @return wait for response value
     */
    public Response waitForResponse(Predicate<Response> predicate, Duration timeout) {
        return Awaitable.await(waitForResponseAsync(predicate, timeout), "Failed to wait for page response.");
    }

    /**
     * Waits for response async.
     *
     * @param predicate predicate
     * @param timeout   timeout value
     * @return wait for response async value
     */
    public CompletableFuture<Response> waitForResponseAsync(Predicate<Response> predicate, Duration timeout) {
        return waitForNetworkEvent(CdpNetworkManager.RESPONSE, Response.class, predicate, timeout);
    }

    /**
     * Returns the reload.
     *
     * @return reload value
     */
    public Response reload() {
        return reload(null);
    }

    /**
     * Returns the reload.
     *
     * @param options operation options
     * @return reload value
     */
    public Response reload(WaitForOptions options) {
        if (session == null) {
            return null;
        }
        CdpLifecycleWatcher watcher = new CdpLifecycleWatcher(frameManager, mainFrame(), lifecycleList(options),
                navigationTimeout(options));
        try {
            Map<String, Object> params = options != null && options.isIgnoreCache() ? Map.of("ignoreCache", true)
                    : Map.of();
            session.send("Page.reload", params);
            return Awaitable.await(waitForNavigationResult(watcher), "Page reload failed.");
        } finally {
            watcher.dispose();
        }
    }

    /**
     * Returns the go back.
     *
     * @return go back value
     */
    public Response goBack() {
        return goBack(null);
    }

    /**
     * Returns the go back.
     *
     * @param options operation options
     * @return go back value
     */
    public Response goBack(WaitForOptions options) {
        return navigateHistory(-1, options);
    }

    /**
     * Returns the go forward.
     *
     * @return go forward value
     */
    public Response goForward() {
        return goForward(null);
    }

    /**
     * Returns the go forward.
     *
     * @param options operation options
     * @return go forward value
     */
    public Response goForward(WaitForOptions options) {
        return navigateHistory(1, options);
    }

    /**
     * Returns the bring to front.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> bringToFront() {
        return sendPageCommand("Page.bringToFront", Map.of());
    }

    /**
     * Returns the frames.
     *
     * @return values
     */
    public List<CdpFrame> frames() {
        List<CdpFrame> frames = new ArrayList<>();
        collectFrames(mainFrame(), frames);
        return List.copyOf(frames);
    }

    /**
     * Waits for frame.
     *
     * @param predicate predicate
     * @param timeout   timeout value
     * @return wait for frame value
     */
    public CdpFrame waitForFrame(Predicate predicate, Duration timeout) {
        Assert.notNull(predicate, "predicate");
        Duration safeTimeout = timeout == null ? defaultTimeout : timeout;
        long deadline = safeTimeout.isZero() || safeTimeout.isNegative() ? Long.MAX_VALUE
                : System.nanoTime() + safeTimeout.toNanos();
        while (true) {
            for (CdpFrame frame : frames()) {
                if (predicate.test(frame)) {
                    return frame;
                }
            }
            if (System.nanoTime() >= deadline) {
                throw new TimeoutException("Timed out waiting for page frame.");
            }
            sleepPollInterval();
        }
    }

    /**
     * Waits for frame.
     *
     * @param predicate predicate
     * @return wait for frame value
     */
    public CdpFrame waitForFrame(Predicate predicate) {
        return waitForFrame(predicate, defaultTimeout);
    }

    /**
     * Updates request interception.
     *
     * @param enabled enabled
     * @return set request interception value
     */
    public CompletableFuture<CdpPayload> setRequestInterception(boolean enabled) {
        if (!enabled) {
            Binding binding = renderSecurityBoundaryBinding;
            if (binding != null) {
                binding.unbind();
                renderSecurityBoundaryBinding = null;
            }
            renderSecurityBoundaryInstalled.set(false);
        }
        return networkManager.setRequestInterception(enabled);
    }

    /**
     * Updates bypass service worker.
     *
     * @param bypass bypass
     * @return set bypass service worker value
     */
    public CompletableFuture<CdpPayload> setBypassServiceWorker(boolean bypass) {
        this.bypassServiceWorker = bypass;
        return sendPageCommand("Network.setBypassServiceWorker", Map.of("bypass", bypass));
    }

    /**
     * Returns the bypass service worker.
     *
     * @return {@code true} when the condition matches
     */
    public boolean bypassServiceWorker() {
        return bypassServiceWorker;
    }

    /**
     * Returns whether service worker bypassed is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isServiceWorkerBypassed() {
        return bypassServiceWorker();
    }

    /**
     * Updates drag interception.
     *
     * @param enabled enabled
     * @return set drag interception value
     */
    public CompletableFuture<CdpPayload> setDragInterception(boolean enabled) {
        this.dragInterceptionEnabled = enabled;
        return sendPageCommand("CdpInput.setInterceptDrags", Map.of("enabled", enabled));
    }

    /**
     * Returns the drag interception enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean dragInterceptionEnabled() {
        return dragInterceptionEnabled;
    }

    /**
     * Returns whether drag interception is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isDragInterceptionEnabled() {
        return dragInterceptionEnabled();
    }

    /**
     * Updates offline mode.
     *
     * @param enabled enabled
     * @return set offline mode value
     */
    public CompletableFuture<CdpPayload> setOfflineMode(boolean enabled) {
        return networkManager.setOfflineMode(enabled);
    }

    /**
     * Returns the emulate network conditions.
     *
     * @param conditions conditions value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateNetworkConditions(NetworkConditions conditions) {
        NetworkConditions actual = conditions == null ? NetworkConditions.online() : conditions;
        return networkManager.emulateNetworkConditions(
                actual.offline(),
                actual.latency(),
                actual.downloadThroughput(),
                actual.uploadThroughput());
    }

    /**
     * Updates extra HTTP headers.
     *
     * @param headers HTTP headers
     * @return set extra HTTP headers value
     */
    public CompletableFuture<CdpPayload> setExtraHTTPHeaders(Map<String, String> headers) {
        this.extraHTTPHeaders = headers == null ? Map.of() : Map.copyOf(headers);
        return networkManager.setExtraHTTPHeaders(extraHTTPHeaders);
    }

    /**
     * Returns the extra HTTP headers.
     *
     * @return mapped values
     */
    public Map<String, String> extraHTTPHeaders() {
        return Map.copyOf(extraHTTPHeaders);
    }

    /**
     * Updates user agent.
     *
     * @param userAgent user agent
     * @return set user agent value
     */
    public CompletableFuture<CdpPayload> setUserAgent(String userAgent) {
        this.userAgent = userAgent == null ? Normal.EMPTY : userAgent;
        return networkManager.setUserAgent(this.userAgent);
    }

    /**
     * Updates user agent.
     *
     * @param options operation options
     * @return set user agent value
     */
    public CompletableFuture<CdpPayload> setUserAgent(UserAgentOptions options) {
        this.userAgent = options == null || options.getUserAgent() == null ? Normal.EMPTY : options.getUserAgent();
        return networkManager.setUserAgent(options);
    }

    /**
     * Returns the user agent.
     *
     * @return user agent value
     */
    public String userAgent() {
        return userAgent;
    }

    /**
     * Returns the authenticate.
     *
     * @param credentials credentials value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> authenticate(Credentials credentials) {
        if (credentials == null) {
            return networkManager.authenticate(null, null);
        }
        return networkManager.authenticate(credentials.username(), credentials.password());
    }

    /**
     * Returns the authenticate.
     *
     * @param username username value
     * @param password password value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> authenticate(String username, String password) {
        return authenticate(new Credentials(username, password));
    }

    /**
     * Updates cache enabled.
     *
     * @param enabled enabled
     * @return set cache enabled value
     */
    public CompletableFuture<CdpPayload> setCacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
        return networkManager.setCacheEnabled(enabled);
    }

    /**
     * Returns the cache enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean cacheEnabled() {
        return cacheEnabled;
    }

    /**
     * Returns the current HTML content.
     *
     * @return current HTML content
     */
    public String content() {
        if (session == null) {
            return localContent;
        }
        return mainFrame().content();
    }

    /**
     * Updates content.
     *
     * @param html HTML content
     */
    public void setContent(String html) {
        if (session == null) {
            this.localContent = html;
            return;
        }
        mainFrame().setContent(html);
    }

    /**
     * Adds script tag.
     *
     * @param content content
     * @return created script element
     */
    public CdpElementHandle addScriptTag(String content) {
        return addScriptTag(TagInjection.scriptTag(content));
    }

    /**
     * Adds script tag.
     *
     * @param options operation options
     * @return created script element
     */
    public CdpElementHandle addScriptTag(ScriptTagOptions options) {
        ScriptTagOptions actual = options == null ? new ScriptTagOptions() : options;
        if (StringKit.isNotBlank(actual.getUrl())) {
            validateRenderRequestUrl(actual.getUrl());
        }
        if (session == null) {
            this.localContent = injectBeforeEnd(localContent, "body", localScriptSnippet(actual));
            return localElementHandle("local-script-");
        }
        return mainFrame().addScriptTag(actual);
    }

    /**
     * Adds style tag.
     *
     * @param content content
     * @return created style element
     */
    public CdpElementHandle addStyleTag(String content) {
        return addStyleTag(TagInjection.styleTag(content));
    }

    /**
     * Adds style tag.
     *
     * @param options operation options
     * @return created style or link element
     */
    public CdpElementHandle addStyleTag(StyleTagOptions options) {
        StyleTagOptions actual = options == null ? new StyleTagOptions() : options;
        if (StringKit.isNotBlank(actual.getUrl())) {
            validateRenderRequestUrl(actual.getUrl());
        }
        if (session == null) {
            this.localContent = injectBeforeEnd(localContent, "head", localStyleSnippet(actual));
            return localElementHandle("local-style-");
        }
        return mainFrame().addStyleTag(actual);
    }

    /**
     * Returns the title.
     *
     * @return title value
     */
    public String title() {
        if (session == null) {
            return extractTitle(localContent);
        }
        return mainFrame().title();
    }

    /**
     * Returns the evaluate on new document.
     *
     * @param source source value
     * @return evaluate on new document value
     */
    public String evaluateOnNewDocument(String source) {
        String actualSource = source == null ? Normal.EMPTY : source;
        if (session == null) {
            String identifier = "local-script-" + scriptsToEvaluateOnNewDocument.size();
            scriptsToEvaluateOnNewDocument.put(identifier, actualSource);
            return identifier;
        }
        CdpPayload result = Awaitable.await(
                session.send("Page.addScriptToEvaluateOnNewDocument", Map.of("source", actualSource)),
                "Failed to add new document script.");
        String identifier = PayloadReader.text(result.get("identifier"));
        if (StringKit.isBlank(identifier)) {
            identifier = "script-" + scriptsToEvaluateOnNewDocument.size();
        }
        scriptsToEvaluateOnNewDocument.put(identifier, actualSource);
        return identifier;
    }

    /**
     * Removes script to evaluate on new document.
     *
     * @param identifier identifier
     * @return remove script to evaluate on new document value
     */
    public CompletableFuture<CdpPayload> removeScriptToEvaluateOnNewDocument(String identifier) {
        String actualIdentifier = Assert.notBlank(identifier, "identifier");
        scriptsToEvaluateOnNewDocument.remove(actualIdentifier);
        return sendPageCommand("Page.removeScriptToEvaluateOnNewDocument", Map.of("identifier", actualIdentifier));
    }

    /**
     * Returns the scripts to evaluate on new document.
     *
     * @return mapped values
     */
    public Map<String, String> scriptsToEvaluateOnNewDocument() {
        return Map.copyOf(scriptsToEvaluateOnNewDocument);
    }

    /**
     * Returns the cookies.
     *
     * @param urls target URLs
     * @return values
     */
    public List<Cookie> cookies(String... urls) {
        if (session == null) {
            return localCookies.stream().map(cookie -> CdpCookie.from(CdpPayload.of(cookie))).toList();
        }
        List<String> targets = urls == null || urls.length == 0 ? List.of(url()) : List.of(urls);
        CdpPayload result = Awaitable
                .await(session.send("Network.getCookies", Map.of("urls", targets)), "Failed to read page cookies.");
        return result.get("cookies").elements().stream().map(CdpCookie::from).toList();
    }

    /**
     * Updates cookie.
     *
     * @param cookies cookies to use
     * @return set cookie value
     */
    public CompletableFuture<CdpPayload> setCookie(CookieParam... cookies) {
        List<Map<String, Object>> values = new java.util.ArrayList<>();
        if (cookies != null) {
            for (CookieParam cookie : cookies) {
                Map<String, Object> value = cookie.toMap();
                values.add(value);
                localCookies.add(value);
            }
        }
        if (session == null) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return session.send("Network.setCookies", Map.of("cookies", values));
    }

    /**
     * Returns the delete cookie.
     *
     * @param cookies cookies to use
     * @return completion future
     */
    public CompletableFuture<Void> deleteCookie(DeleteCookiesParameters... cookies) {
        List<CompletableFuture<CdpPayload>> futures = new java.util.ArrayList<>();
        if (cookies != null) {
            for (DeleteCookiesParameters cookie : cookies) {
                Map<String, Object> value = cookie.toMap();
                localCookies.removeIf(item -> java.util.Objects.equals(value.get("name"), item.get("name")));
                if (session != null) {
                    futures.add(session.send("Network.deleteCookies", value));
                }
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * Waits for the next file chooser.
     *
     * @return wait for file chooser value
     */
    public FileChooser waitForFileChooser() {
        return waitForFileChooser(defaultTimeout);
    }

    /**
     * Waits for the next file chooser.
     *
     * @param timeout timeout
     * @return wait for file chooser value
     */
    public FileChooser waitForFileChooser(Duration timeout) {
        if (session == null) {
            throw new InternalException("File chooser waiting requires an attached CDP session.");
        }
        Duration actualTimeout = timeout == null ? defaultTimeout : timeout;
        CompletableFuture<FileChooser> waiter = new CompletableFuture<>();
        boolean needsEnable = fileChooserWaiters.isEmpty();
        fileChooserWaiters.add(waiter);
        CompletableFuture<CdpPayload> enable = needsEnable
                ? session.send("Page.setInterceptFileChooserDialog", Map.of("enabled", true))
                : CompletableFuture.completedFuture(CdpPayload.NULL);
        CompletableFuture<FileChooser> result = waiter.thenCombine(enable, (chooser, ignored) -> chooser);
        try {
            return result.get(Math.max(1L, actualTimeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fileChooserWaiters.remove(waiter);
            throw new InternalException("Waiting for file chooser was interrupted.", ex);
        } catch (java.util.concurrent.TimeoutException ex) {
            fileChooserWaiters.remove(waiter);
            throw new TimeoutException("Waiting for file chooser timed out.", ex);
        } catch (ExecutionException ex) {
            fileChooserWaiters.remove(waiter);
            throw new InternalException("Waiting for file chooser failed.",
                    ExceptionKit.unwrap(ex.getCause() == null ? ex : ex.getCause()));
        }
    }

    /**
     * Starts page screencasting with a recorder.
     *
     * @param options options
     * @return screencast value
     */
    public CdpScreenRecorder screencast(ScreencastOptions options) {
        ScreencastOptions actualOptions = options == null ? new ScreencastOptions() : options;
        CdpScreenRecorder recorder = CdpScreenRecorder
                .start(this, screencastWidth(), screencastHeight(), actualOptions);
        screenRecorders.add(recorder);
        startScreencast();
        return recorder;
    }

    /**
     * Starts the CDP screencast stream.
     *
     * @return start screencast value
     */
    public CompletableFuture<Void> startScreencast() {
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        int sessions = screencastSessionCount.incrementAndGet();
        if (sessions > 1) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> firstFrame = new CompletableFuture<>();
        firstScreencastFrame.set(firstFrame);
        return session.send("Page.startScreencast", Map.of("format", "png", "everyNthFrame", 1))
                .thenCompose(ignored -> firstFrame);
    }

    /**
     * Stops the CDP screencast stream.
     *
     * @return stop screencast value
     */
    public CompletableFuture<Void> stopScreencast() {
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        int current = screencastSessionCount.get();
        if (current <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        int remaining = screencastSessionCount.decrementAndGet();
        if (remaining > 0) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> firstFrame = firstScreencastFrame.getAndSet(null);
        if (firstFrame != null && !firstFrame.isDone()) {
            firstFrame.complete(null);
        }
        return session.send("Page.stopScreencast").thenApply(ignored -> null);
    }

    /**
     * Updates default screenshot options.
     *
     * @param options operation options
     */
    public static void setDefaultScreenshotOptions(ScreenshotOptions options) {
        PageDefaults.setDefaultScreenshotOptions(options);
    }

    /**
     * Returns the screenshot.
     *
     * @return screenshot value
     */
    public byte[] screenshot() {
        return screenshot(new ScreenshotOptions());
    }

    /**
     * Returns the screenshot.
     *
     * @param options operation options
     * @return screenshot value
     */
    public byte[] screenshot(ScreenshotOptions options) {
        if (session == null) {
            Logger.debug(false, "Page", "Screenshot skipped for local page.");
            return new byte[0];
        }
        installRenderSecurityBoundary();
        ScreenshotOptions safeOptions = mergeScreenshotOptions(options);
        String base64 = captureScreenshotData(safeOptions);
        byte[] bytes = "base64".equalsIgnoreCase(safeOptions.getEncoding()) ? ByteKit.toBytes(base64, Charset.UTF_8)
                : Base64.decode(base64);
        writePath(safeOptions.getPath(), Base64.decode(base64));
        Logger.debug(false, "Page", "Screenshot completed: path={}, bytes={}", safeOptions.getPath(), bytes.length);
        return bytes;
    }

    /**
     * Captures screenshot data as CDP base64.
     *
     * @param safeOptions safe options
     * @return base64 data
     */
    private String captureScreenshotData(ScreenshotOptions safeOptions) {
        Logger.debug(
                true,
                "Page",
                "Screenshot requested: path={}, fullPage={}, type={}, encoding={}",
                safeOptions.getPath(),
                safeOptions.isFullPage(),
                safeOptions.getType(),
                safeOptions.getEncoding());
        Viewport originalViewport = currentViewport;
        boolean restoreViewport = prepareScreenshotViewport(safeOptions);
        if (!safeOptions.isFullPage() && Boolean.FALSE.equals(safeOptions.getCaptureBeyondViewport())) {
            clipToVisualViewport(safeOptions);
        }
        try {
            if (safeOptions.isOmitBackground()) {
                setTransparentBackground(true);
            }
            CdpPayload result = Awaitable
                    .await(session.send("Page.captureScreenshot", safeOptions.toMap()), "Page screenshot failed.");
            return PayloadReader.text(result.get("data"));
        } catch (RuntimeException ex) {
            Logger.error(false, "Page", ex, "Screenshot failed: path={}", safeOptions.getPath());
            throw ex;
        } finally {
            if (safeOptions.isOmitBackground()) {
                setTransparentBackground(false);
            }
            if (restoreViewport) {
                Awaitable.await(setViewport(originalViewport), "Failed to restore viewport after screenshot.");
            }
        }
    }

    /**
     * Returns the PDF.
     *
     * @return PDF value
     */
    public byte[] pdf() {
        return pdf(new PDFOptions());
    }

    /**
     * Returns the PDF.
     *
     * @param options operation options
     * @return PDF value
     */
    public byte[] pdf(PDFOptions options) {
        if (session == null) {
            Logger.debug(false, "Page", "PDF skipped for local page.");
            return new byte[0];
        }
        installRenderSecurityBoundary();
        PDFOptions safeOptions = options == null ? new PDFOptions() : options;
        Logger.debug(true, "Page", "PDF requested: path={}, format={}", safeOptions.getPath(), safeOptions.getFormat());
        try {
            if (safeOptions.isOmitBackground()) {
                setTransparentBackground(true);
            }
            CdpPayload result = Awaitable
                    .await(session.send("Page.printToPDF", safeOptions.toMap()), "Page PDF export failed.");
            byte[] bytes = result.get("stream").isNull() ? Base64.decode(PayloadReader.text(result.get("data")))
                    : Builder.readProtocolStream(session, PayloadReader.text(result.get("stream")));
            writePath(safeOptions.getPath(), bytes);
            Logger.debug(false, "Page", "PDF completed: path={}, bytes={}", safeOptions.getPath(), bytes.length);
            return bytes;
        } catch (RuntimeException ex) {
            Logger.error(false, "Page", ex, "PDF failed: path={}", safeOptions.getPath());
            throw ex;
        } finally {
            if (safeOptions.isOmitBackground()) {
                setTransparentBackground(false);
            }
        }
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return evaluate value
     */
    public Object evaluate(String expression) {
        return mainFrame().evaluate(expression);
    }

    /**
     * Returns the $eval.
     *
     * @param selector     selector text
     * @param pageFunction page function value
     * @return $eval value
     */
    public Object $eval(String selector, String pageFunction) {
        return mainFrame().$eval(selector, pageFunction);
    }

    /**
     * Returns the $$eval.
     *
     * @param selector     selector text
     * @param pageFunction page function value
     * @return $$eval value
     */
    public Object $$eval(String selector, String pageFunction) {
        return mainFrame().$$eval(selector, pageFunction);
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return evaluate handle value
     */
    public CdpJSHandle evaluateHandle(String expression) {
        if (session == null) {
            return new CdpJSHandle(CdpPayload.NULL);
        }
        CdpPayload result = Awaitable.await(
                session.send(
                        "Runtime.evaluate",
                        Map.of(
                                "expression",
                                expression == null ? Normal.EMPTY : expression,
                                "returnByValue",
                                false,
                                "awaitPromise",
                                true)),
                "Page script evaluation with handle failed.").get("result");
        return new CdpJSHandle(result, session);
    }

    /**
     * Returns the query objects.
     *
     * @param prototypeHandle prototype handle value
     * @return query objects value
     */
    public CdpJSHandle queryObjects(Handle prototypeHandle) {
        Assert.notNull(prototypeHandle, "prototypeHandle");
        if (!(prototypeHandle instanceof CdpJSHandle handle)) {
            throw new InternalException("Prototype CdpJSHandle must be created by the current runtime.");
        }
        if (handle.disposed()) {
            throw new InternalException("Prototype CdpJSHandle has been disposed.");
        }
        String objectId = handle.id();
        if (StringKit.isBlank(objectId)) {
            throw new InternalException("Prototype CdpJSHandle must not reference primitive values.");
        }
        if (session == null) {
            return new CdpJSHandle(CdpPayload.NULL);
        }
        CdpPayload result = Awaitable.await(
                session.send("Runtime.queryObjects", Map.of("prototypeObjectId", objectId)),
                "Failed to query runtime objects.");
        return new CdpJSHandle(result.get("objects"), session);
    }

    /**
     * Waits for function.
     *
     * @param expression JavaScript expression
     * @return wait for function value
     */
    public Object waitForFunction(String expression) {
        return waitForFunction(expression, defaultTimeout);
    }

    /**
     * Waits for function.
     *
     * @param expression JavaScript expression
     * @param timeout    timeout value
     * @return wait for function value
     */
    public Object waitForFunction(String expression, Duration timeout) {
        Duration safeTimeout = timeout == null ? defaultTimeout : timeout;
        long deadline = safeTimeout.isZero() || safeTimeout.isNegative() ? Long.MAX_VALUE
                : System.nanoTime() + safeTimeout.toNanos();
        while (true) {
            Object value = evaluate(expression);
            if (Builder.isTruthy(value)) {
                return value;
            }
            if (System.nanoTime() >= deadline) {
                throw new TimeoutException("Timed out waiting for page function result.");
            }
            sleepPollInterval();
        }
    }

    /**
     * Returns the $.
     *
     * @param selector selector text
     * @return optional value
     */
    public Optional<CdpElementHandle> $(String selector) {
        return mainFrame().waitForSelector(selector);
    }

    /**
     * Returns the $$.
     *
     * @param selector selector text
     * @return values
     */
    public List<CdpElementHandle> $$(String selector) {
        return mainFrame().$$(selector);
    }

    /**
     * Waits for selector.
     *
     * @param selector selector text
     * @return wait for selector value
     */
    public Optional<CdpElementHandle> waitForSelector(String selector) {
        return mainFrame().waitForSelector(selector);
    }

    /**
     * Waits for selector.
     *
     * @param selector selector text
     * @param options  selector wait options
     * @return wait for selector value
     */
    public Optional<CdpElementHandle> waitForSelector(String selector, WaitForSelectorOptions options) {
        return mainFrame().waitForSelector(selector, options);
    }

    /**
     * Handles click.
     *
     * @param selector selector text
     */
    public void click(String selector) {
        mainFrame().click(selector);
    }

    /**
     * Handles click.
     *
     * @param selector selector text
     * @param options  operation options
     */
    public void click(String selector, ClickOptions options) {
        mainFrame().click(selector, options);
    }

    /**
     * Handles focus.
     *
     * @param selector selector text
     */
    public void focus(String selector) {
        mainFrame().focus(selector);
    }

    /**
     * Handles hover.
     *
     * @param selector selector text
     */
    public void hover(String selector) {
        mainFrame().hover(selector);
    }

    /**
     * Handles tap.
     *
     * @param selector selector text
     */
    public void tap(String selector) {
        mainFrame().tap(selector);
    }

    /**
     * Handles type.
     *
     * @param selector selector text
     * @param text     text to use
     */
    public void type(String selector, String text) {
        mainFrame().type(selector, text);
    }

    /**
     * Handles type.
     *
     * @param selector selector text
     * @param text     text to use
     * @param options  operation options
     */
    public void type(String selector, String text, KeyboardTypeOptions options) {
        mainFrame().type(selector, text, options);
    }

    /**
     * Returns the select.
     *
     * @param selector selector text
     * @param values   values value
     * @return values
     */
    public List<String> select(String selector, String... values) {
        return mainFrame().select(selector, values);
    }

    /**
     * Returns the locator.
     *
     * @param selector selector text
     * @return locator value
     */
    public Locator locator(String selector) {
        return new ElementLocator(this, selector);
    }

    /**
     * Returns the locator race.
     *
     * @param locators locators value
     * @return locator race value
     */
    public Locator locatorRace(List<? extends Locator> locators) {
        List<Locator> actualLocators = Assert.notNull(locators, "locators").stream().map(locator -> {
            if (locator instanceof Locator actualLocator) {
                return actualLocator;
            }
            throw new InternalException("locatorRace requires Lancia locator instances.");
        }).toList();
        return Locator.race(actualLocators);
    }

    /**
     * Returns the main frame.
     *
     * @return main frame value
     */
    public CdpFrame mainFrame() {
        return frameManager.mainFrame();
    }

    /**
     * Returns the frame manager.
     *
     * @return frame manager value
     */
    public CdpFrameManager frameManager() {
        return frameManager;
    }

    /**
     * Returns the network manager.
     *
     * @return network manager value
     */
    public Network networkManager() {
        return networkManager;
    }

    /**
     * Returns the emulation manager.
     *
     * @return emulation manager value
     */
    public CdpEmulation emulationManager() {
        return emulationManager;
    }

    /**
     * Returns the coverage.
     *
     * @return coverage value
     */
    public CdpCoverage coverage() {
        return coverage;
    }

    /**
     * Returns the tracing.
     *
     * @return tracing value
     */
    public Tracing tracing() {
        return tracing;
    }

    /**
     * Returns the accessibility.
     *
     * @return accessibility value
     */
    public CdpAccessibility accessibility() {
        return accessibility;
    }

    /**
     * Waits for device prompt.
     *
     * @param timeout timeout value
     * @return wait for device prompt value
     */
    public CompletableFuture<CdpDevicePrompt> waitForDevicePrompt(Duration timeout) {
        return deviceRequestPromptManager.waitForDevicePrompt(timeout);
    }

    /**
     * Returns the web authn.
     *
     * @return web authn value
     */
    public CdpWebAuthn webAuthn() {
        return webAuthn;
    }

    /**
     * Returns the bluetooth.
     *
     * @return bluetooth value
     */
    public CdpBluetooth bluetooth() {
        return bluetoothEmulation;
    }

    /**
     * Returns the WebMCP controller.
     *
     * @return WebMCP controller
     */
    public CdpWebMCP webmcp() {
        return webmcp;
    }

    /**
     * Returns the extension realms.
     *
     * @return values
     */
    public List<CdpRealm> extensionRealms() {
        return mainFrame().extensionRealms();
    }

    /**
     * Returns the target ID.
     *
     * @return target ID value
     */
    public String targetId() {
        if (session == null) {
            return "local-page";
        }
        return CDPSession.Internal.targetInfo(session).getTargetId();
    }

    /**
     * Returns the open dev tools.
     *
     * @return open dev tools value
     */
    public Page openDevTools() {
        if (session == null) {
            localDevToolsOpen = true;
            if (localDevToolsPage == null) {
                localDevToolsPage = new CdpPage();
            }
            return localDevToolsPage;
        }
        String devToolsTargetId = devToolsTargetId();
        if (StringKit.isBlank(devToolsTargetId)) {
            CdpPayload opened = Awaitable.await(
                    session.connection().send("Target.openDevTools", Map.of("targetId", targetId())),
                    "Failed to open DevTools.");
            devToolsTargetId = PayloadReader.text(opened.get("targetId"));
        }
        if (StringKit.isBlank(devToolsTargetId)) {
            throw new IllegalStateException("Failed to open DevTools: missing DevTools targetId.");
        }
        Page devToolsPage = createDevToolsPage(devToolsTargetId);
        localDevToolsOpen = true;
        localDevToolsPage = devToolsPage;
        return devToolsPage;
    }

    /**
     * Returns whether dev tools is available.
     *
     * @return {@code true} when the condition matches
     */
    public boolean hasDevTools() {
        if (session == null) {
            return localDevToolsOpen;
        }
        return StringKit.isNotBlank(devToolsTargetId());
    }

    /**
     * Returns the trigger extension action.
     *
     * @param extension extension value
     * @return completion future
     */
    public CompletableFuture<Void> triggerExtensionAction(Extension extension) {
        return Assert.notNull(extension, "extension").triggerAction(this);
    }

    /**
     * Returns the on worker created.
     *
     * @param listener event listener
     * @return on worker created value
     */
    public Binding onWorkerCreated(Consumer<Worker> listener) {
        Consumer<Object> bridge = payload -> listener.accept((Worker) payload);
        workerEmitter.on(WORKER_CREATED, bridge);
        return new EventBinding(() -> workerEmitter.off(WORKER_CREATED, bridge));
    }

    /**
     * Returns the on worker destroyed.
     *
     * @param listener event listener
     * @return on worker destroyed value
     */
    public Binding onWorkerDestroyed(Consumer<Worker> listener) {
        Consumer<Object> bridge = payload -> listener.accept((Worker) payload);
        workerEmitter.on(WORKER_DESTROYED, bridge);
        return new EventBinding(() -> workerEmitter.off(WORKER_DESTROYED, bridge));
    }

    /**
     * Returns the on request.
     *
     * @param listener event listener
     * @return on request value
     */
    public Binding onRequest(Consumer<Request> listener) {
        return networkManager.on(CdpNetworkManager.REQUEST, payload -> listener.accept((Request) payload));
    }

    /**
     * Returns the on request served from cache.
     *
     * @param listener event listener
     * @return on request served from cache value
     */
    public Binding onRequestServedFromCache(Consumer<Request> listener) {
        return networkManager
                .on(CdpNetworkManager.REQUEST_SERVED_FROM_CACHE, payload -> listener.accept((Request) payload));
    }

    /**
     * Returns the on response.
     *
     * @param listener event listener
     * @return on response value
     */
    public Binding onResponse(Consumer<Response> listener) {
        return networkManager.on(CdpNetworkManager.RESPONSE, payload -> listener.accept((Response) payload));
    }

    /**
     * Returns the on request finished.
     *
     * @param listener event listener
     * @return on request finished value
     */
    public Binding onRequestFinished(Consumer<Request> listener) {
        return networkManager.on(CdpNetworkManager.REQUEST_FINISHED, payload -> listener.accept((Request) payload));
    }

    /**
     * Returns the on request failed.
     *
     * @param listener event listener
     * @return on request failed value
     */
    public Binding onRequestFailed(Consumer<Request> listener) {
        return networkManager.on(CdpNetworkManager.REQUEST_FAILED, payload -> listener.accept((Request) payload));
    }

    /**
     * Returns the workers.
     *
     * @return values
     */
    public List<Worker> workers() {
        return List.copyOf(workers.values());
    }

    /**
     * Handles remove worker.
     *
     * @param worker worker value
     */
    void removeWorker(Worker worker) {
        if (worker == null) {
            return;
        }
        String match = null;
        for (Map.Entry<String, Worker> entry : workers.entrySet()) {
            if (entry.getValue() == worker) {
                match = entry.getKey();
                break;
            }
        }
        if (match == null) {
            return;
        }
        Worker removed = workers.remove(match);
        if (removed instanceof CdpWorker actualWorker) {
            actualWorker.dispose();
            workerEmitter.emit(WORKER_DESTROYED, actualWorker);
        } else if (removed != null) {
            workerEmitter.emit(WORKER_DESTROYED, removed);
        }
    }

    /**
     * Returns the keyboard input controller.
     *
     * @return keyboard input controller
     */
    public CdpKeyboard keyboard() {
        return keyboard;
    }

    /**
     * Returns the mouse input controller.
     *
     * @return mouse input controller
     */
    public CdpMouse mouse() {
        return mouse;
    }

    /**
     * Returns the touchscreen input controller.
     *
     * @return touchscreen
     */
    public CdpTouchscreen touchscreen() {
        return touchscreen;
    }

    /**
     * Updates viewport.
     *
     * @param viewport viewport
     * @return set viewport value
     */
    public CompletableFuture<Void> setViewport(Viewport viewport) {
        this.currentViewport = viewport;
        return emulationManager.setViewport(viewport);
    }

    /**
     * Returns the viewport.
     *
     * @return viewport value
     */
    public Viewport viewport() {
        return currentViewport;
    }

    /**
     * Returns the emulate.
     *
     * @param device device value
     * @return completion future
     */
    public CompletableFuture<Void> emulate(Device device) {
        this.currentViewport = device == null ? null : device.viewport();
        return emulationManager.emulate(device);
    }

    /**
     * Returns the emulate media type.
     *
     * @param mediaType media type value
     * @return completion future
     */
    public CompletableFuture<Void> emulateMediaType(String mediaType) {
        return emulationManager.emulateMediaType(mediaType);
    }

    /**
     * Returns the emulate media features.
     *
     * @param mediaFeatures media features value
     * @return completion future
     */
    public CompletableFuture<Void> emulateMediaFeatures(List<MediaFeature> mediaFeatures) {
        return emulationManager.emulateMediaFeatures(mediaFeatures);
    }

    /**
     * Returns the emulate cpu throttling.
     *
     * @param rate rate value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateCPUThrottling(double rate) {
        return emulationManager.emulateCPUThrottling(rate);
    }

    /**
     * Returns the emulate idle state.
     *
     * @param idleState idle state value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateIdleState(IdleState idleState) {
        return emulationManager.emulateIdleState(idleState);
    }

    /**
     * Returns the emulate timezone.
     *
     * @param timezone timezone value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateTimezone(String timezone) {
        return emulationManager.emulateTimezone(timezone);
    }

    /**
     * Returns the emulate vision deficiency.
     *
     * @param visionDeficiency vision deficiency value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateVisionDeficiency(String visionDeficiency) {
        return emulationManager.emulateVisionDeficiency(visionDeficiency);
    }

    /**
     * Updates geolocation.
     *
     * @param geolocation geolocation
     * @return set geolocation value
     */
    public CompletableFuture<CdpPayload> setGeolocation(Geolocation geolocation) {
        return emulationManager.setGeolocation(geolocation);
    }

    /**
     * Updates whether JavaScript execution is enabled.
     *
     * @param enabled enabled
     * @return protocol response future
     */
    public CompletableFuture<CdpPayload> setJavaScriptEnabled(boolean enabled) {
        this.javaScriptEnabled = enabled;
        return emulationManager.setJavaScriptEnabled(enabled);
    }

    /**
     * Returns whether JavaScript execution is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean javaScriptEnabled() {
        return emulationManager.javaScriptEnabled();
    }

    /**
     * Returns whether JavaScript execution is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isJavaScriptEnabled() {
        return javaScriptEnabled();
    }

    /**
     * Updates bypass csp.
     *
     * @param enabled enabled
     * @return set bypass csp value
     */
    public CompletableFuture<CdpPayload> setBypassCSP(boolean enabled) {
        this.bypassCsp = enabled;
        return sendPageCommand("Page.setBypassCSP", Map.of("enabled", enabled));
    }

    /**
     * Returns the bypass CSP.
     *
     * @return {@code true} when the condition matches
     */
    public boolean bypassCSP() {
        return bypassCsp;
    }

    /**
     * Returns the emulate focused page.
     *
     * @param enabled whether the feature should be enabled
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateFocusedPage(boolean enabled) {
        return emulationManager.emulateFocus(enabled);
    }

    /**
     * Returns the metrics.
     *
     * @return mapped values
     */
    public Map<String, Number> metrics() {
        if (session == null) {
            return Map.of();
        }
        CdpPayload result = Awaitable
                .await(session.send("Performance.getMetrics"), "Failed to read page performance metrics.");
        Map<String, Number> metrics = new LinkedHashMap<>();
        for (CdpPayload metric : result.get("metrics").elements()) {
            String name = PayloadReader.text(metric.get("name"));
            if (StringKit.isNotBlank(name) && !metric.get("value").isNull()) {
                Object value = metric.get("value").raw();
                if (value instanceof Number number) {
                    metrics.put(name, number);
                }
            }
        }
        return Map.copyOf(metrics);
    }

    /**
     * Handles capture heap snapshot.
     *
     * @param path file path
     */
    public void captureHeapSnapshot(Path path) {
        Assert.notNull(path, "path");
        if (session == null) {
            writePath(path, new byte[0]);
            return;
        }
        StringBuilder chunks = new StringBuilder();
        Binding binding = session.on(
                "HeapProfiler.addHeapSnapshotChunk",
                params -> chunks.append(PayloadReader.text(params.get("chunk"))));
        try {
            Awaitable.await(session.send("HeapProfiler.enable"), "Failed to enable heap snapshot.");
            Awaitable.await(
                    session.send("HeapProfiler.collectGarbage"),
                    "Failed to collect garbage before heap snapshot.");
            Awaitable.await(
                    session.send("HeapProfiler.takeHeapSnapshot", Map.of("reportProgress", false)),
                    "Failed to capture heap snapshot.");
        } finally {
            binding.unbind();
            Awaitable.await(session.send("HeapProfiler.disable"), "Failed to disable heap snapshot.");
        }
        writePath(path, ByteKit.toBytes(chunks.toString(), Charset.UTF_8));
    }

    /**
     * Creates PDF stream.
     *
     * @param options operation options
     * @return created PDF stream
     */
    public InputStream createPDFStream(PDFOptions options) {
        return new ByteArrayInputStream(pdf(options));
    }

    /**
     * Returns the expose function.
     *
     * @param name name to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> exposeFunction(String name) {
        return exposeFunction(name, args -> null);
    }

    /**
     * Returns the expose function.
     *
     * @param name     name to use
     * @param callback callback to invoke
     * @return completion future
     */
    public CompletableFuture<CdpPayload> exposeFunction(String name, Function<List<Object>, Object> callback) {
        String actualName = Assert.notBlank(name, "name");
        if (exposedFunctions.containsKey(actualName)) {
            throw new InternalException("Exposed function already exists: " + actualName);
        }
        Function<List<Object>, Object> actualCallback = callback == null ? args -> null : callback;
        String source = CdpRuntime.pageBindingInitString("internal", actualName);
        ExposedFunction function = new ExposedFunction(actualName, actualCallback::apply, source);
        String preloadId = frameManager.evaluateOnNewDocument(source);
        exposedFunctionNames.add(actualName);
        exposedFunctions.put(actualName, function);
        exposedFunctionPreloadIds.put(actualName, preloadId);
        return frameManager.addExposedFunction(function).thenApply(ignored -> CdpPayload.NULL);
    }

    /**
     * Removes exposed function.
     *
     * @param name name to use
     * @return remove exposed function value
     */
    public CompletableFuture<CdpPayload> removeExposedFunction(String name) {
        String actualName = Assert.notBlank(name, "name");
        ExposedFunction function = exposedFunctions.remove(actualName);
        if (function == null) {
            throw new InternalException("No exposed function found: " + actualName);
        }
        exposedFunctionNames.remove(actualName);
        String preloadId = exposedFunctionPreloadIds.remove(actualName);
        CompletableFuture<Void> removeFunction = frameManager.removeExposedFunction(function);
        CompletableFuture<Void> removePreload = StringKit.isBlank(preloadId) ? CompletableFuture.completedFuture(null)
                : frameManager.removeScriptToEvaluateOnNewDocument(preloadId);
        return CompletableFuture.allOf(removeFunction, removePreload).thenApply(ignored -> CdpPayload.NULL);
    }

    /**
     * Returns the exposed functions.
     *
     * @return values
     */
    public Set<String> exposedFunctions() {
        return Set.copyOf(exposedFunctionNames);
    }

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    public Optional<CDPSession> session() {
        return Optional.ofNullable(session);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        close(null);
    }

    /**
     * Closes this object and releases its resources.
     *
     * @param options operation options
     */
    public void close(PageCloseOptions options) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        CdpTarget actualTarget = target();
        String actualTargetId = CdpTarget.Internal.id(actualTarget);
        boolean runBeforeUnload = options != null && options.isRunBeforeUnload();
        Logger.debug(
                true,
                "Page",
                "Page close requested: targetId={}, runBeforeUnload={}",
                actualTargetId,
                runBeforeUnload);
        try {
            Binding binding = renderSecurityBoundaryBinding;
            if (binding != null) {
                binding.unbind();
                renderSecurityBoundaryBinding = null;
            }
            if (session == null) {
                CdpTarget.Internal.markClosed(actualTarget);
                return;
            }
            if (session.connection().isClosed() || session.detached()) {
                Logger.debug(
                        false,
                        "Page",
                        "Page close skipped remote command on disconnected session: targetId={}",
                        actualTargetId);
                CdpTarget.Internal.markClosed(actualTarget);
                return;
            }
            if (runBeforeUnload) {
                Awaitable.await(session.send("Page.close"), "Failed to close page.");
            } else {
                Awaitable.await(
                        session.connection().send("Target.closeTarget", Map.of("targetId", actualTargetId)),
                        "Failed to close page target.");
            }
            waitForTargetClosed(actualTarget);
        } finally {
            closeLocally(actualTarget);
        }
        Logger.debug(false, "Page", "Page close completed: targetId={}", actualTargetId);
    }

    /**
     * Waits until the page target is destroyed.
     *
     * @param target target
     */
    private void waitForTargetClosed(CdpTarget target) {
        CompletableFuture<Void> targetClosed = CdpTarget.Internal.closed(target);
        if (targetClosed.isDone()) {
            return;
        }
        Duration timeout = defaultTimeout == null ? DEFAULT_NAVIGATION_TIMEOUT : defaultTimeout;
        try {
            targetClosed.get(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for page close.", ex);
        } catch (java.util.concurrent.TimeoutException ex) {
            throw new TimeoutException("Timed out waiting for page close: " + timeout.toMillis() + "ms");
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Failed to wait for page close.",
                    ExceptionKit.unwrap(ex.getCause() == null ? ex : ex.getCause()));
        }
    }

    /**
     * Completes local page close bookkeeping.
     *
     * @param target target
     */
    private void closeLocally(CdpTarget target) {
        CdpTarget.Internal.markClosed(target);
        networkManager.dispose();
        if (browserContext != null) {
            CdpBrowserContext.Internal.removeTarget(browserContext, target);
        }
        emit(PageEvent.CLOSE, this);
    }

    /**
     * Returns the send page command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return completion future
     */
    private CompletableFuture<CdpPayload> sendPageCommand(String method, Map<String, Object> params) {
        if (session == null) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return session.send(method, params == null ? Map.of() : params);
    }

    /**
     * Handles collect frames.
     *
     * @param frame  frame instance
     * @param frames frames value
     */
    private void collectFrames(CdpFrame frame, List<CdpFrame> frames) {
        if (frame == null || frames.contains(frame)) {
            return;
        }
        frames.add(frame);
        for (CdpFrame childFrame : frame.childFrames()) {
            collectFrames(childFrame, frames);
        }
    }

    /**
     * Waits for network event.
     *
     * @param event     event type
     * @param type      type to use
     * @param predicate predicate value
     * @param timeout   timeout value
     * @return completion future
     */
    private <T> CompletableFuture<T> waitForNetworkEvent(
            String event,
            Class<T> type,
            Predicate<T> predicate,
            Duration timeout) {
        Assert.notBlank(event, "event");
        Assert.notNull(type, "type");
        Assert.notNull(predicate, "predicate");
        Duration safeTimeout = timeout == null ? defaultTimeout : timeout;
        CompletableFuture<T> result = new CompletableFuture<>();
        AtomicReference<Binding> bindingRef = new AtomicReference<>();
        bindingRef.set(networkManager.on(event, payload -> {
            if (!type.isInstance(payload)) {
                return;
            }
            T typedPayload = type.cast(payload);
            if (predicate.test(typedPayload)) {
                result.complete(typedPayload);
            }
        }));
        AtomicReference<ScheduledFuture<?>> timeoutTask = new AtomicReference<>();
        if (!safeTimeout.isZero() && !safeTimeout.isNegative()) {
            timeoutTask.set(
                    PAGE_TIMER.schedule(
                            () -> result.completeExceptionally(
                                    new TimeoutException("Timed out waiting for page network event: " + event)),
                            safeTimeout.toMillis(),
                            TimeUnit.MILLISECONDS));
        }
        result.whenComplete((value, throwable) -> {
            Binding binding = bindingRef.get();
            if (binding != null) {
                binding.unbind();
            }
            ScheduledFuture<?> currentTimeout = timeoutTask.get();
            if (currentTimeout != null) {
                currentTimeout.cancel(false);
            }
        });
        return result;
    }

    /**
     * Waits for lifecycle.
     *
     * @param watcher watcher value
     * @param message message text
     */
    private void waitForLifecycle(CdpLifecycleWatcher watcher, String message) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        watcher.waitForLifecycle().whenComplete((value, throwable) -> {
            if (throwable == null) {
                result.complete(null);
            } else {
                result.completeExceptionally(
                        ExceptionKit.unwrap(
                                (throwable instanceof ExecutionException
                                        || throwable instanceof java.util.concurrent.CompletionException)
                                        && throwable.getCause() != null ? throwable.getCause() : throwable));
            }
        });
        watcher.waitForTermination().whenComplete((value, throwable) -> {
            if (throwable == null) {
                result.completeExceptionally(new IllegalStateException("Page navigation has been terminated."));
            } else {
                result.completeExceptionally(
                        ExceptionKit.unwrap(
                                (throwable instanceof ExecutionException
                                        || throwable instanceof java.util.concurrent.CompletionException)
                                        && throwable.getCause() != null ? throwable.getCause() : throwable));
            }
        });
        Awaitable.await(result, message);
    }

    /**
     * Waits for the navigation result.
     *
     * @param watcher lifecycle watcher
     * @return navigation response future
     */
    private CompletableFuture<Response> waitForNavigationResult(CdpLifecycleWatcher watcher) {
        CompletableFuture<Response> result = new CompletableFuture<>();
        watcher.waitForTermination().whenComplete((value, throwable) -> {
            if (throwable == null) {
                result.completeExceptionally(new IllegalStateException("Page navigation has been terminated."));
            } else {
                result.completeExceptionally(
                        ExceptionKit.unwrap(
                                (throwable instanceof ExecutionException
                                        || throwable instanceof java.util.concurrent.CompletionException)
                                        && throwable.getCause() != null ? throwable.getCause() : throwable));
            }
        });
        watcher.waitForSameDocumentNavigation().thenAccept(value -> result.complete(null));
        watcher.waitForNewDocumentNavigation().thenCompose(value -> watcher.waitForLifecycle())
                .thenAccept(value -> result.complete(watcher.navigationResponse())).whenComplete((value, throwable) -> {
                    if (throwable == null) {
                        return;
                    }
                    result.completeExceptionally(
                            ExceptionKit.unwrap(
                                    (throwable instanceof ExecutionException
                                            || throwable instanceof java.util.concurrent.CompletionException)
                                            && throwable.getCause() != null ? throwable.getCause() : throwable));
                });
        return result;
    }

    /**
     * Returns the navigate history.
     *
     * @param offset  offset value
     * @param options operation options
     * @return navigate history value
     */
    private Response navigateHistory(int offset, WaitForOptions options) {
        if (session == null) {
            return null;
        }
        CdpPayload history = Awaitable
                .await(session.send("Page.getNavigationHistory"), "Failed to read page navigation history.");
        int currentIndex = history.get("currentIndex").asInt();
        List<CdpPayload> entries = history.get("entries").elements();
        int targetIndex = currentIndex + offset;
        if (targetIndex < 0 || targetIndex >= entries.size()) {
            return null;
        }
        int entryId = entries.get(targetIndex).get("id").asInt();
        CdpLifecycleWatcher watcher = new CdpLifecycleWatcher(frameManager, mainFrame(), lifecycleList(options),
                navigationTimeout(options));
        try {
            Awaitable.await(
                    session.send("Page.navigateToHistoryEntry", Map.of("entryId", entryId)),
                    "Page history navigation failed.");
            return Awaitable.await(waitForNavigationResult(watcher), "Page history navigation failed.");
        } finally {
            watcher.dispose();
        }
    }

    /**
     * Returns the lifecycle list.
     *
     * @param waitUntil wait until value
     * @return values
     */
    private List<String> lifecycleList(String... waitUntil) {
        if (waitUntil == null || waitUntil.length == 0) {
            return List.of("load");
        }
        return List.of(waitUntil);
    }

    /**
     * Returns the lifecycle list.
     *
     * @param options operation options
     * @return values
     */
    private List<String> lifecycleList(WaitForOptions options) {
        if (options == null || options.getWaitUntil() == null || options.getWaitUntil().isEmpty()) {
            return List.of("load");
        }
        return List.copyOf(options.getWaitUntil());
    }

    /**
     * Resolves navigation timeout.
     *
     * @param options options
     * @return timeout
     */
    private Duration navigationTimeout(WaitForOptions options) {
        return options == null || options.getTimeout() == null ? defaultNavigationTimeout : options.getTimeout();
    }

    /**
     * Resolves wait timeout.
     *
     * @param options options
     * @return timeout
     */
    private Duration waitTimeout(WaitForOptions options) {
        return options == null || options.getTimeout() == null ? defaultTimeout : options.getTimeout();
    }

    /**
     * Resolves lifecycle options as an array.
     *
     * @param options options
     * @return lifecycle markers
     */
    private String[] lifecycleArray(WaitForOptions options) {
        List<String> values = lifecycleList(options);
        return values.toArray(String[]::new);
    }

    /**
     * Handles initialize worker events.
     */
    private void initializeWorkerEvents() {
        if (session == null) {
            return;
        }
        session.on("Target.attachedToTarget", this::onTargetAttachedToWorker);
        session.on("Target.detachedFromTarget", this::onTargetDetachedFromWorker);
        session.send(
                "Target.setAutoAttach",
                Map.of("autoAttach", true, "waitForDebuggerOnStart", false, "flatten", true));
    }

    /**
     * Handles initialize page events.
     */
    private void initializePageEvents() {
        if (session == null) {
            return;
        }
        session.on("Page.fileChooserOpened", this::onFileChooserOpened);
        session.on("Page.screencastFrame", this::onScreencastFrame);
    }

    /**
     * Handles on file chooser opened.
     *
     * @param params protocol parameters
     */
    private void onFileChooserOpened(CdpPayload params) {
        if (fileChooserWaiters.isEmpty() || session == null) {
            return;
        }
        List<CompletableFuture<FileChooser>> waiters = List.copyOf(fileChooserWaiters);
        int backendNodeId = params.get("backendNodeId").asInt();
        boolean multiple = !"selectSingle".equals(PayloadReader.text(params.get("mode")));
        session.send("DOM.resolveNode", Map.of("backendNodeId", backendNodeId)).whenComplete((result, throwable) -> {
            if (throwable != null) {
                completeFileChooserWaiters(waiters, null, throwable);
                return;
            }
            CdpElementHandle element = new CdpElementHandle(result.get("object"), session);
            completeFileChooserWaiters(waiters, new FileChooserRequest(element, multiple), null);
        });
    }

    /**
     * Completes file chooser waiters.
     *
     * @param waiters   waiters
     * @param chooser   chooser
     * @param throwable throwable
     */
    private void completeFileChooserWaiters(
            List<CompletableFuture<FileChooser>> waiters,
            FileChooser chooser,
            Throwable throwable) {
        for (CompletableFuture<FileChooser> waiter : waiters) {
            fileChooserWaiters.remove(waiter);
            if (throwable == null) {
                waiter.complete(chooser);
            } else {
                waiter.completeExceptionally(throwable);
            }
        }
    }

    /**
     * Handles on screencast frame.
     *
     * @param params protocol parameters
     */
    private void onScreencastFrame(CdpPayload params) {
        byte[] frame = Base64.decode(PayloadReader.text(params.get("data")));
        for (CdpScreenRecorder recorder : screenRecorders) {
            if (!recorder.stopped()) {
                recorder.writeFrame(frame);
            }
        }
        CompletableFuture<Void> firstFrame = firstScreencastFrame.get();
        if (firstFrame != null && !firstFrame.isDone()) {
            firstFrame.complete(null);
        }
        if (session != null && !params.get("sessionId").isNull()) {
            session.send("Page.screencastFrameAck", Map.of("sessionId", params.get("sessionId").asInt()));
        }
    }

    /**
     * Handles on target attached to worker.
     *
     * @param params protocol parameters
     */
    private void onTargetAttachedToWorker(CdpPayload params) {
        TargetInfo targetInfo = TargetInfo.fromAttachedToTarget(params);
        if (!isWorkerType(targetInfo.getType())) {
            return;
        }
        CDPSession workerSession = session.connection().getSession(targetInfo.getSessionId())
                .orElseGet(() -> session.connection().createSession(targetInfo));
        CdpWorker worker = new CdpWorker(targetInfo.getUrl(), workerSession, targetInfo.getTargetId(),
                targetInfo.getType());
        worker.setCloseCallback(() -> removeWorker(worker));
        workers.put(targetInfo.getSessionId(), worker);
        workerEmitter.emit(WORKER_CREATED, worker);
    }

    /**
     * Handles on target detached from worker.
     *
     * @param params protocol parameters
     */
    private void onTargetDetachedFromWorker(CdpPayload params) {
        String sessionId = PayloadReader.text(params.get("sessionId"));
        Worker worker = workers.get(sessionId);
        if (worker != null) {
            removeWorker(worker);
        }
    }

    /**
     * Returns whether worker type is enabled.
     *
     * @param type type to use
     * @return {@code true} when the condition matches
     */
    private boolean isWorkerType(String type) {
        return "worker".equals(type) || "service_worker".equals(type) || "shared_worker".equals(type);
    }

    /**
     * Returns the dev tools target ID.
     *
     * @return dev tools target ID value
     */
    private String devToolsTargetId() {
        if (session == null) {
            return Normal.EMPTY;
        }
        CompletableFuture<CdpPayload> future;
        try {
            future = session.connection().send("Target.getDevToolsTarget", Map.of("targetId", targetId()));
        } catch (RuntimeException ignored) {
            return Normal.EMPTY;
        }
        CdpPayload result = Awaitable.await(future, "Failed to read DevTools state.");
        return PayloadReader.text(result.get("targetId"));
    }

    /**
     * Creates dev tools page.
     *
     * @param devToolsTargetId dev tools target ID value
     * @return create dev tools page value
     */
    private Page createDevToolsPage(String devToolsTargetId) {
        CdpPayload attached = Awaitable.await(
                session.connection()
                        .send("Target.attachToTarget", Map.of("targetId", devToolsTargetId, "flatten", true)),
                "Failed to bind DevTools target.");
        String sessionId = PayloadReader.text(attached.get("sessionId"));
        if (StringKit.isBlank(sessionId)) {
            sessionId = "devtools-" + devToolsTargetId;
        }
        String finalSessionId = sessionId;
        CDPSession devToolsSession = session.connection().getSession(finalSessionId).orElseGet(
                () -> session.connection().createSession(
                        new TargetInfo(devToolsTargetId, "page", "devtools://devtools", finalSessionId)));
        return new CdpPage(devToolsSession);
    }

    /**
     * Resolves the screencast width.
     *
     * @return screencast width value
     */
    private int screencastWidth() {
        return currentViewport == null ? 800 : Math.max(1, currentViewport.getWidth());
    }

    /**
     * Resolves the screencast height.
     *
     * @return screencast height value
     */
    private int screencastHeight() {
        return currentViewport == null ? 600 : Math.max(1, currentViewport.getHeight());
    }

    /**
     * Merges screenshot options with defaults.
     *
     * @param options options
     * @return merge screenshot options value
     */
    private ScreenshotOptions mergeScreenshotOptions(ScreenshotOptions options) {
        ScreenshotOptions merged = PageDefaults.defaultScreenshotOptions();
        if (options == null) {
            return merged;
        }
        if (options.getType() != null) {
            merged.setType(options.getType());
        }
        if (options.getQuality() != null) {
            merged.setQuality(options.getQuality());
        }
        if (options.getClip() != null) {
            merged.setClip(new LinkedHashMap<>(options.getClip()));
        }
        if (options.getPath() != null) {
            merged.setPath(options.getPath());
        }
        if (options.getFromSurface() != null) {
            merged.setFromSurface(options.getFromSurface());
        }
        if (options.getEncoding() != null) {
            merged.setEncoding(options.getEncoding());
        }
        if (options.getCaptureBeyondViewport() != null) {
            merged.setCaptureBeyondViewport(options.getCaptureBeyondViewport());
        }
        merged.setFullPage(merged.isFullPage() || options.isFullPage());
        merged.setScrollIntoView(options.isScrollIntoView());
        merged.setOmitBackground(merged.isOmitBackground() || options.isOmitBackground());
        merged.setOptimizeForSpeed(merged.isOptimizeForSpeed() || options.isOptimizeForSpeed());
        return merged;
    }

    /**
     * Handles prepare full page clip.
     *
     * @param options operation options
     */
    private void prepareFullPageClip(ScreenshotOptions options) {
        if (!options.isFullPage() || options.getClip() != null || session == null) {
            return;
        }
        CdpPayload metrics = Awaitable.await(session.send("Page.getLayoutMetrics"), "Failed to read page layout.");
        CdpPayload contentSize = metrics.get("contentSize");
        options.setClip(
                Map.of(
                        "x",
                        PayloadReader.decimal(contentSize.get("x")),
                        "y",
                        PayloadReader.decimal(contentSize.get("y")),
                        "width",
                        PayloadReader.decimal(contentSize.get("width")),
                        "height",
                        PayloadReader.decimal(contentSize.get("height")),
                        "scale",
                        1));
    }

    /**
     * Prepares viewport state for a screenshot.
     *
     * @param options options
     * @return whether viewport should be restored
     */
    private boolean prepareScreenshotViewport(ScreenshotOptions options) {
        prepareFullPageClip(options);
        if (!options.isFullPage() || !Boolean.FALSE.equals(options.getCaptureBeyondViewport())
                || options.getClip() == null) {
            return false;
        }
        Viewport viewport = currentViewport == null ? new Viewport() : currentViewport.copy();
        viewport.setWidth(Math.max(1, (int) Math.ceil(number(options.getClip(), "width"))));
        viewport.setHeight(Math.max(1, (int) Math.ceil(number(options.getClip(), "height"))));
        Awaitable.await(setViewport(viewport), "Failed to adjust viewport for full-page screenshot.");
        return true;
    }

    /**
     * Clips a screenshot region to the visual viewport.
     *
     * @param options options
     */
    private void clipToVisualViewport(ScreenshotOptions options) {
        if (options.getClip() == null || session == null) {
            return;
        }
        CdpPayload metrics = Awaitable.await(session.send("Page.getLayoutMetrics"), "Failed to read page layout.");
        CdpPayload viewport = metrics.get("cssVisualViewport");
        if (viewport.isNull()) {
            viewport = metrics.get("visualViewport");
        }
        double viewportX = metricNumber(viewport, "pageX", metricNumber(viewport, "offsetX", 0));
        double viewportY = metricNumber(viewport, "pageY", metricNumber(viewport, "offsetY", 0));
        double viewportWidth = metricNumber(
                viewport,
                "clientWidth",
                metricNumber(viewport, "width", screencastWidth()));
        double viewportHeight = metricNumber(
                viewport,
                "clientHeight",
                metricNumber(viewport, "height", screencastHeight()));
        double x = number(options.getClip(), "x");
        double y = number(options.getClip(), "y");
        double width = number(options.getClip(), "width");
        double height = number(options.getClip(), "height");
        double clippedX = Math.max(x, viewportX);
        double clippedY = Math.max(y, viewportY);
        double clippedRight = Math.min(x + width, viewportX + viewportWidth);
        double clippedBottom = Math.min(y + height, viewportY + viewportHeight);
        if (clippedRight <= clippedX || clippedBottom <= clippedY) {
            throw new InternalException("Screenshot clip is outside the visual viewport.");
        }
        Map<String, Object> clip = new LinkedHashMap<>(options.getClip());
        clip.put("x", clippedX);
        clip.put("y", clippedY);
        clip.put("width", clippedRight - clippedX);
        clip.put("height", clippedBottom - clippedY);
        clip.putIfAbsent("scale", 1);
        options.setClip(clip);
    }

    /**
     * Reads a numeric value from a map.
     *
     * @param values values
     * @param key    key
     * @return number
     */
    private double number(Map<String, Object> values, String key) {
        return Convert.toDouble(values.get(key), 0D);
    }

    /**
     * Reads a numeric value from payload.
     *
     * @param payload  payload
     * @param key      key
     * @param fallback fallback
     * @return number
     */
    private double metricNumber(CdpPayload payload, String key, double fallback) {
        if (payload == null || payload.isNull() || payload.get(key).isNull()) {
            return fallback;
        }
        return PayloadReader.decimal(payload.get(key));
    }

    /**
     * Updates transparent background.
     *
     * @param enabled whether the feature should be enabled
     */
    private void setTransparentBackground(boolean enabled) {
        if (enabled) {
            session.send(
                    "Emulation.setDefaultBackgroundColorOverride",
                    Map.of("color", Map.of("r", 0, "g", 0, "b", 0, "a", 0)));
        } else {
            session.send("Emulation.setDefaultBackgroundColorOverride");
        }
    }

    /**
     * Handles write path.
     *
     * @param path  file path
     * @param bytes bytes value
     */
    private void writePath(Path path, byte[] bytes) {
        if (path == null) {
            return;
        }
        try {
            FileKit.writeBytes(bytes, path.toFile());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to write output file: " + path, ex);
        }
    }

    /**
     * Handles sleep poll interval.
     */
    private void sleepPollInterval() {
        if (!ThreadKit.sleep(WAIT_POLL_INTERVAL.toMillis())) {
            throw new InternalException("Page wait was interrupted.");
        }
    }

    /**
     * Installs the render request security boundary.
     */
    private void installRenderSecurityBoundary() {
        if (!renderSecurityBoundaryEnabled || session == null
                || !renderSecurityBoundaryInstalled.compareAndSet(false, true)) {
            return;
        }
        Binding binding = onRequest(request -> {
            if (!(request instanceof CdpRequest cdpRequest) || request.isInterceptResolutionHandled()) {
                return;
            }
            cdpRequest.enqueueInterceptAction(() -> {
                try {
                    validateRenderRequestUrl(request.url());
                    return cdpRequest.continueRequest(Map.of(), -1000);
                } catch (RuntimeException ex) {
                    Logger.warn(
                            false,
                            "Network",
                            ex,
                            "Render request blocked: url={}",
                            securityPolicy.sanitizeUrl(request.url()));
                    return cdpRequest.abort("blockedbyclient", 1000);
                }
            });
        });
        try {
            Awaitable.await(setRequestInterception(true), "Failed to enable CDP render request boundary.");
            renderSecurityBoundaryBinding = binding;
        } catch (RuntimeException ex) {
            binding.unbind();
            renderSecurityBoundaryInstalled.set(false);
            throw ex;
        }
    }

    /**
     * Validates a navigation URL before it reaches the browser.
     *
     * @param url target URL
     */
    private void validateNavigationUrl(String url) {
        securityPolicy.validateNavigationUrl(URI.create(Assert.notBlank(url, "url")));
    }

    /**
     * Validates a render request URL before it reaches the browser.
     *
     * @param url target URL
     */
    private void validateRenderRequestUrl(String url) {
        securityPolicy.validateRenderRequestUrl(URI.create(Assert.notBlank(url, "url")));
    }

    /**
     * Creates a local script tag snippet.
     *
     * @param options options
     * @return snippet
     */
    private String localScriptSnippet(ScriptTagOptions options) {
        TagInjection.validateTagSources("script", options.getContent(), options.getUrl(), options.getPath());
        if (StringKit.isNotBlank(options.getUrl())) {
            return "<script src=\"" + safeAttribute(options.getUrl()) + "\"></script>";
        }
        String content = options.getContent();
        if (StringKit.isNotBlank(options.getPath())) {
            content = TagInjection.readFile(options.getPath(), "script") + "\n//# sourceURL="
                    + Scripts.sourceUrl(options.getPath());
        }
        return "<script>" + safeText(content) + "</script>";
    }

    /**
     * Creates a local style tag snippet.
     *
     * @param options options
     * @return snippet
     */
    private String localStyleSnippet(StyleTagOptions options) {
        TagInjection.validateTagSources("style", options.getContent(), options.getUrl(), options.getPath());
        if (StringKit.isNotBlank(options.getUrl())) {
            return "<link rel=\"stylesheet\" href=\"" + safeAttribute(options.getUrl()) + "\">";
        }
        String content = options.getContent();
        if (StringKit.isNotBlank(options.getPath())) {
            content = TagInjection.readFile(options.getPath(), "style");
        }
        return "<style>" + safeText(content) + "</style>";
    }

    /**
     * Creates a local element handle.
     *
     * @param prefix prefix
     * @return element handle
     */
    private CdpElementHandle localElementHandle(String prefix) {
        return new CdpElementHandle(CdpPayload.of(
                Map.of(
                        "type",
                        "object",
                        "subtype",
                        "node",
                        "objectId",
                        prefix + localTagHandleCount.incrementAndGet())));
    }

    /**
     * Returns the safe text.
     *
     * @param value to use
     * @return safe text value
     */
    private String safeText(String value) {
        return value == null ? Normal.EMPTY : value;
    }

    /**
     * Returns the safe attribute.
     *
     * @param value to use
     * @return safe attribute value
     */
    private String safeAttribute(String value) {
        return safeText(value).replace("&", "&amp;").replace("\"", "&quot;");
    }

    /**
     * Returns the inject before end.
     *
     * @param html    HTML content
     * @param tag     tag value
     * @param snippet snippet value
     * @return inject before end value
     */
    private String injectBeforeEnd(String html, String tag, String snippet) {
        String safeHtml = html == null ? Normal.EMPTY : html;
        String endTag = "</" + tag + Symbol.GT;
        int index = safeHtml.toLowerCase(java.util.Locale.ROOT).lastIndexOf(endTag);
        if (index < 0) {
            return safeHtml + snippet;
        }
        return safeHtml.substring(0, index) + snippet + safeHtml.substring(index);
    }

    /**
     * Returns the extract title.
     *
     * @param html HTML content
     * @return extract title value
     */
    private String extractTitle(String html) {
        if (StringKit.isBlank(html)) {
            return Normal.EMPTY;
        }
        String lower = html.toLowerCase(java.util.Locale.ROOT);
        int start = lower.indexOf("<title>");
        int end = lower.indexOf("</title>");
        if (start < 0 || end <= start) {
            return Normal.EMPTY;
        }
        return html.substring(start + "<title>".length(), end).trim();
    }

    /**
     * Provides internal page collaboration without creating a standalone helper type.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Internal {

        /**
         * Creates no Internal instance.
         */
        private Internal() {
            // No initialization required.
        }

        /**
         * Binds a browser context to a CDP page.
         *
         * @param page    page
         * @param context browser context
         */
        public static void bindBrowserContext(Page page, CdpBrowserContext context) {
            cdp(page).bindBrowserContext(context);
        }

        /**
         * Binds a target to a CDP page.
         *
         * @param page   page
         * @param target target
         */
        public static void bindTarget(Page page, Target target) {
            cdp(page).bindTarget(target);
        }

        /**
         * Returns the target id for a page.
         *
         * @param page page
         * @return target id
         */
        public static String targetId(Page page) {
            return CdpTarget.Internal.id(Assert.notNull(page, "page").target());
        }

        /**
         * Waits for navigation through the CDP page implementation.
         *
         * @param page      page
         * @param timeout   timeout
         * @param waitUntil lifecycle markers
         * @return navigation future
         */
        public static CompletableFuture<Response> waitForNavigationAsync(
                Page page,
                Duration timeout,
                String... waitUntil) {
            return cdp(page).waitForNavigationAsync(timeout, waitUntil);
        }

        /**
         * Navigates a frame through the owning CDP page implementation.
         *
         * @param page    page
         * @param frame   frame
         * @param url     url
         * @param options options
         * @return main resource response
         */
        public static Response goToFrame(Page page, CdpFrame frame, String url, GoToOptions options) {
            return cdp(page).goToFrame(frame, url, options);
        }

        /**
         * Validates a render request URL through the owning CDP page implementation.
         *
         * @param page page
         * @param url  target URL
         */
        public static void validateRenderRequestUrl(Page page, String url) {
            cdp(page).validateRenderRequestUrl(url);
        }

        /**
         * Casts a page contract to the CDP page implementation.
         *
         * @param page page
         * @return CDP page
         */
        private static CdpPage cdp(Page page) {
            if (page instanceof CdpPage cdpPage) {
                return cdpPage;
            }
            throw new InternalException("Unsupported page implementation: " + Assert.notNull(page, "page").getClass());
        }
    }

}
