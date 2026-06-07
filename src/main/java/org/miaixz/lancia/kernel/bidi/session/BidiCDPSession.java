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
package org.miaixz.lancia.kernel.bidi.session;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.Dispatcher;
import org.miaixz.lancia.kernel.Translator;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.page.BidiFrame;
import org.miaixz.lancia.kernel.bidi.runtime.BidiRealm;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;

/**
 * Represents a bidi CDP session.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiCDPSession implements Session {

    /**
     * Shared constant for sessions.
     */
    private static final Map<String, BidiCDPSession> SESSIONS = new ConcurrentHashMap<>();
    /**
     * Current session.
     */
    private final BidiSession session;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current events.
     */
    private final EventEmitter<String> events = new EventEmitter<>();
    /**
     * Current translator.
     */
    private final Translator translator;
    /**
     * Whether native CDP is enabled.
     */
    private final boolean nativeCdp;
    /**
     * Current realm.
     */
    private final BidiRealm realm;
    /**
     * Current frame.
     */
    private final BidiFrame frame;
    /**
     * Whether detached is enabled.
     */
    private volatile boolean detached;

    /**
     * Creates a bidi CDP session.
     *
     * @param session protocol session
     * @param id      identifier
     */
    public BidiCDPSession(BidiSession session, String id) {
        this(session, id, null, null, null, true);
    }

    /**
     * Creates a bidi CDP session.
     *
     * @param session protocol session
     * @param id      identifier
     */
    public BidiCDPSession(BidiProtocolSession session, String id) {
        this(BidiSession.wrap(session), id);
    }

    /**
     * Creates a bidi CDP session.
     *
     * @param session    protocol session
     * @param id         identifier
     * @param translator translator
     * @param realm      realm
     * @param frame      frame instance
     * @param nativeCdp  native cdp
     */
    private BidiCDPSession(BidiSession session, String id, Translator translator, BidiRealm realm, BidiFrame frame,
            boolean nativeCdp) {
        this.session = Assert.notNull(session, "session");
        this.id = Assert.notBlank(id, "id");
        this.translator = translator;
        this.realm = realm;
        this.frame = frame;
        this.nativeCdp = nativeCdp;
        SESSIONS.put(this.id, this);
        Dispatcher.register(this.id, this::emit);
        Logger.debug(false, "Protocol", "BiDi CDP session initialized: sessionId={}, native={}", this.id, nativeCdp);
    }

    /**
     * Creates this value from context.
     *
     * @param session protocol session
     * @param context browser context
     * @return completion future
     */
    public static CompletableFuture<BidiCDPSession> fromContext(BidiSession session, String context) {
        BidiSession actualSession = Assert.notNull(session, "session");
        String actualContext = Assert.notBlank(context, "context");
        Logger.debug(true, "Protocol", "BiDi CDP session lookup requested: context={}", actualContext);
        CompletableFuture<BidiCDPSession> result = new CompletableFuture<>();
        actualSession.send("goog:cdp.getSession", Map.of("context", actualContext))
                .whenComplete((payload, throwable) -> {
                    if (throwable == null) {
                        Logger.debug(false, "Protocol", "BiDi native CDP session resolved: context={}", actualContext);
                        result.complete(
                                new BidiCDPSession(actualSession, payload.get("result").get("session").asText()));
                        return;
                    }
                    Logger.warn(
                            false,
                            "Protocol",
                            "BiDi native CDP session unavailable, using compatible translator: context={}, message={}",
                            actualContext,
                            throwable.getMessage());
                    result.complete(compatible(actualSession, actualContext));
                });
        return result;
    }

    /**
     * Creates this value from context.
     *
     * @param session protocol session
     * @param context browser context
     * @return completion future
     */
    public static CompletableFuture<BidiCDPSession> fromContext(BidiProtocolSession session, String context) {
        return fromContext(BidiSession.wrap(session), context);
    }

    /**
     * Returns the compatible.
     *
     * @param session protocol session
     * @param context browser context
     * @return compatible value
     */
    public static BidiCDPSession compatible(BidiSession session, String context) {
        String actualContext = Assert.notBlank(context, "context");
        Logger.debug(true, "Protocol", "BiDi compatible CDP session requested: context={}", actualContext);
        return new BidiCDPSession(session, "bidi-context-" + actualContext,
                new Translator(session, actualContext, null), null, null, false);
    }

    /**
     * Creates this value from realm.
     *
     * @param session protocol session
     * @param realm   realm value
     * @return from realm value
     */
    public static BidiCDPSession fromRealm(BidiSession session, BidiRealm realm) {
        BidiRealm actualRealm = Assert.notNull(realm, "realm");
        Logger.debug(true, "Protocol", "BiDi realm CDP session requested: realmId={}", actualRealm.id());
        return new BidiCDPSession(session, "bidi-realm-" + actualRealm.id(),
                new Translator(session, null, actualRealm.id()), actualRealm, null, false);
    }

    /**
     * Creates this value from frame.
     *
     * @param session protocol session
     * @param frame   frame instance
     * @return from frame value
     */
    public static BidiCDPSession fromFrame(BidiSession session, BidiFrame frame) {
        BidiFrame actualFrame = Assert.notNull(frame, "frame");
        Logger.debug(true, "Protocol", "BiDi frame CDP session requested: frameId={}", actualFrame.id());
        return new BidiCDPSession(session, "bidi-frame-" + actualFrame.id(),
                new Translator(session, actualFrame.id(), null), null, actualFrame, false);
    }

    /**
     * Returns the find.
     *
     * @param id identifier
     * @return optional value
     */
    public static Optional<BidiCDPSession> find(String id) {
        return Optional.ofNullable(SESSIONS.get(id));
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        if (detached) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new InternalException("BiDi CDP session has been closed."));
            Logger.warn(
                    true,
                    "Protocol",
                    "BiDi CDP command rejected because session is detached: sessionId={}, method={}",
                    id,
                    method);
            return rejected;
        }
        if (!nativeCdp) {
            Logger.debug(
                    true,
                    "Protocol",
                    "BiDi translated CDP command requested: sessionId={}, method={}",
                    id,
                    method);
            return translator.translate(method, params, realm, frame);
        }
        Logger.debug(true, "Protocol", "BiDi native CDP command requested: sessionId={}, method={}", id, method);
        return session.send(
                "goog:cdp.sendCommand",
                Map.of(
                        "method",
                        Assert.notBlank(method, "method"),
                        "params",
                        params == null ? Map.of() : params,
                        "session",
                        id))
                .thenCompose(result -> {
                    Logger.debug(
                            false,
                            "Protocol",
                            "BiDi native CDP command completed: sessionId={}, method={}",
                            id,
                            method);
                    return CompletableFuture.completedFuture(result.get("result"));
                }).exceptionallyCompose(throwable -> {
                    if (translator == null) {
                        Logger.error(
                                false,
                                "Protocol",
                                "BiDi native CDP command failed: sessionId={}, method={}, message={}",
                                id,
                                method,
                                throwable.getMessage());
                        return Awaitable.failed(throwable);
                    }
                    Logger.warn(
                            false,
                            "Protocol",
                            "BiDi native CDP command failed, translating fallback: sessionId={}, method={}, message={}",
                            id,
                            method,
                            throwable.getMessage());
                    return translator.translate(method, params, realm, frame);
                });
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @return command result
     */
    public CompletableFuture<CdpPayload> send(String method) {
        return send(method, Map.of());
    }

    /**
     * Detaches this object from its protocol target.
     *
     * @return detach value
     */
    public CompletableFuture<Void> detach() {
        if (detached) {
            Logger.debug(false, "Protocol", "BiDi CDP session detach skipped: sessionId={}", id);
            return CompletableFuture.completedFuture(null);
        }
        if (!nativeCdp) {
            Logger.debug(true, "Protocol", "BiDi translated CDP session detach requested: sessionId={}", id);
            onClose();
            return CompletableFuture.completedFuture(null);
        }
        Logger.debug(true, "Protocol", "BiDi native CDP session detach requested: sessionId={}", id);
        return send("Target.detachFromTarget", Map.of("sessionId", id)).handle((value, throwable) -> {
            onClose();
            if (throwable != null) {
                Logger.error(
                        false,
                        "Protocol",
                        "BiDi CDP session detach failed: sessionId={}, message={}",
                        id,
                        throwable.getMessage());
                throw new InternalException("Failed to detach BiDi CDP session.", throwable);
            }
            Logger.debug(false, "Protocol", "BiDi CDP session detached: sessionId={}", id);
            return null;
        });
    }

    /**
     * Registers an event listener.
     *
     * @param method   protocol method
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String method, Consumer<CdpPayload> listener) {
        Assert.notBlank(method, "method");
        Assert.notNull(listener, "listener");
        Consumer<Object> bridge = payload -> listener.accept((CdpPayload) payload);
        events.on(method, bridge);
        Logger.debug(true, "Protocol", "BiDi CDP session listener added: sessionId={}, method={}", id, method);
        return new EventBinding(() -> events.off(method, bridge));
    }

    /**
     * Emits an event to registered listeners.
     *
     * @param method protocol method
     * @param params protocol parameters
     */
    public void emit(String method, CdpPayload params) {
        events.emit(Assert.notBlank(method, "method"), params == null ? CdpPayload.NULL : params);
        Logger.debug(false, "Protocol", "BiDi CDP session event emitted: sessionId={}, method={}", id, method);
    }

    /**
     * Handles on close.
     */
    public void onClose() {
        detached = true;
        SESSIONS.remove(id);
        Dispatcher.unregister(id);
        Logger.debug(false, "Protocol", "BiDi CDP session closed: sessionId={}", id);
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
     * Returns the protocol session.
     *
     * @return protocol session
     */
    public BidiSession session() {
        return session;
    }

    /**
     * Returns the detached.
     *
     * @return {@code true} when the condition matches
     */
    public boolean detached() {
        return detached;
    }

}
