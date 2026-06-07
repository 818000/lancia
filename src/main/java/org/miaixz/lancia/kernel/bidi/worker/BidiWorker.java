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
package org.miaixz.lancia.kernel.bidi.worker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Worker;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.kernel.bidi.page.BidiFrame;
import org.miaixz.lancia.kernel.bidi.runtime.BidiRealm;
import org.miaixz.lancia.kernel.bidi.session.BidiCDPSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi worker.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiWorker extends Worker {

    /**
     * Shared constant for console.
     */
    public static final String CONSOLE = "console";

    /**
     * Shared constant for error.
     */
    public static final String ERROR = "error";
    /**
     * Current frame.
     */
    private final BidiFrame frame;
    /**
     * Current URL.
     */
    private final String url;
    /**
     * Current realm.
     */
    private final BidiRealm realm;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Returns the from.
     *
     * @param frame frame instance
     * @param realm realm value
     * @return from value
     */
    public static BidiWorker from(BidiFrame frame, CdpPayload realm) {
        return from(frame, PayloadReader.text(realm.get("realm")), PayloadReader.text(realm.get("origin")));
    }

    /**
     * Returns the from.
     *
     * @param frame   frame instance
     * @param realmId realm ID value
     * @param url     target URL
     * @return from value
     */
    public static BidiWorker from(BidiFrame frame, String realmId, String url) {
        BidiWorker worker = new BidiWorker(frame, realmId, url);
        frame.page().addWorker(worker);
        return worker;
    }

    /**
     * Creates a bidi worker.
     *
     * @param frame   frame instance
     * @param realmId realm id
     * @param url     target URL
     */
    private BidiWorker(BidiFrame frame, String realmId, String url) {
        this.frame = Assert.notNull(frame, "frame");
        this.url = url == null ? Normal.EMPTY : url;
        this.realm = BidiRealm.worker(Assert.notBlank(realmId, "realmId"), frame.page(), this);
        Logger.debug(
                false,
                "Page",
                "BiDi worker initialized: frame={}, realm={}, url={}",
                frame.id(),
                realmId,
                this.url.replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Returns the frame.
     *
     * @return frame value
     */
    public BidiFrame frame() {
        return frame;
    }

    /**
     * Returns the main realm.
     *
     * @return main realm value
     */
    public BidiRealm mainRealm() {
        return realm;
    }

    /**
     * Returns the worker realm.
     *
     * @return worker realm value
     */
    BidiRealm workerRealm() {
        return realm;
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return url;
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    public BidiCDPSession client() {
        assertActive();
        Logger.debug(
                true,
                "Protocol",
                "BiDi worker CDP session requested: url={}",
                url.replaceAll("[?#].*$", "?<redacted>"));
        return BidiCDPSession.fromRealm(frame.page().browser().session(), realm);
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        Logger.debug(false, "Page", "BiDi worker listener added: event={}", event);
        emitter.on(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        return new EventBinding(() -> emitter.off(event, listener));
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String event, Consumer<Object> listener) {
        emitter.once(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        return new EventBinding(() -> emitter.off(event, listener));
    }

    /**
     * Removes an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding off(String event, Consumer<Object> listener) {
        emitter.off(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        return new EventBinding();
    }

    /**
     * Removes all event listeners for one event.
     *
     * @param event event name
     * @return listener binding
     */
    public Binding off(String event) {
        emitter.off(Assert.notBlank(event, "event"));
        return new EventBinding();
    }

    /**
     * Emits an event to registered listeners.
     *
     * @param event   event name
     * @param payload protocol payload
     * @return {@code true} when at least one listener received the event
     */
    public boolean emit(String event, Object payload) {
        Logger.debug(false, "Page", "BiDi worker event emitted: event={}, listeners={}", event, listenerCount(event));
        return emitter.emit(Assert.notBlank(event, "event"), payload);
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
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public Object evaluate(String expression) {
        return Awaitable.await(evaluateAsync(expression), "BiDi worker evaluate failed.", 5_000L);
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public CompletableFuture<Object> evaluateAsync(String expression) {
        assertActive();
        Logger.debug(
                true,
                "Page",
                "BiDi worker evaluate requested: chars={}",
                expression == null ? Normal._0 : expression.length());
        return realm.evaluate(expression);
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public Handle evaluateHandle(String expression) {
        Object value = Awaitable.await(evaluateHandleAsync(expression), "BiDi worker evaluate handle failed.", 5_000L);
        return value instanceof Handle handle ? handle : null;
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public CompletableFuture<Object> evaluateHandleAsync(String expression) {
        assertActive();
        Logger.debug(
                true,
                "Page",
                "BiDi worker evaluate handle requested: chars={}",
                expression == null ? Normal._0 : expression.length());
        return realm.evaluateHandle(expression);
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
     * Returns timeout settings.
     *
     * @return timeout settings
     */
    public Object timeoutSettings() {
        return frame.page().getDefaultTimeout();
    }

    /**
     * Removes all listeners for one event.
     *
     * @param event event name
     * @return listener binding
     */
    public Binding removeAllListeners(String event) {
        emitter.removeAllListeners(Assert.notBlank(event, "event"));
        return new EventBinding();
    }

    /**
     * Removes all listeners.
     *
     * @return listener binding
     */
    public Binding removeAllListeners() {
        emitter.removeAllListeners();
        return new EventBinding();
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            Logger.debug(
                    true,
                    "Page",
                    "BiDi worker dispose requested: url={}",
                    url.replaceAll("[?#].*$", "?<redacted>"));
            realm.dispose();
            frame.page().removeWorker(this);
            Logger.debug(false, "Page", "BiDi worker disposed.");
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
     * Returns the close async.
     *
     * @return completion future
     */
    public CompletableFuture<Void> closeAsync() {
        if (disposed.get()) {
            return CompletableFuture.completedFuture(null);
        }
        Logger.debug(true, "Page", "BiDi worker close requested: url={}", url.replaceAll("[?#].*$", "?<redacted>"));
        return realm.evaluate("self.close()").handle((result, throwable) -> {
            if (throwable != null) {
                Logger.error(
                        false,
                        "Page",
                        throwable,
                        "BiDi worker close failed: url={}",
                        url.replaceAll("[?#].*$", "?<redacted>"));
                throw new InternalException("BiDi Worker close failed.", throwable);
            }
            dispose();
            Logger.debug(false, "Page", "BiDi worker closed.");
            return null;
        });
    }

    /**
     * Asserts the active condition.
     */
    private void assertActive() {
        if (disposed.get()) {
            throw new InternalException("BiDi Worker has been disposed: " + url);
        }
    }

}
