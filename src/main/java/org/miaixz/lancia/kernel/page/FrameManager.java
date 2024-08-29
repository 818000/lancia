/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                               ~
 ~ The MIT License (MIT)                                                         ~
 ~                                                                               ~
 ~ Copyright (c) 2015-2024 miaixz.org and other contributors.                    ~
 ~                                                                               ~
 ~ Permission is hereby granted, free of charge, to any person obtaining a copy  ~
 ~ of this software and associated documentation files (the "Software"), to deal ~
 ~ in the Software without restriction, including without limitation the rights  ~
 ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     ~
 ~ copies of the Software, and to permit persons to whom the Software is         ~
 ~ furnished to do so, subject to the following conditions:                      ~
 ~                                                                               ~
 ~ The above copyright notice and this permission notice shall be included in    ~
 ~ all copies or substantial portions of the Software.                           ~
 ~                                                                               ~
 ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    ~
 ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      ~
 ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   ~
 ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        ~
 ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, ~
 ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     ~
 ~ THE SOFTWARE.                                                                 ~
 ~                                                                               ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.kernel.page;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.CollKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Emitter;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.nimble.page.FramePayload;
import org.miaixz.lancia.nimble.page.FrameStoppedLoadingEvent;
import org.miaixz.lancia.nimble.page.LifecycleEvent;
import org.miaixz.lancia.nimble.page.NavigatedWithinDocumentEvent;
import org.miaixz.lancia.nimble.runtime.ExecutionContextCreatedEvent;
import org.miaixz.lancia.nimble.runtime.ExecutionContextDescription;
import org.miaixz.lancia.nimble.runtime.ExecutionContextDestroyedEvent;
import org.miaixz.lancia.option.GoToOptions;
import org.miaixz.lancia.option.WaitForOptions;
import org.miaixz.lancia.socket.CDPSession;
import org.miaixz.lancia.worker.enums.CDPSessionEvent;
import org.miaixz.lancia.worker.enums.FrameEvent;
import org.miaixz.lancia.worker.enums.FrameManagerType;
import org.miaixz.lancia.worker.enums.PuppeteerLifeCycle;
import org.miaixz.lancia.worker.events.FrameAttachedEvent;
import org.miaixz.lancia.worker.events.FrameDetachedEvent;
import org.miaixz.lancia.worker.events.FrameNavigatedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class FrameManager extends Emitter<FrameManagerType> {

    private static final String UTILITY_WORLD_NAME = "__puppeteer_utility_world__";
    private final TimeoutSettings timeoutSettings;
    private final NetworkManager networkManager;
    private final Map<String, Frame> frames;
    private final Map<Integer, ExecutionContext> contextIdToContext;
    private final Set<String> isolatedWorlds;
    private CDPSession client;
    private Page page;
    private Frame mainFrame;

    public FrameManager(CDPSession client, Page page, TimeoutSettings timeoutSettings) {
        super();
        this.client = client;
        this.page = page;
        this.networkManager = new NetworkManager(client, this);
        this.timeoutSettings = timeoutSettings;
        this.frames = new HashMap<>();
        this.contextIdToContext = new HashMap<>();
        this.isolatedWorlds = new HashSet<>();
        this.client.on(CDPSessionEvent.Page_frameAttached, (Consumer<FrameAttachedEvent>) event -> this
                .onFrameAttached(event.getFrameId(), event.getParentFrameId()));
        this.client.on(CDPSessionEvent.Page_frameNavigated,
                (Consumer<FrameNavigatedEvent>) event -> this.onFrameNavigated(event.getFrame(), event.getType()));
        this.client.on(CDPSessionEvent.Page_navigatedWithinDocument,
                (Consumer<NavigatedWithinDocumentEvent>) event -> this
                        .onFrameNavigatedWithinDocument(event.getFrameId(), event.getUrl()));
        this.client.on(CDPSessionEvent.Page_frameDetached,
                (Consumer<FrameDetachedEvent>) event -> this.onFrameDetached(event.getFrameId(), event.getReason()));
        this.client.on(CDPSessionEvent.Page_frameStoppedLoading,
                (Consumer<FrameStoppedLoadingEvent>) event -> this.onFrameStoppedLoading(event.getFrameId()));
        this.client.on(CDPSessionEvent.Runtime_executionContextCreated,
                (Consumer<ExecutionContextCreatedEvent>) event -> this.onExecutionContextCreated(event.getContext()));
        this.client.on(CDPSessionEvent.Runtime_executionContextDestroyed,
                (Consumer<ExecutionContextDestroyedEvent>) event -> this
                        .onExecutionContextDestroyed(event.getExecutionContextId()));
        this.client.on(CDPSessionEvent.Runtime_executionContextsCleared, ignore -> this.onExecutionContextsCleared());
        this.client.on(CDPSessionEvent.Page_lifecycleEvent, (Consumer<LifecycleEvent>) this::onLifecycleEvent);
    }

    private void onLifecycleEvent(LifecycleEvent event) {
        Frame frame = this.frames.get(event.getFrameId());
        if (frame == null)
            return;
        frame.onLifecycleEvent(event.getLoaderId(), event.getName());
        this.emit(FrameManagerType.LifecycleEvent, frame);
        frame.emit(FrameEvent.LifecycleEvent, null);
    }

    private void onExecutionContextsCleared() {
        for (ExecutionContext context : this.contextIdToContext.values()) {
            if (context.getWorld() != null) {
                context.getWorld().setContext(null);
            }
        }
        this.contextIdToContext.clear();
    }

    private void onExecutionContextDestroyed(int executionContextId) {
        ExecutionContext context = this.contextIdToContext.get(executionContextId);
        if (context == null)
            return;
        this.contextIdToContext.remove(executionContextId);
        if (context.getWorld() != null) {
            context.getWorld().setContext(null);
        }
    }

    public ExecutionContext executionContextById(int contextId) {
        ExecutionContext context = this.contextIdToContext.get(contextId);
        Assert.isTrue(context != null, "INTERNAL ERROR: missing context with id = " + contextId);
        return context;
    }

    private void onExecutionContextCreated(ExecutionContextDescription contextPayload) {
        String frameId = contextPayload.getAuxData() != null ? contextPayload.getAuxData().getFrameId() : null;
        Frame frame = this.frames.get(frameId);
        DOMWorld world = null;
        if (frame != null) {
            if (contextPayload.getAuxData() != null && contextPayload.getAuxData().getIsDefault()) {
                world = frame.getMainWorld();
            } else if (contextPayload.getName().equals(UTILITY_WORLD_NAME) && !frame.getSecondaryWorld().hasContext()) {
                // In case of multiple sessions to the same target, there's a race between
                // connections so we might end up creating multiple isolated worlds.
                // We can use either.
                world = frame.getSecondaryWorld();
            }
        }
        if (contextPayload.getAuxData() != null && "isolated".equals(contextPayload.getAuxData().getType()))
            this.isolatedWorlds.add(contextPayload.getName());
        /* ${@link ExecutionContext} */
        ExecutionContext context = new ExecutionContext(this.client, contextPayload, world);
        if (world != null)
            world.setContext(context);
        this.contextIdToContext.put(contextPayload.getId(), context);

    }

    /**
     * @param frameId frame id
     */
    private void onFrameStoppedLoading(String frameId) {
        Frame frame = this.frames.get(frameId);
        if (frame == null)
            return;
        frame.onLoadingStopped();
        this.emit(FrameManagerType.LifecycleEvent, frame);
        frame.emit(FrameEvent.LifecycleEvent, null);
    }

    /**
     * @param frameId frame id
     */
    private void onFrameDetached(String frameId, String reason) {
        Frame frame = this.frames.get(frameId);
        if (frame == null)
            return;
        switch (reason) {
        case "remove":
            // Only remove the frame if the reason for the detached event is
            // an actual removement of the frame.
            // For frames that become OOP iframes, the reason would be 'swap'.
            this.removeFramesRecursively(frame);
            break;
        case "swap":
            this.emit(FrameManagerType.FrameSwapped, frame);
            frame.emit(FrameEvent.FrameSwapped, null);
            break;
        }
    }

    /**
     * @param frameId frame id
     * @param url     url
     */
    private void onFrameNavigatedWithinDocument(String frameId, String url) {
        Frame frame = this.frames.get(frameId);
        if (frame == null) {
            return;
        }
        frame.navigatedWithinDocument(url);
        this.emit(FrameManagerType.FrameNavigatedWithinDocument, frame);
        frame.emit(FrameEvent.FrameNavigatedWithinDocument, null);
        this.emit(FrameManagerType.FrameNavigated, frame);
        frame.emit(FrameEvent.FrameNavigated, "Navigation");
    }

    public void initialize() {
        this.client.send("Page.enable");
        /* @type Protocol.Page.getFrameTreeReturnValue */
        JsonNode result = this.client.send("Page.getFrameTree");

        FrameTree frameTree;
        try {
            frameTree = Builder.OBJECTMAPPER.treeToValue(result.get("frameTree"), FrameTree.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        this.handleFrameTree(frameTree);

        Map<String, Object> params = new HashMap<>();
        params.put("enabled", true);
        this.client.send("Page.setLifecycleEventsEnabled", params);
        this.client.send("Runtime.enable");
        this.ensureIsolatedWorld(UTILITY_WORLD_NAME);
        this.networkManager.initialize();

    }

    private void ensureIsolatedWorld(String name) {
        if (this.isolatedWorlds.contains(name))
            return;
        this.isolatedWorlds.add(name);
        Map<String, Object> params = new HashMap<>();
        params.put("source", "//# sourceURL=" + ExecutionContext.EVALUATION_SCRIPT_URL);
        params.put("worldName", name);
        this.client.send("Page.addScriptToEvaluateOnNewDocument", params);
        this.frames().forEach(frame -> {
            Map<String, Object> param = new HashMap<>();
            param.put("frameId", frame.getId());
            param.put("grantUniveralAccess", true);
            param.put("worldName", name);
            this.client.send("Page.createIsolatedWorld", param);
        });
    }

    private void handleFrameTree(FrameTree frameTree) {
        if (StringKit.isNotEmpty(frameTree.getFrame().getParentId())) {
            this.onFrameAttached(frameTree.getFrame().getId(), frameTree.getFrame().getParentId());
        }
        this.onFrameNavigated(frameTree.getFrame(), "Navigation");
        if (CollKit.isEmpty(frameTree.getChildFrames()))
            return;
        for (FrameTree child : frameTree.getChildFrames()) {
            this.handleFrameTree(child);
        }
    }

    /**
     * @param frameId       frame id
     * @param parentFrameId parent frame id
     */
    private void onFrameAttached(String frameId, String parentFrameId) {
        if (this.frames.get(frameId) != null)
            return;
        Assert.isTrue(StringKit.isNotEmpty(parentFrameId), "parentFrameId is null");
        Frame parentFrame = this.frames.get(parentFrameId);
        Frame frame = new Frame(this, this.client, parentFrame, frameId);
        this.frames.put(frame.getId(), frame);
        this.emit(FrameManagerType.FrameAttached, frame);
    }

    /**
     * @param framePayload frame荷载
     */
    private void onFrameNavigated(FramePayload framePayload, String navigationType) {
        boolean isMainFrame = StringKit.isEmpty(framePayload.getParentId());
        Frame frame = isMainFrame ? this.mainFrame : this.frames.get(framePayload.getId());
        Assert.isTrue(isMainFrame || frame != null,
                "We either navigate top level or have old version of the navigated frame");

        // Detach all child frames first.
        if (frame != null) {
            if (CollKit.isNotEmpty(frame.getChildFrames())) {
                for (Frame childFrame : frame.getChildFrames()) {
                    this.removeFramesRecursively(childFrame);
                }
            }
        }

        // Update or create main frame.
        if (isMainFrame) {
            if (frame != null) {
                // Update frame id to retain frame identity on cross-process navigation.
                this.frames.remove(frame.getId());
                frame.setId(framePayload.getId());
            } else {
                // Initial main frame navigation.
                frame = new Frame(this, this.client, null, framePayload.getId());
            }
            this.frames.put(framePayload.getId(), frame);
            this.mainFrame = frame;
        }

        // Update frame payload.
        frame.navigated(framePayload);
        this.emit(FrameManagerType.FrameNavigated, frame);
        frame.emit(FrameEvent.FrameNavigated, navigationType);
    }

    public List<Frame> frames() {
        if (this.frames.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(this.frames.values());
    }

    /**
     * @param childFrame 子frame
     */
    private void removeFramesRecursively(Frame childFrame) {
        if (CollKit.isNotEmpty(childFrame.getChildFrames())) {
            for (Frame frame : childFrame.getChildFrames()) {
                this.removeFramesRecursively(frame);
            }
        }
        childFrame.detach();
        this.frames.remove(childFrame.getId());
        this.emit(FrameManagerType.FrameDetached, childFrame);
        childFrame.emit(FrameEvent.FrameDetached, childFrame);
    }

    public CDPSession getClient() {
        return client;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public TimeoutSettings getTimeoutSettings() {
        return timeoutSettings;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public Map<String, Frame> getFrames() {
        return frames;
    }

    public Map<Integer, ExecutionContext> getContextIdToContext() {
        return contextIdToContext;
    }

    public Set<String> getIsolatedWorlds() {
        return isolatedWorlds;
    }

    public Frame getMainFrame() {
        return mainFrame;
    }

    public Response navigateFrame(Frame frame, String url, GoToOptions options, boolean isBlock) {
        String referrer;
        String refererPolicy;
        List<PuppeteerLifeCycle> waitUntil;
        Integer timeout;
        if (options == null) {
            referrer = frame.getFrameManager().getNetworkManager().extraHTTPHeaders().get("referer");
            refererPolicy = frame.getFrameManager().getNetworkManager().extraHTTPHeaders().get("referer_policy");
            waitUntil = new ArrayList<>();
            waitUntil.add(PuppeteerLifeCycle.LOAD);
            timeout = frame.getFrameManager().getTimeoutSettings().navigationTimeout();
        } else {
            if (StringKit.isEmpty(referrer = options.getReferer())) {
                referrer = frame.getFrameManager().getNetworkManager().extraHTTPHeaders().get("referer");
            }
            if (CollKit.isEmpty(waitUntil = options.getWaitUntil())) {
                waitUntil = new ArrayList<>();
                waitUntil.add(PuppeteerLifeCycle.LOAD);
            }
            if ((timeout = options.getTimeout()) == null) {
                timeout = frame.getFrameManager().getTimeoutSettings().navigationTimeout();
            }
            if (StringKit.isEmpty(refererPolicy = options.getReferrerPolicy())) {
                refererPolicy = frame.getFrameManager().getNetworkManager().extraHTTPHeaders().get("referer");
            }
        }
        if (!isBlock) {
            Map<String, Object> params = new HashMap<>();
            params.put("url", url);
            // jackJson 不序列化null值对 HashMap里面的 null值不起作用
            if (referrer != null) {
                params.put("referrer", referrer);
            }
            params.put("frameId", frame.getId());
            this.client.send("Page.navigate", params, null, false);
            return null;
        }
        AtomicBoolean ensureNewDocumentNavigation = new AtomicBoolean(false);
        LifecycleWatcher watcher = new LifecycleWatcher(frame.getFrameManager().getNetworkManager(), frame, waitUntil,
                timeout);
        try {
            String finalReferrer = referrer;
            String finalRefererPolicy = refererPolicy;
            CompletableFuture<Void> navigateFuture = CompletableFuture.runAsync(() -> {
                navigate(this.client, url, finalReferrer, finalRefererPolicy, frame.getId(),
                        ensureNewDocumentNavigation);
            });
            CompletableFuture<Void> terminationFuture = CompletableFuture.runAsync(watcher::waitForTermination);
            CompletableFuture<Object> anyOfFuture1 = CompletableFuture.anyOf(navigateFuture, terminationFuture);
            anyOfFuture1.whenComplete((ignore, throwable1) -> {
                if (throwable1 == null) {// 没有出错就是LifecycleWatcher没有接收到termination事件,那就看看是newDocumentNavigation还是sameDocumentNavigation,并等待它完成
                    CompletableFuture<Void> documentNavigationFuture = CompletableFuture.runAsync(() -> {
                        if (ensureNewDocumentNavigation.get()) {
                            watcher.waitForNewDocumentNavigation();
                        } else {
                            watcher.waitForSameDocumentNavigation();
                        }
                    });
                    CompletableFuture<Object> anyOfFuture2 = CompletableFuture.anyOf(terminationFuture,
                            documentNavigationFuture);
                    anyOfFuture2.whenComplete((ignore1, throwable2) -> {
                        if (throwable2 != null) {
                            throw new InternalException(throwable2);
                        }
                    });
                    anyOfFuture2.join();
                }
            });
            // 等待页面导航事件或者是页面termination事件完成
            anyOfFuture1.join();
            return watcher.navigationResponse();
        } finally {
            watcher.dispose();
        }
    }

    private void navigate(CDPSession client, String url, String referrer, String referrerPolicy, String frameId,
            AtomicBoolean ensureNewDocumentNavigation) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", url);
        params.put("referrer", referrer);
        params.put("frameId", frameId);
        params.put("referrerPolicy", referrerPolicy);
        JsonNode response = client.send("Page.navigate", params);
        if (response == null) {
            return;
        }
        if (StringKit.isNotEmpty(response.get("loaderId").asText())) {
            ensureNewDocumentNavigation.set(true);
        }
        String errorText = null;
        if (response.get("errorText") != null && StringKit.isNotEmpty(errorText = response.get("errorText").asText())
                && "net::ERR_HTTP_RESPONSE_CODE_FAILURE".equals(response.get("errorText").asText())) {
            return;
        }
        if (StringKit.isNotEmpty(errorText))
            throw new InternalException(errorText + " at " + url);
    }

    public Frame getFrame(String frameId) {
        return this.frames.get(frameId);
    }

    public Frame frame(String frameId) {
        return this.frames.get(frameId);
    }

    public Response waitForFrameNavigation(Frame frame, WaitForOptions options, boolean reload) {
        Integer timeout;
        List<PuppeteerLifeCycle> waitUntil;
        boolean ignoreSameDocumentNavigation;
        if (options == null) {
            ignoreSameDocumentNavigation = false;
            waitUntil = new ArrayList<>();
            waitUntil.add(PuppeteerLifeCycle.LOAD);
            timeout = this.getTimeoutSettings().navigationTimeout();
        } else {
            if (CollKit.isEmpty(waitUntil = options.getWaitUntil())) {
                waitUntil = new ArrayList<>();
                waitUntil.add(PuppeteerLifeCycle.LOAD);
            }
            if ((timeout = options.getTimeout()) == null) {
                timeout = this.timeoutSettings.navigationTimeout();
            }
            ignoreSameDocumentNavigation = options.isIgnoreSameDocumentNavigation();
        }
        LifecycleWatcher watcher = new LifecycleWatcher(frame.getFrameManager().getNetworkManager(), frame, waitUntil,
                timeout);
        AtomicReference<Response> result = new AtomicReference<>();
        try {
            CompletableFuture<Void> terminationFuture = CompletableFuture.runAsync(() -> {
                // 如果是reload页面，需要在等待之前发送刷新命令
                if (reload) {
                    this.client.send("Page.reload", null, null, false);
                }
                watcher.waitForTermination();
            });
            CompletableFuture<Void> sameDocumentNavigationFuture = null;
            if (!ignoreSameDocumentNavigation) {
                sameDocumentNavigationFuture = CompletableFuture.runAsync(() -> {
                    watcher.waitForSameDocumentNavigation();
                });
            }
            CompletableFuture<Void> newDocumentNavigationFuture = CompletableFuture.runAsync(() -> {
                watcher.waitForNewDocumentNavigation();
            });
            CompletableFuture<Object> anyOfFutrue1 = sameDocumentNavigationFuture == null
                    ? CompletableFuture.anyOf(terminationFuture, newDocumentNavigationFuture)
                    : CompletableFuture.anyOf(terminationFuture, newDocumentNavigationFuture,
                            sameDocumentNavigationFuture);
            anyOfFutrue1.whenComplete((ignore, throwable) -> {
                if (throwable != null) {
                    return;
                }
                CompletableFuture<Response> responseFuture = CompletableFuture.supplyAsync(watcher::navigationResponse);
                CompletableFuture<Object> anyOfFuture2 = CompletableFuture.anyOf(terminationFuture, responseFuture);
                anyOfFuture2.whenComplete((ignore1, throwable1) -> {
                    result.set((Response) ignore1);
                });
                anyOfFuture2.join();
            });
            anyOfFutrue1.join();
            return result.get();
        } finally {
            watcher.dispose();
        }
    }

    private void assertNoLegacyNavigationOptions(List<PuppeteerLifeCycle> waitUtil) {
        Assert.isTrue(!PuppeteerLifeCycle.NETWORKIDLE.equals(waitUtil.get(0)),
                "ERROR: \"networkidle\" option is no longer supported. Use \"networkidle2\" instead");
    }

    public Frame mainFrame() {
        return this.mainFrame;
    }

    public NetworkManager networkManager() {
        return this.networkManager;
    }

}
