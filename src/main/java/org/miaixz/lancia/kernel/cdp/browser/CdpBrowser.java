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
package org.miaixz.lancia.kernel.cdp.browser;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.IoKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Context;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes;
import org.miaixz.lancia.browser.supervisor.BrowserProcess;
import org.miaixz.lancia.browser.supervisor.PipeEnvelope;
import org.miaixz.lancia.events.BrowserEvent;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.cdp.page.CdpPage;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.kernel.cdp.session.TargetInfo;
import org.miaixz.lancia.kernel.cdp.target.CdpPageTarget;
import org.miaixz.lancia.kernel.cdp.target.CdpTarget;
import org.miaixz.lancia.kernel.cdp.target.CdpTargetManager;
import org.miaixz.lancia.kernel.cdp.target.CdpTargetManagerEvent;
import org.miaixz.lancia.nimble.browser.WindowBounds;
import org.miaixz.lancia.nimble.emulation.Viewport;
import org.miaixz.lancia.nimble.network.Cookie;
import org.miaixz.lancia.nimble.network.CookieParam;
import org.miaixz.lancia.nimble.network.DeleteCookiesParameters;
import org.miaixz.lancia.nimble.network.DownloadBehavior;
import org.miaixz.lancia.nimble.screen.AddScreenParams;
import org.miaixz.lancia.nimble.screen.ScreenInfo;
import org.miaixz.lancia.options.AttachOptions;
import org.miaixz.lancia.options.BrowserContextOptions;
import org.miaixz.lancia.options.CreatePageOptions;
import org.miaixz.lancia.options.PermissionOptions;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.page.PageExtension;
import org.miaixz.lancia.shared.payload.PayloadExtensionInfo;
import org.miaixz.lancia.shared.payload.PayloadScreenInfo;

/**
 * Coordinates CDP browser operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpBrowser implements Browser {

    /**
     * Default wait for target timeout.
     */
    private static final Duration DEFAULT_WAIT_FOR_TARGET_TIMEOUT = Duration.ofSeconds(30L);
    /**
     * Shared constant for target poll interval.
     */
    private static final Duration TARGET_POLL_INTERVAL = Duration.ofMillis(25L);
    /**
     * Default protocol.
     */
    private static final String DEFAULT_PROTOCOL = "cdp";

    /**
     * Current emitter.
     */
    private final EventEmitter<BrowserEvent> emitter = new EventEmitter<>();
    /**
     * Current connection.
     */
    private final Connection connection;

    /**
     * WebSocket endpoint.
     */
    private final String wsEndpoint;
    /**
     * Current close hook.
     */
    private final AutoCloseable closeHook;
    /**
     * Whether network is enabled.
     */
    private final boolean networkEnabled;
    /**
     * Whether issues is enabled.
     */
    private final boolean issuesEnabled;
    /**
     * Current default viewport.
     */
    private volatile Viewport defaultViewport;
    /**
     * Thread-safe connected state.
     */
    private final AtomicBoolean connected = new AtomicBoolean(true);
    /**
     * Thread-safe attached state.
     */
    private final AtomicBoolean attached = new AtomicBoolean(false);
    /**
     * Registered contexts values.
     */
    private final List<CdpBrowserContext> contexts = new ArrayList<>();
    /**
     * Current default context.
     */
    private final CdpBrowserContext defaultContext = new CdpBrowserContext(true);
    /**
     * Current target manager.
     */
    private final CdpTargetManager targetManager = new CdpTargetManager();
    /**
     * Current default target.
     */
    private final CdpTarget defaultTarget;
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
     * Creates a CDP browser.
     */
    public CdpBrowser() {
        this(new Connection(), null);
    }

    /**
     * Creates a CDP browser.
     *
     * @param connection protocol connection
     */
    public CdpBrowser(Connection connection) {
        this(connection, null);
    }

    /**
     * Creates a CDP browser.
     *
     * @param connection protocol connection
     * @param closeHook  close hook
     */
    public CdpBrowser(Connection connection, AutoCloseable closeHook) {
        this(connection, closeHook, connection == null ? Normal.EMPTY : connection.url(), true, true);
    }

    /**
     * Creates a CDP browser.
     *
     * @param connection     protocol connection
     * @param closeHook      close hook
     * @param wsEndpoint     ws endpoint
     * @param networkEnabled network enabled
     * @param issuesEnabled  issues enabled
     */
    public CdpBrowser(Connection connection, AutoCloseable closeHook, String wsEndpoint, boolean networkEnabled,
            boolean issuesEnabled) {
        this(connection, closeHook, wsEndpoint, networkEnabled, issuesEnabled, List.of());
    }

    /**
     * Creates a CDP browser.
     *
     * @param connection        protocol connection
     * @param closeHook         close hook
     * @param wsEndpoint        ws endpoint
     * @param networkEnabled    network enabled
     * @param issuesEnabled     issues enabled
     * @param browserContextIds browser context ids
     */
    public CdpBrowser(Connection connection, AutoCloseable closeHook, String wsEndpoint, boolean networkEnabled,
            boolean issuesEnabled, List<String> browserContextIds) {
        this.connection = Assert.notNull(connection, "connection");
        this.wsEndpoint = wsEndpoint == null ? Normal.EMPTY : wsEndpoint;
        this.closeHook = closeHook;
        this.networkEnabled = networkEnabled;
        this.issuesEnabled = issuesEnabled;
        CdpBrowserContext.Internal.bindConnection(this.defaultContext, this.connection);
        CdpBrowserContext.Internal.bindBrowser(this.defaultContext, this);
        this.contexts.add(defaultContext);
        if (browserContextIds != null) {
            for (String contextId : browserContextIds) {
                if (StringKit.isNotBlank(contextId)) {
                    CdpBrowserContext context = new CdpBrowserContext(false, contextId);
                    CdpBrowserContext.Internal.bindConnection(context, this.connection);
                    CdpBrowserContext.Internal.bindBrowser(context, this);
                    this.contexts.add(context);
                }
            }
        }
        this.targetManager.initialize();
        this.defaultTarget = this.targetManager
                .onTargetCreated(new TargetInfo("browser", "browser", Normal.EMPTY, Normal.EMPTY));
    }

    /**
     * Subscribes a persistent browser event listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return browser
     */
    @Override
    public Browser on(BrowserEvent event, Consumer<Object> listener) {
        emitter.on(event, listener);
        return this;
    }

    /**
     * Subscribes a one-shot browser event listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return browser
     */
    @Override
    public Browser once(BrowserEvent event, Consumer<Object> listener) {
        emitter.once(event, listener);
        return this;
    }

    /**
     * Removes a browser event listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return browser
     */
    @Override
    public Browser off(BrowserEvent event, Consumer<Object> listener) {
        emitter.off(event, listener);
        return this;
    }

    /**
     * Removes all listeners for the browser event.
     *
     * @param event event type
     * @return browser
     */
    @Override
    public Browser off(BrowserEvent event) {
        emitter.off(event);
        return this;
    }

    /**
     * Counts browser event listeners.
     *
     * @param event event type
     * @return listener count
     */
    @Override
    public int listenerCount(BrowserEvent event) {
        return emitter.listenerCount(event);
    }

    /**
     * Removes all listeners for the browser event.
     *
     * @param event event type
     * @return browser
     */
    @Override
    public Browser removeAllListeners(BrowserEvent event) {
        emitter.removeAllListeners(event);
        return this;
    }

    /**
     * Removes all browser event listeners.
     *
     * @return browser
     */
    @Override
    public Browser removeAllListeners() {
        emitter.removeAllListeners();
        return this;
    }

    /**
     * Attaches browser-level CDP target lifecycle listeners.
     *
     * @param options attach options
     */
    public void attach(AttachOptions options) {
        if (!attached.compareAndSet(false, true)) {
            return;
        }
        AttachOptions actualOptions = options == null ? new AttachOptions() : options;
        this.defaultViewport = actualOptions.getDefaultViewport();
        targetManager.on(CdpTargetManagerEvent.TARGET_AVAILABLE, payload -> bindAvailableTarget((CdpTarget) payload));
        targetManager.on(CdpTargetManagerEvent.TARGET_GONE, payload -> removeAvailableTarget((CdpTarget) payload));
        targetManager.on(CdpTargetManagerEvent.TARGET_CHANGED, payload -> {
            CdpTargetManager.TargetChangedEvent event = (CdpTargetManager.TargetChangedEvent) payload;
            emit(BrowserEvent.TARGET_CHANGED, event.target());
        });
        connection.on(CDPSession.Events.DISCONNECTED, payload -> {
            if (connected.compareAndSet(true, false)) {
                emit(BrowserEvent.DISCONNECTED, payload);
            }
        });
        connection.on("Target.targetCreated", payload -> targetManager.onTargetCreated((CdpPayload) payload));
        connection.on("Target.targetDestroyed", payload -> targetManager.onTargetDestroyed((CdpPayload) payload));
        connection.on("Target.targetInfoChanged", payload -> targetManager.onTargetInfoChanged((CdpPayload) payload));
        connection.on(CDPSession.Events.SESSION_ATTACHED, payload -> {
            CDPSession session = (CDPSession) payload;
            if (targetManager.onAttachedToTarget(session)) {
                session.send("Runtime.runIfWaitingForDebugger");
            }
        });
        connection.on(
                CDPSession.Events.SESSION_DETACHED,
                payload -> targetManager.onDetachedFromTarget((CDPSession) payload));
        targetManager.initialize(connection, toTargetManagerOptions(actualOptions)).exceptionally(error -> null);
        if (actualOptions.getDownloadBehavior() != null) {
            CdpBrowserContext.Internal.setDownloadBehavior(defaultContext, actualOptions.getDownloadBehavior());
        }
    }

    /**
     * Returns whether connected is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isConnected() {
        return connected.get() && !connection.isClosed();
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
     * Returns the process.
     *
     * @return process value
     */
    public Process process() {
        return processFrom(closeHook);
    }

    /**
     * Resolves the launched process from a close hook.
     *
     * @param closeHook close hook
     * @return process or null
     */
    private Process processFrom(AutoCloseable closeHook) {
        if (closeHook instanceof BrowserProcess browserProcess) {
            return browserProcess.getProcess();
        }
        if (closeHook instanceof PipeEnvelope pipeEnvelope) {
            return pipeEnvelope.getProcess();
        }
        return null;
    }

    /**
     * Returns the ws endpoint.
     *
     * @return ws endpoint value
     */
    public String wsEndpoint() {
        return wsEndpoint;
    }

    /**
     * Returns the new page.
     *
     * @return new page value
     */
    public Page newPage() {
        return newPage(new CreatePageOptions());
    }

    /**
     * Returns the new page.
     *
     * @param options operation options
     * @return new page value
     */
    public Page newPage(CreatePageOptions options) {
        return createPageInContext(defaultContext.id(), options);
    }

    /**
     * Creates page in context.
     *
     * @param contextId context id
     * @param options   operation options
     * @return created page in context
     */
    public Page createPageInContext(String contextId, CreatePageOptions options) {
        CreatePageOptions actualOptions = options == null ? new CreatePageOptions() : options;
        CdpBrowserContext context = contextById(contextId).orElse(defaultContext);
        Logger.debug(
                true,
                "Page",
                "New page requested: contextId={}, hasTransport={}",
                context.id(),
                connection.hasConfiguredTransport());
        if (!connection.hasConfiguredTransport()) {
            Page page = new CdpPage();
            CdpBrowserContext.Internal.addPage(context, page);
            registerPageTarget(context, "page-" + page.hashCode(), Normal.EMPTY, page, null);
            Logger.debug(false, "Page", "New local page created: contextId={}", context.id());
            return page;
        }
        try {
            Map<String, Object> params = actualOptions.toTargetCreateMap();
            if (!CdpBrowserContext.Internal.isDefaultContext(context)) {
                params.put("browserContextId", context.id());
            }
            CdpPayload targetResult = connection.send("Target.createTarget", params).get(5, TimeUnit.SECONDS);
            String targetId = targetResult.get("targetId").asText();
            CdpPayload attachResult = connection
                    .send("Target.attachToTarget", Map.of("targetId", targetId, "flatten", true))
                    .get(5, TimeUnit.SECONDS);
            String sessionId = attachResult.get("sessionId").asText();
            CDPSession session = connection
                    .createSession(new TargetInfo(targetId, "page", Builder.ABOUT_BLANK, sessionId));
            Page page = new CdpPage(session);
            CdpBrowserContext.Internal.addPage(context, page, targetId, sessionId, session);
            registerPageTarget(context, targetId, sessionId, page, session);
            Logger.debug(
                    false,
                    "Page",
                    "New page created: contextId={}, targetId={}, sessionId={}",
                    context.id(),
                    targetId,
                    sessionId);
            return page;
        } catch (Exception ex) {
            Logger.error(false, "Page", ex, "New page failed: contextId={}", context.id());
            throw new InternalException("Failed to create page.", ex);
        }
    }

    /**
     * Returns the pages.
     *
     * @return values
     */
    public List<Page> pages() {
        List<Page> pages = new ArrayList<>();
        for (CdpBrowserContext context : contexts) {
            pages.addAll(context.pages());
        }
        return List.copyOf(pages);
    }

    /**
     * Returns the browser contexts.
     *
     * @return values
     */
    public List<CdpBrowserContext> browserContexts() {
        return List.copyOf(contexts);
    }

    /**
     * Returns the default browser context.
     *
     * @return default browser context value
     */
    public CdpBrowserContext defaultBrowserContext() {
        return defaultContext;
    }

    /**
     * Creates browser context.
     *
     * @param options operation options
     * @return created browser context
     */
    public CdpBrowserContext createBrowserContext(BrowserContextOptions options) {
        BrowserContextOptions actualOptions = options == null ? new BrowserContextOptions() : options;
        Logger.debug(
                true,
                "Browser",
                "Browser context create requested: hasDownloadBehavior={}, proxy={}",
                actualOptions.getDownloadBehavior() != null,
                StringKit.isNotBlank(actualOptions.getProxyServer()));
        CdpBrowserContext context;
        if (connection.hasConfiguredTransport()) {
            CdpPayload result = Awaitable.await(
                    connection.send("Target.createBrowserContext", browserContextParams(actualOptions)),
                    "Failed to create browser context.");
            context = new CdpBrowserContext(false, result.get("browserContextId").asText());
        } else {
            context = new CdpBrowserContext(false);
        }
        CdpBrowserContext.Internal.bindConnection(context, connection);
        CdpBrowserContext.Internal.bindBrowser(context, this);
        DownloadBehavior behavior = actualOptions.getDownloadBehavior();
        if (behavior != null) {
            behavior.validate();
            CdpBrowserContext.Internal.setDownloadBehavior(context, behavior);
        }
        contexts.add(context);
        Logger.debug(
                false,
                "Browser",
                "Browser context created: contextId={}, default={}",
                context.id(),
                CdpBrowserContext.Internal.isDefaultContext(context));
        return context;
    }

    /**
     * Builds Target.createBrowserContext parameters.
     *
     * @param options context options
     * @return CDP parameters
     */
    private Map<String, Object> browserContextParams(BrowserContextOptions options) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (StringKit.isNotBlank(options.getProxyServer())) {
            params.put("proxyServer", options.getProxyServer());
        }
        if (!options.getProxyBypassList().isEmpty()) {
            params.put("proxyBypassList", String.join(Symbol.COMMA, options.getProxyBypassList()));
        }
        return params;
    }

    /**
     * Handles dispose context.
     *
     * @param context browser context
     */
    public void disposeContext(Context context) {
        CdpBrowserContext actualContext = (CdpBrowserContext) Assert.notNull(context, "context");
        if (CdpBrowserContext.Internal.isDefaultContext(actualContext)) {
            throw new IllegalStateException("The default browser context cannot be closed directly.");
        }
        if (connection.hasConfiguredTransport()) {
            Awaitable.await(
                    connection.send("Target.disposeBrowserContext", Map.of("browserContextId", actualContext.id())),
                    "Failed to dispose browser context.");
        }
        contexts.remove(actualContext);
    }

    /**
     * Creates browser context.
     *
     * @return created browser context
     */
    public CdpBrowserContext createBrowserContext() {
        return createBrowserContext(null);
    }

    /**
     * Returns the install extension.
     *
     * @param path file path
     * @return completion future
     */
    public CompletableFuture<String> installExtension(String path) {
        Assert.notNull(path, "path");
        Logger.debug(true, "Browser", "Extension install requested: path={}", path);
        if (!connection.hasConfiguredTransport()) {
            PayloadExtensionInfo info = PayloadExtensionInfo.local(path);
            registerExtension(info);
            Logger.debug(false, "Browser", "Local extension installed: id={}", info.id());
            return CompletableFuture.completedFuture(info.id());
        }
        try {
            return connection.send("Extensions.loadUnpacked", Map.of("path", path)).thenApply(payload -> {
                PayloadExtensionInfo info = PayloadExtensionInfo.fromInstallResult(payload, path);
                registerExtension(info);
                Logger.debug(false, "Browser", "Extension installed: id={}", info.id());
                return info.id();
            });
        } catch (RuntimeException ex) {
            Logger.error(false, "Browser", ex, "Extension install failed: path={}", path);
            CompletableFuture<String> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(ex);
            return rejected;
        }
    }

    /**
     * Returns the install extension.
     *
     * @param path file path
     * @return completion future
     */
    public CompletableFuture<String> installExtension(Path path) {
        return installExtension(Assert.notNull(path, "path").toString());
    }

    /**
     * Returns the uninstall extension.
     *
     * @param id identifier
     * @return completion future
     */
    public CompletableFuture<Void> uninstallExtension(String id) {
        Assert.notNull(id, "id");
        Logger.debug(true, "Browser", "Extension uninstall requested: id={}", id);
        if (!connection.hasConfiguredTransport()) {
            extensions.remove(id);
            Logger.debug(false, "Browser", "Local extension uninstalled: id={}", id);
            return CompletableFuture.completedFuture(null);
        }
        try {
            return connection.send("Extensions.uninstall", Map.of("id", id)).thenRun(() -> {
                extensions.remove(id);
                Logger.debug(false, "Browser", "Extension uninstalled: id={}", id);
            });
        } catch (RuntimeException ex) {
            Logger.error(false, "Browser", ex, "Extension uninstall failed: id={}", id);
            CompletableFuture<Void> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(ex);
            return rejected;
        }
    }

    /**
     * Returns the extensions.
     *
     * @return mapped values
     */
    public synchronized Map<String, PageExtension> extensions() {
        if (connection.hasConfiguredTransport()) {
            CdpPayload response = Awaitable
                    .await(connection.send("Extensions.getExtensions"), "Failed to read extension list.");
            Map<String, PageExtension> refreshed = new LinkedHashMap<>();
            for (CdpPayload item : response.get("extensions").elements()) {
                PayloadExtensionInfo info = PayloadExtensionInfo.fromProtocol(item);
                refreshed.put(info.id(), extensions.getOrDefault(info.id(), new PageExtension(info, this, connection)));
            }
            extensions.clear();
            extensions.putAll(refreshed);
        }
        return Map.copyOf(extensions);
    }

    /**
     * Returns the extension.
     *
     * @param id identifier
     * @return optional value
     */
    public synchronized Optional<PageExtension> extension(String id) {
        return Optional.ofNullable(extensions.get(id));
    }

    /**
     * Returns the screens.
     *
     * @return values
     */
    public synchronized List<ScreenInfo> screens() {
        CompletableFuture<CdpPayload> future;
        if (!connection.hasConfiguredTransport()) {
            return List.copyOf(localScreens);
        }
        future = connection.send("Emulation.getScreenInfos");
        CdpPayload result = Awaitable.await(future, "Failed to read screen information.");
        List<ScreenInfo> screens = new ArrayList<>();
        for (CdpPayload item : result.get("screenInfos").elements()) {
            screens.add(PayloadScreenInfo.from(item));
        }
        localScreens.clear();
        localScreens.addAll(screens);
        return List.copyOf(screens);
    }

    /**
     * Adds screen.
     *
     * @param params protocol parameters
     * @return add screen value
     */
    public synchronized ScreenInfo addScreen(AddScreenParams params) {
        AddScreenParams actualParams = Assert.notNull(params, "params");
        CompletableFuture<CdpPayload> future;
        if (!connection.hasConfiguredTransport()) {
            return addLocalScreen(actualParams);
        }
        future = connection.send("Emulation.addScreen", actualParams.toMap());
        ScreenInfo screen = PayloadScreenInfo.from(Awaitable.await(future, "Failed to add screen.").get("screenInfo"));
        putLocalScreen(screen);
        return screen;
    }

    /**
     * Removes screen.
     *
     * @param screenId screen id
     */
    public synchronized void removeScreen(String screenId) {
        Assert.notNull(screenId, "screenId");
        CompletableFuture<CdpPayload> future;
        if (!connection.hasConfiguredTransport()) {
            removeLocalScreen(screenId);
            return;
        }
        future = connection.send("Emulation.removeScreen", Map.of("screenId", screenId));
        Awaitable.await(future, "Failed to remove screen.");
        removeLocalScreen(screenId);
    }

    /**
     * Returns the targets.
     *
     * @return values
     */
    public List<CdpTarget> targets() {
        Map<String, CdpTarget> targets = new LinkedHashMap<>();
        for (CdpTarget target : targetManager.getAvailableTargets()) {
            targets.put(CdpTarget.Internal.targetInfo(target).getTargetId(), target);
        }
        for (CdpBrowserContext context : contexts) {
            for (CdpTarget target : context.targets()) {
                targets.putIfAbsent(CdpTarget.Internal.targetInfo(target).getTargetId(), target);
            }
        }
        return List.copyOf(targets.values());
    }

    /**
     * Returns the target.
     *
     * @return target value
     */
    public CdpTarget target() {
        return defaultTarget;
    }

    /**
     * Waits for target.
     *
     * @param predicate predicate
     * @param timeout   timeout value
     * @return wait for target value
     */
    public CdpTarget waitForTarget(Predicate predicate, Duration timeout) {
        Assert.notNull(predicate, "predicate");
        Duration actualTimeout = timeout == null ? DEFAULT_WAIT_FOR_TARGET_TIMEOUT : timeout;
        Logger.debug(true, "Browser", "Wait for target requested: timeout={}", actualTimeout);
        long deadline = actualTimeout.isZero() || actualTimeout.isNegative() ? Long.MAX_VALUE
                : System.nanoTime() + actualTimeout.toNanos();
        while (true) {
            for (CdpTarget target : targets()) {
                if (predicate.test(target)) {
                    Logger.debug(
                            false,
                            "Browser",
                            "Wait for target completed: targetId={}, type={}",
                            CdpTarget.Internal.id(target),
                            target.type());
                    return target;
                }
            }
            if (System.nanoTime() >= deadline) {
                Logger.warn(false, "Browser", "Wait for target timed out: timeout={}", actualTimeout);
                throw new TimeoutException("Timed out waiting for target.");
            }
            sleepTargetPollInterval();
        }
    }

    /**
     * Waits for target.
     *
     * @param predicate predicate
     * @return wait for target value
     */
    public CdpTarget waitForTarget(Predicate predicate) {
        return waitForTarget(predicate, DEFAULT_WAIT_FOR_TARGET_TIMEOUT);
    }

    /**
     * Returns the version.
     *
     * @return version value
     */
    public String version() {
        if (!connection.hasConfiguredTransport()) {
            return BrowserData.DEFAULT_BROWSER + Symbol.SLASH
                    + BrowserData.defaultBuildId(BrowserDataTypes.Browser.CHROME);
        }
        try {
            return connection.send("Browser.getVersion").get(5, TimeUnit.SECONDS).get("product").asText();
        } catch (Exception ex) {
            throw new InternalException("Failed to read browser version.", ex);
        }
    }

    /**
     * Returns the user agent.
     *
     * @return user agent value
     */
    public String userAgent() {
        if (!connection.hasConfiguredTransport()) {
            return "Lancia/" + BrowserData.defaultBuildId(BrowserDataTypes.Browser.CHROME);
        }
        try {
            return connection.send("Browser.getVersion").get(5, TimeUnit.SECONDS).get("userAgent").asText();
        } catch (Exception ex) {
            throw new InternalException("Failed to read browser User-Agent.", ex);
        }
    }

    /**
     * Returns the debug.
     *
     * @return mapped values
     */
    public Map<String, Object> debug() {
        return Map.of(
                "connected",
                connected.get(),
                "contexts",
                contexts.size(),
                "extensions",
                extensions.size(),
                "screens",
                localScreens.size(),
                "targets",
                targets().size(),
                "protocol",
                protocol());
    }

    /**
     * Returns the debug info.
     *
     * @return mapped values
     */
    public Map<String, Object> debugInfo() {
        return Map.of("pendingProtocolErrors", connection.getPendingProtocolErrors());
    }

    /**
     * Returns the protocol.
     *
     * @return protocol value
     */
    public String protocol() {
        return DEFAULT_PROTOCOL;
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
     * Returns whether issue tracking is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isIssuesEnabled() {
        return issuesEnabled;
    }

    /**
     * Returns the cookies.
     *
     * @param urls target URLs
     * @return values
     */
    public List<Cookie> cookies(String... urls) {
        return CdpBrowserContext.Internal.cookieObjects(defaultContext, urls);
    }

    /**
     * Updates cookie.
     *
     * @param cookies cookies to use
     */
    public void setCookie(CookieParam... cookies) {
        if (cookies == null) {
            return;
        }
        for (CookieParam cookie : cookies) {
            if (cookie != null) {
                Awaitable.await(defaultContext.setCookie(cookie), "Failed to set cookie.");
            }
        }
    }

    /**
     * Handles delete cookie.
     *
     * @param cookies cookies to use
     */
    public void deleteCookie(DeleteCookiesParameters... cookies) {
        if (cookies == null) {
            return;
        }
        for (DeleteCookiesParameters cookie : cookies) {
            if (cookie != null) {
                Awaitable.await(defaultContext.deleteCookie(cookie), "Failed to delete cookie.");
            }
        }
    }

    /**
     * Handles delete matching cookies.
     *
     * @param predicate predicate value
     */
    public void deleteMatchingCookies(Predicate<Map<String, Object>> predicate) {
        defaultContext.deleteMatchingCookies(Assert.notNull(predicate, "predicate"));
    }

    /**
     * Updates permission.
     *
     * @param origin      origin
     * @param permissions permissions
     */
    public void setPermission(String origin, PermissionOptions... permissions) {
        Assert.notBlank(origin, "origin");
        if (permissions == null) {
            return;
        }
        for (PermissionOptions permission : permissions) {
            if (permission != null) {
                Awaitable.await(defaultContext.setPermission(origin, permission), "Failed to set permission.");
            }
        }
    }

    /**
     * Returns the window bounds.
     *
     * @param windowId window id
     * @return window bounds
     */
    public synchronized WindowBounds getWindowBounds(String windowId) {
        Assert.notBlank(windowId, "windowId");
        if (!connection.hasConfiguredTransport()) {
            return windowBounds.getOrDefault(windowId, new WindowBounds());
        }
        CdpPayload result = Awaitable.await(
                connection.send("Browser.getWindowBounds", Map.of("windowId", windowIdentifier(windowId))),
                "Failed to get window bounds.");
        WindowBounds bounds = WindowBounds.from(result.get("bounds"));
        windowBounds.put(windowId, bounds);
        return bounds;
    }

    /**
     * Updates window bounds.
     *
     * @param windowId window id
     * @param bounds   bounds
     */
    public synchronized void setWindowBounds(String windowId, WindowBounds bounds) {
        Assert.notBlank(windowId, "windowId");
        WindowBounds actualBounds = Assert.notNull(bounds, "bounds");
        windowBounds.put(windowId, actualBounds);
        if (!connection.hasConfiguredTransport()) {
            return;
        }
        Awaitable.await(
                connection.send(
                        "Browser.setWindowBounds",
                        Map.of("windowId", windowIdentifier(windowId), "bounds", actualBounds.toMap())),
                "Failed to set window bounds.");
    }

    /**
     * Handles disconnect.
     */
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            Logger.debug(true, "Browser", "Browser disconnect requested: contexts={}", contexts.size());
            for (CdpBrowserContext context : List.copyOf(contexts)) {
                if (!CdpBrowserContext.Internal.isDefaultContext(context) && !context.isClosed()) {
                    context.close();
                }
            }
            targetManager.dispose();
            connection.dispose();
            emit(BrowserEvent.DISCONNECTED, this);
            Logger.debug(false, "Browser", "Browser disconnected.");
        }
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        Logger.debug(true, "Browser", "Browser close requested: connected={}", connected.get());
        if (connected.get()) {
            IoKit.closeQuietly(closeHook);
        }
        disconnect();
        Logger.debug(false, "Browser", "Browser close completed.");
    }

    /**
     * Returns the register extension.
     *
     * @param info info value
     * @return register extension value
     */
    private synchronized PageExtension registerExtension(PayloadExtensionInfo info) {
        PageExtension extension = new PageExtension(info, this, connection);
        extensions.put(info.id(), extension);
        return extension;
    }

    /**
     * Returns the add local screen.
     *
     * @param params protocol parameters
     * @return add local screen value
     */
    private ScreenInfo addLocalScreen(AddScreenParams params) {
        return ScreenInfo.registerLocal(params, screenIndex, localScreens);
    }

    /**
     * Handles put local screen.
     *
     * @param screen screen value
     */
    private void putLocalScreen(ScreenInfo screen) {
        ScreenInfo.registerLocal(screen, localScreens);
    }

    /**
     * Handles remove local screen.
     *
     * @param screenId screen ID value
     */
    private void removeLocalScreen(String screenId) {
        ScreenInfo.unregisterLocal(screenId, localScreens);
    }

    /**
     * Binds a target manager target to the matching browser context.
     *
     * @param target available target
     */
    private void bindAvailableTarget(CdpTarget target) {
        CdpTarget actualTarget = Assert.notNull(target, "target");
        CdpTarget.Internal.bindBrowser(actualTarget, this);
        CdpBrowserContext context = contextForTarget(CdpTarget.Internal.targetInfo(actualTarget));
        if (CdpBrowserContext.Internal.addTarget(context, actualTarget)) {
            applyDefaultViewport(actualTarget);
            emit(BrowserEvent.TARGET_CREATED, actualTarget);
        }
    }

    /**
     * Removes a target manager target from browser contexts.
     *
     * @param target gone target
     */
    private void removeAvailableTarget(CdpTarget target) {
        CdpTarget actualTarget = Assert.notNull(target, "target");
        boolean removed = false;
        for (CdpBrowserContext context : contexts) {
            removed = CdpBrowserContext.Internal.removeTarget(context, actualTarget) || removed;
        }
        if (removed) {
            emit(BrowserEvent.TARGET_DESTROYED, actualTarget);
        }
    }

    /**
     * Emits a browser event.
     *
     * @param event   event value
     * @param payload event payload
     * @return emitted state
     */
    @Override
    public boolean emit(BrowserEvent event, Object payload) {
        return emitter.emit(event, payload);
    }

    /**
     * Resolves the browser context for a target.
     *
     * @param targetInfo target info
     * @return browser context
     */
    private CdpBrowserContext contextForTarget(TargetInfo targetInfo) {
        String browserContextId = targetInfo == null ? Normal.EMPTY : targetInfo.getBrowserContextId();
        return contextById(browserContextId).orElse(defaultContext);
    }

    /**
     * Applies the configured default viewport to page targets.
     *
     * @param target target
     */
    private void applyDefaultViewport(CdpTarget target) {
        Viewport viewport = defaultViewport;
        if (viewport == null) {
            return;
        }
        target.page().ifPresent(page -> page.setViewport(viewport));
    }

    /**
     * Handles register page target.
     *
     * @param targetId  target ID value
     * @param sessionId session ID value
     * @param page      page instance
     * @param session   protocol session
     */
    private void registerPageTarget(String targetId, String sessionId, Page page, CDPSession session) {
        registerPageTarget(defaultContext, targetId, sessionId, page, session);
    }

    /**
     * Handles register page target.
     *
     * @param context   browser context
     * @param targetId  target ID value
     * @param sessionId session ID value
     * @param page      page instance
     * @param session   protocol session
     */
    private void registerPageTarget(
            CdpBrowserContext context,
            String targetId,
            String sessionId,
            Page page,
            CDPSession session) {
        TargetInfo targetInfo = new TargetInfo(targetId, "page", Builder.ABOUT_BLANK, sessionId);
        CdpTarget target = new CdpPageTarget(targetInfo, defaultTarget, page);
        CdpTarget.Internal.setSession(target, session);
        CdpTarget.Internal.bindBrowser(target, this);
        CdpTarget.Internal.bindBrowserContext(target, context);
        CdpPage.Internal.bindBrowserContext(page, context);
        CdpPage.Internal.bindTarget(page, target);
        applyDefaultViewport(target);
        targetManager.addTarget(target);
        emit(BrowserEvent.TARGET_CREATED, target);
    }

    /**
     * Returns the context by ID.
     *
     * @param contextId context ID value
     * @return optional value
     */
    private Optional<CdpBrowserContext> contextById(String contextId) {
        String actualContextId = StringKit.isBlank(contextId) ? defaultContext.id() : contextId;
        for (CdpBrowserContext context : contexts) {
            if (actualContextId.equals(context.id())) {
                return Optional.of(context);
            }
        }
        return Optional.empty();
    }

    /**
     * Handles sleep target poll interval.
     */
    private void sleepTargetPollInterval() {
        Awaitable.sleep(TARGET_POLL_INTERVAL.toMillis(), "Interrupted while waiting for target.");
    }

    /**
     * Returns the window identifier.
     *
     * @param windowId window ID value
     * @return window identifier value
     */
    private Object windowIdentifier(String windowId) {
        if (StringKit.isBlank(windowId)) {
            return windowId;
        }
        try {
            return Integer.valueOf(windowId);
        } catch (NumberFormatException ignored) {
            return windowId;
        }
    }

    /**
     * Converts browser attach options to CDP target manager options.
     *
     * @param options browser attach options
     * @return CDP target manager options
     */
    private CdpTargetManager.Options toTargetManagerOptions(AttachOptions options) {
        AttachOptions actualOptions = options == null ? new AttachOptions() : options;
        CdpTargetManager.Options result = new CdpTargetManager.Options();
        result.setTargetFilter((Predicate<CdpTarget>) (Predicate<?>) actualOptions.getTargetFilter());
        result.setIsPageTarget((Predicate<CdpTarget>) (Predicate<?>) actualOptions.getIsPageTarget());
        result.setHandleDevToolsAsPage(actualOptions.isHandleDevToolsAsPage());
        result.setBlocklist(actualOptions.getBlocklist());
        result.setAllowlist(actualOptions.getAllowlist());
        result.setWaitForInitiallyDiscoveredTargets(actualOptions.isWaitForInitiallyDiscoveredTargets());
        result.setTimeoutMillis(actualOptions.getTimeoutMillis());
        return result;
    }

}
