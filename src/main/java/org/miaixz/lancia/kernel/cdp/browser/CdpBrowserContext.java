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

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.miaixz.bus.core.data.id.ID;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Context;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.events.ContextEvent;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.cdp.network.CdpCookie;
import org.miaixz.lancia.kernel.cdp.page.CdpPage;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.kernel.cdp.session.TargetInfo;
import org.miaixz.lancia.kernel.cdp.target.CdpPageTarget;
import org.miaixz.lancia.kernel.cdp.target.CdpTarget;
import org.miaixz.lancia.nimble.network.Cookie;
import org.miaixz.lancia.nimble.network.CookieParam;
import org.miaixz.lancia.nimble.network.DeleteCookiesParameters;
import org.miaixz.lancia.nimble.network.DownloadBehavior;
import org.miaixz.lancia.options.CreatePageOptions;
import org.miaixz.lancia.options.PermissionOptions;
import org.miaixz.lancia.runtime.SecurityPolicy;
import org.miaixz.lancia.shared.async.Awaitable;

/**
 * CDP browser context that owns pages, targets, permissions, cookies, and download behavior.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpBrowserContext extends EventEmitter<ContextEvent> implements Context {

    /**
     * Default timeout when waiting for a target.
     */
    private static final java.time.Duration DEFAULT_WAIT_FOR_TARGET_TIMEOUT = java.time.Duration.ofSeconds(30L);
    /**
     * Poll interval used while waiting for target updates.
     */
    private static final java.time.Duration TARGET_POLL_INTERVAL = java.time.Duration.ofMillis(25L);
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Whether default context is enabled.
     */
    private final boolean defaultContext;
    /**
     * Pages currently attached to this context.
     */
    private final List<Page> pages = new ArrayList<>();
    /**
     * Targets currently attached to this context.
     */
    private final List<CdpTarget> targets = new ArrayList<>();
    /**
     * Permissions granted per origin.
     */
    private final Map<String, List<String>> permissions = new LinkedHashMap<>();
    /**
     * Permission settings applied per origin.
     */
    private final Map<String, Map<String, String>> permissionSettings = new LinkedHashMap<>();
    /**
     * Cookies stored by this context.
     */
    private final List<Map<String, Object>> cookies = new ArrayList<>();
    /**
     * CDP connection used by this context.
     */
    private Connection connection;
    /**
     * Browser that owns this context.
     */
    private Browser browser;
    /**
     * Configured download path for this context.
     */
    private Path downloadPath;
    /**
     * Closed lifecycle state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);
    /**
     * Lock coordinating page screenshot operations.
     */
    private final ReentrantLock pageScreenshotLock = new ReentrantLock(true);
    /**
     * Number of in-flight screenshot operations.
     */
    private final AtomicInteger screenshotOperationsCount = new AtomicInteger();

    /**
     * Creates a browser context.
     *
     * @param defaultContext default context
     */
    public CdpBrowserContext(boolean defaultContext) {
        this(defaultContext, defaultContext ? "default" : ID.objectId());
    }

    /**
     * Creates a browser context.
     *
     * @param defaultContext default context
     * @param id             identifier
     */
    public CdpBrowserContext(boolean defaultContext, String id) {
        this.id = org.miaixz.bus.core.xyz.StringKit.isBlank(id) ? (defaultContext ? "default" : ID.objectId()) : id;
        this.defaultContext = defaultContext;
        Logger.debug(true, "Browser", "Browser context initialized: contextId={}, default={}", this.id, defaultContext);
    }

    /**
     * Creates a new page in this context.
     *
     * @return new page value
     */
    public synchronized Page newPage() {
        return newPage(null);
    }

    /**
     * Creates a new page in this context.
     *
     * @param options operation options
     * @return new page value
     */
    public synchronized Page newPage(CreatePageOptions options) {
        ensureOpen();
        Logger.debug(
                true,
                "Page",
                "Context page create requested: contextId={}, default={}, remote={}",
                id,
                defaultContext,
                browser != null && connection != null && connection.hasConfiguredTransport());
        if (browser instanceof CdpBrowser cdpBrowser && connection != null && connection.hasConfiguredTransport()) {
            return cdpBrowser.createPageInContext(defaultContext ? null : id, options);
        }
        Page page = new CdpPage();
        addPage(page);
        Logger.debug(false, "Page", "Context local page created: contextId={}, pageCount={}", id, pages.size());
        return page;
    }

    /**
     * Registers a local page.
     *
     * @param page page instance
     */
    synchronized void addPage(Page page) {
        ensureOpen();
        addPage(page, "page-" + page.hashCode(), Normal.EMPTY, null);
    }

    /**
     * Registers a page and its target mapping.
     *
     * @param page      page instance
     * @param targetId  target ID value
     * @param sessionId session ID value
     * @param session   protocol session
     */
    synchronized void addPage(Page page, String targetId, String sessionId, CDPSession session) {
        ensureOpen();
        Page actualPage = Assert.notNull(page, "page");
        pages.add(actualPage);
        registerPageTarget(targetId, sessionId, actualPage, session);
        Logger.debug(false, "Page", "Context page registered: contextId={}, pageCount={}", id, pages.size());
    }

    /**
     * Returns the pages.
     *
     * @return values
     */
    public synchronized List<Page> pages() {
        return List.copyOf(pages);
    }

    /**
     * Returns the pages.
     *
     * @param includeAll include all value
     * @return values
     */
    public synchronized List<Page> pages(boolean includeAll) {
        return pages();
    }

    /**
     * Returns the targets.
     *
     * @return values
     */
    public synchronized List<CdpTarget> targets() {
        return List.copyOf(targets);
    }

    /**
     * Adds a target to this browser context.
     *
     * @param target target to add
     * @return {@code true} when the target was newly added
     */
    synchronized boolean addTarget(CdpTarget target) {
        ensureOpen();
        CdpTarget actualTarget = Assert.notNull(target, "target");
        String targetId = CdpTarget.Internal.targetInfo(actualTarget).getTargetId();
        for (CdpTarget existing : targets) {
            if (targetId.equals(CdpTarget.Internal.targetInfo(existing).getTargetId())) {
                return false;
            }
        }
        CdpTarget.Internal.bindBrowserContext(actualTarget, this);
        targets.add(actualTarget);
        actualTarget.page().ifPresent(page -> {
            if (!pages.contains(page)) {
                pages.add(page);
            }
        });
        emit(ContextEvent.TARGET_CREATED, actualTarget);
        return true;
    }

    /**
     * Removes a target from this browser context.
     *
     * @param target target to remove
     * @return {@code true} when the target was present
     */
    synchronized boolean removeTarget(CdpTarget target) {
        CdpTarget actualTarget = Assert.notNull(target, "target");
        boolean removed = targets.removeIf(
                item -> CdpTarget.Internal.targetInfo(actualTarget).getTargetId()
                        .equals(CdpTarget.Internal.targetInfo(item).getTargetId()));
        if (removed) {
            actualTarget.page().ifPresent(pages::remove);
            emit(ContextEvent.TARGET_DESTROYED, actualTarget);
            Logger.debug(
                    false,
                    "Target",
                    "Context target removed: contextId={}, targetId={}, remaining={}",
                    id,
                    CdpTarget.Internal.targetInfo(actualTarget).getTargetId(),
                    targets.size());
        }
        return removed;
    }

    /**
     * Waits for target.
     *
     * @param predicate predicate
     * @param timeout   timeout value
     * @return wait for target value
     */
    public CdpTarget waitForTarget(Predicate<Target> predicate, java.time.Duration timeout) {
        Assert.notNull(predicate, "predicate");
        java.time.Duration actualTimeout = timeout == null ? DEFAULT_WAIT_FOR_TARGET_TIMEOUT : timeout;
        Logger.debug(
                true,
                "Target",
                "Context wait for target requested: contextId={}, timeoutMillis={}",
                id,
                actualTimeout.toMillis());
        long deadline = actualTimeout.isZero() || actualTimeout.isNegative() ? Long.MAX_VALUE
                : System.nanoTime() + actualTimeout.toNanos();
        while (true) {
            for (CdpTarget target : targets()) {
                if (predicate.test(target)) {
                    Logger.debug(
                            false,
                            "Target",
                            "Context wait for target completed: contextId={}, targetId={}",
                            id,
                            CdpTarget.Internal.targetInfo(target).getTargetId());
                    return target;
                }
            }
            if (System.nanoTime() >= deadline) {
                Logger.warn(false, "Target", "Context wait for target timed out: contextId={}", id);
                throw new TimeoutException("Timed out waiting for context target.");
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
    public CdpTarget waitForTarget(Predicate<Target> predicate) {
        return waitForTarget(predicate, DEFAULT_WAIT_FOR_TARGET_TIMEOUT);
    }

    /**
     * Returns the override permissions.
     *
     * @param origin      origin value
     * @param permissions permissions value
     * @return completion future
     */
    public synchronized CompletableFuture<CdpPayload> overridePermissions(String origin, List<String> permissions) {
        ensureOpen();
        String actualOrigin = Assert.notBlank(origin, "origin");
        validatePermissionOrigin(actualOrigin);
        List<String> actualPermissions = List.copyOf(Assert.notNull(permissions, "permissions"));
        this.permissions.put(actualOrigin, actualPermissions);
        Logger.debug(
                true,
                "Browser",
                "Context permissions override requested: contextId={}, permissionCount={}",
                id,
                actualPermissions.size());
        Map<String, Object> params = contextParams();
        params.put("origin", actualOrigin);
        params.put("permissions", actualPermissions);
        return send("Browser.grantPermissions", params);
    }

    /**
     * Updates permission.
     *
     * @param origin     origin
     * @param permission permission
     * @param setting    setting
     */
    public synchronized void setPermission(String origin, String permission, String setting) {
        ensureOpen();
        String actualOrigin = Assert.notBlank(origin, "origin");
        validatePermissionOrigin(actualOrigin);
        permissionSettings.computeIfAbsent(actualOrigin, key -> new LinkedHashMap<>())
                .put(Assert.notBlank(permission, "permission"), Assert.notBlank(setting, "setting"));
        Logger.debug(
                false,
                "Browser",
                "Context permission cached: contextId={}, origin={}, permission={}, setting={}",
                id,
                String.valueOf(origin).replaceAll("[?#].*$", "?<redacted>"),
                permission,
                setting);
    }

    /**
     * Updates permission.
     *
     * @param origin  origin
     * @param options operation options
     * @return set permission value
     */
    public synchronized CompletableFuture<CdpPayload> setPermission(String origin, PermissionOptions options) {
        ensureOpen();
        String actualOrigin = Assert.notBlank(origin, "origin");
        validatePermissionOrigin(actualOrigin);
        PermissionOptions actualOptions = Assert.notNull(options, "options");
        setPermission(actualOrigin, actualOptions.getName(), actualOptions.getSetting());
        Logger.debug(
                true,
                "Browser",
                "Context permission update requested: contextId={}, origin={}, permission={}, setting={}",
                id,
                String.valueOf(origin).replaceAll("[?#].*$", "?<redacted>"),
                actualOptions.getName(),
                actualOptions.getSetting());
        Map<String, Object> params = contextParams();
        if (!Symbol.STAR.equals(actualOrigin)) {
            params.put("origin", actualOrigin);
        }
        params.put("permission", actualOptions.toDescriptor());
        params.put("setting", actualOptions.getSetting());
        return send("Browser.setPermission", params);
    }

    /**
     * Updates permission.
     *
     * @param origin      origin
     * @param permissions permissions
     * @return set permission value
     */
    public synchronized CompletableFuture<CdpPayload> setPermission(String origin, PermissionOptions... permissions) {
        CompletableFuture<CdpPayload> result = CompletableFuture.completedFuture(CdpPayload.NULL);
        if (permissions == null) {
            return result;
        }
        for (PermissionOptions permission : permissions) {
            if (permission != null) {
                result = setPermission(origin, permission);
            }
        }
        return result;
    }

    /**
     * Returns the clear permission overrides.
     *
     * @return completion future
     */
    public synchronized CompletableFuture<CdpPayload> clearPermissionOverrides() {
        permissions.clear();
        permissionSettings.clear();
        Logger.debug(true, "Browser", "Context permissions clear requested: contextId={}", id);
        return send("Browser.resetPermissions", contextParams());
    }

    /**
     * Returns the cookies.
     *
     * @param urls target URLs
     * @return values
     */
    public synchronized List<Map<String, Object>> cookies(String... urls) {
        if (urls == null || urls.length == 0) {
            return copyCookies(cookies);
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> cookie : cookies) {
            Object url = cookie.get("url");
            for (String expected : urls) {
                if (expected.equals(url)) {
                    filtered.add(cookie);
                }
            }
        }
        return copyCookies(filtered);
    }

    /**
     * Returns the cookie objects.
     *
     * @param urls target URLs
     * @return values
     */
    synchronized List<Cookie> cookieObjects(String... urls) {
        if (connection != null) {
            try {
                CdpPayload result = connection.send("Storage.getCookies", contextParams()).get();
                return result.get("cookies").elements().stream().map(CdpCookie::from).toList();
            } catch (Exception ignored) {
                // Implementation note.
            }
        }
        return cookies(urls).stream().map(cookie -> CdpCookie.from(CdpPayload.of(cookie))).toList();
    }

    /**
     * Updates cookie.
     *
     * @param cookie cookie value
     */
    public synchronized void setCookie(Map<String, Object> cookie) {
        ensureOpen();
        Map<String, Object> actualCookie = new LinkedHashMap<>(Assert.notNull(cookie, "cookie"));
        cookies.add(Collections.unmodifiableMap(actualCookie));
        Logger.debug(true, "Network", "Context cookie cached: contextId={}, name={}", id, actualCookie.get("name"));
    }

    /**
     * Updates cookie.
     *
     * @param cookie cookie value
     * @return set cookie value
     */
    public synchronized CompletableFuture<CdpPayload> setCookie(CookieParam cookie) {
        ensureOpen();
        Map<String, Object> map = Assert.notNull(cookie, "cookie").toMap();
        cookies.add(Collections.unmodifiableMap(new LinkedHashMap<>(map)));
        Logger.debug(true, "Network", "Context cookie set requested: contextId={}, name={}", id, map.get("name"));
        Map<String, Object> params = contextParams();
        params.put("cookies", List.of(map));
        return send("Storage.setCookies", params);
    }

    /**
     * Updates cookie.
     *
     * @param cookies cookies to use
     * @return set cookie value
     */
    public synchronized CompletableFuture<CdpPayload> setCookie(CookieParam... cookies) {
        CompletableFuture<CdpPayload> result = CompletableFuture.completedFuture(CdpPayload.NULL);
        if (cookies == null) {
            return result;
        }
        for (CookieParam cookie : cookies) {
            if (cookie != null) {
                result = setCookie(cookie);
            }
        }
        return result;
    }

    /**
     * Handles delete cookie.
     *
     * @param name name to use
     */
    public synchronized void deleteCookie(String name) {
        cookies.removeIf(cookie -> name.equals(cookie.get("name")));
        Logger.debug(true, "Network", "Context cookie deleted from cache: contextId={}, name={}", id, name);
    }

    /**
     * Returns the delete cookie.
     *
     * @param parameters parameters value
     * @return completion future
     */
    public synchronized CompletableFuture<CdpPayload> deleteCookie(DeleteCookiesParameters parameters) {
        Map<String, Object> map = Assert.notNull(parameters, "parameters").toMap();
        cookies.removeIf(cookie -> matchesDeleteFilter(cookie, map));
        Logger.debug(true, "Network", "Context cookie delete requested: contextId={}, name={}", id, map.get("name"));
        return send("Network.deleteCookies", map);
    }

    /**
     * Returns the delete cookie.
     *
     * @param parameters parameters value
     * @return completion future
     */
    public synchronized CompletableFuture<CdpPayload> deleteCookie(DeleteCookiesParameters... parameters) {
        CompletableFuture<CdpPayload> result = CompletableFuture.completedFuture(CdpPayload.NULL);
        if (parameters == null) {
            return result;
        }
        for (DeleteCookiesParameters parameter : parameters) {
            if (parameter != null) {
                result = deleteCookie(parameter);
            }
        }
        return result;
    }

    /**
     * Handles delete matching cookies.
     *
     * @param predicate predicate value
     */
    public synchronized void deleteMatchingCookies(Predicate<Map<String, Object>> predicate) {
        cookies.removeIf(Assert.notNull(predicate, "predicate"));
        Logger.debug(
                true,
                "Network",
                "Context matching cookies deleted: contextId={}, remaining={}",
                id,
                cookies.size());
    }

    /**
     * Handles delete matching cookies.
     *
     * @param filters filters value
     */
    public synchronized void deleteMatchingCookies(DeleteCookiesParameters... filters) {
        if (filters == null || filters.length == 0) {
            return;
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (DeleteCookiesParameters filter : filters) {
            if (filter != null) {
                maps.add(filter.toMap());
            }
        }
        cookies.removeIf(cookie -> maps.stream().anyMatch(filter -> matchesDeleteFilter(cookie, filter)));
        Logger.debug(
                true,
                "Network",
                "Context filtered cookies deleted: contextId={}, filterCount={}, remaining={}",
                id,
                maps.size(),
                cookies.size());
    }

    /**
     * Updates download behavior.
     *
     * @param downloadPath download path value
     */
    synchronized void setDownloadBehavior(Path downloadPath) {
        ensureOpen();
        DownloadBehavior behavior = new DownloadBehavior();
        behavior.setDownloadPath(downloadPath);
        setDownloadBehavior(behavior);
    }

    /**
     * Updates download behavior.
     *
     * @param behavior behavior value
     * @return completion future
     */
    synchronized CompletableFuture<CdpPayload> setDownloadBehavior(DownloadBehavior behavior) {
        ensureOpen();
        this.downloadPath = behavior.getDownloadPath();
        Logger.debug(
                true,
                "Browser",
                "Context download behavior update requested: contextId={}, hasPath={}",
                id,
                downloadPath != null);
        Map<String, Object> params = contextParams();
        params.putAll(behavior.toMap());
        return send("Browser.setDownloadBehavior", params);
    }

    /**
     * Returns the download path.
     *
     * @return optional value
     */
    Optional<Path> downloadPath() {
        return Optional.ofNullable(downloadPath);
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
     * Returns the permission overrides.
     *
     * @return values
     */
    synchronized Map<String, List<String>> permissionOverrides() {
        return Map.copyOf(permissions);
    }

    /**
     * Returns the permission settings.
     *
     * @return mapped values
     */
    synchronized Map<String, Map<String, String>> permissionSettings() {
        return Map.copyOf(permissionSettings);
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
     * Returns whether default context is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isDefaultContext() {
        return defaultContext;
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
     * Returns whether this object is closed.
     *
     * @return whether this object is closed
     */
    public boolean closed() {
        return isClosed();
    }

    /**
     * Binds the protocol connection used by this context.
     *
     * @param connection protocol connection
     */
    synchronized void bindConnection(Connection connection) {
        this.connection = connection;
        Logger.debug(false, "Browser", "Context connection bound: contextId={}, connected={}", id, connection != null);
        if (connection != null && connection.hasConfiguredTransport() && downloadPath == null) {
            setDownloadBehavior(DownloadBehavior.deny());
        }
    }

    /**
     * Binds the owning browser and propagates it to existing targets.
     *
     * @param browser browser instance
     */
    synchronized void bindBrowser(Browser browser) {
        this.browser = browser;
        Logger.debug(false, "Browser", "Context browser bound: contextId={}, hasBrowser={}", id, browser != null);
        for (CdpTarget target : targets) {
            CdpTarget.Internal.bindBrowser(target, browser);
        }
    }

    /**
     * Marks the beginning of a screenshot operation.
     *
     * @return scope to close when the screenshot operation ends
     */
    public AutoCloseable startScreenshot() {
        pageScreenshotLock.lock();
        screenshotOperationsCount.incrementAndGet();
        return () -> {
            try {
                screenshotOperationsCount.updateAndGet(value -> Math.max(Normal._0, value - Normal._1));
            } finally {
                pageScreenshotLock.unlock();
            }
        };
    }

    /**
     * Waits until in-flight screenshot operations can be coordinated.
     *
     * @return lock scope when waiting is needed
     */
    public Optional<AutoCloseable> waitForScreenshotOperations() {
        if (screenshotOperationsCount.get() <= Normal._0) {
            return Optional.empty();
        }
        pageScreenshotLock.lock();
        return Optional.of(pageScreenshotLock::unlock);
    }

    /**
     * Returns the screenshot operations count.
     *
     * @return screenshot operations count value
     */
    int screenshotOperationsCount() {
        return screenshotOperationsCount.get();
    }

    /**
     * Closes this non-default context and releases local state.
     */
    @Override
    public void close() {
        if (defaultContext) {
            Logger.warn(true, "Browser", "Default browser context close rejected: contextId={}", id);
            throw new IllegalStateException("The default browser context cannot be closed directly.");
        }
        if (closed.compareAndSet(false, true)) {
            Logger.debug(
                    true,
                    "Browser",
                    "Browser context close requested: contextId={}, targetCount={}",
                    id,
                    targets.size());
            if (browser instanceof CdpBrowser cdpBrowser) {
                cdpBrowser.disposeContext(this);
            }
            for (CdpTarget target : List.copyOf(targets)) {
                emit(ContextEvent.TARGET_DESTROYED, target);
            }
            pages.clear();
            targets.clear();
            permissions.clear();
            permissionSettings.clear();
            cookies.clear();
            Logger.debug(false, "Browser", "Browser context closed: contextId={}", id);
        }
    }

    /**
     * Handles ensure open.
     */
    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Browser context has been closed.");
        }
    }

    /**
     * Returns the copy cookies.
     *
     * @param source source value
     * @return values
     */
    private List<Map<String, Object>> copyCookies(List<Map<String, Object>> source) {
        List<Map<String, Object>> copied = new ArrayList<>();
        for (Map<String, Object> cookie : source) {
            copied.add(Map.copyOf(cookie));
        }
        return copied;
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
        String actualTargetId = org.miaixz.bus.core.xyz.StringKit.isBlank(targetId) ? "page-" + page.hashCode()
                : targetId;
        TargetInfo targetInfo = new TargetInfo(actualTargetId, "page", Builder.ABOUT_BLANK,
                sessionId == null ? Normal.EMPTY : sessionId);
        CdpTarget target = new CdpPageTarget(targetInfo, null, page);
        CdpTarget.Internal.setSession(target, session);
        CdpTarget.Internal.bindBrowserContext(target, this);
        targets.removeIf(item -> actualTargetId.equals(CdpTarget.Internal.targetInfo(item).getTargetId()));
        targets.add(target);
        CdpPage.Internal.bindBrowserContext(page, this);
        CdpPage.Internal.bindTarget(page, target);
        emit(ContextEvent.TARGET_CREATED, target);
        Logger.debug(
                false,
                "Target",
                "Context page target registered: contextId={}, targetId={}, sessionId={}",
                id,
                actualTargetId,
                sessionId == null ? Normal.EMPTY : sessionId);
    }

    /**
     * Returns the matches delete filter.
     *
     * @param cookie cookie value
     * @param filter filter value
     * @return {@code true} when the condition matches
     */
    private boolean matchesDeleteFilter(Map<String, Object> cookie, Map<String, Object> filter) {
        Object expectedName = filter.get("name");
        if (expectedName != null && !expectedName.equals(cookie.get("name"))) {
            return false;
        }
        Object expectedDomain = filter.get("domain");
        if (expectedDomain != null && !expectedDomain.equals(cookie.get("domain"))) {
            return false;
        }
        Object expectedPath = filter.get("path");
        if (expectedPath != null && !expectedPath.equals(cookie.get("path"))) {
            return false;
        }
        Object expectedUrl = filter.get("url");
        return expectedUrl == null || expectedUrl.equals(cookie.get("url"));
    }

    /**
     * Handles sleep target poll interval.
     */
    private void sleepTargetPollInterval() {
        Awaitable.sleep(TARGET_POLL_INTERVAL.toMillis(), "Interrupted while waiting for context target.");
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return completion future
     */
    private CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        if (connection == null || !connection.hasConfiguredTransport()) {
            Logger.debug(true, "Protocol", "Context protocol command skipped: contextId={}, method={}", id, method);
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        Logger.debug(true, "Protocol", "Context protocol command requested: contextId={}, method={}", id, method);
        return connection.send(method, params);
    }

    /**
     * Validates a permission origin before granting browser permissions.
     *
     * @param origin permission origin
     */
    private void validatePermissionOrigin(String origin) {
        SecurityPolicy.defaultPolicy().validateHttpUrl(URI.create(Assert.notBlank(origin, "origin")));
    }

    /**
     * Returns the context params.
     *
     * @return mapped values
     */
    private Map<String, Object> contextParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        if (!defaultContext) {
            params.put("browserContextId", id);
        }
        return params;
    }

    /**
     * Provides internal browser context collaboration without creating a standalone helper type.
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
         * Adds a page to a context.
         *
         * @param context context
         * @param page    page
         */
        public static void addPage(CdpBrowserContext context, Page page) {
            context.addPage(page);
        }

        /**
         * Adds a page with target metadata to a context.
         *
         * @param context   context
         * @param page      page
         * @param targetId  target id
         * @param sessionId session id
         * @param session   CDP session
         */
        public static void addPage(
                CdpBrowserContext context,
                Page page,
                String targetId,
                String sessionId,
                CDPSession session) {
            context.addPage(page, targetId, sessionId, session);
        }

        /**
         * Adds a target to a context.
         *
         * @param context context
         * @param target  target
         * @return {@code true} when added
         */
        public static boolean addTarget(CdpBrowserContext context, CdpTarget target) {
            return context.addTarget(target);
        }

        /**
         * Removes a target from a context.
         *
         * @param context context
         * @param target  target
         * @return {@code true} when removed
         */
        public static boolean removeTarget(CdpBrowserContext context, CdpTarget target) {
            return context.removeTarget(target);
        }

        /**
         * Binds a CDP connection to a context.
         *
         * @param context    context
         * @param connection connection
         */
        public static void bindConnection(CdpBrowserContext context, Connection connection) {
            context.bindConnection(connection);
        }

        /**
         * Binds a browser to a context.
         *
         * @param context context
         * @param browser browser
         */
        public static void bindBrowser(CdpBrowserContext context, Browser browser) {
            context.bindBrowser(browser);
        }

        /**
         * Updates download behavior.
         *
         * @param context      browser context
         * @param downloadPath download path
         */
        public static void setDownloadBehavior(CdpBrowserContext context, Path downloadPath) {
            context.setDownloadBehavior(downloadPath);
        }

        /**
         * Updates download behavior.
         *
         * @param context  browser context
         * @param behavior behavior
         * @return protocol result
         */
        public static CompletableFuture<CdpPayload> setDownloadBehavior(
                CdpBrowserContext context,
                DownloadBehavior behavior) {
            return context.setDownloadBehavior(behavior);
        }

        /**
         * Returns the configured download path.
         *
         * @param context context
         * @return download path
         */
        public static Optional<Path> downloadPath(CdpBrowserContext context) {
            return context.downloadPath();
        }

        /**
         * Returns typed cookies.
         *
         * @param context context
         * @param urls    URL filters
         * @return cookie list
         */
        public static List<Cookie> cookieObjects(CdpBrowserContext context, String... urls) {
            return context.cookieObjects(urls);
        }

        /**
         * Returns permission overrides.
         *
         * @param context context
         * @return permission overrides
         */
        public static Map<String, List<String>> permissionOverrides(CdpBrowserContext context) {
            return context.permissionOverrides();
        }

        /**
         * Returns permission settings.
         *
         * @param context context
         * @return permission settings
         */
        public static Map<String, Map<String, String>> permissionSettings(CdpBrowserContext context) {
            return context.permissionSettings();
        }

        /**
         * Returns whether this context is the default browser context.
         *
         * @param context browser context
         * @return default state
         */
        public static boolean isDefaultContext(CdpBrowserContext context) {
            return context.isDefaultContext();
        }

        /**
         * Returns the active screenshot operation count.
         *
         * @param context context
         * @return operation count
         */
        public static int screenshotOperationsCount(CdpBrowserContext context) {
            return context.screenshotOperationsCount();
        }
    }

}
