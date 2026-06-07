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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
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
 * WebDriver BiDi Core Realm.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public abstract class BidiRealm implements AutoCloseable {

    /**
     * Shared constant for updated.
     */
    public static final String UPDATED = "updated";

    /**
     * Shared constant for destroyed.
     */
    public static final String DESTROYED = "destroyed";

    /**
     * Shared constant for worker.
     */
    public static final String WORKER = "worker";

    /**
     * Shared constant for shared worker.
     */
    public static final String SHARED_WORKER = "sharedworker";

    /**
     * Shared constant for log.
     */
    public static final String LOG = "log";
    /**
     * Current identifier.
     */
    private String id;
    /**
     * Current origin.
     */
    private String origin;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Current reason.
     */
    private volatile String reason;
    /**
     * Current execution context ID.
     */
    private Integer executionContextId;

    /**
     * Creates a BiDi realm.
     *
     * @param id     identifier
     * @param origin origin
     */
    protected BidiRealm(String id, String origin) {
        this.id = id == null ? Normal.EMPTY : id;
        this.origin = origin == null ? Normal.EMPTY : origin;
    }

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    protected abstract BidiProtocolSession session();

    /**
     * Returns the target.
     *
     * @return mapped values
     */
    public Map<String, Object> target() {
        return Map.of("realm", id);
    }

    /**
     * Returns the disown.
     *
     * @param handles handles value
     * @return completion future
     */
    public CompletableFuture<Void> disown(List<String> handles) {
        if (disposed()) {
            return Awaitable.failed(reason());
        }
        return session()
                .send("script.disown", Map.of("target", target(), "handles", handles == null ? List.of() : handles))
                .thenApply(result -> null);
    }

    /**
     * Returns the call function.
     *
     * @param functionDeclaration function declaration value
     * @param awaitPromise        await promise value
     * @param options             operation options
     * @return completion future
     */
    public CompletableFuture<CdpPayload> callFunction(
            String functionDeclaration,
            boolean awaitPromise,
            Map<String, Object> options) {
        if (disposed()) {
            return Awaitable.failed(reason());
        }
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("functionDeclaration", Assert.notBlank(functionDeclaration, "functionDeclaration"));
        params.put("awaitPromise", awaitPromise);
        params.put("target", target());
        return session().send("script.callFunction", params).thenApply(result -> result.get("result"));
    }

    /**
     * Returns the evaluate.
     *
     * @param expression   JavaScript expression
     * @param awaitPromise await promise value
     * @param options      operation options
     * @return completion future
     */
    public CompletableFuture<CdpPayload> evaluate(
            String expression,
            boolean awaitPromise,
            Map<String, Object> options) {
        if (disposed()) {
            return Awaitable.failed(reason());
        }
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("expression", expression == null ? Normal.EMPTY : expression);
        params.put("awaitPromise", awaitPromise);
        params.put("target", target());
        return session().send("script.evaluate", params).thenApply(result -> result.get("result"));
    }

    /**
     * Resolves execution context ID.
     *
     * @return resolve execution context ID value
     */
    public CompletableFuture<Integer> resolveExecutionContextId() {
        if (disposed()) {
            return Awaitable.failed(reason());
        }
        if (executionContextId != null) {
            return CompletableFuture.completedFuture(executionContextId);
        }
        return session().connection().send("goog:cdp.resolveRealm", Map.of("realm", id)).thenApply(result -> {
            executionContextId = result.get("result").get("executionContextId").asInt();
            return executionContextId;
        });
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
     * Releases resources held by this object.
     *
     * @param reason reason
     */
    public void dispose(String reason) {
        if (this.reason != null) {
            return;
        }
        this.reason = StringKit.isBlank(reason)
                ? "Realm already destroyed, probably because all associated browsing contexts closed."
                : reason;
        binding.unbind();
        binding = new EventBinding();
        emitter.emit(DESTROYED, this.reason);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        dispose(null);
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
    }

    /**
     * Returns the origin.
     *
     * @return origin value
     */
    public String origin() {
        return origin;
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
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return reason != null;
    }

    /**
     * Handles update.
     *
     * @param id     identifier
     * @param origin origin value
     */
    protected void update(String id, String origin) {
        this.id = id == null ? Normal.EMPTY : id;
        this.origin = origin == null ? Normal.EMPTY : origin;
        this.executionContextId = null;
        emitter.emit(UPDATED, this);
    }

    /**
     * Emits an event to registered listeners.
     *
     * @param event   event name
     * @param payload protocol payload
     */
    protected void emit(String event, Object payload) {
        emitter.emit(event, payload);
    }

    /**
     * Adds binding.
     *
     * @param binding binding
     */
    protected void addBinding(Binding binding) {
        this.binding = this.binding.combine(Assert.notNull(binding, "binding"));
    }

    /**
     * Handles bind worker realm events.
     *
     * @param workers workers value
     */
    protected final void bindWorkerRealmEvents(Map<String, DedicatedWorkerRealm> workers) {
        addBinding(session().connection().on("script.realmDestroyed", this::disposeWhenRealmDestroyed));
        addBinding(session().connection().on("script.realmCreated", info -> emitDedicatedWorker(info, workers)));
        addBinding(session().connection().on("log.entryAdded", this::emitRealmLog));
    }

    /**
     * Handles dispose when realm destroyed.
     *
     * @param info info value
     */
    protected final void disposeWhenRealmDestroyed(CdpPayload info) {
        if (id().equals(PayloadReader.text(info.get("realm")))) {
            dispose("Realm already destroyed.");
        }
    }

    /**
     * Returns the emit dedicated worker.
     *
     * @param info    info value
     * @param workers workers value
     * @return {@code true} when the condition matches
     */
    protected final boolean emitDedicatedWorker(CdpPayload info, Map<String, DedicatedWorkerRealm> workers) {
        if (!"dedicated-worker".equals(PayloadReader.text(info.get("type")))
                || !containsText(info.get("owners"), id())) {
            return false;
        }
        DedicatedWorkerRealm worker = DedicatedWorkerRealm
                .from(this, PayloadReader.text(info.get("realm")), PayloadReader.text(info.get("origin")));
        workers.put(worker.id(), worker);
        worker.once(DESTROYED, value -> workers.remove(worker.id()));
        emit(WORKER, worker);
        return true;
    }

    /**
     * Handles emit realm log.
     *
     * @param entry entry value
     */
    protected final void emitRealmLog(CdpPayload entry) {
        if (id().equals(PayloadReader.text(entry.get("source").get("realm")))) {
            emit(LOG, entry);
        }
    }

    /**
     * Returns the contains text.
     *
     * @param payload protocol payload
     * @param value   value to use
     * @return {@code true} when the condition matches
     */
    private static boolean containsText(CdpPayload payload, String value) {
        for (CdpPayload item : PayloadReader.elements(payload, CdpPayload.class)) {
            if (value.equals(PayloadReader.text(item))) {
                return true;
            }
        }
        return false;
    }

    /**
     * WebDriver BiDi Core window Realm.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class WindowRealm extends BidiRealm {

        /**
         * Current browsing context.
         */
        private final BidiBrowsingContext browsingContext;
        /**
         * Current sandbox.
         */
        private final String sandbox;
        /**
         * Mapped workers values.
         */
        private final Map<String, DedicatedWorkerRealm> workers = new ConcurrentHashMap<>();

        /**
         * Returns the from.
         *
         * @param context browser context
         * @param sandbox sandbox value
         * @return from value
         */
        public static WindowRealm from(BidiBrowsingContext context, String sandbox) {
            WindowRealm realm = new WindowRealm(context, sandbox);
            realm.initialize();
            return realm;
        }

        /**
         * Creates an instance.
         *
         * @param context browser context
         * @param sandbox sandbox value
         */
        private WindowRealm(BidiBrowsingContext context, String sandbox) {
            super(Normal.EMPTY, Normal.EMPTY);
            this.browsingContext = Assert.notNull(context, "context");
            this.sandbox = StringKit.isBlank(sandbox) ? null : sandbox;
        }

        /**
         * Handles initialize.
         */
        private void initialize() {
            addBinding(browsingContext.on(BidiBrowsingContext.CLOSED, value -> dispose(String.valueOf(value))));
            addBinding(session().connection().on("script.realmCreated", this::onRealmCreated));
            addBinding(session().connection().on("log.entryAdded", this::onLogEntryAdded));
        }

        /**
         * Returns the target.
         *
         * @return mapped values
         */
        @Override
        public Map<String, Object> target() {
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("context", browsingContext.id());
            if (sandbox != null) {
                target.put("sandbox", sandbox);
            }
            return target;
        }

        /**
         * Returns the protocol session.
         *
         * @return protocol session
         */
        @Override
        protected BidiProtocolSession session() {
            return browsingContext.browser().session();
        }

        /**
         * Returns the browsing context.
         *
         * @return browsing context value
         */
        public BidiBrowsingContext browsingContext() {
            return browsingContext;
        }

        /**
         * Returns the sandbox.
         *
         * @return optional value
         */
        public Optional<String> sandbox() {
            return Optional.ofNullable(sandbox);
        }

        /**
         * Returns the workers.
         *
         * @return mapped values
         */
        public Map<String, DedicatedWorkerRealm> workers() {
            return Map.copyOf(workers);
        }

        /**
         * Handles on realm created.
         *
         * @param info info value
         */
        private void onRealmCreated(CdpPayload info) {
            String type = PayloadReader.text(info.get("type"));
            if ("window".equals(type) && browsingContext.id().equals(PayloadReader.text(info.get("context")))
                    && Objects.equals(sandbox, PayloadReader.nullableText(info.get("sandbox")))) {
                update(PayloadReader.text(info.get("realm")), PayloadReader.text(info.get("origin")));
                return;
            }
            emitDedicatedWorker(info, workers);
        }

        /**
         * Handles on log entry added.
         *
         * @param entry entry value
         */
        private void onLogEntryAdded(CdpPayload entry) {
            emitRealmLog(entry);
        }
    }

    /**
     * WebDriver BiDi Core dedicated worker Realm.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class DedicatedWorkerRealm extends BidiRealm {

        /**
         * Registered owners values.
         */
        private final Set<BidiRealm> owners = new LinkedHashSet<>();
        /**
         * Mapped workers values.
         */
        private final Map<String, DedicatedWorkerRealm> workers = new ConcurrentHashMap<>();

        /**
         * Returns the from.
         *
         * @param owner  owner value
         * @param id     identifier
         * @param origin origin value
         * @return from value
         */
        public static DedicatedWorkerRealm from(BidiRealm owner, String id, String origin) {
            DedicatedWorkerRealm realm = new DedicatedWorkerRealm(owner, id, origin);
            realm.initialize();
            return realm;
        }

        /**
         * Creates an instance.
         *
         * @param owner  owner value
         * @param id     identifier
         * @param origin origin value
         */
        private DedicatedWorkerRealm(BidiRealm owner, String id, String origin) {
            super(Assert.notBlank(id, "id"), origin);
            owners.add(Assert.notNull(owner, "owner"));
        }

        /**
         * Handles initialize.
         */
        private void initialize() {
            bindWorkerRealmEvents(workers);
        }

        /**
         * Returns the protocol session.
         *
         * @return protocol session
         */
        @Override
        protected BidiProtocolSession session() {
            return owners.iterator().next().session();
        }

        /**
         * Returns the owners.
         *
         * @return values
         */
        public Set<BidiRealm> owners() {
            return Set.copyOf(owners);
        }

        /**
         * Returns the workers.
         *
         * @return mapped values
         */
        public Map<String, DedicatedWorkerRealm> workers() {
            return Map.copyOf(workers);
        }

    }

    /**
     * WebDriver BiDi Core shared worker Realm.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class SharedWorkerRealm extends BidiRealm {

        /**
         * Current browser.
         */
        private final BidiBrowser browser;
        /**
         * Mapped workers values.
         */
        private final Map<String, DedicatedWorkerRealm> workers = new ConcurrentHashMap<>();

        /**
         * Returns the from.
         *
         * @param browser browser instance
         * @param id      identifier
         * @param origin  origin value
         * @return from value
         */
        public static SharedWorkerRealm from(BidiBrowser browser, String id, String origin) {
            SharedWorkerRealm realm = new SharedWorkerRealm(browser, id, origin);
            realm.initialize();
            return realm;
        }

        /**
         * Creates an instance.
         *
         * @param browser browser instance
         * @param id      identifier
         * @param origin  origin value
         */
        private SharedWorkerRealm(BidiBrowser browser, String id, String origin) {
            super(Assert.notBlank(id, "id"), origin);
            this.browser = Assert.notNull(browser, "browser");
        }

        /**
         * Handles initialize.
         */
        private void initialize() {
            bindWorkerRealmEvents(workers);
        }

        /**
         * Returns the protocol session.
         *
         * @return protocol session
         */
        @Override
        protected BidiProtocolSession session() {
            return browser.session();
        }

        /**
         * Returns the browser.
         *
         * @return browser value
         */
        public BidiBrowser browser() {
            return browser;
        }

        /**
         * Returns the workers.
         *
         * @return mapped values
         */
        public Map<String, DedicatedWorkerRealm> workers() {
            return Map.copyOf(workers);
        }

    }

}
