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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.kernel.cdp.session.TargetInfo;

/**
 * Manages chrome target state and protocol events.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpTargetManager {

    /**
     * Creates a chrome target manager.
     */
    public CdpTargetManager() {
        // No initialization required.
    }

    /**
     * Mapped targets values.
     */
    private final Map<String, CdpTarget> targets = new LinkedHashMap<>();
    /**
     * Target lifecycle event emitter.
     */
    private final EventEmitter<CdpTargetManagerEvent> emitter = new EventEmitter<>();
    /**
     * Mapped discovered targets values.
     */
    private final Map<String, TargetInfo> discoveredTargets = new LinkedHashMap<>();
    /**
     * Registered ignored targets values.
     */
    private final Set<String> ignoredTargets = new HashSet<>();
    /**
     * Current initialized future.
     */
    private final CompletableFuture<Void> initializedFuture = new CompletableFuture<>();
    /**
     * Registered blocklist values.
     */
    private List<String> blocklist = List.of();

    /**
     * Returns the of.
     */
    private List<UrlRestrictionRule> blocklistRules = List.of();
    /**
     * Registered allowlist values.
     */
    private List<String> allowlist = List.of();

    /**
     * Returns the of.
     */
    private List<UrlRestrictionRule> allowlistRules = List.of();
    /**
     * Current target filter.
     */
    private Predicate<CdpTarget> targetFilter;
    /**
     * Current is page target.
     */
    private Predicate<CdpTarget> isPageTarget;
    /**
     * Whether handle dev tools as page is enabled.
     */
    private boolean handleDevToolsAsPage;
    /**
     * Thread-safe initialized state.
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Initializes protocol state for this object.
     */
    public void initialize() {
        initialized.set(true);
        initializedFuture.complete(null);
        Logger.debug(false, "Target", "Chrome target manager initialized");
    }

    /**
     * Initializes CDP target discovery and auto attach.
     *
     * @param connection CDP connection
     * @param options    target manager options
     * @return initialization future
     */
    public CompletableFuture<Void> initialize(Connection connection, Options options) {
        Options actualOptions = options == null ? new Options() : options;
        this.targetFilter = actualOptions.getTargetFilter();
        this.isPageTarget = actualOptions.getIsPageTarget();
        this.handleDevToolsAsPage = actualOptions.isHandleDevToolsAsPage();
        setBlocklist(actualOptions.getBlocklist());
        setAllowlist(actualOptions.getAllowlist());
        Logger.debug(
                true,
                "Target",
                "Chrome target discovery initialize requested: hasConnection={}, blocklist={}, allowlist={}",
                connection != null && connection.hasConfiguredTransport(),
                blocklist.size(),
                allowlist.size());
        initialize();
        if (connection == null || !connection.hasConfiguredTransport()) {
            Logger.debug(false, "Target", "Chrome target discovery initialized without protocol transport");
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<CdpPayload> discover = connection
                .send("Target.setDiscoverTargets", Map.of("discover", true, "filter", discoveryFilter()));
        CompletableFuture<CdpPayload> autoAttach = connection.send(
                "Target.setAutoAttach",
                Map.of(
                        "autoAttach",
                        true,
                        "waitForDebuggerOnStart",
                        true,
                        "flatten",
                        true,
                        "filter",
                        autoAttachFilter()));
        return discover.thenCombine(autoAttach, (left, right) -> {
            Logger.debug(false, "Target", "Chrome target discovery protocol setup completed");
            return null;
        });
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        targets.clear();
        discoveredTargets.clear();
        ignoredTargets.clear();
        initialized.set(false);
        Logger.debug(false, "Target", "Chrome target manager disposed");
    }

    /**
     * Returns the available targets.
     *
     * @return values
     */
    public Collection<CdpTarget> getAvailableTargets() {
        return java.util.List.copyOf(targets.values());
    }

    /**
     * Registers a target lifecycle listener.
     *
     * @param event    target lifecycle event
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(CdpTargetManagerEvent event, Consumer<Object> listener) {
        return EventHooks.on(emitter, event, listener);
    }

    /**
     * Emits a target lifecycle event.
     *
     * @param event   target lifecycle event
     * @param payload event payload
     */
    private void emit(CdpTargetManagerEvent event, Object payload) {
        emitter.emit(Assert.notNull(event, "event"), payload);
    }

    /**
     * Target change event payload.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class TargetChangedEvent {

        /**
         * Target whose metadata changed.
         */
        private final CdpTarget target;

        /**
         * Whether the target was initialized before the change.
         */
        private final boolean wasInitialized;

        /**
         * URL observed before the change.
         */
        private final String previousUrl;

        /**
         * Creates a target change event payload.
         *
         * @param target         changed target
         * @param wasInitialized previous initialization state
         * @param previousUrl    previous target URL
         */
        public TargetChangedEvent(CdpTarget target, boolean wasInitialized, String previousUrl) {
            this.target = Assert.notNull(target, "target");
            this.wasInitialized = wasInitialized;
            this.previousUrl = previousUrl == null ? Normal.EMPTY : previousUrl;
        }

        /**
         * Returns the changed target.
         *
         * @return changed target
         */
        public CdpTarget target() {
            return target;
        }

        /**
         * Returns the was initialized.
         *
         * @return {@code true} when the condition matches
         */
        public boolean wasInitialized() {
            return wasInitialized;
        }

        /**
         * Returns the URL observed before the change.
         *
         * @return previous target URL
         */
        public String previousUrl() {
            return previousUrl;
        }
    }

    /**
     * Returns the discovered target infos.
     *
     * @return mapped values
     */
    public Map<String, TargetInfo> getDiscoveredTargetInfos() {
        return Map.copyOf(discoveredTargets);
    }

    /**
     * Returns the initialized.
     *
     * @return completion future
     */
    public CompletableFuture<Void> initialized() {
        return initializedFuture;
    }

    /**
     * Adds to ignore target.
     *
     * @param targetId target id
     */
    public void addToIgnoreTarget(String targetId) {
        ignoredTargets.add(targetId);
        Logger.debug(true, "Target", "Chrome target ignored: targetId={}", targetId);
    }

    /**
     * Updates blocklist.
     *
     * @param blocklist blocklist value
     */
    public void setBlocklist(List<String> blocklist) {
        this.blocklist = blocklist == null ? List.of() : List.copyOf(blocklist);
        this.blocklistRules = compileRules(this.blocklist);
        Logger.debug(true, "Target", "Chrome target URL blocklist updated: count={}", this.blocklist.size());
        if (!this.allowlist.isEmpty() && !this.blocklist.isEmpty()) {
            throw new IllegalStateException("URL block and allow rules cannot be set at the same time.");
        }
    }

    /**
     * Updates allowlist.
     *
     * @param allowlist allowlist value
     */
    public void setAllowlist(List<String> allowlist) {
        this.allowlist = allowlist == null ? List.of() : List.copyOf(allowlist);
        this.allowlistRules = compileRules(this.allowlist);
        Logger.debug(true, "Target", "Chrome target URL allowlist updated: count={}", this.allowlist.size());
        if (!this.allowlist.isEmpty() && !this.blocklist.isEmpty()) {
            throw new IllegalStateException("URL block and allow rules cannot be set at the same time.");
        }
    }

    /**
     * Returns the on target created.
     *
     * @param targetInfo target info value
     * @return on target created value
     */
    public CdpTarget onTargetCreated(TargetInfo targetInfo) {
        ensureInitialized();
        discoveredTargets.put(targetInfo.getTargetId(), targetInfo);
        Logger.debug(
                true,
                "Target",
                "Chrome target discovered: targetId={}, type={}, url={}",
                targetInfo.getTargetId(),
                targetInfo.getType(),
                String.valueOf(targetInfo.getUrl()).replaceAll("[?#].*$", "?<redacted>"));
        emit(CdpTargetManagerEvent.TARGET_DISCOVERED, targetInfo);
        CdpTarget opener = targets.get(targetInfo.getOpenerId());
        CdpTarget target = createTarget(targetInfo, opener);
        if (!shouldExpose(target)) {
            Logger.debug(
                    false,
                    "Target",
                    "Chrome target hidden by policy: targetId={}, type={}",
                    targetInfo.getTargetId(),
                    targetInfo.getType());
            return target;
        }
        CdpTarget.Internal.initialize(target);
        targets.put(targetInfo.getTargetId(), target);
        if (opener != null) {
            CdpTarget.Internal.addChildTarget(opener, target);
        }
        emit(CdpTargetManagerEvent.TARGET_AVAILABLE, target);
        Logger.debug(
                false,
                "Target",
                "Chrome target available: targetId={}, type={}, count={}",
                targetInfo.getTargetId(),
                targetInfo.getType(),
                targets.size());
        return target;
    }

    /**
     * Returns the on target created.
     *
     * @param payload protocol payload
     * @return on target created value
     */
    public CdpTarget onTargetCreated(CdpPayload payload) {
        return onTargetCreated(TargetInfo.fromTargetCreated(payload));
    }

    /**
     * Adds target.
     *
     * @param target target object
     * @return add target value
     */
    public CdpTarget addTarget(CdpTarget target) {
        ensureInitialized();
        targets.put(CdpTarget.Internal.targetInfo(target).getTargetId(), target);
        target.opener().ifPresent(opener -> CdpTarget.Internal.addChildTarget(opener, target));
        CdpTarget.Internal.initialize(target);
        emit(CdpTargetManagerEvent.TARGET_AVAILABLE, target);
        Logger.debug(
                false,
                "Target",
                "Chrome target added: targetId={}, type={}, count={}",
                CdpTarget.Internal.targetInfo(target).getTargetId(),
                target.type(),
                targets.size());
        return target;
    }

    /**
     * Handles on target destroyed.
     *
     * @param targetId target ID value
     */
    public void onTargetDestroyed(String targetId) {
        discoveredTargets.remove(targetId);
        CdpTarget target = targets.remove(targetId);
        if (target != null) {
            target.opener().ifPresent(opener -> CdpTarget.Internal.removeChildTarget(opener, target));
            CdpTarget.Internal.markClosed(target);
            emit(CdpTargetManagerEvent.TARGET_GONE, target);
            Logger.debug(
                    false,
                    "Target",
                    "Chrome target destroyed: targetId={}, remaining={}",
                    targetId,
                    targets.size());
        } else {
            Logger.debug(true, "Target", "Chrome target destroy ignored: targetId={}", targetId);
        }
    }

    /**
     * Handles on target destroyed.
     *
     * @param payload protocol payload
     */
    public void onTargetDestroyed(CdpPayload payload) {
        CdpPayload targetId = payload.get("targetId");
        if (!targetId.isNull()) {
            onTargetDestroyed(targetId.asText());
        }
    }

    /**
     * Handles on target info changed.
     *
     * @param targetInfo target info value
     */
    public void onTargetInfoChanged(TargetInfo targetInfo) {
        discoveredTargets.put(targetInfo.getTargetId(), targetInfo);
        if (ignoredTargets.contains(targetInfo.getTargetId())) {
            return;
        }
        CdpTarget previous = targets.get(targetInfo.getTargetId());
        if (previous == null) {
            return;
        }
        String previousUrl = previous.url();
        boolean wasInitialized = CdpTarget.Internal.initialized(previous).isDone()
                && CdpTarget.Internal.initialized(previous)
                        .getNow(CdpTarget.InitializationStatus.ABORTED) == CdpTarget.InitializationStatus.SUCCESS;
        CdpTarget.Internal.targetInfoChanged(previous, targetInfo);
        if (wasInitialized && !previousUrl.equals(previous.url())) {
            emit(CdpTargetManagerEvent.TARGET_CHANGED, new TargetChangedEvent(previous, true, previousUrl));
            Logger.debug(
                    false,
                    "Target",
                    "Chrome target changed: targetId={}, url={}",
                    targetInfo.getTargetId(),
                    String.valueOf(previous.url()).replaceAll("[?#].*$", "?<redacted>"));
        }
    }

    /**
     * Handles on target info changed.
     *
     * @param payload protocol payload
     */
    public void onTargetInfoChanged(CdpPayload payload) {
        onTargetInfoChanged(TargetInfo.fromTargetInfoChanged(payload));
    }

    /**
     * Returns the on attached to target.
     *
     * @param targetInfo target info value
     * @return on attached to target value
     */
    public CdpTarget onAttachedToTarget(TargetInfo targetInfo) {
        CdpTarget target = targets.get(targetInfo.getTargetId());
        boolean created = target == null;
        if (target == null) {
            target = createTarget(targetInfo, targets.get(targetInfo.getOpenerId()));
        }
        if (!shouldExpose(target)) {
            Logger.debug(
                    false,
                    "Target",
                    "Chrome attached target hidden by policy: targetId={}, type={}",
                    targetInfo.getTargetId(),
                    targetInfo.getType());
            return target;
        }
        targets.putIfAbsent(targetInfo.getTargetId(), target);
        CdpTarget.Internal.initialize(target);
        if (created) {
            emit(CdpTargetManagerEvent.TARGET_AVAILABLE, target);
        }
        Logger.debug(
                false,
                "Target",
                "Chrome target attached: targetId={}, type={}, created={}",
                targetInfo.getTargetId(),
                targetInfo.getType(),
                created);
        return target;
    }

    /**
     * Returns the on attached to target.
     *
     * @param session protocol session
     * @return {@code true} when the condition matches
     */
    public boolean onAttachedToTarget(CDPSession session) {
        TargetInfo targetInfo = CDPSession.Internal.targetInfo(session);
        CdpTarget target = onAttachedToTarget(targetInfo);
        CdpTarget.Internal.setSession(target, session);
        if (shouldSilentDetach(target)) {
            silentDetach(session, target);
            return false;
        }
        maybeSetupNetworkConditions(session, targetInfo);
        Logger.debug(
                false,
                "Target",
                "Chrome target session attached: targetId={}, sessionId={}",
                targetInfo.getTargetId(),
                session.id());
        return true;
    }

    /**
     * Handles on detached from target.
     *
     * @param targetId target ID value
     */
    public void onDetachedFromTarget(String targetId) {
        onTargetDestroyed(targetId);
    }

    /**
     * Handles on detached from target.
     *
     * @param session protocol session
     */
    public void onDetachedFromTarget(CDPSession session) {
        if (session != null) {
            onDetachedFromTarget(CDPSession.Internal.targetInfo(session).getTargetId());
        }
    }

    /**
     * Returns the target.
     *
     * @param targetId target ID value
     * @return optional value
     */
    public Optional<CdpTarget> target(String targetId) {
        return Optional.ofNullable(targets.get(targetId));
    }

    /**
     * Returns whether auto attach is available.
     *
     * @param type type name
     * @return {@code true} when the condition matches
     */
    public boolean shouldAutoAttach(String type) {
        return "page".equals(type) || "iframe".equals(type) || "service_worker".equals(type)
                || "shared_worker".equals(type);
    }

    /**
     * Returns whether the URL is allowed by the configured policy.
     *
     * @param url target URL
     * @return {@code true} when the condition matches
     */
    public boolean isUrlAllowed(String url) {
        String actualUrl = url == null ? Normal.EMPTY : url;
        if (actualUrl.isEmpty() || "about:blank".equals(actualUrl)) {
            return true;
        }
        for (UrlRestrictionRule rule : blocklistRules) {
            if (rule.test(actualUrl)) {
                return false;
            }
        }
        if (!allowlistRules.isEmpty()) {
            for (UrlRestrictionRule rule : allowlistRules) {
                if (rule.test(actualUrl)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Creates target.
     *
     * @param targetInfo target info value
     * @param opener     opener value
     * @return create target value
     */
    private CdpTarget createTarget(TargetInfo targetInfo, CdpTarget opener) {
        if ("devtools".equals(targetInfo.getType()) && handleDevToolsAsPage) {
            return new CdpPageTarget(targetInfo, opener);
        }
        return switch (targetInfo.getType()) {
            case "page" -> new CdpPageTarget(targetInfo, opener);
            case "service_worker", "shared_worker" -> new CdpWorkerTarget(targetInfo, opener);
            case "other" -> new CdpOtherTarget(targetInfo, opener);
            case "devtools" -> new CdpDevToolsTarget(targetInfo, opener);
            default -> new CdpOtherTarget(targetInfo, opener);
        };
    }

    /**
     * Returns whether a target should be exposed.
     *
     * @param target target object
     * @return {@code true} when the condition matches
     */
    private boolean shouldExpose(CdpTarget target) {
        TargetInfo targetInfo = CdpTarget.Internal.targetInfo(target);
        if (ignoredTargets.contains(targetInfo.getTargetId()) || !isUrlAllowed(targetInfo.getUrl())) {
            return false;
        }
        if (isPageTarget != null && CdpTarget.Internal.isPageTarget(target) && !isPageTarget.test(target)) {
            return false;
        }
        return targetFilter == null || targetFilter.test(target);
    }

    /**
     * Returns whether a target should be silently detached.
     *
     * @param target target object
     * @return {@code true} when the condition matches
     */
    private boolean shouldSilentDetach(CdpTarget target) {
        TargetInfo targetInfo = CdpTarget.Internal.targetInfo(target);
        if ("service_worker".equals(targetInfo.getType())) {
            return true;
        }
        boolean expose = shouldExpose(target);
        if (!expose) {
            ignoredTargets.add(targetInfo.getTargetId());
        }
        return !expose;
    }

    /**
     * Runs the target if needed and detaches without surfacing an error.
     *
     * @param session session
     * @param target  target
     */
    private void silentDetach(CDPSession session, CdpTarget target) {
        TargetInfo targetInfo = CdpTarget.Internal.targetInfo(target);
        Logger.debug(
                false,
                "Target",
                "Chrome target silent detach requested: targetId={}, type={}",
                targetInfo.getTargetId(),
                targetInfo.getType());
        maybeSetupNetworkConditions(session, targetInfo)
                .thenCompose(ignored -> session.send("Runtime.runIfWaitingForDebugger")).exceptionally(error -> {
                    Logger.debug(
                            false,
                            "Target",
                            "Silent target run-if-waiting failed: targetId={}",
                            targetInfo.getTargetId());
                    return null;
                }).thenCompose(ignored -> session.detach()).exceptionally(error -> {
                    Logger.debug(false, "Target", "Silent target detach failed: targetId={}", targetInfo.getTargetId());
                    return null;
                });
        CdpTarget.Internal.setSession(target, null);
    }

    /**
     * Creates the target discovery filter.
     *
     * @return discovery filter
     */
    private List<Map<String, Object>> discoveryFilter() {
        return List.of(Map.of("type", "browser", "exclude", true));
    }

    /**
     * Creates the auto attach filter.
     *
     * @return auto attach filter
     */
    private List<Map<String, Object>> autoAttachFilter() {
        return List.of(
                Map.of("type", "page", "exclude", false),
                Map.of("type", "iframe", "exclude", false),
                Map.of("type", "service_worker", "exclude", false),
                Map.of("type", "shared_worker", "exclude", false));
    }

    /**
     * Handles ensure initialized.
     */
    private void ensureInitialized() {
        if (!initialized.get()) {
            throw new IllegalStateException("CdpTargetManager has not been initialized.");
        }
    }

    /**
     * Compiles URL restriction rules.
     *
     * @param rules rules
     * @return compiled rules
     */
    private List<UrlRestrictionRule> compileRules(List<String> rules) {
        return rules.stream().map(UrlRestrictionRule::compile).toList();
    }

    /**
     * Handles maybe setup network conditions.
     *
     * @param session protocol session
     */
    private CompletableFuture<Void> maybeSetupNetworkConditions(CDPSession session, TargetInfo targetInfo) {
        if (blocklist.isEmpty() && allowlist.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<Map<String, Object>> matchedNetworkConditions = new ArrayList<>();
        for (String rule : blocklist) {
            matchedNetworkConditions.add(networkCondition(rule, true));
        }
        if (!allowlist.isEmpty()) {
            for (String rule : allowlist) {
                matchedNetworkConditions.add(networkCondition(rule, false));
            }
            matchedNetworkConditions.add(networkCondition(Normal.EMPTY, true));
        }
        Map<String, Object> params = new LinkedHashMap<>();
        if (!blocklist.isEmpty()) {
            params.put("offline", true);
        }
        params.put("matchedNetworkConditions", matchedNetworkConditions);
        List<CompletableFuture<?>> commands = new ArrayList<>();
        if (needsNetworkEnabled(targetInfo)) {
            commands.add(session.send("Network.enable"));
        }
        commands.add(session.send("Network.emulateNetworkConditionsByRule", params));
        return CompletableFuture.allOf(commands.toArray(CompletableFuture[]::new)).exceptionally(error -> {
            Logger.warn(false, "Target", error, "Chrome target URL network rule initialization failed.");
            return null;
        });
    }

    /**
     * Returns whether Network.enable is required before applying URL network rules.
     *
     * @param targetInfo target metadata
     * @return {@code true} when the target can issue worker fetches
     */
    private boolean needsNetworkEnabled(TargetInfo targetInfo) {
        String type = targetInfo == null ? Normal.EMPTY : targetInfo.getType();
        return "worker".equals(type) || "service_worker".equals(type) || "shared_worker".equals(type);
    }

    /**
     * Creates a network condition entry.
     *
     * @param rule    URL pattern rule
     * @param offline whether the rule should be offline
     * @return network condition
     */
    private Map<String, Object> networkCondition(String rule, boolean offline) {
        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("urlPattern", rule);
        condition.put("offline", offline);
        condition.put("latency", 0);
        condition.put("downloadThroughput", -1);
        condition.put("uploadThroughput", -1);
        return condition;
    }

    /**
     * Target manager options.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Options {

        /**
         * Creates target manager options.
         */
        public Options() {
            // No initialization required.
        }

        /**
         * Target filter.
         */
        private Predicate<CdpTarget> targetFilter;

        /**
         * Page target predicate.
         */
        private Predicate<CdpTarget> isPageTarget;

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
         * Initialization timeout in milliseconds.
         */
        private long timeoutMillis;

        /**
         * Returns the target filter.
         *
         * @return target filter
         */
        public Predicate<CdpTarget> getTargetFilter() {
            return targetFilter;
        }

        /**
         * Updates target filter.
         *
         * @param targetFilter target filter value
         */
        public void setTargetFilter(Predicate<CdpTarget> targetFilter) {
            this.targetFilter = targetFilter;
        }

        /**
         * Returns the is page target.
         *
         * @return page target predicate
         */
        public Predicate<CdpTarget> getIsPageTarget() {
            return isPageTarget;
        }

        /**
         * Updates is page target.
         *
         * @param isPageTarget page target predicate
         */
        public void setIsPageTarget(Predicate<CdpTarget> isPageTarget) {
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

}
