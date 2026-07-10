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

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Context;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.kernel.bidi.page.BidiPage;
import org.miaixz.lancia.kernel.bidi.protocol.message.BidiBrowsingContextInfoMessage;
import org.miaixz.lancia.kernel.bidi.targets.BidiTarget;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.browser.PageCreateType;
import org.miaixz.lancia.nimble.network.CookieParam;
import org.miaixz.lancia.nimble.network.DeleteCookiesParameters;
import org.miaixz.lancia.nimble.network.DownloadBehavior;
import org.miaixz.lancia.options.CreatePageOptions;
import org.miaixz.lancia.options.PermissionOptions;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Represents a BiDi browser context.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiBrowserContext implements Context {

    /**
     * Current browser.
     */
    private final BidiBrowser browser;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Whether default context is enabled.
     */
    private final boolean defaultContext;
    /**
     * Registered pages values.
     */
    private final List<BidiPage> pages = new ArrayList<>();
    /**
     * Mapped permissions values.
     */
    private final Map<String, Map<String, String>> permissions = new LinkedHashMap<>();
    /**
     * Mapped cookies values.
     */
    private final List<Map<String, Object>> cookies = new ArrayList<>();
    /**
     * Thread-safe closed state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a bidi browser context.
     *
     * @param browser        browser instance
     * @param id             identifier
     * @param defaultContext default context
     */
    public BidiBrowserContext(BidiBrowser browser, String id, boolean defaultContext) {
        this.browser = browser;
        this.id = StringKit.isBlank(id) ? Normal.DEFAULT : id;
        this.defaultContext = defaultContext;
        Logger.debug(
                false,
                "Browser",
                "BiDi browser context initialized: contextId={}, default={}",
                this.id,
                defaultContext);
        installDefaultDownloadBoundary();
    }

    /**
     * Returns the new page.
     *
     * @return new page value
     */
    public synchronized BidiPage newPage() {
        ensureOpen();
        Logger.debug(true, "Page", "BiDi page create requested: contextId={}, default={}", id, defaultContext);
        try {
            Map<String, Object> params = defaultContext ? Map.of("type", "tab")
                    : Map.of("type", "tab", "userContext", id);
            BidiBrowsingContextInfoMessage info = BidiBrowsingContextInfoMessage
                    .from(browser.session().send("browsingContext.create", params).get(5, TimeUnit.SECONDS));
            BidiPage page = new BidiPage(this, info.context(), info.url());
            pages.add(page);
            Logger.debug(
                    false,
                    "Page",
                    "BiDi page created: contextId={}, pageId={}, total={}",
                    id,
                    info.context(),
                    pages.size());
            return page;
        } catch (Exception ex) {
            BidiPage page = new BidiPage(this, "context-" + (pages.size() + 1), Builder.ABOUT_BLANK);
            pages.add(page);
            Logger.warn(
                    false,
                    "Page",
                    ex,
                    "BiDi page create fell back to local page: contextId={}, pageId={}",
                    id,
                    page.contextId());
            return page;
        }
    }

    /**
     * Returns the new page.
     *
     * @param options operation options
     * @return new page value
     */
    public synchronized BidiPage newPage(CreatePageOptions options) {
        CreatePageOptions actualOptions = options == null ? new CreatePageOptions() : options;
        String type = actualOptions.getType() == PageCreateType.WINDOW ? "window" : "tab";
        return newPage(type, actualOptions.getBackground());
    }

    /**
     * Returns the new page.
     *
     * @param type       type to use
     * @param background background value
     * @return new page value
     */
    public synchronized BidiPage newPage(String type, Boolean background) {
        ensureOpen();
        Logger.debug(
                true,
                "Page",
                "BiDi page create requested: contextId={}, type={}, background={}",
                id,
                type,
                background);
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("type", StringKit.isBlank(type) ? "tab" : type);
            if (!defaultContext) {
                params.put("userContext", id);
            }
            if (background != null) {
                params.put("background", background);
            }
            BidiBrowsingContextInfoMessage info = BidiBrowsingContextInfoMessage
                    .from(browser.session().send("browsingContext.create", params).get(5, TimeUnit.SECONDS));
            BidiPage page = new BidiPage(this, info.context(), info.url());
            pages.add(page);
            Logger.debug(
                    false,
                    "Page",
                    "BiDi page created: contextId={}, pageId={}, total={}",
                    id,
                    info.context(),
                    pages.size());
            return page;
        } catch (Exception ex) {
            BidiPage page = new BidiPage(this, "context-" + (pages.size() + 1), Builder.ABOUT_BLANK);
            pages.add(page);
            Logger.warn(
                    false,
                    "Page",
                    ex,
                    "BiDi page create fell back to local page: contextId={}, pageId={}",
                    id,
                    page.contextId());
            return page;
        }
    }

    /**
     * Returns the pages.
     *
     * @return values
     */
    public synchronized java.util.List<Page> pages() {
        return pages.stream().map(page -> (Page) page).toList();
    }

    /**
     * Returns internal BiDi pages.
     *
     * @return values
     */
    public synchronized List<BidiPage> bidiPages() {
        return List.copyOf(pages);
    }

    /**
     * Returns the pages.
     *
     * @param includeAll include all value
     * @return values
     */
    public synchronized java.util.List<Page> pages(boolean includeAll) {
        return pages();
    }

    /**
     * Removes page.
     *
     * @param page page instance
     */
    public synchronized void removePage(BidiPage page) {
        pages.remove(page);
        Logger.debug(
                false,
                "Page",
                "BiDi page removed from context: contextId={}, pageId={}, remaining={}",
                id,
                page == null ? Normal.EMPTY : page.contextId(),
                pages.size());
    }

    /**
     * Handles override permissions.
     *
     * @param origin      origin value
     * @param permissions permissions value
     */
    public synchronized CompletableFuture<CdpPayload> overridePermissions(String origin, List<String> permissions) {
        ensureOpen();
        String actualOrigin = org.miaixz.bus.core.lang.Assert.notBlank(origin, "origin");
        validatePermissionOrigin(actualOrigin);
        Logger.debug(
                true,
                "Browser",
                "BiDi permissions override requested: contextId={}, origin={}, count={}",
                id,
                actualOrigin,
                permissions == null ? Normal._0 : permissions.size());
        Map<String, String> values = this.permissions.computeIfAbsent(actualOrigin, key -> new LinkedHashMap<>());
        if (permissions != null) {
            for (String permission : permissions) {
                values.put(permission, "granted");
                setRemotePermission(actualOrigin, permission, "granted");
            }
        }
        Logger.debug(
                false,
                "Browser",
                "BiDi permissions override completed: contextId={}, origin={}, count={}",
                id,
                actualOrigin,
                values.size());
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Updates permission.
     *
     * @param origin     origin
     * @param permission permission
     * @param state      state
     */
    public synchronized void setPermission(String origin, String permission, String state) {
        ensureOpen();
        String actualOrigin = org.miaixz.bus.core.lang.Assert.notBlank(origin, "origin");
        validatePermissionOrigin(actualOrigin);
        String actualPermission = org.miaixz.bus.core.lang.Assert.notBlank(permission, "permission");
        String actualState = org.miaixz.bus.core.lang.Assert.notBlank(state, "state");
        Logger.debug(
                true,
                "Browser",
                "BiDi permission update requested: contextId={}, origin={}, permission={}",
                id,
                actualOrigin,
                actualPermission);
        permissions.computeIfAbsent(actualOrigin, key -> new LinkedHashMap<>()).put(actualPermission, actualState);
        setRemotePermission(actualOrigin, actualPermission, actualState);
        Logger.debug(
                false,
                "Browser",
                "BiDi permission updated: contextId={}, origin={}, permission={}, state={}",
                id,
                actualOrigin,
                actualPermission,
                actualState);
    }

    /**
     * Updates permission.
     *
     * @param origin  origin
     * @param options permission options
     * @return completion future
     */
    public synchronized CompletableFuture<CdpPayload> setPermission(String origin, PermissionOptions options) {
        return setPermission(origin, new PermissionOptions[] { options });
    }

    /**
     * Updates permission.
     *
     * @param origin      origin
     * @param permissions permissions
     */
    public synchronized CompletableFuture<CdpPayload> setPermission(String origin, PermissionOptions... permissions) {
        ensureOpen();
        String actualOrigin = org.miaixz.bus.core.lang.Assert.notBlank(origin, "origin");
        validatePermissionOrigin(actualOrigin);
        List<String> origins = Symbol.STAR.equals(actualOrigin) ? knownOrigins() : List.of(actualOrigin);
        if (origins.isEmpty()) {
            origins = List.of(actualOrigin);
        }
        if (permissions == null) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        for (PermissionOptions permission : permissions) {
            PermissionOptions actualPermission = org.miaixz.bus.core.lang.Assert.notNull(permission, "permission");
            String name = org.miaixz.bus.core.lang.Assert.notBlank(actualPermission.getName(), "permission.name");
            String state = StringKit.isBlank(actualPermission.getSetting()) ? "granted" : actualPermission.getSetting();
            for (String targetOrigin : origins) {
                this.permissions.computeIfAbsent(targetOrigin, key -> new LinkedHashMap<>()).put(name, state);
                setRemotePermission(targetOrigin, actualPermission.toDescriptor(), state);
            }
        }
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Handles clear permission overrides.
     */
    public synchronized CompletableFuture<CdpPayload> clearPermissionOverrides() {
        Logger.debug(
                true,
                "Browser",
                "BiDi permissions clear requested: contextId={}, origins={}",
                id,
                permissions.size());
        for (Map.Entry<String, Map<String, String>> origin : permissions.entrySet()) {
            for (String permission : origin.getValue().keySet()) {
                setRemotePermission(origin.getKey(), permission, "prompt");
            }
        }
        permissions.clear();
        Logger.debug(false, "Browser", "BiDi permissions cleared: contextId={}", id);
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Returns the permission overrides.
     *
     * @return mapped values
     */
    public synchronized Map<String, Map<String, String>> permissionOverrides() {
        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : permissions.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    /**
     * Returns cookie maps.
     *
     * @param urls URL filters
     * @return cookies
     */
    public synchronized List<Map<String, Object>> cookies(String... urls) {
        return cookies();
    }

    /**
     * Updates cookie.
     *
     * @param cookies cookies to use
     * @return completion future
     */
    public synchronized CompletableFuture<CdpPayload> setCookie(CookieParam... cookies) {
        if (cookies != null) {
            for (CookieParam cookie : cookies) {
                if (cookie != null) {
                    setCookie(cookie.toMap());
                }
            }
        }
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Deletes cookies.
     *
     * @param parameters cookie filters
     * @return completion future
     */
    public synchronized CompletableFuture<CdpPayload> deleteCookie(DeleteCookiesParameters... parameters) {
        if (parameters != null) {
            for (DeleteCookiesParameters parameter : parameters) {
                if (parameter != null && parameter.getName() != null) {
                    deleteCookie(parameter.getName());
                }
            }
        }
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Deletes matching cookies.
     *
     * @param predicate cookie predicate
     */
    public synchronized void deleteMatchingCookies(java.util.function.Predicate<Map<String, Object>> predicate) {
        if (predicate == null) {
            return;
        }
        for (Map<String, Object> cookie : List.copyOf(cookies)) {
            if (predicate.test(cookie)) {
                Object name = cookie.get("name");
                if (name != null) {
                    deleteCookie(String.valueOf(name));
                }
            }
        }
    }

    /**
     * Returns targets in this context.
     *
     * @return targets
     */
    public java.util.List<? extends Target> targets() {
        return bidiPages().stream().map(BidiTarget::page).toList();
    }

    /**
     * Waits for a target.
     */
    public Target waitForTarget(java.util.function.Predicate<Target> predicate, java.time.Duration timeout) {
        return browser.waitForTarget(
                target -> targets().contains(target) && (predicate == null || predicate.test(target)),
                timeout);
    }

    /**
     * Waits for a target.
     */
    public Target waitForTarget(java.util.function.Predicate<Target> predicate) {
        return waitForTarget(predicate, java.time.Duration.ofSeconds(30L));
    }

    /**
     * Returns the cookies.
     *
     * @return values
     */
    public synchronized List<Map<String, Object>> cookies() {
        return copyCookies(cookies);
    }

    /**
     * Updates cookie.
     *
     * @param cookie cookie value
     */
    public synchronized void setCookie(Map<String, Object> cookie) {
        ensureOpen();
        Map<String, Object> actualCookie = new LinkedHashMap<>(
                org.miaixz.bus.core.lang.Assert.notNull(cookie, "cookie"));
        Logger.debug(true, "Network", "BiDi cookie set requested: contextId={}, name={}", id, actualCookie.get("name"));
        cookies.removeIf(item -> java.util.Objects.equals(item.get("name"), actualCookie.get("name")));
        cookies.add(Map.copyOf(actualCookie));
        browser.session().send("storage.setCookie", Map.of("cookie", actualCookie, "userContext", id));
        Logger.debug(false, "Network", "BiDi cookie cached: contextId={}, total={}", id, cookies.size());
    }

    /**
     * Handles delete cookie.
     *
     * @param name name to use
     */
    public synchronized void deleteCookie(String name) {
        ensureOpen();
        Logger.debug(true, "Network", "BiDi cookie delete requested: contextId={}, name={}", id, name);
        cookies.removeIf(item -> java.util.Objects.equals(name, item.get("name")));
        browser.session().send("storage.deleteCookies", Map.of("filter", Map.of("name", name), "userContext", id));
        Logger.debug(
                false,
                "Network",
                "BiDi cookie deleted from cache: contextId={}, remaining={}",
                id,
                cookies.size());
    }

    /**
     * Closes this object and releases its resources.
     */
    public synchronized void close() {
        if (defaultContext) {
            throw new InternalException("Default BiDi BrowserContext cannot be closed.");
        }
        if (closed.compareAndSet(false, true)) {
            Logger.debug(
                    true,
                    "Browser",
                    "BiDi browser context close requested: contextId={}, pages={}",
                    id,
                    pages.size());
            for (BidiPage page : List.copyOf(pages)) {
                page.close();
            }
            browser.session().send("browser.removeUserContext", Map.of("userContext", id));
            pages.clear();
            permissions.clear();
            cookies.clear();
            browser.removeBrowserContext(this);
            Logger.debug(false, "Browser", "BiDi browser context closed: contextId={}", id);
        }
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
     * @return {@code true} when the condition matches
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Returns whether this object is closed.
     *
     * @return closed state
     */
    public boolean closed() {
        return isClosed();
    }

    /**
     * Returns whether default context is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isDefaultContext() {
        return defaultContext;
    }

    /**
     * Handles ensure open.
     */
    private void ensureOpen() {
        if (closed.get()) {
            throw new InternalException("BiDi BrowserContext has been closed.");
        }
    }

    /**
     * Updates remote permission.
     *
     * @param origin     origin value
     * @param permission permission value
     * @param state      state value
     */
    private void setRemotePermission(String origin, String permission, String state) {
        setRemotePermission(origin, Map.of("name", permission), state);
    }

    /**
     * Updates remote permission.
     *
     * @param origin     origin value
     * @param descriptor descriptor value
     * @param state      state value
     */
    private void setRemotePermission(String origin, Map<String, Object> descriptor, String state) {
        browser.session().send(
                "permissions.setPermission",
                Map.of("origin", origin, "descriptor", descriptor, "state", state, "userContext", id));
    }

    /**
     * Installs the default download deny behavior for the browser context.
     */
    private void installDefaultDownloadBoundary() {
        browser.session().browser().setDownloadBehavior(
                CdpPayload.of(Map.of("userContext", id, "downloadBehavior", DownloadBehavior.deny().toCommonMap())))
                .exceptionally(throwable -> {
                    Logger.warn(false, "Browser", throwable, "BiDi download deny boundary skipped: contextId={}", id);
                    return CdpPayload.NULL;
                });
    }

    /**
     * Validates a permission origin before granting browser permissions.
     *
     * @param origin permission origin
     */
    private void validatePermissionOrigin(String origin) {
        SecurityPolicy.defaultPolicy()
                .validateHttpUrl(URI.create(org.miaixz.bus.core.lang.Assert.notBlank(origin, "origin")));
    }

    /**
     * Returns the known origins.
     *
     * @return values
     */
    private List<String> knownOrigins() {
        List<String> result = new ArrayList<>();
        for (BidiPage page : pages) {
            String origin = originOf(page.url());
            if (StringKit.isNotBlank(origin) && !result.contains(origin)) {
                result.add(origin);
            }
        }
        return result;
    }

    /**
     * Returns the origin of.
     *
     * @param url target URL
     * @return origin of value
     */
    private String originOf(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (StringKit.isBlank(uri.getScheme()) || StringKit.isBlank(uri.getHost())) {
                return Normal.EMPTY;
            }
            int port = uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + (port < 0 ? Normal.EMPTY : Symbol.COLON + port);
        } catch (RuntimeException ex) {
            return Normal.EMPTY;
        }
    }

    /**
     * Returns the copy cookies.
     *
     * @param source source value
     * @return values
     */
    private List<Map<String, Object>> copyCookies(List<Map<String, Object>> source) {
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> item : source) {
            copy.add(Map.copyOf(item));
        }
        return List.copyOf(copy);
    }

}
