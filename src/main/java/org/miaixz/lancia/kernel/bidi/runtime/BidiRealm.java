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
package org.miaixz.lancia.kernel.bidi.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Harness;
import org.miaixz.lancia.Realm;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.page.BidiElementHandle;
import org.miaixz.lancia.kernel.bidi.page.BidiFrame;
import org.miaixz.lancia.kernel.bidi.page.BidiJSHandle;
import org.miaixz.lancia.kernel.bidi.page.BidiPage;
import org.miaixz.lancia.kernel.bidi.protocol.BidiDeserializer;
import org.miaixz.lancia.kernel.bidi.protocol.BidiSerializer;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi execution realm.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiRealm implements Realm {

    /**
     * Creates a worker realm.
     *
     * @param id          identifier
     * @param page        page instance
     * @param environment worker environment
     * @return worker realm
     */
    public static BidiRealm worker(String id, BidiPage page, Object environment) {
        return BidiWorkerRealm.from(id, page, environment);
    }

    /**
     * Creates a frame realm.
     *
     * @param id          identifier
     * @param environment frame environment
     * @param sandbox     sandbox value
     * @return frame realm
     */
    public static BidiRealm frame(String id, BidiFrame environment, String sandbox) {
        return BidiFrameRealm.from(id, environment, sandbox);
    }

    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current page.
     */
    private final BidiPage page;
    /**
     * Current sandbox.
     */
    private final String sandbox;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Creates a bidi realm.
     *
     * @param id      identifier
     * @param page    page instance
     * @param sandbox sandbox
     */
    public BidiRealm(String id, BidiPage page, String sandbox) {
        this.id = Assert.notBlank(id, "id");
        this.page = Assert.notNull(page, "page");
        this.sandbox = StringKit.isBlank(sandbox) ? null : sandbox;
    }

    /**
     * Returns the owning harness.
     *
     * @return owning harness
     */
    public Harness environment() {
        return page.mainFrame();
    }

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    public Session client() {
        return Awaitable.await(page.createCDPSessionAsync(), "BiDi realm CDP session failed.", 5_000L);
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public CompletableFuture<Object> evaluate(String expression) {
        ensureActive();
        return session().send(
                "script.evaluate",
                Map.of(
                        "expression",
                        expression == null ? Normal.EMPTY : expression,
                        "awaitPromise",
                        true,
                        "target",
                        target(),
                        "resultOwnership",
                        "none"))
                .thenApply(result -> BidiDeserializer.deserialize(result.get("result")));
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public CompletableFuture<Object> evaluateHandle(String expression) {
        ensureActive();
        return session().send(
                "script.evaluate",
                Map.of(
                        "expression",
                        expression == null ? Normal.EMPTY : expression,
                        "awaitPromise",
                        true,
                        "target",
                        target(),
                        "resultOwnership",
                        "root"))
                .thenApply(result -> createHandle(result.get("result")));
    }

    /**
     * Returns the call function.
     *
     * @param functionDeclaration function declaration value
     * @param returnByValue       whether the result should be returned by value
     * @param arguments           arguments value
     * @return completion future
     */
    public CompletableFuture<Object> callFunction(
            String functionDeclaration,
            boolean returnByValue,
            Object... arguments) {
        ensureActive();
        List<Object> args = new ArrayList<>();
        if (arguments != null) {
            for (Object argument : arguments) {
                args.add(serialize(argument));
            }
        }
        return session().send(
                "script.callFunction",
                Map.of(
                        "functionDeclaration",
                        Assert.notBlank(functionDeclaration, "functionDeclaration"),
                        "awaitPromise",
                        true,
                        "target",
                        target(),
                        "resultOwnership",
                        returnByValue ? "none" : "root",
                        "arguments",
                        args))
                .thenApply(
                        result -> returnByValue ? BidiDeserializer.deserialize(result.get("result"))
                                : createHandle(result.get("result")));
    }

    /**
     * Creates handle.
     *
     * @param result evaluated result
     * @return created handle
     */
    public Object createHandle(CdpPayload result) {
        String type = PayloadReader.text(result.get("type"));
        if (("node".equals(type) || "window".equals(type)) && this instanceof BidiFrameRealm frameRealm) {
            return BidiElementHandle.from(result, frameRealm.environment().page());
        }
        return BidiJSHandle.from(result, page);
    }

    /**
     * Returns the serialize.
     *
     * @param argument argument value
     * @return serialize value
     */
    public Object serialize(Object argument) {
        if (argument instanceof BidiJSHandle handle) {
            if (handle.disposed()) {
                throw new InternalException("JSHandle has been disposed.");
            }
            return handle.optionalId().<Object>map(value -> Map.of("handle", value))
                    .orElseGet(() -> serialize(handle.jsonValue()));
        }
        if (argument instanceof BidiElementHandle handle) {
            if (handle.disposed()) {
                throw new InternalException("ElementHandle has been disposed.");
            }
            return Map.of("handle", handle.id());
        }
        return BidiSerializer.serialize(argument);
    }

    /**
     * Returns the destroy handles.
     *
     * @param handles handles value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> destroyHandles(List<BidiJSHandle> handles) {
        if (disposed.get() || handles == null || handles.isEmpty()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        List<String> ids = handles.stream().map(BidiJSHandle::optionalId).filter(Optional::isPresent)
                .map(Optional::getOrThrow).toList();
        if (ids.isEmpty()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return session().send("script.disown", Map.of("handles", ids, "target", target()));
    }

    /**
     * Returns the adopt handle.
     *
     * @param handle handle value
     * @return adopt handle value
     */
    public <T> T adoptHandle(T handle) {
        ensureActive();
        return Assert.notNull(handle, "handle");
    }

    /**
     * Returns the transfer handle.
     *
     * @param handle handle value
     * @return transfer handle value
     */
    public <T> T transferHandle(T handle) {
        ensureActive();
        return Assert.notNull(handle, "handle");
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        disposed.set(true);
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
     * Returns the page.
     *
     * @return page value
     */
    public BidiPage page() {
        return page;
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
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return disposed.get();
    }

    /**
     * Returns the target.
     *
     * @return mapped values
     */
    protected Map<String, Object> target() {
        return Map.of("realm", id);
    }

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    protected BidiSession session() {
        return page.browser().session();
    }

    /**
     * Handles ensure active.
     */
    private void ensureActive() {
        if (disposed.get()) {
            throw new InternalException("BiDi Realm has been disposed: " + id);
        }
    }

}

/**
 * WebDriver BiDi frame Realm.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
final class BidiFrameRealm extends BidiRealm {

    /**
     * Current environment.
     */
    private final BidiFrame environment;

    /**
     * Returns the from.
     *
     * @param id          identifier
     * @param environment environment value
     * @param sandbox     sandbox value
     * @return from value
     */
    static BidiFrameRealm from(String id, BidiFrame environment, String sandbox) {
        return new BidiFrameRealm(id, environment, sandbox);
    }

    /**
     * Creates an instance.
     *
     * @param id          identifier
     * @param environment environment value
     * @param sandbox     sandbox value
     */
    private BidiFrameRealm(String id, BidiFrame environment, String sandbox) {
        super(id, environment.page(), sandbox);
        this.environment = Assert.notNull(environment, "environment");
    }

    /**
     * Returns the environment.
     *
     * @return environment value
     */
    public BidiFrame environment() {
        return environment;
    }

    /**
     * Returns the adopt backend node.
     *
     * @param backendNodeId backend node ID value
     * @return completion future
     */
    CompletableFuture<BidiElementHandle> adoptBackendNode(int backendNodeId) {
        return environment.createCDPSessionAsync()
                .thenCompose(
                        session -> session.send(
                                "DOM.resolveNode",
                                Map.of("backendNodeId", backendNodeId, "executionContextId", id())))
                .thenApply(
                        result -> BidiElementHandle.from(
                                CdpPayload.of(
                                        Map.of(
                                                "type",
                                                "node",
                                                "handle",
                                                PayloadReader.text(result.get("object").get("objectId")))),
                                environment.page()));
    }
}

/**
 * WebDriver BiDi worker Realm.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
final class BidiWorkerRealm extends BidiRealm {

    /**
     * Current environment.
     */
    private final Object environment;

    /**
     * Returns the from.
     *
     * @param id          identifier
     * @param page        page instance
     * @param environment environment value
     * @return from value
     */
    static BidiWorkerRealm from(String id, BidiPage page, Object environment) {
        return new BidiWorkerRealm(id, page, environment);
    }

    /**
     * Creates an instance.
     *
     * @param id          identifier
     * @param page        page instance
     * @param environment environment value
     */
    private BidiWorkerRealm(String id, BidiPage page, Object environment) {
        super(id, page, null);
        this.environment = Assert.notNull(environment, "environment");
    }

    /**
     * Returns the environment.
     *
     * @return environment value
     */
    public Harness environment() {
        return environment instanceof Harness harness ? harness : page().mainFrame();
    }

    /**
     * Returns the adopt backend node.
     *
     * @return adopt backend node value
     */
    BidiElementHandle adoptBackendNode() {
        throw new InternalException("Worker Realm cannot adopt DOM nodes.");
    }

}
