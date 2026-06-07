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
package org.miaixz.lancia.options;

import java.util.List;
import java.util.function.Predicate;

import org.miaixz.lancia.Target;
import org.miaixz.lancia.nimble.emulation.Viewport;
import org.miaixz.lancia.nimble.network.DownloadBehavior;

/**
 * Browser attach options.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class AttachOptions {

    /**
     * Default viewport.
     */
    private Viewport defaultViewport;

    /**
     * Download behavior.
     */
    private DownloadBehavior downloadBehavior;

    /**
     * Target filter.
     */
    private Predicate<Target> targetFilter;

    /**
     * Page target predicate.
     */
    private Predicate<Target> isPageTarget;

    /**
     * Whether DevTools targets should be handled as pages.
     */
    private boolean handleDevToolsAsPage;

    /**
     * URL blocklist.
     */
    private List<String> blocklist = List.of();

    /**
     * URL allowlist.
     */
    private List<String> allowlist = List.of();

    /**
     * Whether initially discovered targets should be awaited.
     */
    private boolean waitForInitiallyDiscoveredTargets;

    /**
     * Timeout in milliseconds.
     */
    private long timeoutMillis;

    /**
     * Creates attach options.
     */
    public AttachOptions() {
        // No initialization required.
    }

    /**
     * Creates attach options from connect options.
     *
     * @param options connect options
     * @return attach options
     */
    public static AttachOptions from(ConnectOptions options) {
        AttachOptions attachOptions = new AttachOptions();
        if (options == null) {
            return attachOptions;
        }
        attachOptions.setDefaultViewport(options.getDefaultViewport());
        attachOptions.setDownloadBehavior(options.getDownloadBehavior());
        attachOptions.setTargetFilter(options.getTargetFilter());
        attachOptions.setHandleDevToolsAsPage(options.isHandleDevToolsAsPage());
        attachOptions.setBlocklist(options.getBlocklist());
        attachOptions.setAllowlist(options.getAllowlist());
        attachOptions.setTimeoutMillis(options.getProtocolTimeoutMillis());
        return attachOptions;
    }

    /**
     * Returns the default viewport.
     *
     * @return default viewport
     */
    public Viewport getDefaultViewport() {
        return defaultViewport;
    }

    /**
     * Updates default viewport.
     *
     * @param defaultViewport default viewport value
     */
    public void setDefaultViewport(Viewport defaultViewport) {
        this.defaultViewport = defaultViewport;
    }

    /**
     * Returns the download behavior.
     *
     * @return download behavior
     */
    public DownloadBehavior getDownloadBehavior() {
        return downloadBehavior;
    }

    /**
     * Updates download behavior.
     *
     * @param downloadBehavior download behavior value
     */
    public void setDownloadBehavior(DownloadBehavior downloadBehavior) {
        this.downloadBehavior = downloadBehavior;
    }

    /**
     * Returns the target filter.
     *
     * @return target filter
     */
    public Predicate<Target> getTargetFilter() {
        return targetFilter;
    }

    /**
     * Updates target filter.
     *
     * @param targetFilter target filter value
     */
    public void setTargetFilter(Predicate<Target> targetFilter) {
        this.targetFilter = targetFilter;
    }

    /**
     * Returns the is page target.
     *
     * @return page target predicate
     */
    public Predicate<Target> getIsPageTarget() {
        return isPageTarget;
    }

    /**
     * Updates is page target.
     *
     * @param isPageTarget page target predicate
     */
    public void setIsPageTarget(Predicate<Target> isPageTarget) {
        this.isPageTarget = isPageTarget;
    }

    /**
     * Returns whether DevTools targets should be exposed as pages.
     *
     * @return {@code true} when DevTools targets are pages
     */
    public boolean isHandleDevToolsAsPage() {
        return handleDevToolsAsPage;
    }

    /**
     * Updates handle dev tools as page.
     *
     * @param handleDevToolsAsPage DevTools target mode
     */
    public void setHandleDevToolsAsPage(boolean handleDevToolsAsPage) {
        this.handleDevToolsAsPage = handleDevToolsAsPage;
    }

    /**
     * Returns the blocklist.
     *
     * @return URL blocklist
     */
    public List<String> getBlocklist() {
        return blocklist;
    }

    /**
     * Updates blocklist.
     *
     * @param blocklist URL blocklist
     */
    public void setBlocklist(List<String> blocklist) {
        this.blocklist = blocklist == null ? List.of() : List.copyOf(blocklist);
    }

    /**
     * Returns the allowlist.
     *
     * @return URL allowlist
     */
    public List<String> getAllowlist() {
        return allowlist;
    }

    /**
     * Updates allowlist.
     *
     * @param allowlist URL allowlist
     */
    public void setAllowlist(List<String> allowlist) {
        this.allowlist = allowlist == null ? List.of() : List.copyOf(allowlist);
    }

    /**
     * Returns whether startup waits for initially discovered targets.
     *
     * @return {@code true} when initial targets should be awaited
     */
    public boolean isWaitForInitiallyDiscoveredTargets() {
        return waitForInitiallyDiscoveredTargets;
    }

    /**
     * Updates wait for initially discovered targets.
     *
     * @param waitForInitiallyDiscoveredTargets initial target wait flag
     */
    public void setWaitForInitiallyDiscoveredTargets(boolean waitForInitiallyDiscoveredTargets) {
        this.waitForInitiallyDiscoveredTargets = waitForInitiallyDiscoveredTargets;
    }

    /**
     * Returns the timeout millis.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Updates timeout millis.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

}
