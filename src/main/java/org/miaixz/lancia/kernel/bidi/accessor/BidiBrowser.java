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
package org.miaixz.lancia.kernel.bidi.accessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.bidi.session.BidiProtocolSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Coordinates remote browser operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiBrowser implements AutoCloseable {

    /**
     * Shared constant for closed.
     */
    public static final String CLOSED = "closed";

    /**
     * Shared constant for disconnected.
     */
    public static final String DISCONNECTED = "disconnected";

    /**
     * Shared constant for shared worker.
     */
    public static final String SHARED_WORKER = "sharedworker";

    /**
     * Default user context.
     */
    public static final String DEFAULT_USER_CONTEXT = Normal.DEFAULT;
    /**
     * Current session.
     */
    private final BidiProtocolSession session;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Registered user contexts values.
     */
    private final Set<String> userContexts = ConcurrentHashMap.newKeySet();
    /**
     * Mapped browsing contexts values.
     */
    private final Map<String, CdpPayload> browsingContexts = new ConcurrentHashMap<>();
    /**
     * Mapped shared workers values.
     */
    private final Map<String, CdpPayload> sharedWorkers = new ConcurrentHashMap<>();
    /**
     * Mapped allow and name download paths values.
     */
    private final Map<String, Path> allowAndNameDownloadPaths = new ConcurrentHashMap<>();
    /**
     * Mapped download contexts values.
     */
    private final Map<String, String> downloadContexts = new ConcurrentHashMap<>();
    /**
     * Mapped named downloads values.
     */
    private final Map<String, Path> namedDownloads = new ConcurrentHashMap<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Thread-safe closed state.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    /**
     * Current reason.
     */
    private volatile String reason;

    /**
     * Returns the from.
     *
     * @param session protocol session
     * @return completion future
     */
    public static CompletableFuture<BidiBrowser> from(BidiProtocolSession session) {
        BidiBrowser browser = new BidiBrowser(session);
        return browser.initialize().thenApply(value -> browser);
    }

    /**
     * Creates a BiDi browser.
     *
     * @param session protocol session
     */
    public BidiBrowser(BidiProtocolSession session) {
        this.session = Assert.notNull(session, "session");
        this.userContexts.add(DEFAULT_USER_CONTEXT);
    }

    /**
     * Returns the initialize.
     *
     * @return completion future
     */
    private CompletableFuture<Void> initialize() {
        binding = binding.combine(session.connection().on("script.realmCreated", this::onRealmCreated));
        binding = binding
                .combine(session.connection().on("browsingContext.downloadWillBegin", this::onDownloadWillBegin));
        binding = binding.combine(session.connection().on("browsingContext.downloadEnd", this::onDownloadEnd));
        binding = binding.combine(session.connection().on("browser.downloadWillBegin", this::onDownloadWillBegin));
        binding = binding.combine(session.connection().on("browser.downloadProgress", this::onDownloadEnd));
        return syncUserContexts().thenCompose(value -> syncBrowsingContexts());
    }

    /**
     * Returns the sync user contexts.
     *
     * @return completion future
     */
    private CompletableFuture<Void> syncUserContexts() {
        return session.send("browser.getUserContexts", Map.of()).thenAccept(result -> {
            CdpPayload contexts = result.get("userContexts");
            if (contexts == null || !contexts.isArray()) {
                return;
            }
            for (CdpPayload context : contexts.elements()) {
                String id = PayloadReader.text(context.get("userContext"));
                if (StringKit.isNotBlank(id)) {
                    userContexts.add(id);
                }
            }
        });
    }

    /**
     * Returns the sync browsing contexts.
     *
     * @return completion future
     */
    private CompletableFuture<Void> syncBrowsingContexts() {
        return session.send("browsingContext.getTree", Map.of()).thenAccept(result -> {
            CdpPayload contexts = result.get("contexts");
            if (contexts == null || !contexts.isArray()) {
                return;
            }
            for (CdpPayload context : contexts.elements()) {
                registerBrowsingContext(context);
            }
        });
    }

    /**
     * Adds preload script.
     *
     * @param functionDeclaration function declaration
     * @param contextIds          context ids
     * @param options             operation options
     * @return add preload script value
     */
    public CompletableFuture<String> addPreloadScript(
            String functionDeclaration,
            List<String> contextIds,
            Map<String, Object> options) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("functionDeclaration", Assert.notBlank(functionDeclaration, "functionDeclaration"));
        if (contextIds != null && !contextIds.isEmpty()) {
            params.put("contexts", List.copyOf(contextIds));
        }
        return session.send("script.addPreloadScript", params)
                .thenApply(result -> PayloadReader.text(result.get("script")));
    }

    /**
     * Removes intercept.
     *
     * @param intercept intercept
     * @return remove intercept value
     */
    public CompletableFuture<Void> removeIntercept(String intercept) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        return session.send("network.removeIntercept", Map.of("intercept", Assert.notBlank(intercept, "intercept")))
                .thenApply(result -> null);
    }

    /**
     * Removes preload script.
     *
     * @param script script source
     * @return remove preload script value
     */
    public CompletableFuture<Void> removePreloadScript(String script) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        return session.send("script.removePreloadScript", Map.of("script", Assert.notBlank(script, "script")))
                .thenApply(result -> null);
    }

    /**
     * Creates user context.
     *
     * @param options operation options
     * @return created user context
     */
    public CompletableFuture<String> createUserContext(Map<String, Object> options) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        Map<String, Object> actualOptions = options == null ? Map.of() : options;
        Object proxyServer = actualOptions.get("proxyServer");
        if (proxyServer instanceof String server && StringKit.isNotBlank(server)) {
            params.put(
                    "proxy",
                    Map.of(
                            "proxyType",
                            "manual",
                            "httpProxy",
                            server,
                            "sslProxy",
                            server,
                            "noProxy",
                            actualOptions.getOrDefault("proxyBypassList", List.of())));
        }
        return session.send("browser.createUserContext", params).thenCompose(result -> {
            String userContext = PayloadReader.text(result.get("userContext"));
            if (StringKit.isBlank(userContext)) {
                return Awaitable.failed("browser.createUserContext did not return userContext.");
            }
            userContexts.add(userContext);
            return configureDownloadBehavior(userContext, actualOptions).thenApply(value -> userContext);
        });
    }

    /**
     * Returns the install extension.
     *
     * @param path file path
     * @return completion future
     */
    public CompletableFuture<String> installExtension(String path) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        return session
                .send(
                        "webExtension.install",
                        Map.of("extensionData", Map.of("type", "path", "path", Assert.notBlank(path, "path"))))
                .thenApply(result -> PayloadReader.text(result.get("extension")));
    }

    /**
     * Returns the uninstall extension.
     *
     * @param id identifier
     * @return completion future
     */
    public CompletableFuture<Void> uninstallExtension(String id) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        return session.send("webExtension.uninstall", Map.of("extension", Assert.notBlank(id, "id")))
                .thenApply(result -> null);
    }

    /**
     * Lists installed extensions.
     *
     * @return list extensions value
     */
    public CompletableFuture<List<CdpPayload>> listExtensions() {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        return session.send("webExtension.getInstalled", Map.of()).thenApply(result -> {
            List<CdpPayload> infos = new ArrayList<>();
            CdpPayload extensions = PayloadReader
                    .first(CdpPayload.NULL, result.get("extensions"), result.get("webExtensions"));
            if (extensions == null || !extensions.isArray()) {
                return infos;
            }
            for (CdpPayload extension : extensions.elements()) {
                infos.add(extensionInfo(extension));
            }
            return infos;
        });
    }

    /**
     * Updates client window state.
     *
     * @param params protocol parameters
     * @return set client window state value
     */
    public CompletableFuture<Void> setClientWindowState(Map<String, Object> params) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        return session.send("browser.setClientWindowState", params == null ? Map.of() : params)
                .thenApply(result -> null);
    }

    /**
     * Updates download behavior.
     *
     * @param params protocol parameters
     * @return set download behavior value
     */
    public CompletableFuture<CdpPayload> setDownloadBehavior(CdpPayload params) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        CdpPayload actualParams = params == null ? CdpPayload.NULL : params;
        String userContext = PayloadReader.text(actualParams.get("userContext"));
        if (StringKit.isBlank(userContext)) {
            userContext = DEFAULT_USER_CONTEXT;
        }
        Map<String, Object> downloadBehavior = new LinkedHashMap<>();
        String policy = PayloadReader
                .text(PayloadReader.first(CdpPayload.NULL, actualParams.get("policy"), actualParams.get("behavior")));
        if (StringKit.isBlank(policy)) {
            policy = PayloadReader.text(
                    PayloadReader.first(
                            CdpPayload.NULL,
                            actualParams.get("downloadBehavior").get("policy"),
                            actualParams.get("downloadBehavior").get("behavior")));
        }
        downloadBehavior.put("policy", policy);
        String downloadPath = PayloadReader.text(
                PayloadReader.first(
                        CdpPayload.NULL,
                        actualParams.get("downloadPath"),
                        actualParams.get("downloadBehavior").get("downloadPath")));
        if (StringKit.isNotBlank(downloadPath)) {
            downloadBehavior.put("downloadPath", downloadPath);
        }
        Map<String, Object> options = Map.of("downloadBehavior", downloadBehavior);
        return configureDownloadBehavior(userContext, options).thenApply(value -> CdpPayload.of(Map.of()));
    }

    /**
     * Returns the named download path.
     *
     * @param userContext user context value
     * @param guid        guid value
     * @return optional value
     */
    public Optional<Path> namedDownloadPath(String userContext, String guid) {
        String actualUserContext = StringKit.isBlank(userContext) ? DEFAULT_USER_CONTEXT : userContext;
        String actualGuid = Assert.notBlank(guid, "guid");
        return Optional.ofNullable(namedDownloads.get(downloadKey(actualUserContext, actualGuid)));
    }

    /**
     * Returns the client window info.
     *
     * @param windowId window id
     * @return client window info
     */
    public CompletableFuture<CdpPayload> getClientWindowInfo(String windowId) {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        String actualWindowId = Assert.notBlank(windowId, "windowId");
        return session.send("browser.getClientWindows", Map.of()).thenApply(result -> {
            CdpPayload windows = result.get("clientWindows");
            if (windows != null && windows.isArray()) {
                for (CdpPayload window : windows.elements()) {
                    if (actualWindowId.equals(PayloadReader.text(window.get("clientWindow")))) {
                        return window;
                    }
                }
            }
            throw new InternalException("Could not find client window: " + actualWindowId);
        });
    }

    /**
     * Returns the close async.
     *
     * @return completion future
     */
    public CompletableFuture<Void> closeAsync() {
        if (disposed.get()) {
            return Awaitable.failed("BiDi Core Browser has been disposed.");
        }
        return session.send("browser.close", Map.of()).handle((result, throwable) -> {
            dispose("Browser already closed.", true);
            if (throwable != null) {
                throw new InternalException("Failed to close BiDi Core Browser.", throwable);
            }
            return null;
        });
    }

    /**
     * Releases resources held by this object.
     *
     * @param reason reason
     * @param closed closed
     */
    public void dispose(String reason, boolean closed) {
        if (disposed.compareAndSet(false, true)) {
            this.closed.set(closed);
            this.reason = StringKit.isBlank(reason) ? "Browser was disconnected, probably because the session ended."
                    : reason;
            if (closed) {
                emitter.emit(CLOSED, this.reason);
            }
            emitter.emit(DISCONNECTED, this.reason);
            binding.unbind();
            binding = new EventBinding();
            allowAndNameDownloadPaths.clear();
            downloadContexts.clear();
        }
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        closeAsync().join();
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        return EventHooks.onNamed(emitter, event, listener);
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String event, Consumer<Object> listener) {
        return EventHooks.onceNamed(emitter, event, listener);
    }

    /**
     * Returns the listener count.
     *
     * @param event event type
     * @return listener count value
     */
    public int listenerCount(String event) {
        return emitter.listenerCount(Assert.notBlank(event, "event"));
    }

    /**
     * Returns whether this object is closed.
     *
     * @return whether this object is closed
     */
    public boolean closed() {
        return closed.get();
    }

    /**
     * Returns the disconnected.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disconnected() {
        return disposed.get();
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return disposed.get();
    }

    /**
     * Returns the default user context.
     *
     * @return default user context value
     */
    public String defaultUserContext() {
        return DEFAULT_USER_CONTEXT;
    }

    /**
     * Returns the user contexts.
     *
     * @return values
     */
    public Set<String> userContexts() {
        return Set.copyOf(userContexts);
    }

    /**
     * Returns the browsing contexts.
     *
     * @return mapped values
     */
    public Map<String, CdpPayload> browsingContexts() {
        return Map.copyOf(browsingContexts);
    }

    /**
     * Returns the shared workers.
     *
     * @return mapped values
     */
    public Map<String, CdpPayload> sharedWorkers() {
        return Map.copyOf(sharedWorkers);
    }

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    public BidiProtocolSession session() {
        return session;
    }

    /**
     * Returns the reason.
     *
     * @return reason value
     */
    public String reason() {
        return reason == null ? Normal.EMPTY : reason;
    }

    /**
     * Handles on realm created.
     *
     * @param info info value
     */
    private void onRealmCreated(CdpPayload info) {
        if (!"shared-worker".equals(PayloadReader.text(info.get("type")))) {
            return;
        }
        String realm = PayloadReader.text(info.get("realm"));
        if (StringKit.isBlank(realm)) {
            return;
        }
        sharedWorkers.put(realm, info);
        emitter.emit(SHARED_WORKER, info);
    }

    /**
     * Handles register browsing context.
     *
     * @param context browser context
     */
    private void registerBrowsingContext(CdpPayload context) {
        String id = PayloadReader.text(context.get("context"));
        if (StringKit.isNotBlank(id)) {
            browsingContexts.put(id, context);
        }
        CdpPayload children = context.get("children");
        if (children != null && children.isArray()) {
            for (CdpPayload child : children.elements()) {
                registerBrowsingContext(child);
            }
        }
    }

    /**
     * Handles on download will begin.
     *
     * @param event event type
     */
    private void onDownloadWillBegin(CdpPayload event) {
        String guid = downloadGuid(event);
        if (StringKit.isBlank(guid)) {
            return;
        }
        downloadContexts.put(guid, downloadUserContext(event));
    }

    /**
     * Handles on download end.
     *
     * @param event event type
     */
    private void onDownloadEnd(CdpPayload event) {
        if (!downloadComplete(event)) {
            return;
        }
        String guid = downloadGuid(event);
        if (StringKit.isBlank(guid)) {
            return;
        }
        String userContext = downloadContexts.getOrDefault(guid, downloadUserContext(event));
        Path directory = allowAndNameDownloadPaths.get(userContext);
        if (directory == null) {
            return;
        }
        Path target = directory.resolve(guid);
        Path source = downloadSource(event, directory);
        try {
            FileKit.mkdir(directory.toFile());
            if (source != null && Files.exists(source) && !source.equals(target)) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.exists(target)) {
                namedDownloads.put(downloadKey(userContext, guid), target);
            }
        } catch (IOException | RuntimeException ignored) {
            namedDownloads.remove(downloadKey(userContext, guid));
        } finally {
            downloadContexts.remove(guid);
        }
    }

    /**
     * Returns the download complete.
     *
     * @param event event type
     * @return {@code true} when the condition matches
     */
    private static boolean downloadComplete(CdpPayload event) {
        String status = PayloadReader
                .text(PayloadReader.first(CdpPayload.NULL, event.get("status"), event.get("state")));
        return StringKit.isBlank(status) || "complete".equals(status) || "completed".equals(status);
    }

    /**
     * Returns the download source.
     *
     * @param event     event type
     * @param directory directory value
     * @return download source value
     */
    private static Path downloadSource(CdpPayload event, Path directory) {
        String downloadedFilepath = PayloadReader.text(
                PayloadReader.first(
                        CdpPayload.NULL,
                        event.get("downloadedFilepath"),
                        event.get("downloadedFilePath"),
                        event.get("filePath"),
                        event.get("filepath")));
        if (StringKit.isNotBlank(downloadedFilepath)) {
            return Path.of(downloadedFilepath);
        }
        String suggestedFilename = PayloadReader.text(event.get("suggestedFilename"));
        return StringKit.isBlank(suggestedFilename) ? null : directory.resolve(suggestedFilename);
    }

    /**
     * Returns the download guid.
     *
     * @param event event type
     * @return download guid value
     */
    private static String downloadGuid(CdpPayload event) {
        return PayloadReader.text(
                PayloadReader.first(
                        CdpPayload.NULL,
                        event.get("guid"),
                        event.get("downloadId"),
                        event.get("download"),
                        event.get("navigation")));
    }

    /**
     * Returns the download user context.
     *
     * @param event event type
     * @return download user context value
     */
    private String downloadUserContext(CdpPayload event) {
        String userContext = PayloadReader.text(event.get("userContext"));
        if (StringKit.isNotBlank(userContext)) {
            return userContext;
        }
        CdpPayload context = browsingContexts.get(PayloadReader.text(event.get("context")));
        userContext = context == null ? Normal.EMPTY : PayloadReader.text(context.get("userContext"));
        return StringKit.isBlank(userContext) ? DEFAULT_USER_CONTEXT : userContext;
    }

    /**
     * Returns the configure download behavior.
     *
     * @param userContext user context value
     * @param options     operation options
     * @return completion future
     */
    private CompletableFuture<Void> configureDownloadBehavior(String userContext, Map<String, Object> options) {
        Object behavior = options.get("downloadBehavior");
        if (!(behavior instanceof Map<?, ?> raw)) {
            return CompletableFuture.completedFuture(null);
        }
        Map<String, Object> downloadBehavior = (Map<String, Object>) raw;
        String policy = String.valueOf(downloadBehavior.getOrDefault("policy", Normal.EMPTY));
        if ("allowAndName".equals(policy) || "allow".equals(policy)) {
            Object path = downloadBehavior.get("downloadPath");
            if (!(path instanceof String downloadPath) || StringKit.isBlank(downloadPath)) {
                return Awaitable.failed(policy + " download policy requires downloadPath.");
            }
            if ("allowAndName".equals(policy)) {
                allowAndNameDownloadPaths.put(userContext, Path.of(downloadPath));
            } else {
                allowAndNameDownloadPaths.remove(userContext);
            }
            return session.send(
                    "browser.setDownloadBehavior",
                    Map.of(
                            "downloadBehavior",
                            Map.of("type", "allowed", "destinationFolder", downloadPath),
                            "userContexts",
                            List.of(userContext)))
                    .thenApply(result -> null);
        }
        if ("deny".equals(policy)) {
            return session
                    .send(
                            "browser.setDownloadBehavior",
                            Map.of("downloadBehavior", Map.of("type", "denied"), "userContexts", List.of(userContext)))
                    .thenApply(result -> null);
        }
        allowAndNameDownloadPaths.remove(userContext);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the download key.
     *
     * @param userContext user context value
     * @param guid        guid value
     * @return download key value
     */
    private static String downloadKey(String userContext, String guid) {
        return userContext + Symbol.C_COLON + guid;
    }

    /**
     * Converts a payload into extension info.
     *
     * @param payload payload
     * @return extension info value
     */
    private static CdpPayload extensionInfo(CdpPayload payload) {
        String id = PayloadReader
                .text(PayloadReader.first(CdpPayload.NULL, payload.get("id"), payload.get("extension")));
        String version = PayloadReader.text(payload.get("version"));
        String name = PayloadReader.text(payload.get("name"));
        String path = PayloadReader.text(payload.get("path"));
        boolean enabled = payload.get("enabled").isNull() || payload.get("enabled").asBoolean();
        return CdpPayload.of(Map.of("id", id, "version", version, "name", name, "path", path, "enabled", enabled));
    }

}
