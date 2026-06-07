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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.runtime.CdpExecutionContext;
import org.miaixz.lancia.kernel.cdp.runtime.CdpRealm;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.kernel.cdp.target.CdpTarget;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.frame.FrameManager;
import org.miaixz.lancia.shared.frame.FrameTree;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Tracks the page frame tree, lifecycle events, and execution contexts.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpFrameManager extends EventEmitter<String> {

    /**
     * Returns the event name.
     */
    public static final String FRAME_ATTACHED = FrameManager.FRAME_ATTACHED.eventName();

    /**
     * Returns the event name.
     */
    public static final String FRAME_NAVIGATED = FrameManager.FRAME_NAVIGATED.eventName();

    /**
     * Returns the event name.
     */
    public static final String FRAME_DETACHED = FrameManager.FRAME_DETACHED.eventName();

    /**
     * Returns the event name.
     */
    public static final String FRAME_NAVIGATED_WITHIN_DOCUMENT = FrameManager.FRAME_NAVIGATED_WITHIN_DOCUMENT
            .eventName();

    /**
     * Returns the event name.
     */
    public static final String LIFECYCLE_EVENT = FrameManager.LIFECYCLE_EVENT.eventName();

    /**
     * Returns the event name.
     */
    public static final String FRAME_SWAPPED = FrameManager.FRAME_SWAPPED.eventName();

    /**
     * Returns the event name.
     */
    public static final String CONSOLE_API_CALLED = FrameManager.CONSOLE_API_CALLED.eventName();

    /**
     * Returns the event name.
     */
    public static final String BINDING_CALLED = FrameManager.BINDING_CALLED.eventName();

    /**
     * Shared constant for issue.
     */
    public static final String ISSUE = "FrameManager.Issue";
    /**
     * Shared constant for chrome extension prefix.
     */
    private static final String CHROME_EXTENSION_PREFIX = "chrome-extension://";
    /**
     * Current page.
     */
    private final Page page;
    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Current main frame.
     */
    private final CdpFrame mainFrame;
    /**
     * Current frame tree.
     */
    private final FrameTree<CdpFrame> frameTree;
    /**
     * Registered listened sessions values.
     */
    private final Set<CDPSession> listenedSessions = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Mapped scripts to evaluate on new document values.
     */
    private final Map<String, CdpPreloadScript> scriptsToEvaluateOnNewDocument = new LinkedHashMap<>();
    /**
     * Registered exposed functions values.
     */
    private final Set<ExposedFunction> exposedFunctions = new LinkedHashSet<>();
    /**
     * Registered frame navigated received values.
     */
    private final Set<String> frameNavigatedReceived = new LinkedHashSet<>();
    /**
     * Registered isolated worlds values.
     */
    private final Set<String> isolatedWorlds = new LinkedHashSet<>();
    /**
     * Whether initialized is enabled.
     */
    private boolean initialized;

    /**
     * Creates a frame manager.
     *
     * @param page page instance
     */
    public CdpFrameManager(Page page) {
        this(page, null);
    }

    /**
     * Creates a frame manager.
     *
     * @param page    page instance
     * @param session protocol session
     */
    public CdpFrameManager(Page page, CDPSession session) {
        this.page = page;
        this.session = session;
        this.mainFrame = new CdpFrame(page, session);
        this.frameTree = new FrameTree<>(mainFrame);
        Logger.debug(false, "Page", "CdpFrame manager initialized: hasSession={}", session != null);
    }

    /**
     * Initializes protocol state for this object.
     */
    public void initialize() {
        Logger.debug(true, "Page", "CdpFrame manager initialize requested: hasSession={}", session != null);
        if (session != null) {
            setupEventListeners(session);
            session.once(CDPSession.Events.DISCONNECTED, payload -> onClientDisconnect());
            session.send("Page.enable");
            session.send("Page.getFrameTree").thenAccept(result -> handleFrameTree(session, result.get("frameTree")));
            session.send("Page.setLifecycleEventsEnabled", Map.of("enabled", true));
            session.send("Runtime.enable").thenRun(() -> createIsolatedWorld(session, "__puppeteer_utility_world__"));
        }
        initialized = true;
        Logger.debug(false, "Page", "CdpFrame manager initialized: frames={}", frames().size());
    }

    /**
     * Returns the register speculative session.
     *
     * @param client protocol client
     * @return completion future
     */
    public CompletableFuture<Void> registerSpeculativeSession(CDPSession client) {
        if (client == null) {
            return CompletableFuture.completedFuture(null);
        }
        Logger.debug(true, "Page", "Speculative frame session register requested: session={}", client.id());
        setupEventListeners(client);
        Logger.debug(false, "Page", "Speculative frame session registered: session={}", client.id());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the swap frame tree.
     *
     * @param client protocol client
     * @return completion future
     */
    public CompletableFuture<Void> swapFrameTree(CDPSession client) {
        Logger.debug(
                true,
                "Page",
                "CdpFrame tree swap requested: session={}",
                client == null ? Normal.EMPTY : client.id());
        this.session = client;
        CdpFrame frame = mainFrame();
        if (client != null && CDPSession.Internal.targetInfo(client) != null) {
            removeFrameMapping(frame);
            frame.updateId(CDPSession.Internal.targetInfo(client).getTargetId());
            putFrame(frame);
            frame.updateClient(client);
        }
        if (client != null) {
            setupEventListeners(client);
            initializeClient(client, frame);
        }
        emit(FRAME_SWAPPED, frame);
        Logger.debug(
                false,
                "Page",
                "CdpFrame tree swapped: frame={}, session={}",
                frame.id(),
                client == null ? Normal.EMPTY : client.id());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles initialize client.
     *
     * @param client protocol client
     * @param frame  frame instance
     */
    public void initializeClient(CDPSession client, CdpFrame frame) {
        if (client == null) {
            return;
        }
        Logger.debug(
                true,
                "Page",
                "CdpFrame client initialize requested: session={}, frame={}",
                client.id(),
                frame == null ? Normal.EMPTY : frame.id());
        setupEventListeners(client);
        client.send("Page.enable");
        client.send("Page.getFrameTree").thenAccept(result -> handleFrameTree(client, result.get("frameTree")));
        client.send("Page.setLifecycleEventsEnabled", Map.of("enabled", true));
        client.send("Runtime.enable").thenRun(() -> createIsolatedWorld(client, "__puppeteer_utility_world__"));
        if (frame != null) {
            for (CdpPreloadScript script : scriptsToEvaluateOnNewDocument.values()) {
                frame.addPreloadScript(script);
            }
            for (ExposedFunction function : exposedFunctions) {
                frame.addExposedFunction(function);
            }
        }
        Logger.debug(
                false,
                "Page",
                "CdpFrame client initialized: session={}, frame={}",
                client.id(),
                frame == null ? Normal.EMPTY : frame.id());
    }

    /**
     * Returns the main frame.
     *
     * @return main frame value
     */
    public CdpFrame mainFrame() {
        return mainFrame;
    }

    /**
     * Returns the frames.
     *
     * @return values
     */
    public List<CdpFrame> frames() {
        return frameTree.frames();
    }

    /**
     * Returns the frame.
     *
     * @param frameId frame ID value
     * @return frame value
     */
    public CdpFrame frame(String frameId) {
        if (StringKit.isBlank(frameId)) {
            return null;
        }
        return frameTree.getById(frameId);
    }

    /**
     * Waits for frame.
     *
     * @param frameId frame id
     * @return wait for frame value
     */
    public CompletableFuture<CdpFrame> waitForFrame(String frameId) {
        return frameTree.waitForFrame(frameId);
    }

    /**
     * Returns whether initialized is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns the page.
     *
     * @return page value
     */
    public Page page() {
        return page;
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    public CDPSession client() {
        return session;
    }

    /**
     * Adds exposed function.
     *
     * @param function exposed function
     * @return add exposed function value
     */
    public CompletableFuture<Void> addExposedFunction(ExposedFunction function) {
        if (function == null) {
            return CompletableFuture.completedFuture(null);
        }
        exposedFunctions.add(function);
        Logger.debug(true, "Page", "CdpFrame exposed function add requested: frames={}", frames().size());
        return Awaitable.all(frames().stream().map(frame -> frame.addExposedFunction(function)).toList());
    }

    /**
     * Removes exposed function.
     *
     * @param function exposed function
     * @return remove exposed function value
     */
    public CompletableFuture<Void> removeExposedFunction(ExposedFunction function) {
        if (function == null) {
            return CompletableFuture.completedFuture(null);
        }
        exposedFunctions.remove(function);
        Logger.debug(true, "Page", "CdpFrame exposed function remove requested: frames={}", frames().size());
        return Awaitable.all(frames().stream().map(frame -> frame.removeExposedFunction(function)).toList());
    }

    /**
     * Returns the evaluate on new document.
     *
     * @param source source value
     * @return evaluate on new document value
     */
    public String evaluateOnNewDocument(String source) {
        if (session == null) {
            String identifier = "local-script-" + scriptsToEvaluateOnNewDocument.size();
            scriptsToEvaluateOnNewDocument.put(identifier, new CdpPreloadScript(mainFrame, identifier, source));
            Logger.debug(
                    false,
                    "Page",
                    "Preload script registered locally: id={}, chars={}",
                    identifier,
                    source == null ? Normal._0 : source.length());
            return identifier;
        }
        try {
            Logger.debug(
                    true,
                    "Page",
                    "Preload script add requested: chars={}",
                    source == null ? Normal._0 : source.length());
            CdpPayload result = session.send(
                    "Page.addScriptToEvaluateOnNewDocument",
                    Map.of("source", source == null ? Normal.EMPTY : source)).get();
            String identifier = PayloadReader.text(result.get("identifier"));
            identifier = uniquePreloadIdentifier(identifier);
            CdpPreloadScript script = new CdpPreloadScript(mainFrame, identifier, source);
            scriptsToEvaluateOnNewDocument.put(identifier, script);
            for (CdpFrame frame : frames()) {
                frame.addPreloadScript(script);
            }
            Logger.debug(false, "Page", "Preload script added: id={}, frames={}", identifier, frames().size());
            return identifier;
        } catch (Exception ex) {
            Logger.error(false, "Page", ex, "Preload script add failed.");
            throw new InternalException("Failed to add new document script.", ex);
        }
    }

    /**
     * Returns a unique preload identifier.
     *
     * @param identifier protocol identifier
     * @return unique identifier
     */
    private String uniquePreloadIdentifier(String identifier) {
        String actual = StringKit.isBlank(identifier) ? "script-" + scriptsToEvaluateOnNewDocument.size() : identifier;
        int index = scriptsToEvaluateOnNewDocument.size();
        while (scriptsToEvaluateOnNewDocument.containsKey(actual)) {
            actual = "script-" + (++index);
        }
        return actual;
    }

    /**
     * Removes script to evaluate on new document.
     *
     * @param identifier identifier
     * @return remove script to evaluate on new document value
     */
    public CompletableFuture<Void> removeScriptToEvaluateOnNewDocument(String identifier) {
        CdpPreloadScript script = scriptsToEvaluateOnNewDocument.remove(identifier);
        if (script == null) {
            CompletableFuture<Void> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(
                    new InternalException("Script to evaluate on new document with id " + identifier + " not found"));
            Logger.warn(false, "Page", "Preload script remove rejected: id={}", identifier);
            return rejected;
        }
        Logger.debug(true, "Page", "Preload script remove requested: id={}, frames={}", identifier, frames().size());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (CdpFrame frame : frames()) {
            String frameIdentifier = script.getIdForFrame(frame);
            if (StringKit.isNotBlank(frameIdentifier) && frame.client() != null) {
                futures.add(
                        frame.client()
                                .send("Page.removeScriptToEvaluateOnNewDocument", Map.of("identifier", frameIdentifier))
                                .exceptionally(error -> CdpPayload.NULL).thenApply(value -> null));
            }
        }
        return Awaitable.all(futures);
    }

    /**
     * Handles on attached to target.
     *
     * @param target target object
     */
    public void onAttachedToTarget(CdpTarget target) {
        if (target == null || !"iframe".equals(CdpTarget.Internal.targetInfo(target).getType())) {
            return;
        }
        CdpFrame frame = frame(CdpTarget.Internal.targetInfo(target).getTargetId());
        CdpTarget.Internal.session(target).ifPresent(client -> {
            if (frame != null) {
                frame.updateClient(client);
            }
            initializeClient(client, frame);
        });
    }

    /**
     * Updates up event listeners.
     *
     * @param client protocol client
     */
    private void setupEventListeners(CDPSession client) {
        if (client == null || !listenedSessions.add(client)) {
            return;
        }
        Logger.debug(false, "Page", "CdpFrame event listeners registered: session={}", client.id());
        client.on(
                "Page.frameAttached",
                event -> onFrameAttached(
                        client,
                        PayloadReader.text(event.get("frameId")),
                        PayloadReader.text(event.get("parentFrameId"))));
        client.on("Page.frameNavigated", event -> {
            frameNavigatedReceived.add(PayloadReader.text(event.get("frame").get("id")));
            onFrameNavigated(client, event.get("frame"), PayloadReader.text(event.get("type")));
        });
        client.on(
                "Page.navigatedWithinDocument",
                event -> onFrameNavigatedWithinDocument(
                        PayloadReader.text(event.get("frameId")),
                        PayloadReader.text(event.get("url"))));
        client.on(
                "Page.frameDetached",
                event -> onFrameDetached(
                        PayloadReader.text(event.get("frameId")),
                        PayloadReader.text(event.get("reason"))));
        client.on("Page.frameStartedLoading", event -> onFrameStartedLoading(PayloadReader.text(event.get("frameId"))));
        client.on("Page.frameStoppedLoading", event -> onFrameStoppedLoading(PayloadReader.text(event.get("frameId"))));
        client.on("Runtime.executionContextCreated", event -> onExecutionContextCreated(client, event.get("context")));
        client.on("Page.lifecycleEvent", this::onLifecycleEvent);
        client.on("Audits.issueAdded", event -> emit(ISSUE, CdpIssue.from(event.get("issue"))));
    }

    /**
     * Handles handle frame tree.
     *
     * @param client    protocol client
     * @param frameTree frame tree value
     */
    private void handleFrameTree(CDPSession client, CdpPayload frameTree) {
        if (frameTree == null || frameTree.isNull()) {
            return;
        }
        CdpPayload framePayload = frameTree.get("frame");
        String parentId = PayloadReader.text(framePayload.get("parentId"));
        if (StringKit.isNotBlank(parentId)) {
            onFrameAttached(client, PayloadReader.text(framePayload.get("id")), parentId);
        }
        String frameId = PayloadReader.text(framePayload.get("id"));
        if (!frameNavigatedReceived.remove(frameId)) {
            onFrameNavigated(client, framePayload, "Navigation");
        }
        CdpPayload childFrames = frameTree.get("childFrames");
        if (childFrames.isArray()) {
            for (CdpPayload child : childFrames.elements()) {
                handleFrameTree(client, child);
            }
        }
    }

    /**
     * Handles on frame attached.
     *
     * @param client        protocol client
     * @param frameId       frame ID value
     * @param parentFrameId parent frame ID value
     */
    private void onFrameAttached(CDPSession client, String frameId, String parentFrameId) {
        if (StringKit.isBlank(frameId)) {
            return;
        }
        CdpFrame frame = frame(frameId);
        CdpFrame parent = frame(parentFrameId);
        if (frame != null) {
            if (parent != null && frame.client() != parent.client()) {
                frame.updateClient(client);
            }
            return;
        }
        frame = new CdpFrame(page, client);
        frame.updateId(frameId);
        putFrame(frame);
        if (parent != null) {
            parent.addChildFrame(frame);
        }
        emit(FRAME_ATTACHED, frame);
        Logger.debug(
                false,
                "Page",
                "CdpFrame attached: frame={}, parent={}, session={}",
                frameId,
                parentFrameId,
                client == null ? Normal.EMPTY : client.id());
    }

    /**
     * Handles on frame navigated.
     *
     * @param client         protocol client
     * @param framePayload   frame payload value
     * @param navigationType navigation type value
     */
    private void onFrameNavigated(CDPSession client, CdpPayload framePayload, String navigationType) {
        String frameId = PayloadReader.text(framePayload.get("id"));
        if (StringKit.isBlank(frameId)) {
            return;
        }
        boolean main = framePayload.get("parentId").isNull();
        CdpFrame frame = frame(frameId);
        if (main) {
            frame = mainFrame;
            for (CdpFrame child : frame.childFrames()) {
                removeFramesRecursively(child);
            }
            removeFrameMapping(frame);
            frame.updateId(frameId);
            frame.updateClient(client);
            putFrame(frame);
        } else if (frame == null) {
            onFrameAttached(client, frameId, PayloadReader.text(framePayload.get("parentId")));
            frame = frame(frameId);
        }
        if (frame == null) {
            return;
        }
        frame._navigated(framePayload);
        emit(FRAME_NAVIGATED, frame);
        Logger.debug(
                false,
                "Page",
                "CdpFrame navigated: frame={}, type={}, url={}",
                frameId,
                navigationType,
                frame.url() == null ? Normal.EMPTY : frame.url().replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Handles on frame navigated within document.
     *
     * @param frameId frame ID value
     * @param url     target URL
     */
    private void onFrameNavigatedWithinDocument(String frameId, String url) {
        CdpFrame frame = frame(frameId);
        if (frame == null) {
            return;
        }
        frame._navigatedWithinDocument(url);
        emit(FRAME_NAVIGATED_WITHIN_DOCUMENT, frame);
        emit(FRAME_NAVIGATED, frame);
        Logger.debug(
                false,
                "Page",
                "CdpFrame navigated within document: frame={}, url={}",
                frameId,
                url == null ? Normal.EMPTY : url.replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Handles on frame detached.
     *
     * @param frameId frame ID value
     * @param reason  reason value
     */
    private void onFrameDetached(String frameId, String reason) {
        CdpFrame frame = frame(frameId);
        if (frame == null) {
            return;
        }
        if ("swap".equals(reason)) {
            emit(FRAME_SWAPPED, frame);
            Logger.debug(false, "Page", "CdpFrame swapped: frame={}", frameId);
            return;
        }
        Logger.debug(false, "Page", "CdpFrame detached: frame={}, reason={}", frameId, reason);
        removeFramesRecursively(frame);
    }

    /**
     * Handles on frame started loading.
     *
     * @param frameId frame ID value
     */
    private void onFrameStartedLoading(String frameId) {
        CdpFrame frame = frame(frameId);
        if (frame != null) {
            frame._onLoadingStarted();
            Logger.debug(false, "Page", "CdpFrame loading started: frame={}", frameId);
        }
    }

    /**
     * Handles on frame stopped loading.
     *
     * @param frameId frame ID value
     */
    private void onFrameStoppedLoading(String frameId) {
        CdpFrame frame = frame(frameId);
        if (frame != null) {
            frame._onLoadingStopped();
            emit(LIFECYCLE_EVENT, frame);
            Logger.debug(false, "Page", "CdpFrame loading stopped: frame={}", frameId);
        }
    }

    /**
     * Handles on lifecycle event.
     *
     * @param event event type
     */
    private void onLifecycleEvent(CdpPayload event) {
        CdpFrame frame = frame(PayloadReader.text(event.get("frameId")));
        if (frame == null) {
            return;
        }
        String name = PayloadReader.text(event.get("name"));
        frame._onLifecycleEvent(PayloadReader.text(event.get("loaderId")), name);
        emit(LIFECYCLE_EVENT, frame);
        Logger.debug(false, "Page", "CdpFrame lifecycle event: frame={}, event={}", frame.id(), name);
    }

    /**
     * Handles on execution context created.
     *
     * @param client         protocol client
     * @param contextPayload context payload value
     */
    private void onExecutionContextCreated(CDPSession client, CdpPayload contextPayload) {
        String frameId = PayloadReader.text(contextPayload.get("auxData").get("frameId"));
        CdpFrame frame = frame(frameId);
        if (frame == null || frame.client() != client) {
            return;
        }
        String origin = PayloadReader.text(contextPayload.get("origin"));
        if (origin.startsWith(CHROME_EXTENSION_PREFIX)) {
            CdpRealm realm = frame.extensionRealm(extractExtensionId(origin));
            realm.setOrigin(origin);
        }
        CdpExecutionContext context = new CdpExecutionContext(client, contextPayload.get("id").asInt(),
                PayloadReader.text(contextPayload.get("name")));
        context.on(CdpExecutionContext.CONSOLE_API_CALLED, event -> emit(CONSOLE_API_CALLED, List.of(frame, event)));
        context.on(CdpExecutionContext.BINDING_CALLED, event -> emit(BINDING_CALLED, List.of(frame, event)));
    }

    /**
     * Handles on client disconnect.
     */
    private void onClientDisconnect() {
        Logger.warn(
                false,
                "Page",
                "CdpFrame manager client disconnected: childFrames={}",
                mainFrame.childFrames().size());
        for (CdpFrame child : mainFrame.childFrames()) {
            removeFramesRecursively(child);
        }
    }

    /**
     * Creates isolated world.
     *
     * @param client protocol client
     * @param name   name to use
     */
    private void createIsolatedWorld(CDPSession client, String name) {
        String key = client.id() + Symbol.COLON + name;
        if (!isolatedWorlds.add(key)) {
            return;
        }
        Logger.debug(true, "Page", "Isolated world create requested: session={}, world={}", client.id(), name);
        client.send(
                "Page.addScriptToEvaluateOnNewDocument",
                Map.of("source", "//# sourceURL=__lancia_utility_world__", "worldName", name));
        for (CdpFrame frame : frames()) {
            if (frame.client() == client && StringKit.isNotBlank(frame.id())) {
                client.send(
                        "Page.createIsolatedWorld",
                        Map.of("frameId", frame.id(), "worldName", name, "grantUniveralAccess", true))
                        .exceptionally(error -> CdpPayload.NULL);
            }
        }
        Logger.debug(
                false,
                "Page",
                "Isolated world create dispatched: session={}, world={}, frames={}",
                client.id(),
                name,
                frames().size());
    }

    /**
     * Handles remove frames recursively.
     *
     * @param frame frame instance
     */
    private void removeFramesRecursively(CdpFrame frame) {
        for (CdpFrame child : frame.childFrames()) {
            removeFramesRecursively(child);
        }
        removeFrameMapping(frame);
        CdpFrame parent = frame.parentFrame();
        if (parent != null) {
            parent.removeChildFrame(frame);
        }
        emit(FRAME_DETACHED, frame);
        frame.detach();
        Logger.debug(false, "Page", "CdpFrame removed recursively: frame={}", frame.id());
    }

    /**
     * Handles put frame.
     *
     * @param frame frame instance
     */
    private void putFrame(CdpFrame frame) {
        frameTree.addFrame(frame);
    }

    /**
     * Handles remove frame mapping.
     *
     * @param frame frame instance
     */
    private void removeFrameMapping(CdpFrame frame) {
        frameTree.removeFrame(frame);
    }

    /**
     * Returns the extract extension ID.
     *
     * @param origin origin value
     * @return extract extension ID value
     */
    private String extractExtensionId(String origin) {
        if (StringKit.isBlank(origin) || !origin.startsWith(CHROME_EXTENSION_PREFIX)) {
            return Normal.EMPTY;
        }
        String pathPart = origin.substring(CHROME_EXTENSION_PREFIX.length());
        int slashIndex = pathPart.indexOf(Symbol.C_SLASH);
        return slashIndex < 0 ? pathPart : pathPart.substring(0, slashIndex);
    }

}
