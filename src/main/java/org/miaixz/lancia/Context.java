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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.miaixz.lancia.nimble.network.CookieParam;
import org.miaixz.lancia.nimble.network.DeleteCookiesParameters;
import org.miaixz.lancia.options.CreatePageOptions;
import org.miaixz.lancia.options.PermissionOptions;

/**
 * Public browser context API for isolated pages, permissions, cookies, and download behavior.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Context extends AutoCloseable {

    /**
     * Creates a new page.
     *
     * @return page
     */
    Page newPage();

    /**
     * Creates a new page.
     *
     * @param options page options
     * @return page
     */
    Page newPage(CreatePageOptions options);

    /**
     * Returns pages in this context.
     *
     * @return page list
     */
    List<Page> pages();

    /**
     * Returns pages in this context.
     *
     * @param includeAll include all pages
     * @return page list
     */
    List<Page> pages(boolean includeAll);

    /**
     * Returns targets in this context.
     *
     * @return target list
     */
    List<? extends Target> targets();

    /**
     * Waits for a target.
     *
     * @param predicate target predicate
     * @param timeout   timeout
     * @return target
     */
    Target waitForTarget(Predicate<Target> predicate, java.time.Duration timeout);

    /**
     * Waits for a target.
     *
     * @param predicate target predicate
     * @return target
     */
    Target waitForTarget(Predicate<Target> predicate);

    /**
     * Overrides permissions.
     *
     * @param origin      origin
     * @param permissions permissions
     * @return protocol result
     */
    CompletableFuture<? extends Payload> overridePermissions(String origin, List<String> permissions);

    /**
     * Updates permission.
     *
     * @param origin     origin value
     * @param permission permission value
     * @param setting    setting value
     */
    void setPermission(String origin, String permission, String setting);

    /**
     * Updates permission.
     *
     * @param origin  origin value
     * @param options operation options
     * @return completion future
     */
    CompletableFuture<? extends Payload> setPermission(String origin, PermissionOptions options);

    /**
     * Updates permission.
     *
     * @param origin      origin value
     * @param permissions permissions value
     * @return completion future
     */
    CompletableFuture<? extends Payload> setPermission(String origin, PermissionOptions... permissions);

    /**
     * Clears permission overrides.
     *
     * @return protocol result
     */
    CompletableFuture<? extends Payload> clearPermissionOverrides();

    /**
     * Returns cookie maps.
     *
     * @param urls URL filters
     * @return cookies
     */
    List<Map<String, Object>> cookies(String... urls);

    /**
     * Updates cookie.
     *
     * @param cookies cookies to use
     * @return completion future
     */
    CompletableFuture<? extends Payload> setCookie(CookieParam... cookies);

    /**
     * Deletes cookies.
     *
     * @param parameters cookie filters
     * @return protocol result
     */
    CompletableFuture<? extends Payload> deleteCookie(DeleteCookiesParameters... parameters);

    /**
     * Deletes matching cookies.
     *
     * @param predicate cookie predicate
     */
    void deleteMatchingCookies(Predicate<Map<String, Object>> predicate);

    /**
     * Returns the owning browser.
     *
     * @return browser
     */
    Browser browser();

    /**
     * Returns the context id.
     *
     * @return context id
     */
    String id();

    /**
     * Returns whether this object is closed.
     *
     * @return {@code true} when the condition matches
     */
    boolean isClosed();

    /**
     * Returns the closed.
     *
     * @return {@code true} when the condition matches
     */
    boolean closed();

    /**
     * Closes the context.
     */
    @Override
    void close();

}
