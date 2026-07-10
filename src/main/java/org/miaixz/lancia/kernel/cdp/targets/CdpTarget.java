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
package org.miaixz.lancia.kernel.cdp.targets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.Worker;
import org.miaixz.lancia.kernel.cdp.browser.CdpBrowserContext;
import org.miaixz.lancia.kernel.cdp.page.CdpPage;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.kernel.cdp.session.TargetInfo;
import org.miaixz.lancia.nimble.browser.TargetType;

/**
 * page target implementation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpTarget implements Target {

    /**
     * Current target info.
     */
    private TargetInfo targetInfo;
    /**
     * Current opener.
     */
    private final CdpTarget opener;
    /**
     * Registered child targets values.
     */
    private final List<CdpTarget> childTargets = new ArrayList<>();
    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Current as page.
     */
    private Page asPage;
    /**
     * Current initialized.
     */
    private final CompletableFuture<InitializationStatus> initialized = new CompletableFuture<>();
    /**
     * Current closed.
     */
    private final CompletableFuture<Void> closed = new CompletableFuture<>();
    /**
     * Thread-safe initialization completed state.
     */
    private final AtomicBoolean initializationCompleted = new AtomicBoolean(false);
    /**
     * Current browser.
     */
    private Browser browser;
    /**
     * Current browser context.
     */
    private CdpBrowserContext browserContext;

    /**
     * Creates a page target.
     *
     * @param targetInfo target info
     * @param opener     opener
     */
    public CdpTarget(TargetInfo targetInfo, CdpTarget opener) {
        this.targetInfo = Assert.notNull(targetInfo, "targetInfo");
        this.opener = opener;
    }

    /**
     * Returns the page.
     *
     * @return optional value
     */
    public Optional<Page> page() {
        return Optional.empty();
    }

    /**
     * Returns the as page.
     *
     * @return as page value
     */
    public Page asPage() {
        Page page = page().orElseGet(() -> {
            if (asPage == null) {
                asPage = new CdpPage(session);
            }
            return asPage;
        });
        CdpPage.Internal.bindTarget(page, this);
        if (browserContext != null) {
            CdpPage.Internal.bindBrowserContext(page, browserContext);
        }
        return page;
    }

    /**
     * Returns the worker.
     *
     * @return optional value
     */
    public Optional<Worker> worker() {
        return Optional.empty();
    }

    /**
     * Creates CDP session.
     *
     * @return created CDP session
     */
    public Optional<CDPSession> createCDPSession() {
        return Optional.ofNullable(session);
    }

    /**
     * Returns the session.
     *
     * @return optional value
     */
    Optional<CDPSession> session() {
        return Optional.ofNullable(session);
    }

    /**
     * Returns the require CDP session.
     *
     * @return require CDP session value
     */
    CDPSession requireCDPSession() {
        return createCDPSession().orElseThrow(() -> new InternalException("Target is missing a CDP session."));
    }

    /**
     * Returns the opener.
     *
     * @return optional value
     */
    public Optional<CdpTarget> opener() {
        return Optional.ofNullable(opener);
    }

    /**
     * Returns the subtype.
     *
     * @return subtype value
     */
    String subtype() {
        return StringKit.isBlank(targetInfo.getSubtype()) ? Normal.EMPTY : targetInfo.getSubtype();
    }

    /**
     * Returns whether target exposed is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isTargetExposed() {
        return type() != TargetType.TAB && StringKit.isBlank(subtype());
    }

    /**
     * Returns the initialized.
     *
     * @return completion future
     */
    CompletableFuture<InitializationStatus> initialized() {
        return initialized;
    }

    /**
     * Returns the closed.
     *
     * @return completion future
     */
    CompletableFuture<Void> closed() {
        return closed;
    }

    /**
     * Initializes protocol state for this object.
     */
    protected void initialize() {
        completeInitialization(InitializationStatus.SUCCESS);
    }

    /**
     * Handles abort initialization.
     */
    void abortInitialization() {
        completeInitialization(InitializationStatus.ABORTED);
    }

    /**
     * Handles mark closed.
     */
    void markClosed() {
        closed.complete(null);
    }

    /**
     * Handles target info changed.
     *
     * @param targetInfo target info value
     */
    void targetInfoChanged(TargetInfo targetInfo) {
        this.targetInfo = Assert.notNull(targetInfo, "targetInfo");
        checkIfInitialized();
    }

    /**
     * Returns the child targets.
     *
     * @return values
     */
    List<CdpTarget> childTargets() {
        return List.copyOf(childTargets);
    }

    /**
     * Handles add child target.
     *
     * @param child child value
     */
    void addChildTarget(CdpTarget child) {
        CdpTarget actualChild = Assert.notNull(child, "child");
        if (!childTargets.contains(actualChild)) {
            childTargets.add(actualChild);
        }
    }

    /**
     * Handles remove child target.
     *
     * @param child child value
     */
    void removeChildTarget(CdpTarget child) {
        childTargets.remove(child);
    }

    /**
     * Updates session.
     *
     * @param session protocol session
     */
    protected void setSession(CDPSession session) {
        this.session = session;
    }

    /**
     * Handles bind browser.
     *
     * @param browser browser instance
     */
    protected void bindBrowser(Browser browser) {
        this.browser = browser;
    }

    /**
     * Handles bind browser context.
     *
     * @param browserContext browser context value
     */
    protected void bindBrowserContext(CdpBrowserContext browserContext) {
        this.browserContext = browserContext;
        this.browser = browserContext == null ? browser : browserContext.browser();
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
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return targetInfo.getTargetId();
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return StringKit.isBlank(targetInfo.getUrl()) ? Normal.EMPTY : targetInfo.getUrl();
    }

    /**
     * Returns the type.
     *
     * @return type value
     */
    public TargetType type() {
        return switch (StringKit.isBlank(targetInfo.getType()) ? Normal.EMPTY : targetInfo.getType()) {
            case "page" -> TargetType.PAGE;
            case "background_page" -> TargetType.BACKGROUND_PAGE;
            case "service_worker" -> TargetType.SERVICE_WORKER;
            case "shared_worker" -> TargetType.SHARED_WORKER;
            case "browser" -> TargetType.BROWSER;
            case "webview" -> TargetType.WEBVIEW;
            case "tab" -> TargetType.TAB;
            default -> TargetType.OTHER;
        };
    }

    /**
     * Returns whether page target is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isPageTarget() {
        TargetType type = type();
        return type == TargetType.PAGE || type == TargetType.BACKGROUND_PAGE || type == TargetType.WEBVIEW;
    }

    /**
     * Returns whether worker target is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isWorkerTarget() {
        TargetType type = type();
        return type == TargetType.SERVICE_WORKER || type == TargetType.SHARED_WORKER;
    }

    /**
     * Returns the target info.
     *
     * @return target info value
     */
    protected TargetInfo targetInfo() {
        return targetInfo;
    }

    /**
     * Handles check if initialized.
     */
    protected void checkIfInitialized() {
        completeInitialization(InitializationStatus.SUCCESS);
    }

    /**
     * Handles complete initialization.
     *
     * @param status status value
     */
    protected void completeInitialization(InitializationStatus status) {
        if (initializationCompleted.compareAndSet(false, true)) {
            initialized.complete(status);
        }
    }

    /**
     * Provides package-level target collaboration without creating a standalone helper type.
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
         * Returns the CDP session attached to a target.
         *
         * @param target target
         * @return session
         */
        public static Optional<CDPSession> session(CdpTarget target) {
            return pageTarget(target).session();
        }

        /**
         * Returns the required CDP session attached to a target.
         *
         * @param target target
         * @return session
         */
        public static CDPSession requireCDPSession(CdpTarget target) {
            return pageTarget(target).requireCDPSession();
        }

        /**
         * Returns the target subtype.
         *
         * @param target target
         * @return subtype
         */
        public static String subtype(CdpTarget target) {
            return pageTarget(target).subtype();
        }

        /**
         * Returns whether the target is exposed to callers.
         *
         * @param target target object
         * @return exposed state
         */
        public static boolean isTargetExposed(CdpTarget target) {
            return pageTarget(target).isTargetExposed();
        }

        /**
         * Returns the target initialization future.
         *
         * @param target target
         * @return initialization future
         */
        public static CompletableFuture<InitializationStatus> initialized(CdpTarget target) {
            return pageTarget(target).initialized();
        }

        /**
         * Returns the target close future.
         *
         * @param target target
         * @return close future
         */
        public static CompletableFuture<Void> closed(CdpTarget target) {
            return pageTarget(target).closed();
        }

        /**
         * Completes target initialization successfully.
         *
         * @param target target
         */
        public static void initialize(CdpTarget target) {
            pageTarget(target).initialize();
        }

        /**
         * Aborts target initialization.
         *
         * @param target target
         */
        public static void abortInitialization(CdpTarget target) {
            pageTarget(target).abortInitialization();
        }

        /**
         * Marks a target closed.
         *
         * @param target target
         */
        public static void markClosed(CdpTarget target) {
            pageTarget(target).markClosed();
        }

        /**
         * Updates target info.
         *
         * @param target     target
         * @param targetInfo target info
         */
        public static void targetInfoChanged(CdpTarget target, TargetInfo targetInfo) {
            pageTarget(target).targetInfoChanged(targetInfo);
        }

        /**
         * Returns child targets.
         *
         * @param target target
         * @return child targets
         */
        public static List<CdpTarget> childTargets(CdpTarget target) {
            return pageTarget(target).childTargets();
        }

        /**
         * Adds a child target.
         *
         * @param target target
         * @param child  child target
         */
        public static void addChildTarget(CdpTarget target, CdpTarget child) {
            pageTarget(target).addChildTarget(child);
        }

        /**
         * Removes a child target.
         *
         * @param target target
         * @param child  child target
         */
        public static void removeChildTarget(CdpTarget target, CdpTarget child) {
            pageTarget(target).removeChildTarget(child);
        }

        /**
         * Binds a CDP session to a target.
         *
         * @param target  target
         * @param session session
         */
        public static void setSession(CdpTarget target, CDPSession session) {
            pageTarget(target).setSession(session);
        }

        /**
         * Binds a browser to a target.
         *
         * @param target  target
         * @param browser browser
         */
        public static void bindBrowser(CdpTarget target, Browser browser) {
            pageTarget(target).bindBrowser(browser);
        }

        /**
         * Binds a browser context to a target.
         *
         * @param target  target
         * @param context browser context
         */
        public static void bindBrowserContext(CdpTarget target, CdpBrowserContext context) {
            pageTarget(target).bindBrowserContext(context);
        }

        /**
         * Returns the target id.
         *
         * @param target target
         * @return target id
         */
        public static String id(Target target) {
            return pageTarget(target).id();
        }

        /**
         * Returns whether this target represents a page.
         *
         * @param target target object
         * @return page target state
         */
        public static boolean isPageTarget(CdpTarget target) {
            return pageTarget(target).isPageTarget();
        }

        /**
         * Returns whether this target represents a worker.
         *
         * @param target target object
         * @return worker target state
         */
        public static boolean isWorkerTarget(CdpTarget target) {
            return pageTarget(target).isWorkerTarget();
        }

        /**
         * Returns target protocol information.
         *
         * @param target target
         * @return target information
         */
        public static TargetInfo targetInfo(CdpTarget target) {
            return pageTarget(target).targetInfo();
        }

        /**
         * Casts a target contract to the page target implementation.
         *
         * @param target target
         * @return page target
         */
        private static CdpTarget pageTarget(Target target) {
            if (target instanceof CdpTarget pageTarget) {
                return pageTarget;
            }
            throw new InternalException(
                    "Unsupported target implementation: " + Assert.notNull(target, "target").getClass());
        }

        /**
         * Validates a page target.
         *
         * @param target target
         * @return page target
         */
        private static CdpTarget pageTarget(CdpTarget target) {
            return Assert.notNull(target, "target");
        }
    }

    /**
     * Enumerates InitializationStatuss.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public enum InitializationStatus {

        /**
         * Represents the success enum member.
         */
        SUCCESS,

        /**
         * Shared constant for aborted.
         */
        ABORTED
    }

}
