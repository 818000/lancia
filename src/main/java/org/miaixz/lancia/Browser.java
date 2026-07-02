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
package org.miaixz.lancia;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.miaixz.lancia.events.BrowserEvent;
import org.miaixz.lancia.nimble.browser.WindowBounds;
import org.miaixz.lancia.nimble.network.Cookie;
import org.miaixz.lancia.nimble.network.CookieParam;
import org.miaixz.lancia.nimble.network.DeleteCookiesParameters;
import org.miaixz.lancia.nimble.screen.AddScreenParams;
import org.miaixz.lancia.nimble.screen.ScreenInfo;
import org.miaixz.lancia.options.BrowserContextOptions;
import org.miaixz.lancia.options.CreatePageOptions;
import org.miaixz.lancia.options.ExtensionInstallOptions;
import org.miaixz.lancia.options.PermissionOptions;

/**
 * Public browser API for contexts, pages, targets, extensions, and connection lifecycle.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Browser extends Emitter<BrowserEvent>, AutoCloseable {

    /**
     * Returns the connected.
     *
     * @return {@code true} when the condition matches
     */
    boolean connected();

    /**
     * Returns the launched process.
     *
     * @return process or {@code null}
     */
    Process process();

    /**
     * Returns the browser WebSocket endpoint.
     *
     * @return endpoint value
     */
    String wsEndpoint();

    /**
     * Creates a new page.
     *
     * @return page instance
     */
    Page newPage();

    /**
     * Creates a new page.
     *
     * @param options page creation options
     * @return page instance
     */
    Page newPage(CreatePageOptions options);

    /**
     * Returns browser pages.
     *
     * @return page list
     */
    List<Page> pages();

    /**
     * Returns browser contexts.
     *
     * @return browser context list
     */
    List<? extends Context> browserContexts();

    /**
     * Returns the default browser context.
     *
     * @return default browser context
     */
    Context defaultBrowserContext();

    /**
     * Creates a browser context.
     *
     * @param options browser context options
     * @return browser context
     */
    Context createBrowserContext(BrowserContextOptions options);

    /**
     * Creates a browser context.
     *
     * @return browser context
     */
    Context createBrowserContext();

    /**
     * Installs an extension.
     *
     * @param path extension path
     * @return extension id future
     */
    default CompletableFuture<String> installExtension(String path) {
        return installExtension(path, null);
    }

    /**
     * Installs an extension.
     *
     * @param path    extension path
     * @param options install options
     * @return extension id future
     */
    CompletableFuture<String> installExtension(String path, ExtensionInstallOptions options);

    /**
     * Installs an extension.
     *
     * @param path extension path
     * @return extension id future
     */
    default CompletableFuture<String> installExtension(Path path) {
        return installExtension(path, null);
    }

    /**
     * Installs an extension.
     *
     * @param path    extension path
     * @param options install options
     * @return extension id future
     */
    CompletableFuture<String> installExtension(Path path, ExtensionInstallOptions options);

    /**
     * Uninstalls an extension.
     *
     * @param id extension id
     * @return completion future
     */
    CompletableFuture<Void> uninstallExtension(String id);

    /**
     * Returns installed extensions.
     *
     * @return extension map
     */
    Map<String, ? extends Extension> extensions();

    /**
     * Returns emulated screens.
     *
     * @return screen list
     */
    List<ScreenInfo> screens();

    /**
     * Adds an emulated screen.
     *
     * @param params screen parameters
     * @return screen info
     */
    ScreenInfo addScreen(AddScreenParams params);

    /**
     * Removes an emulated screen.
     *
     * @param screenId screen id
     */
    void removeScreen(String screenId);

    /**
     * Returns targets.
     *
     * @return target list
     */
    List<? extends Target> targets();

    /**
     * Returns the browser target.
     *
     * @return browser target
     */
    Target target();

    /**
     * Waits for a target.
     *
     * @param predicate target predicate
     * @param timeout   timeout value
     * @return matched target
     */
    Target waitForTarget(Predicate<Target> predicate, Duration timeout);

    /**
     * Waits for a target.
     *
     * @param predicate target predicate
     * @return matched target
     */
    Target waitForTarget(Predicate<Target> predicate);

    /**
     * Returns the browser version.
     *
     * @return browser version
     */
    String version();

    /**
     * Returns the browser user agent.
     *
     * @return user agent
     */
    String userAgent();

    /**
     * Returns protocol debug details.
     *
     * @return debug info map
     */
    Map<String, Object> debugInfo();

    /**
     * Returns the protocol name.
     *
     * @return protocol name
     */
    String protocol();

    /**
     * Returns whether network tracking is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isNetworkEnabled();

    /**
     * Returns whether issue tracking is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isIssuesEnabled();

    /**
     * Returns cookies.
     *
     * @param urls URL filters
     * @return cookie list
     */
    List<Cookie> cookies(String... urls);

    /**
     * Updates cookie.
     *
     * @param cookies cookies to use
     */
    void setCookie(CookieParam... cookies);

    /**
     * Deletes cookies.
     *
     * @param cookies cookie parameters
     */
    void deleteCookie(DeleteCookiesParameters... cookies);

    /**
     * Deletes matching cookies.
     *
     * @param predicate cookie predicate
     */
    void deleteMatchingCookies(Predicate<Map<String, Object>> predicate);

    /**
     * Updates permission.
     *
     * @param origin      origin value
     * @param permissions permissions value
     */
    void setPermission(String origin, PermissionOptions... permissions);

    /**
     * Returns window bounds.
     *
     * @param windowId window id
     * @return window bounds
     */
    WindowBounds getWindowBounds(String windowId);

    /**
     * Updates window bounds.
     *
     * @param windowId window ID value
     * @param bounds   bounds value
     */
    void setWindowBounds(String windowId, WindowBounds bounds);

    /**
     * Disconnects the browser.
     */
    void disconnect();

    /**
     * Closes the browser.
     */
    @Override
    void close();

}
