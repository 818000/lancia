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
package org.miaixz.lancia.kernel.cdp.page;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.bus.core.xyz.CollKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.kernel.cdp.network.CdpRequest;
import org.miaixz.lancia.kernel.cdp.network.CdpResponse;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Tracks frame lifecycle events until navigation reaches the requested state.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpLifecycleWatcher {

    /**
     * Executor used for lifecycle timeout tasks.
     */
    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-lifecycle-watcher-", true));
    /**
     * Current frame manager.
     */
    private final CdpFrameManager frameManager;
    /**
     * Current frame.
     */
    private final CdpFrame frame;
    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Registered expected lifecycle values.
     */
    private final List<String> expectedLifecycle;
    /**
     * Current initial loader ID.
     */
    private final String initialLoaderId;
    /**
     * Registered observed lifecycle values.
     */
    private final Set<String> observedLifecycle = ConcurrentHashMap.newKeySet();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Current same document navigation.
     */
    private final CompletableFuture<String> sameDocumentNavigation = new CompletableFuture<>();
    /**
     * Current new document navigation.
     */
    private final CompletableFuture<String> newDocumentNavigation = new CompletableFuture<>();
    /**
     * Current lifecycle.
     */
    private final CompletableFuture<Void> lifecycle = new CompletableFuture<>();
    /**
     * Current termination.
     */
    private final CompletableFuture<Void> termination = new CompletableFuture<>();
    /**
     * Current navigation response.
     */
    private volatile Response navigationResponse;
    /**
     * Current navigation request ID.
     */
    private volatile String navigationRequestId;
    /**
     * Current navigation request.
     */
    private volatile CdpRequest navigationRequest;
    /**
     * Current timeout task.
     */
    private final ScheduledFuture<?> timeoutTask;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Creates a lifecycle watcher.
     *
     * @param frameManager frame manager
     * @param frame        frame instance
     * @param waitUntil    wait until
     * @param timeout      timeout value
     */
    public CdpLifecycleWatcher(CdpFrameManager frameManager, CdpFrame frame, Collection<String> waitUntil,
            Duration timeout) {
        this.frameManager = Assert.notNull(frameManager, "frameManager");
        this.frame = Assert.notNull(frame, "frame");
        this.session = frame.mainWorld() == null ? null : frame.mainWorld().session();
        this.initialLoaderId = frame.loaderId();
        this.expectedLifecycle = normalizeLifecycle(waitUntil);
        Duration safeTimeout = Assert.notNull(timeout, "timeout");
        Logger.debug(
                true,
                "Page",
                "Lifecycle watcher created: frame={}, waitUntil={}, timeout={}",
                frame.id(),
                expectedLifecycle,
                safeTimeout);
        if (session == null) {
            lifecycle.complete(null);
            timeoutTask = null;
            Logger.debug(
                    false,
                    "Page",
                    "Lifecycle watcher completed immediately: frame={}, reason=noSession",
                    frame.id());
            return;
        }
        subscribe();
        timeoutTask = TIMEOUT_EXECUTOR.schedule(() -> {
            Logger.warn(
                    false,
                    "Page",
                    "Lifecycle watcher timeout: frame={}, timeoutMillis={}",
                    frame.id(),
                    safeTimeout.toMillis());
            termination.completeExceptionally(
                    new TimeoutException("Timed out waiting for page navigation: " + safeTimeout.toMillis() + "ms"));
        }, safeTimeout.toMillis(), TimeUnit.MILLISECONDS);
        checkLifecycleComplete();
    }

    /**
     * Waits for termination.
     *
     * @return wait for termination value
     */
    public CompletableFuture<Void> waitForTermination() {
        return termination;
    }

    /**
     * Waits for same document navigation.
     *
     * @return wait for same document navigation value
     */
    public CompletableFuture<String> waitForSameDocumentNavigation() {
        return sameDocumentNavigation;
    }

    /**
     * Waits for new document navigation.
     *
     * @return wait for new document navigation value
     */
    public CompletableFuture<String> waitForNewDocumentNavigation() {
        return newDocumentNavigation;
    }

    /**
     * Waits for lifecycle.
     *
     * @return wait for lifecycle value
     */
    public CompletableFuture<Void> waitForLifecycle() {
        return lifecycle;
    }

    /**
     * Returns the navigation response.
     *
     * @return navigation response value
     */
    public Response navigationResponse() {
        return navigationResponse;
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            Logger.debug(true, "Page", "Lifecycle watcher dispose requested: frame={}", frame.id());
            binding.unbind();
            binding = new EventBinding();
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }
            termination.completeExceptionally(new InternalException("CdpLifecycleWatcher disposed"));
            Logger.debug(false, "Page", "Lifecycle watcher disposed: frame={}", frame.id());
        }
    }

    /**
     * Handles subscribe.
     */
    private void subscribe() {
        Logger.debug(false, "Page", "Lifecycle watcher subscribed: frame={}", frame.id());
        binding = binding.combine(session.on("Page.lifecycleEvent", this::onLifecycleEvent));
        binding = binding.combine(session.on("Page.frameNavigated", this::onFrameNavigated));
        binding = binding.combine(session.on("Page.navigatedWithinDocument", this::onNavigatedWithinDocument));
        binding = binding.combine(session.on("Page.frameDetached", this::onFrameDetached));
        binding = binding.combine(session.on("Network.requestWillBeSent", this::onRequestWillBeSent));
        binding = binding.combine(session.on("Network.responseReceived", this::onResponseReceived));
        binding = binding.combine(session.on("Network.loadingFailed", this::onLoadingFailed));
    }

    /**
     * Handles on lifecycle event.
     *
     * @param params protocol parameters
     */
    private void onLifecycleEvent(CdpPayload params) {
        String frameId = PayloadReader.text(params.get("frameId"));
        CdpFrame target = StringKit.isBlank(frameId) ? frame : frameManager.frame(frameId);
        if (target == null) {
            target = findFrame(frame, frameId);
        }
        if (target == null && matchesFrame(frameId)) {
            target = frame;
        }
        if (target == null) {
            return;
        }
        String name = PayloadReader.text(params.get("name"));
        if (StringKit.isNotBlank(name)) {
            if (target == frame) {
                observedLifecycle.add(name);
                Logger.debug(false, "Page", "Lifecycle event observed: frame={}, event={}", frame.id(), name);
            }
            target._onLifecycleEvent(PayloadReader.text(params.get("loaderId")), name);
            checkLifecycleComplete();
        }
    }

    /**
     * Handles on frame navigated.
     *
     * @param params protocol parameters
     */
    private void onFrameNavigated(CdpPayload params) {
        CdpPayload framePayload = params.get("frame");
        String frameId = PayloadReader.text(framePayload.get("id"));
        if (!matchesFrame(frameId)) {
            return;
        }
        observedLifecycle.clear();
        String previousLoaderId = frame.loaderId();
        updateFrame(framePayload);
        if (StringKit.isBlank(initialLoaderId) || !initialLoaderId.equals(frame.loaderId())
                || (StringKit.isNotBlank(previousLoaderId) && !previousLoaderId.equals(frame.loaderId()))) {
            if (newDocumentNavigation.complete(frame.url())) {
                Logger.debug(
                        false,
                        "Page",
                        "New document navigation observed: frame={}, url={}",
                        frame.id(),
                        frame.url() == null ? Normal.EMPTY : frame.url().replaceAll("[?#].*$", "?<redacted>"));
            }
        }
        checkLifecycleComplete();
    }

    /**
     * Handles on navigated within document.
     *
     * @param params protocol parameters
     */
    private void onNavigatedWithinDocument(CdpPayload params) {
        String frameId = PayloadReader.text(params.get("frameId"));
        if (!matchesFrame(frameId)) {
            return;
        }
        String url = PayloadReader.text(params.get("url"));
        if (StringKit.isNotBlank(url)) {
            frame.updateUrl(url);
        }
        if (sameDocumentNavigation.complete(frame.url())) {
            Logger.debug(
                    false,
                    "Page",
                    "Same document navigation observed: frame={}, url={}",
                    frame.id(),
                    frame.url() == null ? Normal.EMPTY : frame.url().replaceAll("[?#].*$", "?<redacted>"));
        }
        checkLifecycleComplete();
    }

    /**
     * Handles on frame detached.
     *
     * @param params protocol parameters
     */
    private void onFrameDetached(CdpPayload params) {
        String frameId = PayloadReader.text(params.get("frameId"));
        if (matchesFrame(frameId)) {
            Logger.warn(false, "Page", "Lifecycle watcher frame detached: frame={}", frameId);
            termination.completeExceptionally(new IllegalStateException("Navigating frame was detached"));
        } else {
            checkLifecycleComplete();
        }
    }

    /**
     * Handles on request will be sent.
     *
     * @param params protocol parameters
     */
    private void onRequestWillBeSent(CdpPayload params) {
        String type = PayloadReader.text(params.get("type"));
        String requestId = PayloadReader.text(params.get("requestId"));
        String loaderId = PayloadReader.text(params.get("loaderId"));
        String frameId = PayloadReader.text(params.get("frameId"));
        if ("Document".equals(type) && (matchesFrame(frameId) || requestId.equals(loaderId))) {
            navigationRequestId = requestId;
            navigationRequest = CdpRequest.from(session, frame, null, false, params, List.of());
            navigationResponse = null;
            Logger.debug(
                    false,
                    "Network",
                    "Navigation request observed: frame={}, requestId={}",
                    frame.id(),
                    requestId);
        }
    }

    /**
     * Handles on response received.
     *
     * @param params protocol parameters
     */
    private void onResponseReceived(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        String type = PayloadReader.text(params.get("type"));
        CdpPayload response = params.get("response");
        String responseUrl = PayloadReader.text(response.get("url"));
        if ((StringKit.isNotBlank(navigationRequestId) && navigationRequestId.equals(requestId))
                || "Document".equals(type) || responseUrl.equals(frame.url())) {
            CdpRequest request = navigationRequest;
            if (request == null || !request.requestId().equals(requestId)) {
                request = new CdpRequest(session, requestId, responseUrl, "GET", Map.of(), null, "document", frame,
                        true, Map.of(), List.of());
            }
            navigationResponse = CdpResponse.from(session, request, params);
            Logger.debug(
                    false,
                    "Network",
                    "Navigation response observed: frame={}, requestId={}, url={}",
                    frame.id(),
                    requestId,
                    responseUrl.replaceAll("[?#].*$", "?<redacted>"));
        }
    }

    /**
     * Handles on loading failed.
     *
     * @param params protocol parameters
     */
    private void onLoadingFailed(CdpPayload params) {
        String requestId = PayloadReader.text(params.get("requestId"));
        if (StringKit.isNotBlank(navigationRequestId) && navigationRequestId.equals(requestId)) {
            navigationRequest = null;
            navigationResponse = null;
            Logger.warn(false, "Network", "Navigation loading failed: frame={}, requestId={}", frame.id(), requestId);
        }
    }

    /**
     * Handles update frame.
     *
     * @param framePayload frame payload value
     */
    private void updateFrame(CdpPayload framePayload) {
        String id = PayloadReader.text(framePayload.get("id"));
        String url = PayloadReader.text(framePayload.get("url"));
        String loaderId = PayloadReader.text(framePayload.get("loaderId"));
        if (StringKit.isNotBlank(id)) {
            frame.updateId(id);
        }
        if (StringKit.isNotBlank(url)) {
            frame.updateUrl(url);
        }
        if (StringKit.isNotBlank(loaderId)) {
            frame.updateLoaderId(loaderId);
        }
    }

    /**
     * Handles check lifecycle complete.
     */
    private void checkLifecycleComplete() {
        if (expectedLifecycle.isEmpty() || checkLifecycle(frame)) {
            if (lifecycle.complete(null)) {
                Logger.debug(
                        false,
                        "Page",
                        "Lifecycle watcher completed: frame={}, events={}",
                        frame.id(),
                        expectedLifecycle);
            }
        }
    }

    /**
     * Returns the check lifecycle.
     *
     * @param candidate candidate value
     * @return {@code true} when the condition matches
     */
    private boolean checkLifecycle(CdpFrame candidate) {
        Set<String> events = new HashSet<>(candidate.lifecycleEvents());
        if (candidate == frame) {
            events.addAll(observedLifecycle);
        }
        for (String expected : expectedLifecycle) {
            if (!events.contains(expected)) {
                return false;
            }
        }
        for (CdpFrame child : candidate.childFrames()) {
            if (child.hasStartedLoading() && !checkLifecycle(child)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the find frame.
     *
     * @param candidate candidate value
     * @param frameId   frame ID value
     * @return find frame value
     */
    private CdpFrame findFrame(CdpFrame candidate, String frameId) {
        if (candidate == null || StringKit.isBlank(frameId)) {
            return null;
        }
        if (frameId.equals(candidate.id())) {
            return candidate;
        }
        for (CdpFrame child : candidate.childFrames()) {
            CdpFrame found = findFrame(child, frameId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Returns the matches frame.
     *
     * @param frameId frame ID value
     * @return {@code true} when the condition matches
     */
    private boolean matchesFrame(String frameId) {
        String current = frame.id();
        if (StringKit.isBlank(current)) {
            if (StringKit.isNotBlank(frameId)) {
                frame.updateId(frameId);
            }
            return true;
        }
        return StringKit.isBlank(frameId) || current.equals(frameId);
    }

    /**
     * Returns the normalize lifecycle.
     *
     * @param waitUntil wait until value
     * @return values
     */
    private List<String> normalizeLifecycle(Collection<String> waitUntil) {
        Collection<String> source = CollKit.isEmpty(waitUntil) ? List.of("load") : waitUntil;
        List<String> result = new ArrayList<>();
        for (String item : source) {
            if (StringKit.isBlank(item)) {
                continue;
            }
            result.add(normalizeLifecycle(item));
        }
        return result;
    }

    /**
     * Returns the normalize lifecycle.
     *
     * @param value to use
     * @return normalize lifecycle value
     */
    private String normalizeLifecycle(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "domcontentloaded" -> "DOMContentLoaded";
            case "networkidle0" -> "networkIdle";
            case "networkidle2" -> "networkAlmostIdle";
            case "load" -> "load";
            default -> {
                if ("domcontentloaded".equals(lower)) {
                    yield "DOMContentLoaded";
                }
                throw new InternalException("Unknown value for options.waitUntil: " + value);
            }
        };
    }

}
