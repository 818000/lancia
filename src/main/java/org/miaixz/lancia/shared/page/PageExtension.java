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
package org.miaixz.lancia.shared.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Extension;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.Worker;
import org.miaixz.lancia.nimble.browser.TargetType;
import org.miaixz.lancia.shared.payload.PayloadExtensionInfo;

/**
 * page extension implementation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class PageExtension implements Extension {

    /**
     * Current info.
     */
    private final PayloadExtensionInfo info;
    /**
     * Current browser.
     */
    private final Browser browser;
    /**
     * Current connection.
     */
    private final Session session;

    /**
     * Creates a page extension.
     *
     * @param info    info
     * @param browser browser instance
     * @param session protocol session
     */
    public PageExtension(PayloadExtensionInfo info, Browser browser, Session session) {
        this.info = Assert.notNull(info, "info");
        this.browser = browser;
        this.session = session;
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return info.id();
    }

    /**
     * Returns the ID.
     *
     * @return ID
     */
    public String getId() {
        return id();
    }

    /**
     * Returns the version.
     *
     * @return version value
     */
    public String version() {
        return info.version();
    }

    /**
     * Returns the version.
     *
     * @return version
     */
    public String getVersion() {
        return version();
    }

    /**
     * Returns the name.
     *
     * @return name value
     */
    public String name() {
        return info.name();
    }

    /**
     * Returns the name.
     *
     * @return name
     */
    public String getName() {
        return name();
    }

    /**
     * Returns the path.
     *
     * @return path value
     */
    public String path() {
        return info.path();
    }

    /**
     * Returns the path.
     *
     * @return path
     */
    public String getPath() {
        return path();
    }

    /**
     * Returns the enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean enabled() {
        return info.enabled();
    }

    /**
     * Returns the enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean getEnabled() {
        return enabled();
    }

    /**
     * Returns whether this feature is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isEnabled() {
        return enabled();
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
     * Returns the info.
     *
     * @return info value
     */
    public PayloadExtensionInfo info() {
        return info;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        return info.toMap();
    }

    /**
     * Returns the workers.
     *
     * @return values
     */
    public List<Worker> workers() {
        List<Worker> result = new ArrayList<>();
        if (browser == null) {
            return result;
        }
        for (Target target : browser.targets()) {
            if (isExtensionTarget(target, TargetType.SERVICE_WORKER)) {
                try {
                    target.worker().ifPresent(result::add);
                } catch (RuntimeException ex) {
                    if (!canIgnoreError(ex)) {
                        throw ex;
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns the pages.
     *
     * @return values
     */
    public List<Page> pages() {
        List<Page> result = new ArrayList<>();
        if (browser == null) {
            return result;
        }
        for (Target target : browser.targets()) {
            if (isExtensionPageTarget(target)) {
                try {
                    result.add(target.asPage());
                } catch (RuntimeException ex) {
                    if (!canIgnoreError(ex)) {
                        throw ex;
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns the trigger action.
     *
     * @param page page instance
     * @return completion future
     */
    public CompletableFuture<Void> triggerAction(Page page) {
        Assert.notNull(page, "page");
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        return session.send("Extensions.triggerAction", Map.of("id", id(), "targetId", page.target().id()))
                .thenApply(payload -> null);
    }

    /**
     * Returns whether extension target is enabled.
     *
     * @param target target object
     * @param type   type to use
     * @return {@code true} when the condition matches
     */
    private boolean isExtensionTarget(Target target, TargetType type) {
        if (target == null || target.type() != type) {
            return false;
        }
        return target.url().startsWith(extensionUrlPrefix());
    }

    /**
     * Returns whether extension page target is enabled.
     *
     * @param target target object
     * @return {@code true} when the condition matches
     */
    private boolean isExtensionPageTarget(Target target) {
        if (target == null) {
            return false;
        }
        TargetType type = target.type();
        return (type == TargetType.PAGE || type == TargetType.BACKGROUND_PAGE)
                && target.url().startsWith(extensionUrlPrefix());
    }

    /**
     * Returns the extension URL prefix.
     *
     * @return extension URL prefix value
     */
    private String extensionUrlPrefix() {
        return "chrome-extension://" + id();
    }

    /**
     * Returns the can ignore error.
     *
     * @param error error to propagate
     * @return {@code true} when the condition matches
     */
    private boolean canIgnoreError(Throwable error) {
        String message = error == null ? Normal.EMPTY : StringKit.toString(error.getMessage());
        return message.contains("Target closed") || message.contains("target closed")
                || message.contains("No target with given id found");
    }

}
