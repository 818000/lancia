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
package org.miaixz.lancia.kernel.bidi.browser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.IoKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Extension;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.events.BrowserEvent;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.page.BidiPage;
import org.miaixz.lancia.kernel.bidi.session.BidiConnection;
import org.miaixz.lancia.kernel.bidi.session.BidiProtocolSession;
import org.miaixz.lancia.kernel.bidi.target.BidiTarget;
import org.miaixz.lancia.kernel.bidi.worker.BidiWorker;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.nimble.browser.WindowBounds;
import org.miaixz.lancia.nimble.browser.WindowState;
import org.miaixz.lancia.nimble.network.Cookie;
import org.miaixz.lancia.nimble.network.CookieParam;
import org.miaixz.lancia.nimble.network.DeleteCookiesParameters;
import org.miaixz.lancia.nimble.screen.AddScreenParams;
import org.miaixz.lancia.nimble.screen.ScreenInfo;
import org.miaixz.lancia.options.BrowserContextOptions;
import org.miaixz.lancia.options.CreatePageOptions;
import org.miaixz.lancia.options.PermissionOptions;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.page.PageExtension;
import org.miaixz.lancia.shared.payload.PayloadExtensionInfo;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.payload.PayloadScreenInfo;

/**
 * Coordinates bidi browser operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiBrowser implements Browser {

    /**
     * Shared constant for protocol.
     */
    public static final String PROTOCOL = "webDriverBiDi";
    /**
     * Default public API wait timeout.
     */
    private static final long TIMEOUT_MILLIS = 5_000L;
    /**
     * Default events.
     */
    private static final List<String> DEFAULT_EVENTS = List.of("browsingContext", "network", "log", "script", "input");
    /**
     * Default CDP events.
     */
    private static final List<String> DEFAULT_CDP_EVENTS = List.of(
            "goog:cdp.Debugger.scriptParsed",
            "goog:cdp.CSS.styleSheetAdded",
            "goog:cdp.Runtime.executionContextsCleared",
            "goog:cdp.Tracing.tracingComplete",
            "goog:cdp.Network.requestWillBeSent",
            "goog:cdp.Page.screencastFrame");
    /**
     * Current session.
     */
    private final EventEmitter<BrowserEvent> emitter = new EventEmitter<>();
    /**
     * Current session.
     */
    private final BidiSession session;
    /**
     * Current accessor.
     */
    private final org.miaixz.lancia.kernel.bidi.accessor.BidiBrowser accessor;
    /**
     * Current process.
     */
    private Process process;
    /**
     * Current CDP connection.
     */
    private Connection cdpConnection;
    /**
     * Current close hook.
     */
    private AutoCloseable closeHook;
    /**
     * Registered contexts values.
     */
    private final List<BidiBrowserContext> contexts = new ArrayList<>();
    /**
     * Current default context.
     */
    private final BidiBrowserContext defaultContext;
    /**
     * Mapped extensions values.
     */
    private final Map<String, PageExtension> extensions = new LinkedHashMap<>();
    /**
     * Mapped window bounds values.
     */
    private final Map<String, WindowBounds> windowBounds = new LinkedHashMap<>();
    /**
     * Registered local screens values.
     */
    private final List<ScreenInfo> localScreens = new ArrayList<>();
    /**
     * Thread-safe screen index state.
     */
    private final AtomicInteger screenIndex = new AtomicInteger();
    /**
     * Thread-safe connected state.
     */
    private final AtomicBoolean connected = new AtomicBoolean(true);
    /**
     * Whether network is enabled.
     */
    private boolean networkEnabled = true;
    /**
     * Whether issues is enabled.
     */
    private boolean issuesEnabled = true;

    /**
     * Creates a bidi browser.
     *
     * @param session protocol session
     */
    public BidiBrowser(BidiProtocolSession session) {
        this(BidiSession.wrap(session));
    }

    /**
     * Creates a bidi browser.
     *
     * @param session protocol session
     */
    public BidiBrowser(BidiSession session) {
        this(session, null, null, null, true, true);
    }

    /**
     * Creates a BidiBrowser instance with runtime-owned resources.
     *
     * @param session        session
     * @param process        process
     * @param cdpConnection  CDP connection
     * @param closeHook      close hook
     * @param networkEnabled network enabled
     * @param issuesEnabled  issues enabled
     */
    public BidiBrowser(BidiSession session, Process process, Connection cdpConnection, AutoCloseable closeHook,
            boolean networkEnabled, boolean issuesEnabled) {
        this.session = session;
        this.accessor = session.browser();
        this.process = process;
        this.cdpConnection = cdpConnection;
        this.closeHook = closeHook;
        this.networkEnabled = networkEnabled;
        this.issuesEnabled = issuesEnabled;
        this.defaultContext = new BidiBrowserContext(this, Normal.DEFAULT, true);
        this.contexts.add(defaultContext);
        Logger.debug(false, "Browser", "BiDi browser initialized: contexts={}, protocol={}", contexts.size(), PROTOCOL);
    }

    /**
     * Creates a BiDi browser from an established protocol connection.
     *
     * @param connection protocol connection
     * @return completion future
     */
    static CompletableFuture<BidiBrowser> connect(BidiConnection connection) {
        Logger.debug(true, "Browser", "BiDi browser connect requested: cdpEvents=false");
        return BidiSession.from(
                org.miaixz.lancia.kernel.bidi.accessor.BidiConnection.from(connection),
                Map.of("alwaysMatch", Map.of("webSocketUrl", true))).thenCompose(session -> {
                    BidiBrowser browser = new BidiBrowser(session);
                    return session.subscribe(DEFAULT_EVENTS).thenApply(value -> {
                        Logger.debug(false, "Browser", "BiDi browser connected: events={}", DEFAULT_EVENTS.size());
                        return browser;
                    });
                }).whenComplete((browser, throwable) -> {
                    if (throwable != null) {
                        Logger.error(false, "Browser", throwable, "BiDi browser connect failed: cdpEvents=false");
                    }
                });
    }

    /**
     * Returns the connect with CDP events.
     *
     * @param connection protocol connection
     * @return completion future
     */
    static CompletableFuture<BidiBrowser> connectWithCdpEvents(BidiConnection connection) {
        Logger.debug(true, "Browser", "BiDi browser connect requested: cdpEvents=true");
        return BidiSession.from(
                org.miaixz.lancia.kernel.bidi.accessor.BidiConnection.from(connection),
                Map.of("alwaysMatch", Map.of("webSocketUrl", true))).thenCompose(session -> {
                    BidiBrowser browser = new BidiBrowser(session);
                    List<String> events = new ArrayList<>(DEFAULT_EVENTS);
                    events.addAll(DEFAULT_CDP_EVENTS);
                    return session.subscribe(events).thenApply(value -> {
                        Logger.debug(false, "Browser", "BiDi browser connected: events={}", events.size());
                        return browser;
                    });
                }).whenComplete((browser, throwable) -> {
                    if (throwable != null) {
                        Logger.error(false, "Browser", throwable, "BiDi browser connect failed: cdpEvents=true");
                    }
                });
    }

    /**
     * Returns the new page.
     *
     * @return new page value
     */
    public BidiPage newPage() {
        return defaultContext.newPage();
    }

    /**
     * Creates a new page.
     *
     * @param options page creation options
     * @return page instance
     */
    public BidiPage newPage(CreatePageOptions options) {
        return newPage();
    }

    /**
     * Creates browser context.
     *
     * @return created browser context
     */
    public BidiBrowserContext createBrowserContext() {
        Logger.debug(true, "Browser", "BiDi browser context create requested: existing={}", contexts.size());
        try {
            String userContext = session.send("browser.createUserContext", Map.of()).get(5, TimeUnit.SECONDS)
                    .get("userContext").asText();
            BidiBrowserContext context = new BidiBrowserContext(this, userContext, false);
            contexts.add(context);
            Logger.debug(
                    false,
                    "Browser",
                    "BiDi browser context created: contextId={}, total={}",
                    userContext,
                    contexts.size());
            return context;
        } catch (Exception ex) {
            BidiBrowserContext context = new BidiBrowserContext(this, "context-" + contexts.size(), false);
            contexts.add(context);
            Logger.warn(
                    false,
                    "Browser",
                    ex,
                    "BiDi browser context create fell back to local context: contextId={}",
                    context.id());
            return context;
        }
    }

    /**
     * Returns the default browser context.
     *
     * @return default browser context value
     */
    public BidiBrowserContext defaultBrowserContext() {
        return defaultContext;
    }

    /**
     * Returns the browser contexts.
     *
     * @return values
     */
    public List<BidiBrowserContext> browserContexts() {
        return List.copyOf(contexts);
    }

    /**
     * Handles remove browser context.
     *
     * @param context browser context
     */
    synchronized void removeBrowserContext(BidiBrowserContext context) {
        contexts.remove(context);
        Logger.debug(
                false,
                "Browser",
                "BiDi browser context removed: contextId={}, remaining={}",
                context == null ? Normal.EMPTY : context.id(),
                contexts.size());
    }

    /**
     * Returns the pages.
     *
     * @return values
     */
    List<BidiPage> bidiPages() {
        List<BidiPage> pages = new ArrayList<>();
        for (BidiBrowserContext context : contexts) {
            pages.addAll(context.bidiPages());
        }
        return List.copyOf(pages);
    }

    /**
     * Returns whether connected is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isConnected() {
        return connected.get() && !session.connection().closed();
    }

    /**
     * Returns the connected.
     *
     * @return {@code true} when the condition matches
     */
    public boolean connected() {
        return isConnected();
    }

    /**
     * Returns the protocol.
     *
     * @return protocol value
     */
    public String protocol() {
        return PROTOCOL;
    }

    /**
     * Returns the ws endpoint.
     *
     * @return ws endpoint value
     */
    public String wsEndpoint() {
        return session.connection().url();
    }

    /**
     * Returns the protocol connection.
     *
     * @return protocol connection
     */
    public BidiConnection connection() {
        return session.connection().unwrap();
    }

    /**
     * Returns the process.
     *
     * @return process value
     */
    public Process process() {
        return process;
    }

    /**
     * Returns the CDP supported.
     *
     * @return {@code true} when the condition matches
     */
    public boolean cdpSupported() {
        return cdpConnection != null;
    }

    /**
     * Returns the CDP connection.
     *
     * @return optional value
     */
    public Optional<Connection> cdpConnection() {
        return Optional.ofNullable(cdpConnection);
    }

    /**
     * Returns the install extension.
     *
     * @param path file path
     * @return completion future
     */
    public CompletableFuture<String> installExtension(String path) {
        String actualPath = Assert.notBlank(path, "path");
        Logger.debug(
                true,
                "Browser",
                "BiDi extension install requested: path={}, cdp={}",
                actualPath,
                hasCdpTransport());
        if (hasCdpTransport()) {
            return guard(
                    cdpConnection.send("Extensions.loadUnpacked", Map.of("path", actualPath)).thenApply(payload -> {
                        PayloadExtensionInfo info = PayloadExtensionInfo.fromInstallResult(payload, actualPath);
                        registerExtension(info);
                        Logger.debug(false, "Browser", "BiDi extension installed: id={}, cdp=true", info.id());
                        return info.id();
                    }),
                    "BiDi browser extension installation failed.");
        }
        return guard(accessor.installExtension(actualPath).thenApply(id -> {
            PayloadExtensionInfo info = installedPayloadExtensionInfo(id, actualPath);
            registerExtension(info);
            Logger.debug(false, "Browser", "BiDi extension installed: id={}, cdp=false", info.id());
            return info.id();
        }), "BiDi browser extension installation failed.");
    }

    /**
     * Returns the uninstall extension.
     *
     * @param id identifier
     * @return completion future
     */
    public CompletableFuture<Void> uninstallExtension(String id) {
        String actualId = Assert.notBlank(id, "id");
        Logger.debug(true, "Browser", "BiDi extension uninstall requested: id={}, cdp={}", actualId, hasCdpTransport());
        if (hasCdpTransport()) {
            return guard(cdpConnection.send("Extensions.uninstall", Map.of("id", actualId)).thenApply(payload -> {
                extensions.remove(actualId);
                Logger.debug(false, "Browser", "BiDi extension uninstalled: id={}, cdp=true", actualId);
                return null;
            }), "BiDi browser extension uninstall failed.");
        }
        return guard(accessor.uninstallExtension(actualId).thenApply(value -> {
            extensions.remove(actualId);
            Logger.debug(false, "Browser", "BiDi extension uninstalled: id={}, cdp=false", actualId);
            return null;
        }), "BiDi browser extension uninstall failed.");
    }

    /**
     * Returns the extensions.
     *
     * @return completion future
     */
    public synchronized CompletableFuture<Map<String, PageExtension>> extensionsAsync() {
        if (hasCdpTransport()) {
            return guard(cdpConnection.send("Extensions.getExtensions").thenApply(payload -> {
                Map<String, PageExtension> refreshed = new LinkedHashMap<>();
                for (CdpPayload item : payload.get("extensions").elements()) {
                    PayloadExtensionInfo info = PayloadExtensionInfo.fromProtocol(item);
                    refreshed.put(
                            info.id(),
                            extensions.getOrDefault(info.id(), new PageExtension(info, null, cdpConnection)));
                }
                extensions.clear();
                extensions.putAll(refreshed);
                return Map.copyOf(extensions);
            }), "BiDi browser extension listing failed.");
        }
        return guard(accessor.listExtensions().thenApply(infos -> {
            Map<String, PageExtension> refreshed = new LinkedHashMap<>();
            for (CdpPayload item : infos) {
                PayloadExtensionInfo info = PayloadExtensionInfo.fromProtocol(item);
                refreshed.put(info.id(), extensions.getOrDefault(info.id(), new PageExtension(info, null, null)));
            }
            extensions.clear();
            extensions.putAll(refreshed);
            return Map.copyOf(extensions);
        }), "BiDi browser extension listing failed.");
    }

    /**
     * Returns the screens.
     *
     * @return completion future
     */
    public synchronized CompletableFuture<List<ScreenInfo>> screensAsync() {
        Logger.debug(
                true,
                "Browser",
                "BiDi screen listing requested: cdp={}, cached={}",
                hasCdpTransport(),
                localScreens.size());
        if (!hasCdpTransport()) {
            return CompletableFuture.completedFuture(List.copyOf(localScreens));
        }
        return guard(cdpConnection.send("Emulation.getScreenInfos").thenApply(payload -> {
            List<ScreenInfo> screens = new ArrayList<>();
            for (CdpPayload item : payload.get("screenInfos").elements()) {
                screens.add(PayloadScreenInfo.from(item));
            }
            localScreens.clear();
            localScreens.addAll(screens);
            Logger.debug(false, "Browser", "BiDi screen listing completed: count={}", localScreens.size());
            return List.copyOf(localScreens);
        }), "BiDi browser screen listing failed.");
    }

    /**
     * Adds screen.
     *
     * @param params protocol parameters
     * @return add screen value
     */
    public synchronized CompletableFuture<ScreenInfo> addScreenAsync(AddScreenParams params) {
        AddScreenParams actualParams = Assert.notNull(params, "params");
        Logger.debug(true, "Browser", "BiDi screen add requested: cdp={}", hasCdpTransport());
        if (!hasCdpTransport()) {
            return CompletableFuture.completedFuture(addLocalScreen(actualParams));
        }
        return guard(cdpConnection.send("Emulation.addScreen", actualParams.toMap()).thenApply(payload -> {
            ScreenInfo screen = PayloadScreenInfo.from(payload.get("screenInfo"));
            putLocalScreen(screen);
            Logger.debug(false, "Browser", "BiDi screen added: screenId={}", screen.id());
            return screen;
        }), "BiDi browser add screen failed.");
    }

    /**
     * Removes screen.
     *
     * @param screenId screen id
     * @return remove screen value
     */
    public synchronized CompletableFuture<Void> removeScreenAsync(String screenId) {
        String actualScreenId = Assert.notBlank(screenId, "screenId");
        Logger.debug(
                true,
                "Browser",
                "BiDi screen remove requested: screenId={}, cdp={}",
                actualScreenId,
                hasCdpTransport());
        if (!hasCdpTransport()) {
            removeLocalScreen(actualScreenId);
            Logger.debug(false, "Browser", "BiDi screen removed locally: screenId={}", actualScreenId);
            return CompletableFuture.completedFuture(null);
        }
        return guard(
                cdpConnection.send("Emulation.removeScreen", Map.of("screenId", actualScreenId)).thenApply(payload -> {
                    removeLocalScreen(actualScreenId);
                    Logger.debug(false, "Browser", "BiDi screen removed: screenId={}", actualScreenId);
                    return null;
                }),
                "BiDi browser remove screen failed.");
    }

    /**
     * Returns the window bounds.
     *
     * @param windowId window id
     * @return window bounds
     */
    public synchronized CompletableFuture<WindowBounds> getWindowBoundsAsync(String windowId) {
        String actualWindowId = Assert.notBlank(windowId, "windowId");
        Logger.debug(
                true,
                "Browser",
                "BiDi window bounds read requested: windowId={}, cdp={}",
                actualWindowId,
                hasCdpTransport());
        if (hasCdpTransport()) {
            return guard(
                    cdpConnection.send("Browser.getWindowBounds", Map.of("windowId", windowIdentifier(actualWindowId)))
                            .thenApply(payload -> {
                                WindowBounds bounds = WindowBounds.from(payload.get("bounds"));
                                windowBounds.put(actualWindowId, bounds);
                                Logger.debug(
                                        false,
                                        "Browser",
                                        "BiDi window bounds read completed: windowId={}",
                                        actualWindowId);
                                return bounds;
                            }),
                    "BiDi browser get window bounds failed.");
        }
        return guard(accessor.getClientWindowInfo(actualWindowId).thenApply(payload -> {
            WindowBounds bounds = windowBoundsFromBidi(payload);
            windowBounds.put(actualWindowId, bounds);
            Logger.debug(false, "Browser", "BiDi window bounds read completed: windowId={}", actualWindowId);
            return bounds;
        }), "BiDi browser get window bounds failed.");
    }

    /**
     * Updates window bounds.
     *
     * @param windowId window id
     * @param bounds   bounds
     * @return set window bounds value
     */
    public synchronized CompletableFuture<Void> setWindowBoundsAsync(String windowId, WindowBounds bounds) {
        String actualWindowId = Assert.notBlank(windowId, "windowId");
        WindowBounds actualBounds = Assert.notNull(bounds, "bounds");
        windowBounds.put(actualWindowId, actualBounds);
        Logger.debug(
                true,
                "Browser",
                "BiDi window bounds update requested: windowId={}, cdp={}",
                actualWindowId,
                hasCdpTransport());
        if (hasCdpTransport()) {
            return guard(
                    cdpConnection.send(
                            "Browser.setWindowBounds",
                            Map.of("windowId", windowIdentifier(actualWindowId), "bounds", actualBounds.toMap()))
                            .thenApply(payload -> {
                                Logger.debug(
                                        false,
                                        "Browser",
                                        "BiDi window bounds update completed: windowId={}, cdp=true",
                                        actualWindowId);
                                return null;
                            }),
                    "BiDi browser set window bounds failed.");
        }
        return guard(
                accessor.setClientWindowState(toBidiWindowParams(actualWindowId, actualBounds)).thenApply(value -> {
                    Logger.debug(
                            false,
                            "Browser",
                            "BiDi window bounds update completed: windowId={}, cdp=false",
                            actualWindowId);
                    return null;
                }),
                "BiDi browser set window bounds failed.");
    }

    /**
     * Creates browser context.
     *
     * @param options context options
     * @return created browser context
     */
    public BidiBrowserContext createBrowserContext(BrowserContextOptions options) {
        return createBrowserContext();
    }

    /**
     * Returns the pages.
     *
     * @return values
     */
    public java.util.List<Page> pages() {
        return bidiPages().stream().map(page -> (Page) page).toList();
    }

    /**
     * Installs an extension.
     *
     * @param path extension path
     * @return extension id future
     */
    public CompletableFuture<String> installExtension(Path path) {
        return installExtension(path == null ? null : path.toString());
    }

    /**
     * Returns installed extensions.
     *
     * @return extension map
     */
    public synchronized java.util.Map<String, ? extends Extension> extensions() {
        return Awaitable.await(extensionsAsync(), "BiDi browser extension listing failed.", TIMEOUT_MILLIS);
    }

    /**
     * Returns emulated screens.
     *
     * @return screen list
     */
    public synchronized java.util.List<ScreenInfo> screens() {
        return Awaitable.await(screensAsync(), "BiDi browser screen listing failed.", TIMEOUT_MILLIS);
    }

    /**
     * Adds an emulated screen.
     *
     * @param params screen parameters
     * @return screen info
     */
    public synchronized ScreenInfo addScreen(AddScreenParams params) {
        return Awaitable.await(addScreenAsync(params), "BiDi browser add screen failed.", TIMEOUT_MILLIS);
    }

    /**
     * Removes an emulated screen.
     *
     * @param screenId screen id
     */
    public synchronized void removeScreen(String screenId) {
        Awaitable.await(removeScreenAsync(screenId), "BiDi browser remove screen failed.", TIMEOUT_MILLIS);
    }

    /**
     * Returns window bounds.
     *
     * @param windowId window id
     * @return window bounds
     */
    public synchronized WindowBounds getWindowBounds(String windowId) {
        return Awaitable
                .await(getWindowBoundsAsync(windowId), "BiDi browser get window bounds failed.", TIMEOUT_MILLIS);
    }

    /**
     * Updates window bounds.
     *
     * @param windowId window id
     * @param bounds   bounds
     */
    public synchronized void setWindowBounds(String windowId, WindowBounds bounds) {
        Awaitable.await(
                setWindowBoundsAsync(windowId, bounds),
                "BiDi browser set window bounds failed.",
                TIMEOUT_MILLIS);
    }

    /**
     * Returns cookies.
     *
     * @param urls URL filters
     * @return cookies
     */
    public java.util.List<Cookie> cookies(String... urls) {
        return java.util.List.of();
    }

    /**
     * Updates cookie.
     *
     * @param cookies cookie parameters
     */
    public void setCookie(CookieParam... cookies) {
    }

    /**
     * Deletes cookies.
     *
     * @param cookies cookie parameters
     */
    public void deleteCookie(DeleteCookiesParameters... cookies) {
    }

    /**
     * Deletes matching cookies.
     *
     * @param predicate cookie predicate
     */
    public void deleteMatchingCookies(java.util.function.Predicate<java.util.Map<String, Object>> predicate) {
    }

    /**
     * Updates permission.
     *
     * @param origin      origin
     * @param permissions permissions
     */
    public void setPermission(String origin, PermissionOptions... permissions) {
        defaultBrowserContext().setPermission(origin, permissions);
    }

    /**
     * Returns the target.
     *
     * @return target value
     */
    public BidiTarget target() {
        return BidiTarget.browser(this);
    }

    /**
     * Returns the targets.
     *
     * @return values
     */
    public List<BidiTarget> targets() {
        List<BidiTarget> targets = new ArrayList<>();
        targets.add(target());
        for (BidiPage page : bidiPages()) {
            targets.add(BidiTarget.page(page));
            for (BidiWorker worker : page.workers()) {
                targets.add(BidiTarget.worker(worker));
            }
        }
        return List.copyOf(targets);
    }

    /**
     * Returns the user agent.
     *
     * @return user agent value
     */
    public String userAgent() {
        return PayloadReader.text(session.capabilities().get("userAgent"));
    }

    /**
     * Returns the version.
     *
     * @return version value
     */
    public String version() {
        String name = PayloadReader.text(session.capabilities().get("browserName"));
        String version = PayloadReader.text(session.capabilities().get("browserVersion"));
        if (name.isEmpty() && version.isEmpty()) {
            return Normal.EMPTY;
        }
        if (version.isEmpty()) {
            return name;
        }
        return name + Symbol.SLASH + version;
    }

    /**
     * Returns whether network tracking is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isNetworkEnabled() {
        return networkEnabled;
    }

    /**
     * Updates network enabled.
     *
     * @param networkEnabled network enabled value
     */
    void setNetworkEnabled(boolean networkEnabled) {
        this.networkEnabled = networkEnabled;
    }

    /**
     * Returns whether issue tracking is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isIssuesEnabled() {
        return issuesEnabled;
    }

    /**
     * Updates issues enabled.
     *
     * @param issuesEnabled issues enabled value
     */
    void setIssuesEnabled(boolean issuesEnabled) {
        this.issuesEnabled = issuesEnabled;
    }

    /**
     * Returns the debug info.
     *
     * @return mapped values
     */
    public Map<String, Object> debugInfo() {
        return Map.of("pendingProtocolErrors", session.connection().unwrap().getPendingProtocolErrors());
    }

    /**
     * Subscribes a persistent browser event listener.
     */
    public BidiBrowser on(BrowserEvent event, java.util.function.Consumer<Object> listener) {
        emitter.on(event, listener);
        return this;
    }

    /**
     * Subscribes a one-shot browser event listener.
     */
    public BidiBrowser once(BrowserEvent event, java.util.function.Consumer<Object> listener) {
        emitter.once(event, listener);
        return this;
    }

    /**
     * Removes a browser event listener.
     */
    public BidiBrowser off(BrowserEvent event, java.util.function.Consumer<Object> listener) {
        emitter.off(event, listener);
        return this;
    }

    /**
     * Removes browser event listeners.
     */
    public BidiBrowser off(BrowserEvent event) {
        emitter.off(event);
        return this;
    }

    /**
     * Emits a browser event.
     */
    public boolean emit(BrowserEvent event, Object payload) {
        return emitter.emit(event, payload);
    }

    /**
     * Counts browser event listeners.
     */
    public int listenerCount(BrowserEvent event) {
        return emitter.listenerCount(event);
    }

    /**
     * Removes browser event listeners.
     */
    public BidiBrowser removeAllListeners(BrowserEvent event) {
        emitter.removeAllListeners(event);
        return this;
    }

    /**
     * Removes all browser event listeners.
     */
    public BidiBrowser removeAllListeners() {
        emitter.removeAllListeners();
        return this;
    }

    /**
     * Waits for a target.
     */
    public Target waitForTarget(java.util.function.Predicate<Target> predicate, java.time.Duration timeout) {
        long deadline = System.nanoTime() + (timeout == null ? java.time.Duration.ofSeconds(30L) : timeout).toNanos();
        while (System.nanoTime() <= deadline) {
            for (Target target : targets()) {
                if (predicate == null || predicate.test(target)) {
                    return target;
                }
            }
            if (!org.miaixz.bus.core.xyz.ThreadKit.sleep(25L)) {
                throw new InternalException("Interrupted while waiting for BiDi target.");
            }
        }
        throw new InternalException("Timed out while waiting for BiDi target.");
    }

    /**
     * Waits for a target.
     */
    public Target waitForTarget(java.util.function.Predicate<Target> predicate) {
        return waitForTarget(predicate, java.time.Duration.ofSeconds(30L));
    }

    /**
     * Returns the session.
     *
     * @return session value
     */
    public BidiSession session() {
        return session;
    }

    /**
     * Returns the with close hook.
     *
     * @param closeHook close hook value
     * @return with close hook value
     */
    BidiBrowser withCloseHook(AutoCloseable closeHook) {
        this.closeHook = closeHook;
        Logger.debug(false, "Browser", "BiDi browser close hook updated: present={}", closeHook != null);
        return this;
    }

    /**
     * Returns the with process.
     *
     * @param process process value
     * @return with process value
     */
    BidiBrowser withProcess(Process process) {
        this.process = process;
        Logger.debug(false, "Launcher", "BiDi browser process attached: present={}", process != null);
        return this;
    }

    /**
     * Returns the with CDP connection.
     *
     * @param cdpConnection CDP connection value
     * @return with CDP connection value
     */
    BidiBrowser withCdpConnection(Connection cdpConnection) {
        this.cdpConnection = cdpConnection;
        Logger.debug(false, "Browser", "BiDi browser CDP connection attached: present={}", cdpConnection != null);
        return this;
    }

    /**
     * Handles disconnect.
     */
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            Logger.debug(
                    true,
                    "Browser",
                    "BiDi browser disconnect requested: contexts={}, pages={}",
                    contexts.size(),
                    bidiPages().size());
            try {
                session.end().get(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
                Logger.warn(false, "Browser", ex, "BiDi browser session end failed during disconnect.");
            } finally {
                session.connection().close();
                IoKit.closeQuietly(closeHook);
                Logger.debug(false, "Browser", "BiDi browser disconnected.");
            }
        }
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        if (connected.compareAndSet(true, false)) {
            Logger.debug(
                    true,
                    "Browser",
                    "BiDi browser close requested: contexts={}, pages={}",
                    contexts.size(),
                    bidiPages().size());
            try {
                IoKit.closeQuietly(closeHook);
            } finally {
                session.connection().close();
                Logger.debug(false, "Browser", "BiDi browser closed.");
            }
        }
    }

    /**
     * Returns whether CDP transport is available.
     *
     * @return {@code true} when the condition matches
     */
    private boolean hasCdpTransport() {
        return cdpConnection != null && cdpConnection.hasConfiguredTransport();
    }

    /**
     * Registers an extension in the local extension map.
     *
     * @param info info
     */
    private synchronized void registerExtension(PayloadExtensionInfo info) {
        extensions.put(info.id(), new PageExtension(info, null, cdpConnection));
    }

    /**
     * Creates extension information for an installed BiDi extension.
     *
     * @param id   id
     * @param path path
     * @return installed payload extension info value
     */
    private PayloadExtensionInfo installedPayloadExtensionInfo(String id, String path) {
        String actualId = StringKit.isBlank(id) ? PayloadExtensionInfo.local(path).id() : id;
        return new PayloadExtensionInfo(actualId, Normal.UNKNOWN, extensionName(path), path, true);
    }

    /**
     * Resolves an extension display name.
     *
     * @param path path
     * @return extension name value
     */
    private String extensionName(String path) {
        try {
            Path fileName = Path.of(path).getFileName();
            return fileName == null ? "extension" : fileName.toString();
        } catch (RuntimeException ex) {
            return "extension";
        }
    }

    /**
     * Adds a screen to the local screen model.
     *
     * @param params params
     * @return add local screen value
     */
    private ScreenInfo addLocalScreen(AddScreenParams params) {
        return ScreenInfo.registerLocal(params, screenIndex, localScreens);
    }

    /**
     * Adds or replaces a local screen.
     *
     * @param screen screen
     */
    private void putLocalScreen(ScreenInfo screen) {
        ScreenInfo.registerLocal(screen, localScreens);
    }

    /**
     * Removes a local screen.
     *
     * @param screenId screen id
     */
    private void removeLocalScreen(String screenId) {
        ScreenInfo.unregisterLocal(screenId, localScreens);
    }

    /**
     * Converts a string window id into the CDP window id type.
     *
     * @param windowId window id
     * @return window identifier value
     */
    private Object windowIdentifier(String windowId) {
        try {
            return Integer.valueOf(windowId);
        } catch (NumberFormatException ignored) {
            return windowId;
        }
    }

    /**
     * Converts BiDi client window info into window bounds.
     *
     * @param payload payload
     * @return window bounds from BiDi value
     */
    private WindowBounds windowBoundsFromBidi(CdpPayload payload) {
        WindowBounds bounds = new WindowBounds();
        CdpPayload actual = payload == null ? CdpPayload.NULL : payload;
        bounds.setLeft(PayloadReader.numberObject(actual.get("x")));
        bounds.setTop(PayloadReader.numberObject(actual.get("y")));
        bounds.setWidth(PayloadReader.numberObject(actual.get("width")));
        bounds.setHeight(PayloadReader.numberObject(actual.get("height")));
        String state = PayloadReader.text(actual.get("state"));
        if (StringKit.isNotBlank(state)) {
            bounds.setWindowState(WindowState.from(state));
        }
        return bounds;
    }

    /**
     * Converts window bounds into BiDi window state parameters.
     *
     * @param windowId window id
     * @param bounds   bounds
     * @return BiDi window params
     */
    private Map<String, Object> toBidiWindowParams(String windowId, WindowBounds bounds) {
        Map<String, Object> params = new LinkedHashMap<>();
        WindowState state = bounds.getWindowState() == null ? WindowState.NORMAL : bounds.getWindowState();
        params.put("clientWindow", windowId);
        params.put("state", state.value());
        if (state == WindowState.NORMAL) {
            put(params, "x", bounds.getLeft());
            put(params, "y", bounds.getTop());
            put(params, "width", bounds.getWidth());
            put(params, "height", bounds.getHeight());
        }
        return params;
    }

    /**
     * Puts a non-null value into a map.
     *
     * @param target target
     * @param name   name
     * @param value  value
     */
    private void put(Map<String, Object> target, String name, Object value) {
        if (value != null) {
            target.put(name, value);
        }
    }

    /**
     * Converts protocol failures into bus-all internal exceptions.
     *
     * @param future  future
     * @param message message
     * @param <T>     the generic type handled by this member
     * @return guard value
     */
    private <T> CompletableFuture<T> guard(CompletableFuture<T> future, String message) {
        CompletableFuture<T> guarded = new CompletableFuture<>();
        future.whenComplete((value, throwable) -> {
            if (throwable == null) {
                guarded.complete(value);
                return;
            }
            guarded.completeExceptionally(asInternalException(message, throwable));
        });
        return guarded;
    }

    /**
     * Converts a throwable into a bus-all internal exception.
     *
     * @param message   message
     * @param throwable throwable
     * @return as internal exception value
     */
    private InternalException asInternalException(String message, Throwable throwable) {
        Throwable actual = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        return actual instanceof InternalException internalException ? internalException
                : new InternalException(message, actual);
    }

}
